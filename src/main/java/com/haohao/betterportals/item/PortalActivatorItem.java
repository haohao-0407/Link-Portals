package com.haohao.betterportals.item;

import com.haohao.betterportals.block.ModBlocks;
import com.haohao.betterportals.world.PortalActivationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;
import java.util.UUID;

public class PortalActivatorItem extends Item {
    public PortalActivatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (state.is(ModBlocks.PORTAL_CORE)) {
            if (!level.isClientSide()) {
                com.haohao.betterportals.BetterPortals.LOGGER.info("Portal Activator used on core at {}", pos);
                Optional<PortalActivationHelper.FrameResult> frame =
                        PortalActivationHelper.detectFrame(level, pos);
                if (frame.isPresent()) {
                    com.haohao.betterportals.BetterPortals.LOGGER.info("Frame detected: {}x{}", frame.get().width(), frame.get().height());
                    PortalActivationHelper.fillPortal((ServerLevel) level, frame.get(), UUID.randomUUID());
                } else {
                    com.haohao.betterportals.BetterPortals.LOGGER.info("No valid frame detected around core at {}", pos);
                }
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}
