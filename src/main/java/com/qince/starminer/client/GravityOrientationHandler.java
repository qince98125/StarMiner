package com.qince.starminer.client;

import com.qince.starminer.gravity.GravityFrame;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/** The body uses the same frame and feet pivot as physics. Mouse angles stay local. */
public final class GravityOrientationHandler {
    private static final java.util.Set<Integer> TRANSFORMED = new java.util.HashSet<>();

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        var direction = GravityFrame.direction(event.getEntity());
        if (direction.isVanilla()) return;
        event.getPoseStack().pushPose();
        event.getPoseStack().mulPose(GravityFrame.rotation(direction));
        TRANSFORMED.add(event.getEntity().getId());
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (TRANSFORMED.remove(event.getEntity().getId())) event.getPoseStack().popPose();
    }

    @SubscribeEvent
    public static void onLogout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        TRANSFORMED.clear();
    }

    private GravityOrientationHandler() {}
}
