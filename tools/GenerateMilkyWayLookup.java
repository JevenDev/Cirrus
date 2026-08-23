import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;

/**
 * Generates the static Milky Way lookup texture used by the runtime shader.
 *
 * <p>AI-use disclosure: AI was used to port Cirrus's
 * original GLSL equations into this offline Java generator. No generative-image
 * model or third-party artwork is used; the texture is produced deterministically
 * from the shader math.
 *
 * <p>The generated RGBA texture is a finite-resolution approximation of the
 * original procedural shader. Runtime rotation, pixelation, opacity, weather
 * fading, and sky-gradient compositing remain shader-controlled.
 *
 * <p>Precomputing the expensive noise into a lookup texture substantially improves
 * runtime rendering performance with negligible visual difference.
 *
 * <pre>{@code
 * java tools/GenerateMilkyWayLookup.java
 * }</pre>
 */
public final class GenerateMilkyWayLookup {
    private static final int DEFAULT_WIDTH = 2048;
    private static final int DEFAULT_HEIGHT = 1024;
    private static final float PI = (float)Math.PI;
    private static final float TWO_PI = PI * 2.0F;
    private static final float LUMINOSITY_RANGE = 1.32F;
    private static final Path DEFAULT_OUTPUT = Path.of(
            "common", "src", "main", "resources", "assets", "cirrus",
            "textures", "environment", "milky_way_lookup.png"
    );

    private static final float[] GALACTIC_NORMAL = normalize(0.31F, 0.84F, 0.44F);
    private static final float[] CORE_DIRECTION = coreDirection();

    private GenerateMilkyWayLookup() {
    }

    public static void main(String[] arguments) throws IOException {
        Path output = arguments.length > 0 ? Path.of(arguments[0]) : DEFAULT_OUTPUT;
        int width = arguments.length > 1 ? Integer.parseInt(arguments[1]) : DEFAULT_WIDTH;
        int height = arguments.length > 2 ? Integer.parseInt(arguments[2]) : DEFAULT_HEIGHT;
        if (width < 2 || height < 2) {
            throw new IllegalArgumentException("texture dimensions must be at least 2x2");
        }

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
        IntStream.range(0, height).parallel().forEach(y -> generateRow(pixels, width, height, y));

        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (!ImageIO.write(image, "png", output.toFile())) {
            throw new IOException("No PNG writer is available");
        }
        System.out.printf("Generated %dx%d Milky Way lookup at %s%n", width, height, output);
    }

    private static void generateRow(int[] pixels, int width, int height, int y) {
        float latitude = (((y + 0.5F) / height) - 0.5F) * PI;
        float horizontal = (float)Math.cos(latitude);
        float directionY = (float)Math.sin(latitude);
        int rowOffset = y * width;

        for (int x = 0; x < width; x++) {
            float longitude = (((x + 0.5F) / width) - 0.5F) * TWO_PI;
            float directionX = horizontal * (float)Math.cos(longitude);
            float directionZ = horizontal * (float)Math.sin(longitude);
            pixels[rowOffset + x] = sample(directionX, directionY, directionZ);
        }
    }

    private static int sample(float directionX, float directionY, float directionZ) {
        float coreFacing = Math.max(
                directionX * CORE_DIRECTION[0]
                        + directionY * CORE_DIRECTION[1]
                        + directionZ * CORE_DIRECTION[2],
                0.0F
        );
        float coreBulge = (float)Math.pow(coreFacing, 3.2);

        float warpAlong = fbm(
                directionX * 2.15F + 1.7F,
                directionY * 2.15F + 9.2F,
                directionZ * 2.15F + 4.3F
        );
        float warpAcross = fbm(
                directionX * 3.65F + 13.1F,
                directionY * 3.65F + 2.7F,
                directionZ * 3.65F + 8.4F
        );
        float warpX = warpAlong - 0.47F;
        float warpY = warpAcross - 0.47F;
        float warpZ = warpAlong - warpAcross;

        float latitude = directionX * GALACTIC_NORMAL[0]
                + directionY * GALACTIC_NORMAL[1]
                + directionZ * GALACTIC_NORMAL[2];
        float bentLatitude = latitude + warpX * 0.16F + warpY * 0.075F;
        float bandDistance = Math.abs(bentLatitude);
        float halfWidth = mix(0.145F, 0.34F, coreBulge);

        float outerEnvelope = gaussian(bandDistance / (halfWidth * 1.72F));
        float broadBand = gaussian(bandDistance / halfWidth);
        float innerBand = gaussian(bandDistance / (halfWidth * 0.48F));

        float cloudField = fbm(
                directionX * 5.4F + warpX * 3.4F + 11.0F,
                directionY * 5.4F + warpY * 3.4F + 2.0F,
                directionZ * 5.4F + warpZ * 3.4F + 17.0F
        );
        float fineField = fbm(
                directionX * 11.5F + warpX * 5.2F + 3.0F,
                directionY * 11.5F + warpY * 5.2F + 19.0F,
                directionZ * 11.5F + warpZ * 5.2F + 7.0F
        );
        float dustField = fbm(
                directionX * 8.2F + warpX * 4.6F + 23.0F,
                directionY * 8.2F + warpY * 4.6F + 5.0F,
                directionZ * 8.2F + warpZ * 4.6F + 13.0F
        );

        float billows = smoothstep(0.30F, 0.69F, cloudField + (fineField - 0.47F) * 0.30F);
        float filaments = smoothstep(0.47F, 0.76F, fineField + cloudField * 0.16F);
        float luminousKnots = smoothstep(0.69F, 0.86F, fineField + cloudField * 0.22F);

        float luminosity = outerEnvelope * (0.030F + billows * 0.075F)
                + broadBand * (0.075F + billows * 0.235F)
                + innerBand * (0.045F + filaments * 0.16F);
        luminosity += coreBulge
                * broadBand
                * (0.16F + billows * 0.34F + luminousKnots * 0.20F);

        float dustClouds = smoothstep(
                0.51F,
                0.76F,
                dustField + (1.0F - billows) * 0.10F
        ) * innerBand;
        luminosity *= 1.0F - dustClouds * mix(0.36F, 0.70F, coreBulge);

        float warmth = coreBulge * (0.58F + billows * 0.42F);
        float colorMix = clamp(broadBand * 0.38F + billows * 0.50F + filaments * 0.16F);
        float red = mix(0.28F, 0.66F, colorMix);
        float green = mix(0.36F, 0.51F, colorMix);
        float blue = mix(0.76F, 0.80F, colorMix);

        red = mix(red, 0.95F, warmth * 0.58F);
        green = mix(green, 0.42F, warmth * 0.58F);
        blue = mix(blue, 0.54F, warmth * 0.58F);

        float coreColorMix = warmth * (0.12F + luminousKnots * 0.30F);
        red = mix(red, 1.0F, coreColorMix);
        green = mix(green, 0.72F, coreColorMix);
        blue = mix(blue, 0.36F, coreColorMix);

        float knotBrightness = 0.93F + luminousKnots * 0.15F;
        red *= knotBrightness;
        green *= knotBrightness;
        blue *= knotBrightness;

        int encodedLuminosity = channel((float)Math.sqrt(clamp(luminosity / LUMINOSITY_RANGE)));
        return encodedLuminosity << 24
                | channel(red) << 16
                | channel(green) << 8
                | channel(blue);
    }

