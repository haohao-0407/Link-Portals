package com.haohao.betterportals.block;

import com.haohao.betterportals.BetterPortals;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BetterPortals.MODID);

    public static final DeferredBlock<Block> PORTAL_FRAME = BLOCKS.registerSimpleBlock("portal_frame",
            props -> props.strength(3.0F).requiresCorrectToolForDrops());

    public static final DeferredBlock<PortalCoreBlock> PORTAL_CORE = BLOCKS.registerBlock("portal_core",
            props -> new PortalCoreBlock(props.strength(3.0F).requiresCorrectToolForDrops()));

    public static final DeferredBlock<PortalBlock> PORTAL = BLOCKS.registerBlock("portal",
            props -> new PortalBlock(props.noCollision().noOcclusion().lightLevel(s -> 11)));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
