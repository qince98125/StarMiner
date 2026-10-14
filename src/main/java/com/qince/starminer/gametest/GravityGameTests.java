package com.qince.starminer.gametest;

import com.qince.starminer.api.GravityDirection;
import com.qince.starminer.api.GravityState;
import com.qince.starminer.gravity.GravityFrame;
import com.qince.starminer.registry.SMEntityData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Runtime checks exercise the actual transformed Minecraft methods, not just the math helpers. */
@GameTestHolder("starminer")
@PrefixGameTestTemplate(false)
public final class GravityGameTests {
    @GameTest(template = "gravity_empty")
    public static void sixDirectionsStandWalkJumpAndTarget(GameTestHelper helper) {
        for (var direction : GravityDirection.VALUES) {
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            Vec3 feet = helper.absoluteVec(new Vec3(8.5, 8.5, 8.5));
            // A three-by-three floor, aligned with local -Y.
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
                Vec3 at = feet.add(GravityFrame.toWorld(direction, new Vec3(x, 0, z)));
                helper.getLevel().setBlock(BlockPos.containing(at), Blocks.STONE.defaultBlockState(), 3);
            }
            // Put the feet exactly on the face of the floor block.
            Vec3 pull = direction.vector();
            feet = feet.subtract(pull.scale(0.5));
            player.setData(SMEntityData.GRAVITY_STATE, new GravityState(direction, BlockPos.ZERO));
            player.setPos(feet);
            player.setDeltaMovement(pull.scale(0.08));
            player.move(MoverType.SELF, player.getDeltaMovement());
            helper.assertTrue(player.onGround(), direction + " did not land on its floor");
            player.setSpeed(0.1F);
            player.setDeltaMovement(Vec3.ZERO);
            Vec3 walkedFrom = player.position();
            player.travel(new Vec3(0, 0, 1));
            Vec3 walked = GravityFrame.toLocal(direction, player.position().subtract(walkedFrom));
            helper.assertTrue(walked.z > 0.09 && Math.abs(walked.y) < 1.0E-5,
                    direction + " walk did not follow floor plane: " + walked);
            Vec3 eye = player.getEyePosition().subtract(player.position());
            helper.assertTrue(eye.subtract(pull.scale(-player.getEyeHeight())).length() < 1.0E-5, direction + " eye offset wrong");
            player.setYRot(0);
            player.setXRot(0);
            helper.assertTrue(player.getLookAngle().subtract(GravityFrame.toWorld(direction, new Vec3(0, 0, 1))).length() < 1.0E-5,
                    direction + " targeting does not follow the view");
            player.jumpFromGround();
            helper.assertTrue(player.getDeltaMovement().dot(pull) < -0.4, direction + " jump not away from floor");
            Vec3 before = player.position();
            player.move(MoverType.SELF, player.getDeltaMovement());
            helper.assertTrue(player.position().subtract(before).dot(pull) < -0.4, direction + " jumped into floor");
            // Clear the floor before testing the next frame.
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
                Vec3 at = feet.add(pull.scale(0.5)).add(GravityFrame.toWorld(direction, new Vec3(x, 0, z)));
                helper.getLevel().setBlock(BlockPos.containing(at), Blocks.AIR.defaultBlockState(), 3);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "gravity_empty")
    public static void radiusAndCoreLifecycle(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(8, 8, 8));
        level.setBlock(pos, com.qince.starminer.registry.SMBlocks.GRAVITY_CORE.get().defaultBlockState(), 3);
        var core = (com.qince.starminer.content.blockentity.GravityCoreBlockEntity) level.getBlockEntity(pos);
        core.setGravityRange(64);
        // Check the entire radius across several chunk boundaries, without loading distant chunks.
        var manager = com.qince.starminer.gravity.GravityFieldManager.of(level);
        manager.add(pos);
        helper.assertTrue(manager.resolveField(pos.getCenter().add(60, 0, 0)).isPresent(), "64-block field lost past first chunk");
        helper.assertTrue(manager.resolveField(pos.getCenter().add(65, 0, 0)).isEmpty(), "field extends past configured radius");
        level.removeBlock(pos, false);
        helper.assertTrue(manager.resolveField(pos.getCenter()).isEmpty(), "removed core still exerts gravity");
        helper.succeed();
    }

    @GameTest(template = "gravity_empty")
    public static void sixDirectionsFallDamage(GameTestHelper helper) {
        for (var direction : GravityDirection.VALUES) {
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            Vec3 center = helper.absoluteVec(new Vec3(8.5, 8.5, 8.5));
            BlockPos floor = BlockPos.containing(center);
            helper.getLevel().setBlock(floor, Blocks.STONE.defaultBlockState(), 3);
            player.setData(SMEntityData.GRAVITY_STATE, new GravityState(direction, BlockPos.ZERO));
            player.setPos(center.subtract(direction.vector().scale(0.5)));
            player.fallDistance = 6;
            player.setDeltaMovement(direction.vector().scale(0.08));
            player.move(MoverType.SELF, player.getDeltaMovement());
            helper.assertTrue(player.getHealth() < 20, direction + " does not apply fall damage");
            helper.assertTrue(player.fallDistance == 0, direction + " does not reset landing distance");
            helper.getLevel().removeBlock(floor, false);
        }
        helper.succeed();
    }

    @GameTest(template = "gravity_empty")
    public static void sixDirectionsSneakAndCrouch(GameTestHelper helper) {
        for (var direction : GravityDirection.VALUES) {
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            Vec3 center = helper.absoluteVec(new Vec3(8.5, 8.5, 8.5));
            var floor = BlockPos.containing(center);
            helper.getLevel().setBlock(floor, Blocks.STONE.defaultBlockState(), 3);
            player.setData(SMEntityData.GRAVITY_STATE, new GravityState(direction, BlockPos.ZERO));
            player.setPos(center.subtract(direction.vector().scale(0.5)));
            player.setOnGround(true);
            player.setShiftKeyDown(true);
            player.setPose(net.minecraft.world.entity.Pose.CROUCHING);
            Vec3 start = player.position();
            player.move(MoverType.SELF, GravityFrame.toWorld(direction, new Vec3(2, -0.08, 0)));
            Vec3 moved = GravityFrame.toLocal(direction, player.position().subtract(start));
            helper.assertTrue(moved.x > 0 && moved.x < 0.81, direction + " sneak walked off edge: " + moved);
            helper.assertTrue(Math.abs(moved.y) < 1.0E-5, direction + " crouch box lost floor");
            helper.getLevel().removeBlock(floor, false);
        }
        helper.succeed();
    }

    @GameTest(template = "gravity_empty")
    public static void switchingDirectionPreservesCenterAndRestoresVanilla(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(new Vec3(8.5, 8, 8.5)));
        player.baseTick();
        Vec3 center = player.getBoundingBox().getCenter();
        for (var direction : GravityDirection.VALUES) {
            player.setData(SMEntityData.GRAVITY_STATE, new GravityState(direction, BlockPos.ZERO));
            player.baseTick();
            helper.assertTrue(player.getBoundingBox().getCenter().distanceTo(center) < 1.0E-5,
                    direction + " transition moved body center");
        }
        player.setData(SMEntityData.GRAVITY_STATE, GravityState.DEFAULT);
        player.baseTick();
        helper.assertTrue(player.getBoundingBox().getCenter().distanceTo(center) < 1.0E-5, "leaving field shifted center");
        helper.assertTrue(Math.abs(player.getBoundingBox().getYsize() - player.getBbHeight()) < 1.0E-5, "vanilla box was not restored");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest(template = "gravity_empty")
    public static void serverPacketFallDistanceUsesGravityAxis(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(player.connection.getConnection());
        try {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.getAbilities().invulnerable = false;
            for (var direction : GravityDirection.VALUES) {
                player.setData(SMEntityData.GRAVITY_STATE, new GravityState(direction, BlockPos.ZERO));
                player.setPos(helper.absoluteVec(new Vec3(8.5, 8, 8.5)));
                player.resetFallDistance();
                Vec3 movement = direction.vector().scale(2);
                for (int i = 0; i < 3; i++) player.doCheckFallDamage(movement.x, movement.y, movement.z, false);
                helper.assertTrue(Math.abs(player.fallDistance - 6) < 1.0E-5, direction + " server packet fall distance wrong: " + player.fallDistance);
                movement = direction.vector().scale(-0.42);
                player.doCheckFallDamage(movement.x, movement.y, movement.z, false);
                helper.assertTrue(player.fallDistance == 0, direction + " server jump did not reset falling");
            }
        } finally {
            player.setData(SMEntityData.GRAVITY_STATE, GravityState.DEFAULT);
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
        helper.succeed();
    }
}
