package com.qince.starminer.mixin;

import com.qince.starminer.Config;
import com.qince.starminer.gravity.GravityFrame;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityGravityMixin {
    @Shadow protected abstract float getJumpPower();
    @Shadow protected abstract float getFlyingSpeed();

    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;tickHeadTurn(FF)F"), index = 0)
    private float starminer$bodyYaw(float original) {
        LivingEntity self = (LivingEntity) (Object) this;
        var direction = GravityFrame.direction(self);
        if (direction.isVanilla()) return original;
        Vec3 delta = GravityFrame.toLocal(direction, self.position().subtract(self.xo, self.yo, self.zo));
        if (self.attackAnim > 0) return self.getYRot();
        if (delta.horizontalDistanceSqr() <= 0.0025000002) return self.yBodyRot;
        float facing = (float)Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90;
        float difference = net.minecraft.util.Mth.abs(net.minecraft.util.Mth.wrapDegrees(self.getYRot()) - facing);
        return difference > 95 && difference < 265 ? facing - 180 : facing;
    }

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void starminer$jump(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        var direction = GravityFrame.direction(self);
        if (direction.isVanilla()) return;
        float power = getJumpPower();
        if (power > 1.0E-5F) {
            Vec3 v = GravityFrame.toLocal(direction, self.getDeltaMovement());
            v = new Vec3(v.x, power, v.z);
            if (self.isSprinting()) {
                double yaw = Math.toRadians(self.getYRot());
                v = v.add(-Math.sin(yaw) * 0.2, 0, Math.cos(yaw) * 0.2);
            }
            self.setDeltaMovement(GravityFrame.toWorld(direction, v));
            self.hasImpulse = true;
            net.neoforged.neoforge.common.CommonHooks.onLivingJump(self);
        }
        ci.cancel();
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void starminer$travel(Vec3 input, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        var direction = GravityFrame.direction(self);
        if (direction.isVanilla()) return;
        if (self.isControlledByLocalInstance()) {
            var floor = self.getBlockPosBelowThatAffectsMyMovement();
            float friction = self.level().getBlockState(floor).getFriction(self.level(), floor, self);
            float drag = self.onGround() ? friction * 0.91F : 0.91F;
            float speed = self.onGround() ? self.getSpeed() * (0.21600002F / (friction * friction * friction)) : getFlyingSpeed();
            Vec3 acceleration = input.lengthSqr() > 1 ? input.normalize() : input;
            acceleration = acceleration.scale(speed);
            double yaw = Math.toRadians(self.getYRot());
            Vec3 local = new Vec3(acceleration.x * Math.cos(yaw) - acceleration.z * Math.sin(yaw), acceleration.y,
                    acceleration.z * Math.cos(yaw) + acceleration.x * Math.sin(yaw));
            self.setDeltaMovement(self.getDeltaMovement().add(GravityFrame.toWorld(direction, local)));
            self.move(MoverType.SELF, self.getDeltaMovement());
            Vec3 velocity = GravityFrame.toLocal(direction, self.getDeltaMovement());
            double g = self.isNoGravity() ? 0 : self.getAttributeValue(Attributes.GRAVITY);
            g = direction.isSideways() ? (self.isNoGravity() ? 0 : Config.FIELD_GRAVITY_STRENGTH.getAsDouble())
                    : g * Config.FLIPPED_GRAVITY_MULTIPLIER.getAsDouble();
            if (self.hasEffect(MobEffects.SLOW_FALLING) && velocity.y <= 0) g = Math.min(g, 0.01);
            double y = velocity.y - g;
            if (self.hasEffect(MobEffects.LEVITATION)) {
                y = velocity.y + (0.05 * (self.getEffect(MobEffects.LEVITATION).getAmplifier() + 1) - velocity.y) * 0.2;
                self.resetFallDistance();
            }
            self.setDeltaMovement(GravityFrame.toWorld(direction, self.shouldDiscardFriction()
                    ? new Vec3(velocity.x, y, velocity.z) : new Vec3(velocity.x * drag, y * 0.98F, velocity.z * drag)));
        }
        self.calculateEntityAnimation(true);
        ci.cancel();
    }
}
