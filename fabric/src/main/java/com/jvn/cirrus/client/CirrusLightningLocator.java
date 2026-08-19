package com.jvn.cirrus.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class CirrusLightningLocator {
    private static final int SCAN_INTERVAL_TICKS = 4;

    private static ClientLevel cachedLevel;
    private static List<LightningBolt> cachedBolts = List.of();
    private static long lastScanTick = Long.MIN_VALUE;
    private static long nextScanTick = Long.MIN_VALUE;

    private CirrusLightningLocator() {
    }

    public static LightningBolt nearestHorizontal(ClientLevel level, double x, double z) {
        refresh(level);
        LightningBolt nearest = null;
        double nearestDistanceSquared = Double.POSITIVE_INFINITY;
        for (LightningBolt lightning : cachedBolts) {
            double offsetX = lightning.getX() - x;
            double offsetZ = lightning.getZ() - z;
            double distanceSquared = offsetX * offsetX + offsetZ * offsetZ;
            if (distanceSquared < nearestDistanceSquared) {
                nearest = lightning;
                nearestDistanceSquared = distanceSquared;
            }
        }
        return nearest;
    }

    public static LightningBolt nearest(ClientLevel level, Vec3 position) {
        refresh(level);
        LightningBolt nearest = null;
        double nearestDistanceSquared = Double.POSITIVE_INFINITY;
        for (LightningBolt lightning : cachedBolts) {
            double distanceSquared = lightning.distanceToSqr(position);
            if (distanceSquared < nearestDistanceSquared) {
                nearest = lightning;
                nearestDistanceSquared = distanceSquared;
            }
        }
        return nearest;
    }

    private static void refresh(ClientLevel level) {
        long gameTime = level.getGameTime();
        boolean removedBolt = false;
        for (LightningBolt lightning : cachedBolts) {
            if (lightning.isRemoved()) {
                removedBolt = true;
                break;
            }
        }
        boolean flashNeedsInitialScan = cachedBolts.isEmpty()
                && lastScanTick != gameTime;
        if (cachedLevel == level && gameTime < nextScanTick && !removedBolt && !flashNeedsInitialScan) {
            return;
        }

        List<LightningBolt> bolts = new ArrayList<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof LightningBolt lightning && !lightning.isRemoved()) {
                bolts.add(lightning);
            }
        }
        cachedLevel = level;
        cachedBolts = List.copyOf(bolts);
        lastScanTick = gameTime;
        nextScanTick = gameTime + SCAN_INTERVAL_TICKS;
    }

    public static void invalidate() {
        cachedLevel = null;
        cachedBolts = List.of();
        lastScanTick = Long.MIN_VALUE;
        nextScanTick = Long.MIN_VALUE;
    }
}
