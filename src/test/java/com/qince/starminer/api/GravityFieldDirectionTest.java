package com.qince.starminer.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GravityFieldDirectionTest {
    @Test void cylinderIgnoresLengthAxisAndPullsTowardInnerSurface() {
        var xTube = new GravityField(BlockPos.ZERO, 64, GravityConst.FIELD_TYPE_X_CYLINDER);
        var yTube = new GravityField(BlockPos.ZERO, 64, GravityConst.FIELD_TYPE_Y_CYLINDER);
        var zTube = new GravityField(BlockPos.ZERO, 64, GravityConst.FIELD_TYPE_Z_CYLINDER);
        assertEquals(GravityDirection.DOWN_TO_UP_Y_P, xTube.directionAt(new Vec3(50.5, 4.5, 0.5), null, false));
        assertEquals(GravityDirection.NORTH_TO_SOUTH_Z_P, yTube.directionAt(new Vec3(0.5, 50.5, 4.5), null, false));
        assertEquals(GravityDirection.WEST_TO_EAST_X_P, zTube.directionAt(new Vec3(4.5, 0.5, 50.5), null, false));
        assertEquals(GravityDirection.UP_TO_DOWN_Y_N, xTube.directionAt(new Vec3(50.5, -3.5, 0.5), null, false));
    }

    @Test void bufferCanBeDisabledAndCannotKeepWrongSignOrTubeAxis() {
        var sphere = new GravityField(BlockPos.ZERO, 64, 0, false);
        var sample = new Vec3(10.5, 11.5, 0.5);
        GravityDirection[] out = new GravityDirection[1];
        sphere.directionAt(sample.x, sample.y, sample.z, GravityDirection.EAST_TO_WEST_X_N, out);
        assertEquals(GravityDirection.UP_TO_DOWN_Y_N, out[0]);
        assertEquals(GravityDirection.WEST_TO_EAST_X_P,
                GravityDirection.pickByDominantAxis(-10, 0, 0, GravityDirection.EAST_TO_WEST_X_N, true));
        var tube = new GravityField(BlockPos.ZERO, 64, 2);
        assertEquals(GravityDirection.DOWN_TO_UP_Y_P, tube.directionAt(new Vec3(50.5, 2.5, 0.5), GravityDirection.WEST_TO_EAST_X_P, true));
    }
}
