package com.haohao.link_portals.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class PortalFrameBlock extends Block {
    public PortalFrameBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            if (level.getBlockState(neighbor).is(ModBlocks.PORTAL)) {
                level.removeBlock(neighbor, false);
                return;
            }
        }
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
