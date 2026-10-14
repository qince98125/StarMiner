package com.qince.starminer.api;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * The gravity an entity is currently subject to, synced from server to client.
 *
 * @param direction the direction gravity pulls this entity
 * @param fieldPos  the core projecting the field, or null when outside every field
 */
public record GravityState(GravityDirection direction, @Nullable BlockPos fieldPos) {

    /** Ordinary gravity, no field involvement. */
    public static final GravityState DEFAULT = new GravityState(GravityDirection.UP_TO_DOWN_Y_N, null);

    public boolean inField() {
        return this.fieldPos != null;
    }

    public boolean isVanilla() {
        return !inField() || this.direction.isVanilla();
    }

    public GravityState withField(BlockPos pos, GravityDirection dir) {
        return new GravityState(dir, pos);
    }
}
