package com.qince.starminer.registry;

import com.qince.starminer.StarMiner;
import com.qince.starminer.gravity.GravityFieldManager;

import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Data attachment registration. */
public final class SMAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, StarMiner.MODID);

    /**
     * Per-level index of every Gravity Core, so field lookups do not scan blocks.
     *
     * <p>The default value is built from the holder so the manager knows which level it belongs to. The
     * lambda is typed explicitly because {@code serializable} is overloaded on {@code Supplier} and
     * {@code Function}.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<GravityFieldManager>> GRAVITY_FIELDS =
            ATTACHMENT_TYPES.register("gravity_fields",
                    () -> AttachmentType.<CompoundTag, GravityFieldManager>serializable(
                            (IAttachmentHolder holder) -> new GravityFieldManager(
                                    holder instanceof net.minecraft.world.level.Level level ? level : null))
                            .build());

    private SMAttachments() {
    }
}
