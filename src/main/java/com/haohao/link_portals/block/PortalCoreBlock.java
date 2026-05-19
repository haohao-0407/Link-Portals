package com.haohao.link_portals.block;

import com.haohao.link_portals.block.entity.ModBlockEntities;
import com.haohao.link_portals.block.entity.PortalCoreBlockEntity;
import com.haohao.link_portals.world.PortalNetworkSavedData;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class PortalCoreBlock extends BaseEntityBlock {
    public static final MapCodec<PortalCoreBlock> CODEC = simpleCodec(PortalCoreBlock::new);

    public PortalCoreBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PortalCoreBlockEntity(ModBlockEntities.PORTAL_CORE.get(), pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        PortalNetworkSavedData data = level.getServer().overworld()
                .getDataStorage().computeIfAbsent(PortalNetworkSavedData.TYPE);
        PortalNetworkSavedData.PortalInfo info = data.getPortalByCorePos(pos);
        if (info != null) {
            List<BlockPos> portalBlocks = data.getPortalBlockPositions(info.id());
            data.removePortal(info.id());
            for (BlockPos portalPos : portalBlocks) {
                if (level.getBlockState(portalPos).is(ModBlocks.PORTAL)) {
                    level.removeBlock(portalPos, false);
                }
            }
        }
    }
}
