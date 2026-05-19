package com.haohao.link_portals.block;

import com.haohao.link_portals.LinkPortals;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(LinkPortals.MODID);

    public static final DeferredBlock<PortalFrameBlock> PORTAL_FRAME = BLOCKS.registerBlock("portal_frame",
            props -> new PortalFrameBlock(props.strength(3.0F).requiresCorrectToolForDrops()));

    public static final DeferredBlock<PortalBlock> PORTAL = BLOCKS.registerBlock("portal",
            props -> new PortalBlock(props.noCollision().noOcclusion().lightLevel(s -> 11)));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
