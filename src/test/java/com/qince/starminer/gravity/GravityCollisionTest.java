package com.qince.starminer.gravity;

import java.util.List;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GravityCollisionTest {
    @Test void contactRepairOnlyAdjustsTinyFloorPenetration() {
        AABB body = new AABB(-0.3, 0, -0.3, 0.3, 1.8, 0.3);
        var slight = new AABB(-2, -1, -2, 2, 1.0E-6, 2);
        assertEquals(1.0E-6, GravityCollision.supportPenetration(body, List.of(slight), 1.0E-5), 1.0E-12);
        var outside = new AABB(0.4, -1, -2, 2, 1.0E-6, 2);
        var wall = new AABB(-2, -1, -2, 2, 1.0, 2);
        assertEquals(0, GravityCollision.supportPenetration(body, List.of(outside, wall), 1.0E-5));
        var touching = new AABB(-2, -1, -2, 2, 0, 2);
        assertEquals(0, GravityCollision.supportPenetration(body, List.of(touching), 1.0E-5));
    }

    private static final AABB BODY = new AABB(-0.3, 0, -0.3, 0.3, 1.8, 0.3);
    @Test void floorStopsFallButNotJump() {
        var floor = List.of(Shapes.create(new AABB(-10, -1, -10, 10, 0, 10)));
        assertEquals(0, GravityCollision.collide(BODY, new Vec3(0, -1, 0), floor, 0.6, false).y, 1.0E-9);
        assertEquals(0.42, GravityCollision.collide(BODY, new Vec3(0, 0.42, 0), floor, 0.6, true).y, 1.0E-9);
    }
    @Test void wallIsNotAFloorAndHalfBlockCanBeSteppedOnto() {
        var floor = Shapes.create(new AABB(-10, -1, -10, 10, 0, 10));
        var wall = Shapes.create(new AABB(0.6, 0, -2, 1.6, 3, 2));
        Vec3 stopped = GravityCollision.collide(BODY, new Vec3(1, -0.08, 0), List.of(floor, wall), 0.6, true);
        assertEquals(0.3, stopped.x, 1.0E-9);
        var slab = Shapes.create(new AABB(0.6, 0, -2, 1.6, 0.5, 2));
        Vec3 stepped = GravityCollision.collide(BODY, new Vec3(1, -0.08, 0), List.of(floor, slab), 0.6, true);
        assertEquals(1, stepped.x, 1.0E-9);
        assertEquals(0.5, stepped.y, 1.0E-9);
    }
}
