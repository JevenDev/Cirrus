import com.jvn.cirrus.client.CirrusEndVeil;

public final class VerifyEndVeil {
    public static void main(String[] args) {
        CirrusEndVeil normal = new CirrusEndVeil();
        normal.update(true, 0, 0);
        normal.update(true, 1, 101);
        equal(1.0F, normal.opacity(200), "fight glass");
        equal(1.0F, normal.charge(280), "defeat beams");
        if (normal.flash(282) <= 0.0F) {
            throw new AssertionError("normal defeat must flash");
        }
        equal(1.0F, normal.shatter(320), "normal defeat shatters");
        equal(0.0F, normal.opacity(320), "normal defeat removes glass");

        CirrusEndVeil leaving = new CirrusEndVeil();
        leaving.update(true, 0, 0);
        leaving.update(false, 0, 100);
        equal(0.5F, leaving.opacity(115), "leaving the fight fades glass");
        equal(0.0F, leaving.opacity(130), "leaving without a defeat removes glass");
        equal(0.0F, leaving.shatter(130), "leaving does not shatter");

        CirrusEndVeil persistent = new CirrusEndVeil();
        persistent.update(false, 0, 0, true);
        equal(1.0F, persistent.opacity(0), "glass persists on returning to the End");
        equal(0.0F, persistent.charge(0), "returning does not replay beams");
        persistent.update(true, 0, 50, true);
        persistent.update(true, 1, 101, true);
        for (int tick = 101; tick <= 500; tick++) {
            equal(1.0F, persistent.opacity(tick), "persistent glass remains visible");
            equal(0.0F, persistent.shatter(tick), "persistent glass never shatters");
            equal(0.0F, persistent.flash(tick), "persistent glass never flashes");
        }
        equal(1.0F, persistent.charge(280), "persistent defeat keeps the beam buildup");
        equal(0.5F, persistent.charge(300), "persistent defeat fades the beams");
        equal(0.0F, persistent.charge(320), "persistent defeat finishes the beams");
        persistent.update(false, 0, 321, true);
        equal(1.0F, persistent.opacity(500), "glass remains without boss fog");
        if (!persistent.occludesSky(500)) {
            throw new AssertionError("settled persistent glass must cover the sky");
        }
        persistent.update(false, 0, 500, false);
        equal(0.0F, persistent.opacity(500), "disabling persistence restores the completed defeat");
        persistent.update(true, 0, 600, true);
        equal(0.0F, persistent.charge(600), "a new fight resets the beams");
        equal(1.0F, persistent.opacity(600), "a new fight keeps persistent glass");

        normal.update(true, 190, 290, true);
        equal(1.0F, normal.opacity(290), "enabling persistence during shattering restores glass");
        equal(0.0F, normal.shatter(290), "enabling persistence stops shattering");
        equal(0.0F, normal.flash(290), "enabling persistence stops the flash");
        System.out.println("End Glass state checks passed");
    }

    private static void equal(float expected, float actual, String message) {
        if (Math.abs(expected - actual) > 0.00001F) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }
}
