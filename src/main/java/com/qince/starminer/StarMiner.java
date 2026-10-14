package com.qince.starminer;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.qince.starminer.datagen.SMDataGenerators;
import com.qince.starminer.gravity.GravityEventHandler;
import com.qince.starminer.gravity.GravityService;
import com.qince.starminer.network.SMNetwork;
import com.qince.starminer.registry.SMAttachments;
import com.qince.starminer.registry.SMBlockEntities;
import com.qince.starminer.registry.SMBlocks;
import com.qince.starminer.registry.SMCreativeTabs;
import com.qince.starminer.registry.SMEntityData;
import com.qince.starminer.registry.SMItems;
import com.qince.starminer.registry.SMMenus;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * StarMiner for Minecraft 1.21.1 / NeoForge.
 *
 * <p>A clean-room reimplementation of the 1.7.10 mod "Starminer" (jp.mc.ancientred.starminer).
 * Only the gameplay behaviour was used as a specification; no original code or bytecode was copied.
 */
@Mod(StarMiner.MODID)
public class StarMiner {
    public static final String MODID = "starminer";
    public static final Logger LOGGER = LogUtils.getLogger();

    public StarMiner(IEventBus modEventBus, ModContainer modContainer) {
        SMBlocks.BLOCKS.register(modEventBus);
        SMItems.ITEMS.register(modEventBus);
        SMBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        SMMenus.MENUS.register(modEventBus);
        SMCreativeTabs.CREATIVE_TABS.register(modEventBus);
        SMAttachments.ATTACHMENT_TYPES.register(modEventBus);
        SMEntityData.ATTACHMENT_TYPES.register(modEventBus);

        // Game events: keeps the gravity field index in sync with the world, and applies gravity.
        NeoForge.EVENT_BUS.register(GravityEventHandler.class);
        NeoForge.EVENT_BUS.register(GravityService.class);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        modEventBus.addListener(SMNetwork::register);
        modEventBus.addListener(SMDataGenerators::gatherData);
    }
}
