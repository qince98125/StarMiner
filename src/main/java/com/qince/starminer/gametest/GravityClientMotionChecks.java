package com.qince.starminer.gametest;

import java.util.LinkedHashMap;
import com.qince.starminer.StarMiner;
import com.qince.starminer.api.GravityDirection;
import com.qince.starminer.api.GravityState;
import com.qince.starminer.gravity.GravityFrame;
import com.qince.starminer.registry.SMEntityData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Exercises LocalPlayer.aiStep, including client-only push-out code missed by server GameTests. */
public final class GravityClientMotionChecks {
    private static LocalPlayer probe;
    public static boolean isProbe(LocalPlayer player) { return player == probe; }

    private static void tick(LocalPlayer player) {
        player.setOldPosAndRot();
        player.tick();
    }

    private static Object overlay(LocalPlayer player) {
        try {
            var method = net.minecraft.client.renderer.ScreenEffectRenderer.class.getDeclaredMethod("getOverlayBlock", net.minecraft.world.entity.player.Player.class);
            method.setAccessible(true);
            return method.invoke(null, player);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    public static void check(Minecraft mc) {
        int ticks = 0;
        for (var direction : GravityDirection.VALUES) {
            Vec3 center = new Vec3(8.5, 80.5, 8.5);
            var originalBlocks = new LinkedHashMap<BlockPos, BlockState>();
            try {
                for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
                    BlockPos pos = BlockPos.containing(center.add(GravityFrame.toWorld(direction, new Vec3(x, 0, z))));
                    originalBlocks.put(pos, mc.level.getBlockState(pos));
                    mc.level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
                }
                var player = new LocalPlayer(mc, mc.level, mc.player.connection, mc.player.getStats(), mc.player.getRecipeBook(), false, false);
                probe = player;
                player.input = new Input();
                player.setSpeed(0.1F);
                player.setData(SMEntityData.GRAVITY_STATE, new GravityState(direction, BlockPos.ZERO));
                Vec3 feet = center.subtract(direction.vector().scale(0.5));
                player.setPos(feet);
                player.setOnGround(true);
                player.setDeltaMovement(direction.vector().scale(0.08));
                for (int tick = 0; tick < 120; tick++) {
                    tick(player);
                    double height = player.position().subtract(feet).dot(direction.vector());
                    if (Math.abs(height) > 1.0E-5 || !player.onGround())
                        throw new AssertionError(direction + " unstable client standing at tick " + tick + ": height=" + height + ", grounded=" + player.onGround());
                    ticks++;
                }
                if (Math.abs(player.bob) > 1.0E-5)
                    throw new AssertionError(direction + " idle camera is bobbing: " + player.bob);
                if (overlay(player) != null)
                    throw new AssertionError(direction + " standing first person is covered by supporting block overlay");
                Vec3 standing = player.position();
                player.setPos(standing.add(direction.vector().scale(player.getEyeHeight() + 0.1)));
                if (overlay(player) == null)
                    throw new AssertionError(direction + " real head obstruction no longer draws overlay");
                player.setPos(standing);
                if (!direction.isVanilla()) {
                    for (double penetration : new double[]{-1.0E-6, 0, 1.0E-8, 1.0E-6}) {
                        player.setPos(feet.add(direction.vector().scale(penetration)));
                        player.setDeltaMovement(direction.vector().scale(0.08));
                        player.setOnGround(true);
                        for (int step = 0; step < 20; step++) {
                            tick(player);
                            double height = player.position().subtract(feet).dot(direction.vector());
                            if (Math.abs(height) > 1.0E-5 || !player.onGround())
                                throw new AssertionError(direction + " small contact error caused bounce: penetration=" + penetration + ", height=" + height);
                            ticks++;
                        }
                    }
                }
                // Release all input with tangential motion: stone should retain 0.6 * 0.91 each tick.
                player.setDeltaMovement(GravityFrame.toWorld(direction, new Vec3(0, -0.08, 0.2)));
                tick(player);
                double speed = GravityFrame.toLocal(direction, player.getDeltaMovement()).z;
                if (Math.abs(speed - 0.2 * 0.6 * 0.91) > 1.0E-5)
                    throw new AssertionError(direction + " client stone friction wrong: " + speed);
                for (int tick = 0; tick < 20; tick++) {
                    tick(player);
                    if (!player.onGround()) throw new AssertionError(direction + " lost support while stopping");
                    ticks++;
                }
                if (GravityFrame.toLocal(direction, player.getDeltaMovement()).horizontalDistance() > 0.003)
                    throw new AssertionError(direction + " did not stop sliding");
                player.jumpFromGround();
                if (player.getDeltaMovement().dot(direction.vector()) > -0.4)
                    throw new AssertionError(direction + " support fix suppressed jumping");
                Vec3 jumpedFrom = player.position();
                tick(player);
                if (player.position().subtract(jumpedFrom).dot(direction.vector()) > -0.4)
                    throw new AssertionError(direction + " contact correction prevented leaving the floor");
            } finally {
                probe = null;
                originalBlocks.forEach((pos, state) -> mc.level.setBlock(pos, state, 3));
            }
        }
        StarMiner.LOGGER.info("GRAVITY CLIENT MOTION CHECKS PASSED: {} standing/stopping ticks across six directions", ticks);
    }
    private GravityClientMotionChecks() {}
}
