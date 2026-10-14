package com.qince.starminer.network;

import com.qince.starminer.StarMiner;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Payload registration. */
public final class SMNetwork {

    /** Bumped whenever a payload's layout changes, so mismatched clients are rejected early. */
    private static final String PROTOCOL_VERSION = "2";

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(
                GravityCoreSettingsPayload.TYPE,
                GravityCoreSettingsPayload.STREAM_CODEC,
                GravityCoreSettingsPayload::handle);
        StarMiner.LOGGER.debug("Registered starminer payloads at protocol version {}", PROTOCOL_VERSION);
    }

    private SMNetwork() {
    }
}
