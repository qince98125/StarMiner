package com.qince.starminer.mixin.client;

import com.qince.starminer.gravity.GravityFrame;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerGravityMixin {
    @Inject(method = "aiStep", at = @At("HEAD"))
    private void starminer$stableContact(CallbackInfo ci) {
        com.qince.starminer.gravity.GravityCollision.correctSupportContact((LocalPlayer) (Object) this);
    }

    @Inject(method = "sendPosition", at = @At("HEAD"), cancellable = true)
    private void starminer$isolatedProbe(CallbackInfo ci) {
        // Explicit development probes exercise full ticks without moving the real server player.
        if (Boolean.getBoolean("starminer.verifyCamera")
                && com.qince.starminer.gametest.GravityClientMotionChecks.isProbe((LocalPlayer) (Object) this)) ci.cancel();
    }

    @Inject(method = "suffocatesAt", at = @At("HEAD"), cancellable = true)
    private void starminer$actualBodySuffocation(net.minecraft.core.BlockPos pos, CallbackInfoReturnable<Boolean> ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (GravityFrame.direction(self).isVanilla()) return;
        var body = self.getBoundingBox();
        var column = new net.minecraft.world.phys.AABB(pos.getX(), body.minY, pos.getZ(),
                pos.getX() + 1, body.maxY, pos.getZ() + 1);
        // Vanilla's foot-corner columns extend outside a rotated body and into its floor.
        // Push out only for actual body penetration, never for touching a supporting face.
        ci.setReturnValue(column.intersects(body)
                && self.level().collidesWithSuffocatingBlock(self, column.intersect(body).deflate(1.0E-7)));
    }

    @Inject(method = "updateAutoJump", at = @At("HEAD"), cancellable = true)
    private void starminer$autoJump(float x, float z, CallbackInfo ci) {
        if (!GravityFrame.direction((LocalPlayer) (Object) this).isVanilla()) ci.cancel();
    }
}
