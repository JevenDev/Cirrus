package com.jvn.cirrus.client;

import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;

public interface CirrusClientClockAccess {
    void cirrus$setClock(Holder<WorldClock> clock);
}
