package com.qince.starminer.gravity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.qince.starminer.api.GravityField;
import com.qince.starminer.content.blockentity.GravityCoreBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;

/**
 * Index of every Gravity Core in a level, keyed by chunk.
 *
 * <p>Field parameters are always read back from the live block entity rather than cached, so a core
 * whose radius or shape changed is reflected immediately. Only positions are indexed, which keeps the
 * manager cheap to rebuild and impossible to go stale.
 */
public class GravityFieldManager implements INBTSerializable<CompoundTag> {

    private static final String NBT_FIELDS = "stmn.fields";

    /** Constructor required by the attachment type; the holder is filled in by {@link #setLevel}. */
    public GravityFieldManager() {
        this(null);
    }

    public GravityFieldManager(@Nullable Level level) {
        this.level = level;
    }

    private Level level;
    private final Map<Long, List<BlockPos>> positionsByChunk = new HashMap<>();

    public void setLevel(Level level) {
        this.level = level;
    }

    public boolean isEmpty() {
        return this.positionsByChunk.isEmpty();
    }

    public int size() {
        return this.positionsByChunk.values().stream().mapToInt(List::size).sum();
    }

    // ------------------------------------------------------------------ index maintenance

    public void add(BlockPos pos) {
        long key = chunkKey(pos.getX() >> 4, pos.getZ() >> 4);
        List<BlockPos> list = this.positionsByChunk.computeIfAbsent(key, k -> new ArrayList<>(2));
        if (!list.contains(pos)) {
            list.add(pos);
        }
    }

    public void remove(BlockPos pos) {
        long key = chunkKey(pos.getX() >> 4, pos.getZ() >> 4);
        List<BlockPos> list = this.positionsByChunk.get(key);
        if (list != null) {
            list.remove(pos);
            if (list.isEmpty()) {
                this.positionsByChunk.remove(key);
            }
        }
    }

    public void clear() {
        this.positionsByChunk.clear();
    }

    /**
     * The field that governs {@code at}, or empty when the position is not inside any field.
     *
     * <p>Overlapping fields are resolved deterministically: the core closest to {@code at} wins, with
     * the block position breaking ties.
     */
    public Optional<GravityField> resolveField(Vec3 at) {
        Level level = this.level;
        if (level == null) {
            return Optional.empty();
        }

        int chunkX = ((int) Math.floor(at.x)) >> 4;
        int chunkZ = ((int) Math.floor(at.z)) >> 4;
        int chunkRadius = (com.qince.starminer.Config.MAX_GRAVITY_RANGE.get() + 15) / 16;

        GravityField best = null;
        double bestScore = Double.MAX_VALUE;
        int bestX = 0;
        int bestY = 0;
        int bestZ = 0;

        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                List<BlockPos> list = this.positionsByChunk.get(chunkKey(chunkX + dx, chunkZ + dz));
                if (list == null) {
                    continue;
                }
                for (BlockPos pos : list) {
                    GravityField field = fieldAt(level, pos);
                    if (field == null || !field.isActive() || !field.contains(at)) {
                        continue;
                    }
                    Vec3 center = field.centerVec();
                    double score = at.distanceToSqr(center);
                    int px = pos.getX();
                    int py = pos.getY();
                    int pz = pos.getZ();
                    if (score < bestScore
                            || (score == bestScore
                                    && (px < bestX || (px == bestX && (py < bestY || (py == bestY && pz < bestZ)))))) {
                        best = field;
                        bestScore = score;
                        bestX = px;
                        bestY = py;
                        bestZ = pz;
                    }
                }
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * The live field projected by the core at {@code pos}, or null when there is none.
     *
     * <p>Queries always go through this check, so an index entry left behind by a removed core simply
     * reports nothing rather than needing eager cleanup.
     */
    @Nullable
    public static GravityField fieldAt(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return null;
        if (level.getBlockEntity(pos) instanceof GravityCoreBlockEntity core) {
            GravityField field = core.field();
            return field.isActive() ? field : null;
        }
        return null;
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    // ------------------------------------------------------------------ persistence

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        long[] packed = this.positionsByChunk.values().stream()
                .flatMap(List::stream)
                .mapToLong(BlockPos::asLong)
                .toArray();
        tag.put(NBT_FIELDS, new LongArrayTag(packed));
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.clear();
        for (long packed : tag.getLongArray(NBT_FIELDS)) {
            this.add(BlockPos.of(packed));
        }
    }

    /** Returns the manager attached to a level, creating and binding it on first use. */
    public static GravityFieldManager of(Level level) {
        GravityFieldManager manager = level.getData(
                com.qince.starminer.registry.SMAttachments.GRAVITY_FIELDS);
        manager.setLevel(level);
        return manager;
    }
}
