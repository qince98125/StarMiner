package com.qince.starminer.mixin;

import java.util.Optional;
import com.qince.starminer.gravity.GravityCollision;
import com.qince.starminer.gravity.GravityFrame;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep vanilla move's fall damage, sounds, block effects and ground flags, using local components. */
@Mixin(Entity.class)
public abstract class EntityGravityMixin {
    @Shadow public Optional<BlockPos> mainSupportingBlockPos;
    @Unique private com.qince.starminer.api.GravityDirection starminer$lastDirection;

    @Inject(method = "baseTick", at = @At("HEAD"))
    private void starminer$transition(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof net.minecraft.world.entity.player.Player)) return;
        var next = GravityFrame.direction(self);
        if (starminer$lastDirection != null && starminer$lastDirection != next) {
            Vec3 center = self.getBoundingBox().getCenter();
            Vec3 feet = center.add(next.vector().scale(self.getBbHeight() / 2));
            for (double shift = 0; shift <= self.getBbHeight(); shift += 0.1) {
                Vec3 candidate = feet.subtract(next.vector().scale(shift));
                if (self.level().noCollision(self, GravityFrame.box(next, candidate, self.getBbWidth(), self.getBbHeight()).deflate(1.0E-7))) {
                    feet = candidate;
                    break;
                }
            }
            self.setPos(feet);
            self.setOnGround(false);
            self.resetFallDistance();
        } else if (starminer$lastDirection == null) {
            self.setPos(self.position());
        }
        starminer$lastDirection = next;
    }

    @Inject(method = "makeBoundingBox", at = @At("HEAD"), cancellable = true)
    private void starminer$box(CallbackInfoReturnable<AABB> ci) {
        Entity self = (Entity) (Object) this;
        var direction = GravityFrame.direction(self);
        if (!direction.isVanilla()) ci.setReturnValue(GravityFrame.box(direction, self.position(), self.getBbWidth(), self.getBbHeight()));
    }

    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true)
    private Vec3 starminer$localMove(Vec3 world) {
        Entity self = (Entity) (Object) this;
        GravityCollision.correctSupportContact(self);
        return GravityFrame.toLocal(GravityFrame.direction(self), world);
    }

    @Inject(method = "collide", at = @At("HEAD"), cancellable = true)
    private void starminer$collide(Vec3 local, CallbackInfoReturnable<Vec3> ci) {
        Entity self = (Entity) (Object) this;
        if (!GravityFrame.direction(self).isVanilla()) ci.setReturnValue(GravityCollision.collide(self, local));
    }

    @Redirect(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setPos(DDD)V"))
    private void starminer$worldPosition(Entity self, double x, double y, double z) {
        Vec3 delta = GravityFrame.toWorld(GravityFrame.direction(self), new Vec3(x - self.getX(), y - self.getY(), z - self.getZ()));
        self.setPos(self.position().add(delta));
    }

    @Redirect(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getDeltaMovement()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 starminer$localVelocity(Entity self) {
        return GravityFrame.toLocal(GravityFrame.direction(self), self.getDeltaMovement());
    }

    @Redirect(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private void starminer$worldVelocity(Entity self, Vec3 local) {
        self.setDeltaMovement(GravityFrame.toWorld(GravityFrame.direction(self), local));
    }

    @Redirect(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;clip(Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;"))
    private net.minecraft.world.phys.BlockHitResult starminer$fallClip(Level level, net.minecraft.world.level.ClipContext context) {
        Entity self = (Entity) (Object) this;
        var direction = GravityFrame.direction(self);
        if (direction.isVanilla()) return level.clip(context);
        Vec3 end = context.getFrom().add(GravityFrame.toWorld(direction, context.getTo().subtract(context.getFrom())));
        return level.clip(new net.minecraft.world.level.ClipContext(context.getFrom(), end,
                net.minecraft.world.level.ClipContext.Block.FALLDAMAGE_RESETTING, net.minecraft.world.level.ClipContext.Fluid.WATER, self));
    }

    @Redirect(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;updateEntityAfterFallOn(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;)V"))
    private void starminer$landing(Block block, net.minecraft.world.level.BlockGetter level, Entity self) {
        var direction = GravityFrame.direction(self);
        // Slime/bed landing callbacks operate on Y. Expose local velocity only during this callback.
        self.setDeltaMovement(GravityFrame.toLocal(direction, self.getDeltaMovement()));
        try {
            block.updateEntityAfterFallOn(level, self);
        } finally {
            self.setDeltaMovement(GravityFrame.toWorld(direction, self.getDeltaMovement()));
        }
    }

    @Inject(method = "checkSupportingBlock", at = @At("HEAD"), cancellable = true)
    private void starminer$support(boolean grounded, Vec3 movement, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        var direction = GravityFrame.direction(self);
        if (direction.isVanilla()) return;
        AABB box = self.getBoundingBox();
        Vec3 pull = direction.vector().scale(1.0E-6);
        this.mainSupportingBlockPos = grounded
                ? self.level().findSupportingBlock(self, box.move(pull).deflate(1.0E-7)) : Optional.empty();
        ci.cancel();
    }

    @Inject(method = "getOnPos(F)Lnet/minecraft/core/BlockPos;", at = @At("HEAD"), cancellable = true)
    private void starminer$floor(float offset, CallbackInfoReturnable<BlockPos> ci) {
        Entity self = (Entity) (Object) this;
        var direction = GravityFrame.direction(self);
        if (!direction.isVanilla()) ci.setReturnValue(mainSupportingBlockPos.orElseGet(() ->
                BlockPos.containing(self.position().add(direction.vector().scale(offset)))));
    }

    @Inject(method = "calculateViewVector", at = @At("RETURN"), cancellable = true)
    private void starminer$look(float pitch, float yaw, CallbackInfoReturnable<Vec3> ci) {
        ci.setReturnValue(GravityFrame.toWorld(GravityFrame.direction((Entity) (Object) this), ci.getReturnValue()));
    }

    @Inject(method = "getEyePosition()Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void starminer$eye(CallbackInfoReturnable<Vec3> ci) {
        Entity self = (Entity) (Object) this;
        var direction = GravityFrame.direction(self);
        if (!direction.isVanilla()) ci.setReturnValue(self.position().add(GravityFrame.toWorld(direction, new Vec3(0, self.getEyeHeight(), 0))));
    }

    @Inject(method = "getEyePosition(F)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void starminer$interpolatedEye(float tick, CallbackInfoReturnable<Vec3> ci) {
        Entity self = (Entity) (Object) this;
        var direction = GravityFrame.direction(self);
        if (!direction.isVanilla()) {
            Vec3 feet = new Vec3(Mth.lerp(tick, self.xo, self.getX()), Mth.lerp(tick, self.yo, self.getY()), Mth.lerp(tick, self.zo, self.getZ()));
            ci.setReturnValue(feet.add(GravityFrame.toWorld(direction, new Vec3(0, self.getEyeHeight(), 0))));
        }
    }
}
