package com.haohao.link_portals.block;

import com.haohao.link_portals.network.OpenPortalScreenPayload;
import com.haohao.link_portals.world.PortalNetworkSavedData;
import com.haohao.link_portals.world.PortalNetworkSavedData.PortalInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PortalBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final Map<Direction.Axis, VoxelShape> SHAPES = Shapes.rotateHorizontalAxis(Block.column(4.0, 16.0, 0.0, 16.0));
    private static final int COOLDOWN_TICKS = 60;

    public PortalBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(AXIS));
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        PortalNetworkSavedData data = level.getServer().overworld()
                .getDataStorage().computeIfAbsent(PortalNetworkSavedData.TYPE);
        PortalInfo info = data.getPortalAtBlock(pos);
        if (info != null) {
            List<BlockPos> siblings = data.getPortalBlockPositions(info.id());
            data.removePortal(info.id());
            for (BlockPos sibling : siblings) {
                if (!sibling.equals(pos) && level.getBlockState(sibling).is(this)) {
                    level.removeBlock(sibling, false);
                }
            }
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            if (player.getPersistentData().contains("link_portals_cooldown")) {
                long cooldown = player.getPersistentData().getLong("link_portals_cooldown").orElse(0L);
                if (level.getGameTime() < cooldown) return;
            }

            PortalNetworkSavedData data = level.getServer().overworld()
                    .getDataStorage().computeIfAbsent(PortalNetworkSavedData.TYPE);
            PortalInfo current = data.getPortalAtBlock(pos);
            if (current == null) return;

            List<PortalInfo> networkPortals = data.getNetworkPortals(current.networkName(), current.id());
            if (networkPortals.isEmpty()) return;

            // Set cooldown
            player.getPersistentData().putLong("link_portals_cooldown", level.getGameTime() + COOLDOWN_TICKS);

            List<OpenPortalScreenPayload.Destination> destinations = new ArrayList<>();
            for (PortalInfo info : networkPortals) {
                destinations.add(new OpenPortalScreenPayload.Destination(
                        info.id(), info.dimension(), info.corePos(), info.networkName()));
            }
            PacketDistributor.sendToPlayer(player, new OpenPortalScreenPayload(
                    current.id(), current.networkName(), destinations));
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                      Direction directionToNeighbour, BlockPos neighbourPos,
                                      BlockState neighbourState, RandomSource random) {
        Direction.Axis updateAxis = directionToNeighbour.getAxis();
        Direction.Axis axis = state.getValue(AXIS);
        boolean wrongAxis = axis != updateAxis && updateAxis.isHorizontal();
        return !wrongAxis && !neighbourState.is(this) && !neighbourState.is(ModBlocks.PORTAL_FRAME)
                && !neighbourState.is(ModBlocks.PORTAL_CORE)
                ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : state;
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return ItemStack.EMPTY;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        switch (rotation) {
            case COUNTERCLOCKWISE_90:
            case CLOCKWISE_90:
                switch (state.getValue(AXIS)) {
                    case X:
                        return state.setValue(AXIS, Direction.Axis.Z);
                    case Z:
                        return state.setValue(AXIS, Direction.Axis.X);
                    default:
                        return state;
                }
            default:
                return state;
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }
}
