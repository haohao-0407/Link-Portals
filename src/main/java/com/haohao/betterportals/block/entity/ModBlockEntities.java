package com.haohao.betterportals.block.entity;

import com.haohao.betterportals.BetterPortals;
import com.haohao.betterportals.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;
import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BetterPortals.MODID);

    public static final Supplier<BlockEntityType<PortalCoreBlockEntity>> PORTAL_CORE =
            BLOCK_ENTITIES.register("portal_core",
                    () -> new BlockEntityType<>(
                            new BlockEntityType.BlockEntitySupplier<>() {
                                @Override
                                public PortalCoreBlockEntity create(BlockPos pos, BlockState state) {
                                    return new PortalCoreBlockEntity(PORTAL_CORE.get(), pos, state);
                                }
                            },
                            Set.<Block>of(ModBlocks.PORTAL_CORE.get())));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
