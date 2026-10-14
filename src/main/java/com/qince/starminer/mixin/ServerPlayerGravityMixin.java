package com.qince.starminer.mixin;

import com.qince.starminer.gravity.GravityFrame;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerGravityMixin {
    @ModifyVariable(method = "doCheckFallDamage", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private double starminer$fall(double original, double x, double y, double z, boolean grounded) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        double localY = GravityFrame.toLocal(GravityFrame.direction(self), new Vec3(x, y, z)).y;
        if (localY > 0) self.resetFallDistance();
        return localY;
    }
}
