package com.qince.starminer.registry;

import com.qince.starminer.StarMiner;
import com.qince.starminer.content.block.GravityCoreBlock;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Block registration. */
public final class SMBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(StarMiner.MODID);

    public static final DeferredBlock<GravityCoreBlock> GRAVITY_CORE = BLOCKS.registerBlock(
            "gravitycore",
            GravityCoreBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(4.0F, 12.0F)
                    .sound(SoundType.METAL)
                    .lightLevel(state -> 7)
                    .requiresCorrectToolForDrops());

    private SMBlocks() {
    }
}
