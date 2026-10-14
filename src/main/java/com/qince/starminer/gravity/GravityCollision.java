package com.qince.starminer.gravity;

import java.util.ArrayList;
import java.util.List;
import com.qince.starminer.api.GravityDirection;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Colliders are rotated about the feet into local space; the world itself is never modified. */
public final class GravityCollision {
    /** Repair sub-pixel penetration before vanilla push-out or shape collision treats it as airborne. */
    public static void correctSupportContact(Entity entity) {
        var direction = GravityFrame.direction(entity);
        if (direction.isVanilla() || entity.noPhysics || entity.getDeltaMovement().dot(direction.vector()) < 0) return;
        double tolerance = 1.0E-5;
        Vec3 feet = entity.position();
        AABB body = GravityFrame.toLocal(direction, entity.getBoundingBox(), feet);
        List<AABB> colliders = new ArrayList<>();
        for (VoxelShape shape : entity.level().getBlockCollisions(entity, entity.getBoundingBox().inflate(tolerance))) {
            for (AABB world : shape.toAabbs()) colliders.add(GravityFrame.toLocal(direction, world, feet));
        }
        double correction = supportPenetration(body, colliders, tolerance);
        if (correction > 0) entity.setPos(feet.add(GravityFrame.toWorld(direction, new Vec3(0, correction, 0))));
    }

    public static double supportPenetration(AABB body, List<AABB> colliders, double tolerance) {
        double correction = 0;
        for (AABB block : colliders) {
            double penetration = block.maxY - body.minY;
            if (penetration > 0 && penetration <= tolerance && block.minY < body.minY
                    && block.maxX > body.minX && block.minX < body.maxX
                    && block.maxZ > body.minZ && block.minZ < body.maxZ)
                correction = Math.max(correction, penetration);
        }
        return correction;
    }

    public static Vec3 collide(Entity entity, Vec3 local) {
        GravityDirection direction = GravityFrame.direction(entity);
        Vec3 origin = entity.position();
        AABB worldBox = entity.getBoundingBox();
        Vec3 worldMove = GravityFrame.toWorld(direction, local);
        Vec3 up = GravityFrame.toWorld(direction, new Vec3(0, entity.maxUpStep(), 0));
        AABB region = worldBox.expandTowards(worldMove).expandTowards(up).inflate(1.0E-5);
        List<VoxelShape> shapes = new ArrayList<>();
        for (VoxelShape shape : entity.level().getCollisions(entity, region)) {
            for (AABB box : shape.toAabbs()) {
                shapes.add(Shapes.create(GravityFrame.toLocal(direction, box, origin)));
            }
        }
        var border = entity.level().getWorldBorder();
        if (border.isInsideCloseToBorder(entity, region)) {
            for (AABB box : border.getCollisionShape().toAabbs()) {
                shapes.add(Shapes.create(GravityFrame.toLocal(direction, box, origin)));
            }
        }
        return collide(GravityFrame.toLocal(direction, worldBox, origin), local, shapes,
                entity.maxUpStep(), entity.onGround());
    }

    public static Vec3 collide(AABB box, Vec3 local, List<VoxelShape> shapes, double step, boolean grounded) {
        Vec3 result = clip(box, local, shapes);
        boolean landed = local.y < 0 && local.y != result.y;
        if (step > 0 && (grounded || landed) && (local.x != result.x || local.z != result.z)) {
            Vec3 raised = clip(box, new Vec3(0, step, 0), shapes);
            Vec3 across = clip(box.move(raised), new Vec3(local.x, 0, local.z), shapes);
            Vec3 down = clip(box.move(raised).move(across), new Vec3(0, local.y - raised.y, 0), shapes);
            Vec3 stepped = raised.add(across).add(down);
            if (stepped.horizontalDistanceSqr() > result.horizontalDistanceSqr()) result = stepped;
        }
        return result;
    }

    private static Vec3 clip(AABB box, Vec3 movement, List<VoxelShape> shapes) {
        double y = Shapes.collide(Direction.Axis.Y, box, shapes, movement.y);
        box = box.move(0, y, 0);
        boolean zFirst = Math.abs(movement.x) < Math.abs(movement.z);
        double x = movement.x;
        double z = movement.z;
        if (zFirst) {
            z = Shapes.collide(Direction.Axis.Z, box, shapes, z);
            box = box.move(0, 0, z);
        }
        x = Shapes.collide(Direction.Axis.X, box, shapes, x);
        if (!zFirst) z = Shapes.collide(Direction.Axis.Z, box.move(x, 0, 0), shapes, z);
        return new Vec3(x, y, z);
    }

    private GravityCollision() {}
}