    private static float fbm(float x, float y, float z) {
        float sum = 0.0F;
        float amplitude = 0.5F;
        for (int octave = 0; octave < 4; octave++) {
            sum += valueNoise(x, y, z) * amplitude;
            x = x * 2.03F + 7.1F;
            y = y * 2.03F + 3.7F;
            z = z * 2.03F + 5.9F;
            amplitude *= 0.5F;
        }
        return sum;
    }

    private static float valueNoise(float x, float y, float z) {
        float cellX = (float)Math.floor(x);
        float cellY = (float)Math.floor(y);
        float cellZ = (float)Math.floor(z);
        float localX = smoothInterpolation(fract(x));
        float localY = smoothInterpolation(fract(y));
        float localZ = smoothInterpolation(fract(z));

        float x00 = mix(hash(cellX, cellY, cellZ), hash(cellX + 1.0F, cellY, cellZ), localX);
        float x10 = mix(hash(cellX, cellY + 1.0F, cellZ), hash(cellX + 1.0F, cellY + 1.0F, cellZ), localX);
        float x01 = mix(hash(cellX, cellY, cellZ + 1.0F), hash(cellX + 1.0F, cellY, cellZ + 1.0F), localX);
        float x11 = mix(hash(cellX, cellY + 1.0F, cellZ + 1.0F), hash(cellX + 1.0F, cellY + 1.0F, cellZ + 1.0F), localX);
        return mix(mix(x00, x10, localY), mix(x01, x11, localY), localZ);
    }

    private static float hash(float x, float y, float z) {
        x = fract(x * 0.1031F);
        y = fract(y * 0.1031F);
        z = fract(z * 0.1031F);
        float dot = x * (y + 33.33F)
                + y * (z + 33.33F)
                + z * (x + 33.33F);
        x += dot;
        y += dot;
        z += dot;
        return fract((x + y) * z);
    }

    private static float[] coreDirection() {
        float projected = 0.86F * GALACTIC_NORMAL[0]
                - 0.18F * GALACTIC_NORMAL[1]
                - 0.48F * GALACTIC_NORMAL[2];
        return normalize(
                0.86F - GALACTIC_NORMAL[0] * projected,
                -0.18F - GALACTIC_NORMAL[1] * projected,
                -0.48F - GALACTIC_NORMAL[2] * projected
        );
    }

    private static float[] normalize(float x, float y, float z) {
        float inverseLength = 1.0F / (float)Math.sqrt(x * x + y * y + z * z);
        return new float[] { x * inverseLength, y * inverseLength, z * inverseLength };
    }

    private static float gaussian(float value) {
        return (float)Math.exp(-(float)Math.pow(value, 2.0));
    }

    private static float smoothInterpolation(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float t = clamp((value - edge0) / (edge1 - edge0));
        return t * t * (3.0F - 2.0F * t);
    }

    private static float fract(float value) {
        return value - (float)Math.floor(value);
    }

    private static float mix(float start, float end, float amount) {
        return start * (1.0F - amount) + end * amount;
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(value, 1.0F));
    }

    private static int channel(float value) {
        return Math.round(clamp(value) * 255.0F);
    }
}
