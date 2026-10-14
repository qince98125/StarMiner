package com.qince.starminer.registry;

import com.qince.starminer.StarMiner;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Creative tab registration. */
public final class SMCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, StarMiner.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> STARMiner_TAB = CREATIVE_TABS.register(
            "starminer",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.starminer"))
                    .icon(() -> SMItems.GRAVITY_CORE.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(SMItems.GRAVITY_CORE.get());
                        output.accept(SMItems.STARCORE_DUST.get());
                        output.accept(SMItems.EARTH_STONE.get());
                    })
                    .build());

    private SMCreativeTabs() {
    }
}
