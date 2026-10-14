package com.qince.starminer.registry;

import com.qince.starminer.StarMiner;
import com.qince.starminer.content.menu.GravityCoreMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Menu registration. */
public final class SMMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, StarMiner.MODID);

    /** The factory must be an {@link IContainerFactory} so the block position travels with the open packet. */
    private static final IContainerFactory<GravityCoreMenu> GRAVITY_CORE_FACTORY = GravityCoreMenu::new;

    public static final DeferredHolder<MenuType<?>, MenuType<GravityCoreMenu>> GRAVITY_CORE =
            MENUS.register("gravitycore", () -> new MenuType<>(GRAVITY_CORE_FACTORY, FeatureFlags.DEFAULT_FLAGS));

    private SMMenus() {
    }
}
