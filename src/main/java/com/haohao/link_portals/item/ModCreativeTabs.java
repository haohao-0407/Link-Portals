package com.haohao.link_portals.item;

import com.haohao.link_portals.LinkPortals;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LinkPortals.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> LINK_PORTALS_TAB =
            CREATIVE_MODE_TABS.register("link_portals_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.link_portals"))
                    .icon(() -> new ItemStack(ModItems.PORTAL_ACTIVATOR.get()))
                    .displayItems((parameters, output) -> {

                        output.accept(ModItems.PORTAL_ACTIVATOR.get());
                    })
                    .build());
}
