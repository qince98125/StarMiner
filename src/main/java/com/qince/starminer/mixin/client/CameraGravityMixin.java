package com.qince.starminer.mixin.client;

import com.qince.starminer.gravity.GravityFrame;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraGravityMixin {
    @Shadow private Entity entity;
    @Shadow @Final private Quaternionf rotation;
    @Shadow @Final private Vector3f forwards;
    @Shadow @Final private Vector3f up;
    @Shadow @Final private Vector3f left;
    @Shadow private float eyeHeight;
    @Shadow private float eyeHeightOld;
    @Shadow private float partialTickTime;
    @Shadow protected abstract void setPosition(double x, double y, double z);

    @Inject(method = "setRotation(FFF)V", at = @At("TAIL"))
    private void starminer$rotation(float yaw, float pitch, float roll, CallbackInfo ci) {
        if (entity == null) return;
        var direction = GravityFrame.direction(entity);
        if (direction.isVanilla()) return;
        rotation.premul(GravityFrame.rotation(direction));
        rotation.transform(new Vector3f(0, 0, -1), forwards);
        rotation.transform(new Vector3f(0, 1, 0), up);
        rotation.transform(new Vector3f(-1, 0, 0), left);
    }

    @Inject(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V", shift = At.Shift.AFTER))
    private void starminer$eye(net.minecraft.world.level.BlockGetter level, Entity entity, boolean detached,
                               boolean reverse, float tick, CallbackInfo ci) {
        var direction = GravityFrame.direction(entity);
        if (direction.isVanilla()) return;
        var offset = GravityFrame.toWorld(direction, new net.minecraft.world.phys.Vec3(0, Mth.lerp(tick, eyeHeightOld, eyeHeight), 0));
        setPosition(Mth.lerp(tick, entity.xo, entity.getX()) + offset.x,
                Mth.lerp(tick, entity.yo, entity.getY()) + offset.y,
                Mth.lerp(tick, entity.zo, entity.getZ()) + offset.z);
    }
}
