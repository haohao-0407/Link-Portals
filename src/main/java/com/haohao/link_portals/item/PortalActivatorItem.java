package com.haohao.link_portals.item;

import com.haohao.link_portals.block.ModBlocks;
import com.haohao.link_portals.world.PortalActivationHelper;
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
                com.haohao.link_portals.LinkPortals.LOGGER.info("Portal Activator used on core at {}", pos);
                Optional<PortalActivationHelper.FrameResult> frame =
                        PortalActivationHelper.detectFrame(level, pos);
                if (frame.isPresent()) {
                    com.haohao.link_portals.LinkPortals.LOGGER.info("Frame detected: {}x{}", frame.get().width(), frame.get().height());
                    PortalActivationHelper.fillPortal((ServerLevel) level, frame.get(), UUID.randomUUID());
                } else {
                    com.haohao.link_portals.LinkPortals.LOGGER.info("No valid frame detected around core at {}", pos);
                }
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}
