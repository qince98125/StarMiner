package com.qince.starminer.gravity;

import com.qince.starminer.content.blockentity.GravityCoreBlockEntity;
import com.qince.starminer.registry.SMBlocks;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

/**
 * Keeps {@link GravityFieldManager} in sync with the world.
 *
 * <p>Three sources of truth are covered: chunks as they load, cores as they are placed, and cores as
 * they are broken. Because queries fall back to the live block entity an occasional stale entry is
 * harmless, but the index is kept clean anyway so lookups stay cheap.
 */
public final class GravityEventHandler {

    @SubscribeEvent
    public static void onEntityPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide()
                && event.getPlacedBlock().is(SMBlocks.GRAVITY_CORE.get())) {
            GravityFieldManager.of(level).add(event.getPos());
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide()
                && event.getState().is(SMBlocks.GRAVITY_CORE.get())) {
            GravityFieldManager.of(level).remove(event.getPos());
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()
                || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        GravityFieldManager manager = GravityFieldManager.of(level);
        for (BlockEntity be : chunk.getBlockEntities().values()) {
            if (be instanceof GravityCoreBlockEntity) {
                manager.add(be.getBlockPos());
            }
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()
                || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        GravityFieldManager manager = GravityFieldManager.of(level);
        for (BlockEntity be : chunk.getBlockEntities().values()) {
            if (be instanceof GravityCoreBlockEntity) {
                manager.remove(be.getBlockPos());
            }
        }
    }

    private GravityEventHandler() {
    }
}
