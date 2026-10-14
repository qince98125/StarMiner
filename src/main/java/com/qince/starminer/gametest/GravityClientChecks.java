package com.qince.starminer.gametest;

import com.qince.starminer.StarMiner;
import com.qince.starminer.api.GravityDirection;
import com.qince.starminer.api.GravityState;
import com.qince.starminer.gravity.GravityFrame;
import com.qince.starminer.registry.SMEntityData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3f;

/** Explicit opt-in development checks, confined to the isolated runGravityClientCheck directory. */
public final class GravityClientChecks {
    private static boolean complete;
    @SubscribeEvent
    public static void check(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (complete || !Boolean.getBoolean("starminer.verifyCamera")
                || !mc.gameDirectory.toPath().toAbsolutePath().normalize().getFileName().toString().equals("run-gravity-client-checks")
                || mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
        complete = true;
        StarMiner.LOGGER.info("Running gravity development checks inside isolated directory {}", mc.gameDirectory.toPath().toAbsolutePath().normalize());
        String result;
        try {
            run(mc);
            result = "PASSED: camera, full client ticks, first-person overlays, friction and jumping";
        } catch (AssertionError | RuntimeException failure) {
            result = "FAILED: " + failure;
            StarMiner.LOGGER.error("GRAVITY CLIENT CHECKS FAILED (game remains running)", failure);
        }
        try {
            java.nio.file.Files.writeString(mc.gameDirectory.toPath().resolve("gravity-check-result.txt"), result);
        } catch (java.io.IOException failure) {
            StarMiner.LOGGER.error("Unable to write isolated gravity test result", failure);
        }
        if (Boolean.getBoolean("starminer.checkExit")) mc.stop();
    }

    private static void run(Minecraft mc) {
        // Never change the actual player's gravity, flying state, pose or camera angles for a probe.
        var player = new net.minecraft.client.player.LocalPlayer(mc, mc.level, mc.player.connection,
                mc.player.getStats(), mc.player.getRecipeBook(), false, false);
        player.input = new net.minecraft.client.player.Input();
        var saved = player.getData(SMEntityData.GRAVITY_STATE);
        Vec3 position = player.position();
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        int checks = 0;
        try {
            player.setPos(0.5, 80, 0.5);
            Camera camera = new Camera();
            for (var direction : GravityDirection.VALUES) {
                player.setData(SMEntityData.GRAVITY_STATE, new GravityState(direction, BlockPos.ZERO));
                for (float y : new float[]{0, 45, 90, 180, -90}) for (float p : new float[]{-89, -45, 0, 45, 89}) {
                    player.setYRot(y);
                    player.setXRot(p);
                    for (int mode = 0; mode < 3; mode++) {
                        camera.setup(mc.level, player, mode != 0, mode == 2, 1);
                        Vec3 expected = player.getViewVector(1).scale(mode == 2 ? -1 : 1);
                        Vec3 actual = new Vec3(camera.getLookVector());
                        if (expected.distanceTo(actual) > 1.0E-4) throw new AssertionError(direction + " camera look mismatch at " + y + "," + p + " mode=" + mode);
                        Vector3f localUp = new Vector3f(0, 1, 0).rotate(new org.joml.Quaternionf().rotationYXZ(
                                (float)Math.PI - (y + (mode == 2 ? 180 : 0)) * (float)Math.PI / 180,
                                -(mode == 2 ? -p : p) * (float)Math.PI / 180, 0));
                        Vec3 expectedUp = GravityFrame.toWorld(direction, new Vec3(localUp));
                        if (expectedUp.distanceTo(new Vec3(camera.getUpVector())) > 1.0E-4) throw new AssertionError(direction + " camera up mismatch");
                        checks++;
                    }
                }
            }
            StarMiner.LOGGER.info("GRAVITY CAMERA CHECKS PASSED: {} combinations (six directions, yaw/pitch, first/third person)", checks);
        } finally {
            player.setData(SMEntityData.GRAVITY_STATE, saved);
            player.setPos(position);
            player.setYRot(yaw);
            player.setXRot(pitch);
        }
        GravityClientMotionChecks.check(mc);
        if (Boolean.getBoolean("starminer.checkExit")) return;
        // Reproducible survival fixture for visual QA, inside the isolated GravityQA save.
        mc.getSingleplayerServer().execute(() -> {
            var server = mc.getSingleplayerServer();
            var level = server.overworld();
            for (int x = -4; x <= 4; x++) for (int y = 76; y <= 84; y++) for (int z = -4; z <= 4; z++)
                if (Math.abs(x) == 4 || Math.abs(y - 80) == 4 || Math.abs(z) == 4)
                    level.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            BlockPos core = new BlockPos(0, 80, 0);
            level.setBlock(core, com.qince.starminer.registry.SMBlocks.GRAVITY_CORE.get().defaultBlockState(), 3);
            com.qince.starminer.gravity.GravityFieldManager.of(level).add(core);
            var serverPlayer = server.getPlayerList().getPlayer(mc.player.getUUID());
            serverPlayer.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            serverPlayer.teleportTo(6, 80.5, 0.5);
            serverPlayer.setYRot(0);
            serverPlayer.setXRot(0);
        });
    }
}
