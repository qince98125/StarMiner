package com.qince.starminer.mixin;

import com.qince.starminer.gravity.GravityFrame;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerMovementGravityMixin {
    @Shadow public ServerPlayer player;
    @Shadow private boolean clientIsFloating;
    @Shadow private double lastGoodX;
    @Shadow private double lastGoodY;
    @Shadow private double lastGoodZ;

    @Redirect(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;resetFallDistance()V"))
    private void starminer$resetFall(ServerPlayer player) {
        if (GravityFrame.direction(player).isVanilla()) player.resetFallDistance();
        // Non-vanilla jumps are reset by doCheckFallDamage using local Y.
    }

    @Redirect(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;jumpFromGround()V"))
    private void starminer$serverJump(ServerPlayer player) {
        var direction = GravityFrame.direction(player);
        var delta = player.position().subtract(lastGoodX, lastGoodY, lastGoodZ);
        if (direction.isVanilla() || GravityFrame.toLocal(direction, delta).y > 0) player.jumpFromGround();
    }

    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void starminer$floating(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        // The vanilla floating check only measures world Y. Keep collision/speed validation intact.
        if (!GravityFrame.direction(player).isVanilla()) clientIsFloating = false;
    }
}
