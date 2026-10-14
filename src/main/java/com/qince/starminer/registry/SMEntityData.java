package com.qince.starminer.registry;

import com.qince.starminer.StarMiner;
import com.qince.starminer.api.GravityState;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Per-entity data attachments. */
public final class SMEntityData {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, StarMiner.MODID);

    /** Gravity is sent as the direction name plus an optional field position. */
    private static final StreamCodec<RegistryFriendlyByteBuf, GravityState> GRAVITY_STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.map(GravityDirectionCodec::parse, dir -> dir.name()),
                    GravityState::direction,
                    ByteBufCodecs.optional(net.minecraft.core.BlockPos.STREAM_CODEC),
                    state -> java.util.Optional.ofNullable(state.fieldPos()),
                    (direction, fieldPos) -> new GravityState(direction, fieldPos.orElse(null)));

    /**
     * The gravity an entity currently experiences. Persisted, and synced to the owner and tracking
     * players so remote collision boxes and third-person poses use the same direction.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<GravityState>> GRAVITY_STATE =
            ATTACHMENT_TYPES.register("gravity_state",
                    () -> AttachmentType.builder(() -> GravityState.DEFAULT)
                            .serialize(new GravityStateSerializer())
                            .sync(GRAVITY_STREAM_CODEC)
                            .build());

    /** NBT serializer for {@link GravityState}. */
    private static final class GravityStateSerializer
            implements net.neoforged.neoforge.attachment.IAttachmentSerializer<CompoundTag, GravityState> {
        @Override
        public GravityState read(IAttachmentHolder holder, CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
            return GravityStateNbt.read(tag);
        }

        @Override
        public CompoundTag write(GravityState attachment, net.minecraft.core.HolderLookup.Provider provider) {
            return GravityStateNbt.write(attachment);
        }
    }

    /** Wraps the enum so a stream codec can be composed from a string. */
    private static final class GravityDirectionCodec {
        static com.qince.starminer.api.GravityDirection parse(String name) {
            try {
                return com.qince.starminer.api.GravityDirection.valueOf(name);
            } catch (IllegalArgumentException ignored) {
                return GravityState.DEFAULT.direction();
            }
        }
    }

    /** Keeps the unused import of {@link ServerPlayer} meaningful for readers of the sync predicate. */
    private SMEntityData() {
    }
}
