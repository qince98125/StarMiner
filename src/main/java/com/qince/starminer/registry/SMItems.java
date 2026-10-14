package com.qince.starminer.registry;

import com.qince.starminer.StarMiner;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Item registration. */
public final class SMItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(StarMiner.MODID);

    /** "Stardust" - the generic crafting/terraform material of the original mod. */
    public static final DeferredItem<Item> STARCORE_DUST = ITEMS.registerSimpleItem("starcore_dust");

    /** "Earth Stone" - the non-floating building material. */
    public static final DeferredItem<Item> EARTH_STONE = ITEMS.registerSimpleItem("earthstone");

    public static final DeferredItem<BlockItem> GRAVITY_CORE = ITEMS.registerSimpleBlockItem("gravitycore", SMBlocks.GRAVITY_CORE);

    private SMItems() {
    }
}
