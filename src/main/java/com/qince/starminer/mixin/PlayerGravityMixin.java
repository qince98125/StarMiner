package com.qince.starminer.mixin;

import com.qince.starminer.gravity.GravityFrame;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerGravityMixin {
    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;horizontalDistance()D"))
    private double starminer$walkingBob(Vec3 worldVelocity) {
        return GravityFrame.toLocal(GravityFrame.direction((Player) (Object) this), worldVelocity).horizontalDistance();
    }

    @Inject(method = "canPlayerFitWithinBlocksAndEntitiesWhen", at = @At("HEAD"), cancellable = true)
    private void starminer$pose(net.minecraft.world.entity.Pose pose, CallbackInfoReturnable<Boolean> ci) {
        Player self = (Player) (Object) this;
        var direction = GravityFrame.direction(self);
        if (direction.isVanilla()) return;
        var dimensions = self.getDimensions(pose);
        ci.setReturnValue(self.level().noCollision(self,
                GravityFrame.box(direction, self.position(), dimensions.width(), dimensions.height()).deflate(1.0E-7)));
    }

    @Inject(method = "maybeBackOffFromEdge", at = @At("HEAD"), cancellable = true)
    private void starminer$sneak(Vec3 local, MoverType mover, CallbackInfoReturnable<Vec3> ci) {
        Player self = (Player) (Object) this;
        var direction = GravityFrame.direction(self);
        if (direction.isVanilla()) return;
        double x = local.x;
        double z = local.z;
        if (self.isShiftKeyDown() && self.onGround() && local.y <= 0
                && (mover == MoverType.SELF || mover == MoverType.PLAYER)) {
            double depth = Math.max(self.maxUpStep(), 0.05);
            while (x != 0 && self.level().noCollision(self, self.getBoundingBox().move(GravityFrame.toWorld(direction, new Vec3(x, -depth, 0)))))
                x = Math.abs(x) <= 0.05 ? 0 : x - Math.copySign(0.05, x);
            while (z != 0 && self.level().noCollision(self, self.getBoundingBox().move(GravityFrame.toWorld(direction, new Vec3(0, -depth, z)))))
                z = Math.abs(z) <= 0.05 ? 0 : z - Math.copySign(0.05, z);
            while (x != 0 && z != 0 && self.level().noCollision(self, self.getBoundingBox().move(GravityFrame.toWorld(direction, new Vec3(x, -depth, z))))) {
                x = Math.abs(x) <= 0.05 ? 0 : x - Math.copySign(0.05, x);
                z = Math.abs(z) <= 0.05 ? 0 : z - Math.copySign(0.05, z);
            }
        }
        ci.setReturnValue(new Vec3(x, local.y, z));
    }
}
