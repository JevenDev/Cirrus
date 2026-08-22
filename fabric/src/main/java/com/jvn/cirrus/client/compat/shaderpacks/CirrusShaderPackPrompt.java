package com.jvn.cirrus.client.compat.shaderpacks;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
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

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal(APPLY_COMMAND)
                        .executes(context -> applyFix(context.getSource())))
        );
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
        ClientTickEvents.END_CLIENT_TICK.register(CirrusShaderPackPrompt::tick);
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

        String currentPack = CirrusShaderPackCompat.currentPackName().orElse("");
        if (Objects.equals(currentPack, lastObservedPack)) {
            return;
        }
        lastObservedPack = currentPack;

        CirrusShaderPackCompat.activeFix()
                .filter(CirrusShaderPackCompat.ActiveFix::needsChanges)
                .ifPresent(fix -> showPrompt(player, fix));
    }

    private static void showPrompt(LocalPlayer player, CirrusShaderPackCompat.ActiveFix fix) {
        MutableComponent apply = Component.translatable("cirrus.shaderpack.fix.apply")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.UNDERLINE)
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent.RunCommand(
                                "/" + APPLY_COMMAND
                        ))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.translatable("cirrus.shaderpack.fix.apply.hover")
                        )));
        player.displayClientMessage(
                Component.translatable("cirrus.shaderpack.fix.prompt", fix.displayName())
                        .append(Component.literal(" "))
                        .append(apply),
                false
        );
    }

    private static int applyFix(FabricClientCommandSource source) {
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
            source.sendError(message);
            return 0;
        }
        source.sendFeedback(message);
        return 1;
    }
}
