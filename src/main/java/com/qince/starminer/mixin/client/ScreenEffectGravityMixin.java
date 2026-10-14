package com.qince.starminer.mixin.client;

import com.qince.starminer.gravity.GravityFrame;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectGravityMixin {
    @Redirect(method = "getOverlayBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/BlockPos$MutableBlockPos;set(DDD)Lnet/minecraft/core/BlockPos$MutableBlockPos;"))
    private static BlockPos.MutableBlockPos starminer$headSamples(BlockPos.MutableBlockPos pos,
                                                                 double x, double y, double z, Player player) {
        var direction = GravityFrame.direction(player);
        if (direction.isVanilla()) return pos.set(x, y, z);
        Vec3 local = new Vec3(x - player.getX(), y - player.getEyeY(), z - player.getZ());
        Vec3 sample = player.getEyePosition().add(GravityFrame.toWorld(direction, local));
        return pos.set(sample.x, sample.y, sample.z);
    }
}
