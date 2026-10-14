package com.qince.starminer.client;

import com.qince.starminer.registry.SMMenus;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-only registrations. */
public final class SMClientSetup {

    public static void registerScreens() {
    }

    /** Called from the mod event bus. */
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(SMMenus.GRAVITY_CORE.get(), GravityCoreScreen::new);
    }

    private SMClientSetup() {
    }
}
