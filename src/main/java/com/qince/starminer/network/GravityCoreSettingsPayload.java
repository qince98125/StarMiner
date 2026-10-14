package com.qince.starminer.network;

import com.qince.starminer.content.blockentity.GravityCoreBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client to server request that changes a Gravity Core's settings.
 *
 * <p>All values are re-clamped against the config on the server, so a tampered client cannot widen a
 * field beyond {@code maxGravityRange} or grow a star beyond {@code maxStarRadius}.
 */
public record GravityCoreSettingsPayload(BlockPos pos, int gravityRange, int starRadius, int fieldType,
                                         boolean useBufferArea, int speedIndex)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<GravityCoreSettingsPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(com.qince.starminer.StarMiner.MODID, "gravitycore_settings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GravityCoreSettingsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    GravityCoreSettingsPayload::pos,
                    ByteBufCodecs.VAR_INT,
                    GravityCoreSettingsPayload::gravityRange,
                    ByteBufCodecs.VAR_INT,
                    GravityCoreSettingsPayload::starRadius,
                    ByteBufCodecs.VAR_INT,
                    GravityCoreSettingsPayload::fieldType,
                    ByteBufCodecs.BOOL,
                    GravityCoreSettingsPayload::useBufferArea,
                    ByteBufCodecs.VAR_INT,
                    GravityCoreSettingsPayload::speedIndex,
                    GravityCoreSettingsPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Applies the request, but only when the sender actually owns the block entity it names. */
    public static void handle(GravityCoreSettingsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!(player.level().getBlockEntity(payload.pos()) instanceof GravityCoreBlockEntity core)) {
                return;
            }
            // Guards against a client naming somebody else's core.
            if (!core.stillValid(player)) {
                return;
            }
            core.applySettings(payload.gravityRange(), payload.starRadius(), payload.fieldType(),
                    payload.useBufferArea(), payload.speedIndex());
        });
    }
}
