package com.jvn.cirrus.client.compat.shaderpacks;

import com.jvn.cirrus.client.compat.distanthorizons.DistantHorizonsCompat;
import com.jvn.cirrus.config.CirrusConfig;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CirrusShaderPackPrompt {
    private static final String APPLY_COMMAND = "cirrus-apply-shader-settings";
    private static final int INITIAL_CHECK_DELAY_TICKS = 40;
    private static final int RECHECK_DELAY_TICKS = 100;
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();

    private static int checkDelayTicks = INITIAL_CHECK_DELAY_TICKS;
    private static String lastObservedPack;

    private CirrusShaderPackPrompt() {
    }

    public static void init() {
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }

        ClientCommandRegistrationEvent.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandRegistrationEvent.literal(APPLY_COMMAND)
                        .executes(context -> applyFix(context.getSource())))
        );
        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> reset());
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> reset());
        ClientTickEvent.CLIENT_POST.register(CirrusShaderPackPrompt::tick);
    }

    private static void reset() {
        checkDelayTicks = INITIAL_CHECK_DELAY_TICKS;
        lastObservedPack = null;
    }

    private static void tick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || checkDelayTicks-- > 0) {
            return;
        }
        checkDelayTicks = RECHECK_DELAY_TICKS;

        if (!CirrusConfig.SHADER_PACK_WARNINGS_ENABLED.get()) {
            lastObservedPack = null;
            return;
        }

        String currentPack = CirrusShaderPackCompat.currentPackName().orElse("");
        if (Objects.equals(currentPack, lastObservedPack)) {
            return;
        }
        lastObservedPack = currentPack;

        boolean distantHorizonsLoaded = DistantHorizonsCompat.isLoaded();
        if (!currentPack.isEmpty() && !CirrusShaderPackCompat.hasCompatibilityProfile(currentPack)) {
            showUnknownPackWarning(player, currentPack, distantHorizonsLoaded);
            return;
        }

        var activeFix = CirrusShaderPackCompat.activeFix();
        if (distantHorizonsLoaded && !currentPack.isEmpty()) {
            activeFix.filter(CirrusShaderPackCompat.ActiveFix::needsChanges)
                    .ifPresentOrElse(
                            fix -> showPrompt(player, fix, true),
                            () -> showDistantHorizonsWarning(player, currentPack)
                    );
            return;
        }
        activeFix.filter(CirrusShaderPackCompat.ActiveFix::needsChanges)
                .ifPresent(fix -> showPrompt(player, fix, false));
    }

    private static void showPrompt(
            LocalPlayer player,
            CirrusShaderPackCompat.ActiveFix fix,
            boolean distantHorizonsLoaded
    ) {
        MutableComponent apply = Component.translatable("cirrus.shaderpack.fix.apply")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.UNDERLINE)
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent(
                                ClickEvent.Action.RUN_COMMAND,
                                "/" + APPLY_COMMAND
                        ))
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("cirrus.shaderpack.fix.apply.hover")
                        )));
        String promptKey = distantHorizonsLoaded
                ? "cirrus.shaderpack.fix.prompt.distantHorizons"
                : "cirrus.shaderpack.fix.prompt";
        player.displayClientMessage(
                Component.translatable(promptKey, fix.displayName())
                        .withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal(" "))
                        .append(apply),
                false
        );
    }

    private static void showUnknownPackWarning(
            LocalPlayer player,
            String packName,
            boolean distantHorizonsLoaded
    ) {
        String promptKey = distantHorizonsLoaded
                ? "cirrus.shaderpack.unknown.prompt.distantHorizons"
                : "cirrus.shaderpack.unknown.prompt";
        player.displayClientMessage(
                Component.translatable(promptKey, packName)
                        .withStyle(ChatFormatting.YELLOW),
                false
        );
    }

    private static void showDistantHorizonsWarning(LocalPlayer player, String packName) {
        player.displayClientMessage(
                Component.translatable("cirrus.shaderpack.distantHorizons.prompt", packName)
                        .withStyle(ChatFormatting.YELLOW),
                false
        );
    }

    private static int applyFix(
            ClientCommandRegistrationEvent.ClientCommandSourceStack source
    ) {
        CirrusShaderPackCompat.ApplyResult result = CirrusShaderPackCompat.applyActiveFix();
        Component message = switch (result.status()) {
            case APPLIED -> Component.translatable(
                    "cirrus.shaderpack.fix.applied",
                    result.displayName()
            ).withStyle(ChatFormatting.GREEN);
            case ALREADY_COMPATIBLE -> Component.translatable(
                    "cirrus.shaderpack.fix.alreadyCompatible",
                    result.displayName()
            ).withStyle(ChatFormatting.YELLOW);
            case SAVED_RELOAD_FAILED -> Component.translatable(
                    "cirrus.shaderpack.fix.reloadFailed",
                    result.displayName()
            ).withStyle(ChatFormatting.YELLOW);
            case UNSUPPORTED -> Component.translatable(
                    "cirrus.shaderpack.fix.unsupported"
            ).withStyle(ChatFormatting.RED);
            case FAILED -> Component.translatable(
                    "cirrus.shaderpack.fix.failed",
                    result.displayName()
            ).withStyle(ChatFormatting.RED);
        };

        if (result.status() == CirrusShaderPackCompat.ApplyStatus.UNSUPPORTED
                || result.status() == CirrusShaderPackCompat.ApplyStatus.FAILED) {
            source.arch$sendFailure(message);
            return 0;
        }
        source.arch$sendSuccess(() -> message, false);
        return 1;
    }
}
