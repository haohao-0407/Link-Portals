package com.haohao.link_portals.item;

import com.haohao.link_portals.LinkPortals;
import com.haohao.link_portals.block.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LinkPortals.MODID);

    public static final DeferredItem<Item> ICE_ETHER = ITEMS.registerSimpleItem("ice_ether");
    public static final DeferredItem<Item> RAW_ICE_ETHER = ITEMS.registerSimpleItem("raw_ice_ether");
    public static final DeferredItem<Item> CARDBOARD = ITEMS.registerSimpleItem("material/cardboard");

    public static final DeferredItem<PortalActivatorItem> PORTAL_ACTIVATOR = ITEMS.registerItem("portal_activator",
            props -> new PortalActivatorItem(props.stacksTo(1)));

    public static final DeferredItem<BlockItem> PORTAL_FRAME = ITEMS.registerItem("portal_frame",
            props -> new BlockItem(ModBlocks.PORTAL_FRAME.get(), props));
    public static final DeferredItem<BlockItem> PORTAL_CORE = ITEMS.registerItem("portal_core",
            props -> new BlockItem(ModBlocks.PORTAL_CORE.get(), props));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
