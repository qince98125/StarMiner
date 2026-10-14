package com.qince.starminer.registry;

import com.qince.starminer.api.GravityDirection;
import com.qince.starminer.api.GravityState;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** NBT round trip for {@link GravityState}. */
public final class GravityStateNbt {

    private static final String NBT_DIRECTION = "stmn.gdir";
    private static final String NBT_FIELD = "stmn.gfield";

    public static CompoundTag write(GravityState state) {
        CompoundTag tag = new CompoundTag();
        tag.putString(NBT_DIRECTION, state.direction().name());
        if (state.fieldPos() != null) {
            tag.putLong(NBT_FIELD, state.fieldPos().asLong());
        }
        return tag;
    }

    public static GravityState read(CompoundTag tag) {
        GravityDirection direction = GravityState.DEFAULT.direction();
        if (tag.contains(NBT_DIRECTION, Tag.TAG_STRING)) {
            try {
                direction = GravityDirection.valueOf(tag.getString(NBT_DIRECTION));
            } catch (IllegalArgumentException ignored) {
                // An unknown name means a save from a future revision; ordinary gravity is a safe default.
            }
        }
        BlockPos field = tag.contains(NBT_FIELD, Tag.TAG_LONG) ? BlockPos.of(tag.getLong(NBT_FIELD)) : null;
        return new GravityState(direction, field);
    }

    private GravityStateNbt() {
    }
}
