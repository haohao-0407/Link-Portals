package com.haohao.link_portals.item;

import com.haohao.link_portals.LinkPortals;
import com.haohao.link_portals.block.ModBlocks;
import com.haohao.link_portals.network.OpenNamingScreenPayload;
import com.haohao.link_portals.world.PortalActivationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;

public class PortalActivatorItem extends Item {
    public PortalActivatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        LinkPortals.LOGGER.info("PortalActivator useOn: pos={}, block={}, isClient={}", pos, state.getBlock(), level.isClientSide());

        if (state.is(ModBlocks.PORTAL_FRAME) || state.is(net.minecraft.world.level.block.Blocks.CRYING_OBSIDIAN)) {
            if (!level.isClientSide() && context.getPlayer() instanceof ServerPlayer player) {
                Direction face = context.getClickedFace();
                LinkPortals.LOGGER.info("Detecting frame: clickedFace={}", face);
                Optional<PortalActivationHelper.FrameResult> frame =
                        PortalActivationHelper.detectFrame(level, pos, face);
                LinkPortals.LOGGER.info("Frame detection result: {}", frame.isPresent() ? "FOUND" : "NOT FOUND");
                if (frame.isPresent()) {
                    PortalActivationHelper.FrameResult f = frame.get();
                    LinkPortals.LOGGER.info("Sending OpenNamingScreenPayload to player");
                    PacketDistributor.sendToPlayer(player, new OpenNamingScreenPayload(
                            f.axis(), f.minCorner(), f.width(), f.height()));
                }
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}
