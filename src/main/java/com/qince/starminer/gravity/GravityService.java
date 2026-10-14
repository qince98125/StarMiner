package com.qince.starminer.gravity;

import com.qince.starminer.Config;
import com.qince.starminer.StarMiner;
import com.qince.starminer.api.GravityDirection;
import com.qince.starminer.api.GravityHelper;
import com.qince.starminer.api.GravityState;
import com.qince.starminer.registry.SMEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Resolve before physics. Positive gravity strength is applied in the local frame. */
public final class GravityService {
    public static final ResourceLocation GRAVITY_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(StarMiner.MODID, "field_gravity");

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LivingEntity living) || living.level().isClientSide()) return;
        // Remove modifiers left by the earlier implementation, including from mobs.
        var gravity = living.getAttribute(Attributes.GRAVITY);
        if (gravity != null && gravity.getModifier(GRAVITY_MODIFIER_ID) != null) gravity.removeModifier(GRAVITY_MODIFIER_ID);
        if (!(living instanceof Player) || !Config.APPLY_GRAVITY_ATTRIBUTE.getAsBoolean()) {
            if (living.hasData(SMEntityData.GRAVITY_STATE)
                    && !living.getData(SMEntityData.GRAVITY_STATE).equals(GravityState.DEFAULT))
                living.setData(SMEntityData.GRAVITY_STATE, GravityState.DEFAULT);
            return;
        }
        GravityState previous = living.getData(SMEntityData.GRAVITY_STATE);
        GravityDirection current = previous.inField() ? previous.direction() : null;
        var sample = GravityHelper.resolveSamplePosition(living, current);
        var next = GravityFieldManager.of(living.level()).resolveField(sample)
                .map(field -> new GravityState(field.directionAt(sample, current, field.useBuffer()), field.center()))
                .orElse(GravityState.DEFAULT);
        if (!next.equals(previous)) living.setData(SMEntityData.GRAVITY_STATE, next);
    }

    public static void clear(LivingEntity living) {
        var gravity = living.getAttribute(Attributes.GRAVITY);
        if (gravity != null) gravity.removeModifier(GRAVITY_MODIFIER_ID);
        living.setData(SMEntityData.GRAVITY_STATE, GravityState.DEFAULT);
    }
    private GravityService() {}
}
