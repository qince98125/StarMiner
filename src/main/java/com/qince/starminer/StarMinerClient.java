package com.qince.starminer;

import com.qince.starminer.client.GravityHud;
import com.qince.starminer.client.GravityOrientationHandler;
import com.qince.starminer.client.SMClientSetup;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only entrypoint. Never loaded on a dedicated server. */
@Mod(value = StarMiner.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = StarMiner.MODID, value = Dist.CLIENT)
public class StarMinerClient {
    public StarMinerClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        // HUD layers live on the game event bus.
        NeoForge.EVENT_BUS.register(GravityHud.class);
        // Camera roll and third-person model orientation also live on the game event bus.
        NeoForge.EVENT_BUS.register(GravityOrientationHandler.class);
        if (Boolean.getBoolean("starminer.verifyCamera"))
            NeoForge.EVENT_BUS.register(com.qince.starminer.gametest.GravityClientChecks.class);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Screens are registered through RegisterMenuScreensEvent below.
    }

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        SMClientSetup.onRegisterMenuScreens(event);
    }
}
