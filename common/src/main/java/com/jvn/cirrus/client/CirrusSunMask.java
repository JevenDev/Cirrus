package com.jvn.cirrus.client;

import com.mojang.blaze3d.vertex.MeshData;

public interface CirrusSunMask {
    boolean cirrus$beginSunMask(MeshData sunMesh);

    void cirrus$endSunMask();
}
