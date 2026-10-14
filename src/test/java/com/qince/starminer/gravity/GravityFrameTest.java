package com.qince.starminer.gravity;

import com.qince.starminer.api.GravityDirection;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import org.joml.Vector3f;
import static org.junit.jupiter.api.Assertions.*;

class GravityFrameTest {
    @Test void allFramesAgreeWithCameraAndInvertExactly() {
        for (var direction : GravityDirection.VALUES) {
            for (Vec3 v : new Vec3[]{new Vec3(1, 0, 0), new Vec3(0, 1, 0), new Vec3(0, 0, 1), new Vec3(-2.7, 9.8, 4.1)}) {
                Vec3 world = GravityFrame.toWorld(direction, v);
                assertEquals(v, GravityFrame.toLocal(direction, world), direction.toString());
                Vector3f render = GravityFrame.rotation(direction).transform(new Vector3f((float)v.x, (float)v.y, (float)v.z));
                assertEquals(world.x, render.x, 1.0E-5);
                assertEquals(world.y, render.y, 1.0E-5);
                assertEquals(world.z, render.z, 1.0E-5);
            }
            assertEquals(0, direction.vector().distanceTo(GravityFrame.toWorld(direction, new Vec3(0, -1, 0))), 0);
            var box = GravityFrame.box(direction, new Vec3(10, 20, 30), 0.6, 1.8);
            var local = GravityFrame.toLocal(direction, box, new Vec3(10, 20, 30));
            assertEquals(0, local.minY, 1.0E-9);
            assertEquals(1.8, local.maxY, 1.0E-9);
            assertEquals(0.6, local.getXsize(), 1.0E-9);
            assertEquals(0.6, local.getZsize(), 1.0E-9);
        }
    }
}
