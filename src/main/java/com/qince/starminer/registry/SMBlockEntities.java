package com.qince.starminer.registry;

import com.qince.starminer.StarMiner;
import com.qince.starminer.content.blockentity.GravityCoreBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Block entity registration. */
public final class SMBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, StarMiner.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GravityCoreBlockEntity>> GRAVITY_CORE =
            BLOCK_ENTITIES.register("gravitycore", () -> BlockEntityType.Builder
                    .of(GravityCoreBlockEntity::new, SMBlocks.GRAVITY_CORE.get())
                    .build(null));

    private SMBlockEntities() {
    }
}
