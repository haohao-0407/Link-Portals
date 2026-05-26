package com.haohao.link_portals.world;

import com.haohao.link_portals.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.*;

public class PortalActivationHelper {

    public record FrameResult(Direction.Axis axis, BlockPos minCorner, int width, int height) {}

    public static Optional<FrameResult> detectFrame(Level level, BlockPos framePos, Direction clickedFace) {
        for (Direction dir : Direction.values()) {
            BlockPos candidate = framePos.relative(dir);
            if (!isFrameBlock(level, candidate) && isEmpty(level, candidate)) {
                Optional<FrameResult> result = tryDetectFromInterior(level, candidate);
                if (result.isPresent()) return result;
            }
        }
        return Optional.empty();
    }

    private static Optional<FrameResult> tryDetectFromInterior(Level level, BlockPos interiorPos) {
        Optional<FrameResult> result = detectFrameFromInterior(level, interiorPos, Direction.EAST);
        if (result.isPresent()) return result;
        result = detectFrameFromInterior(level, interiorPos, Direction.WEST);
        if (result.isPresent()) return result;
        result = detectFrameFromInterior(level, interiorPos, Direction.SOUTH);
        if (result.isPresent()) return result;
        return detectFrameFromInterior(level, interiorPos, Direction.NORTH);
    }

    private static boolean isEmpty(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }

    private static Optional<FrameResult> detectFrameFromInterior(Level level, BlockPos interiorPos, Direction right) {
        BlockPos bottomLeft = findBottomLeft(level, interiorPos, right);
        if (bottomLeft == null) return Optional.empty();

        int width = measureWidth(level, bottomLeft, right);
        if (width < 2 || width > 21) return Optional.empty();

        int height = measureHeight(level, bottomLeft, right);
        if (height < 3 || height > 21) return Optional.empty();

        if (!validateFrame(level, bottomLeft, right, width, height)) return Optional.empty();
        if (!validateInterior(level, bottomLeft, right, width, height)) return Optional.empty();

        Direction.Axis axis = right.getAxis();
        return Optional.of(new FrameResult(axis, bottomLeft, width, height));
    }

    private static BlockPos findBottomLeft(Level level, BlockPos start, Direction right) {
        Direction left = right.getOpposite();
        BlockPos pos = start;
        int limit = 21;
        while (!isFrameBlock(level, pos.below()) && limit-- > 0) {
            pos = pos.below();
        }
        if (!isFrameBlock(level, pos.below())) return null;
        limit = 21;
        while (!isFrameBlock(level, pos.relative(left)) && limit-- > 0) {
            pos = pos.relative(left);
        }
        if (!isFrameBlock(level, pos.relative(left))) return null;
        return pos.relative(left).below();
    }

    private static int measureWidth(Level level, BlockPos bottomLeft, Direction right) {
        BlockPos pos = bottomLeft.above().relative(right);
        int width = 0;
        while (!isFrameBlock(level, pos) && width < 21) {
            width++;
            pos = pos.relative(right);
        }
        if (!isFrameBlock(level, pos)) return 0;
        return width;
    }

    private static int measureHeight(Level level, BlockPos bottomLeft, Direction right) {
        BlockPos pos = bottomLeft.above().relative(right);
        int height = 0;
        while (!isFrameBlock(level, pos) && height < 21) {
            height++;
            pos = pos.above();
        }
        if (!isFrameBlock(level, pos)) return 0;
        return height;
    }

    private static boolean validateFrame(Level level, BlockPos bottomLeft, Direction right, int width, int height) {
        for (int w = 0; w < width + 2; w++) {
            if (!isFrameBlock(level, bottomLeft.relative(right, w))) return false;
        }
        BlockPos topLeft = bottomLeft.above(height + 1);
        for (int w = 0; w < width + 2; w++) {
            if (!isFrameBlock(level, topLeft.relative(right, w))) return false;
        }
        for (int h = 1; h <= height; h++) {
            if (!isFrameBlock(level, bottomLeft.above(h))) return false;
        }
        BlockPos bottomRight = bottomLeft.relative(right, width + 1);
        for (int h = 1; h <= height; h++) {
            if (!isFrameBlock(level, bottomRight.above(h))) return false;
        }
        return true;
    }

    private static boolean validateInterior(Level level, BlockPos bottomLeft, Direction right, int width, int height) {
        for (int h = 1; h <= height; h++) {
            for (int w = 1; w <= width; w++) {
                BlockPos pos = bottomLeft.relative(right, w).above(h);
                BlockState state = level.getBlockState(pos);
                if (!state.isAir() && !state.canBeReplaced()) return false;
            }
        }
        return true;
    }

    private static boolean isFrameBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(ModBlocks.PORTAL_FRAME) || state.is(net.minecraft.world.level.block.Blocks.CRYING_OBSIDIAN);
    }

    public static void fillPortal(ServerLevel level, FrameResult frame, UUID portalId, String networkName, String portalName, Direction facing) {
        Direction right = frame.axis() == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        Set<BlockPos> portalPositions = new HashSet<>();

        for (int h = 1; h <= frame.height(); h++) {
            for (int w = 1; w <= frame.width(); w++) {
                BlockPos pos = frame.minCorner().relative(right, w).above(h);
                level.setBlock(pos,
                        ModBlocks.PORTAL.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_AXIS, frame.axis()),
                        3);
                portalPositions.add(pos.immutable());
            }
        }

        PortalNetworkSavedData data = level.getServer().overworld()
                .getDataStorage().computeIfAbsent(PortalNetworkSavedData.TYPE);
        data.addPortal(portalId, level.dimension(), networkName, portalName, facing, portalPositions);
    }
}