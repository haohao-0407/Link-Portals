package com.haohao.link_portals.block;

import com.haohao.link_portals.world.PortalNetworkSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class PortalFrameBlock extends Block {
    public PortalFrameBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighbor);
            if (neighborState.is(ModBlocks.PORTAL)) {
                level.removeBlock(neighbor, false);
                return;
            }
            if (neighborState.is(ModBlocks.PORTAL_CORE)) {
                PortalNetworkSavedData data = level.getServer().overworld()
                        .getDataStorage().computeIfAbsent(PortalNetworkSavedData.TYPE);
                PortalNetworkSavedData.PortalInfo info = data.getPortalByCorePos(neighbor);
                if (info != null) {
                    List<BlockPos> portalBlocks = data.getPortalBlockPositions(info.id());
                    data.removePortal(info.id());
                    for (BlockPos portalPos : portalBlocks) {
                        if (level.getBlockState(portalPos).is(ModBlocks.PORTAL)) {
                            level.removeBlock(portalPos, false);
                        }
                    }
                }
                return;
            }
        }
        // Corner frame: check neighbors' neighbors for portal blocks
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            if (!level.getBlockState(neighbor).is(ModBlocks.PORTAL_FRAME)) continue;
            for (Direction dir2 : Direction.values()) {
                BlockPos neighbor2 = neighbor.relative(dir2);
                if (level.getBlockState(neighbor2).is(ModBlocks.PORTAL)) {
                    level.removeBlock(neighbor2, false);
                    return;
                }
            }
        }
    }
}
