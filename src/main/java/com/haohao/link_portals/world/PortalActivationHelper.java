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

    public static Optional<FrameResult> detectFrame(Level level, BlockPos corePos) {
        // Try horizontal directions to find frame orientation
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            Optional<FrameResult> result = detectFrameAlong(level, corePos, dir);
            if (result.isPresent()) return result;
        }
        return Optional.empty();
    }

    private static Optional<FrameResult> detectFrameAlong(Level level, BlockPos corePos, Direction facing) {
        // The portal interior faces along the facing direction
        // Frame goes left-right (perpendicular to facing) and up-down
        Direction right = facing.getClockWise();

        // Find bottom-left corner by scanning from core
        BlockPos bottomLeft = findBottomLeft(level, corePos, right);
        if (bottomLeft == null) return Optional.empty();

        // Measure width (along right direction) - count interior blocks
        int width = measureWidth(level, bottomLeft, right);
        if (width < 2 || width > 21) return Optional.empty();

        // Measure height - count interior blocks
        int height = measureHeight(level, bottomLeft, right);
        if (height < 3 || height > 21) return Optional.empty();

        // Validate frame: check borders are all valid frame blocks
        if (!validateFrame(level, bottomLeft, right, width, height)) return Optional.empty();

        // Count core blocks
        int coreCount = countCoreBlocks(level, bottomLeft, right, width, height);
        if (coreCount != 1) return Optional.empty();

        // Check interior is empty
        if (!validateInterior(level, bottomLeft, right, width, height)) return Optional.empty();

        Direction.Axis axis = right.getAxis();
        return Optional.of(new FrameResult(axis, bottomLeft, width, height));
    }

    private static BlockPos findBottomLeft(Level level, BlockPos start, Direction right) {
        Direction left = right.getOpposite();
        BlockPos pos = start;
        int limit = 21;
        while (isFrameBlock(level, pos.below()) && limit-- > 0) {
            pos = pos.below();
        }
        limit = 21;
        while (isFrameBlock(level, pos.relative(left)) && limit-- > 0) {
            pos = pos.relative(left);
        }
        limit = 21;
        while (isFrameBlock(level, pos.below()) && limit-- > 0) {
            pos = pos.below();
        }
        return pos;
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
        // Bottom row
        for (int w = 0; w < width + 2; w++) {
            if (!isFrameBlock(level, bottomLeft.relative(right, w))) return false;
        }
        // Top row
        BlockPos topLeft = bottomLeft.above(height + 1);
        for (int w = 0; w < width + 2; w++) {
            if (!isFrameBlock(level, topLeft.relative(right, w))) return false;
        }
        // Left column
        for (int h = 1; h <= height; h++) {
            if (!isFrameBlock(level, bottomLeft.above(h))) return false;
        }
        // Right column
        BlockPos bottomRight = bottomLeft.relative(right, width + 1);
        for (int h = 1; h <= height; h++) {
            if (!isFrameBlock(level, bottomRight.above(h))) return false;
        }
        return true;
    }

    private static int countCoreBlocks(Level level, BlockPos bottomLeft, Direction right, int width, int height) {
        int count = 0;
        for (int h = 0; h <= height + 1; h++) {
            for (int w = 0; w <= width + 1; w++) {
                if (h > 0 && h <= height && w > 0 && w <= width) continue; // skip interior
                BlockPos pos = bottomLeft.relative(right, w).above(h);
                if (level.getBlockState(pos).is(ModBlocks.PORTAL_CORE)) count++;
            }
        }
        return count;
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
        return state.is(ModBlocks.PORTAL_FRAME) || state.is(ModBlocks.PORTAL_CORE);
    }

    public static void fillPortal(ServerLevel level, FrameResult frame, UUID portalId) {
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

        // Register in saved data
        PortalNetworkSavedData data = level.getServer().overworld()
                .getDataStorage().computeIfAbsent(PortalNetworkSavedData.TYPE);

        // Get network name from core block entity
        for (int h = 0; h <= frame.height() + 1; h++) {
            for (int w = 0; w <= frame.width() + 1; w++) {
                if (h > 0 && h <= frame.height() && w > 0 && w <= frame.width()) continue;
                BlockPos pos = frame.minCorner().relative(right, w).above(h);
                if (level.getBlockState(pos).is(ModBlocks.PORTAL_CORE)) {
                    if (level.getBlockEntity(pos) instanceof com.haohao.link_portals.block.entity.PortalCoreBlockEntity be) {
                        be.setPortalId(portalId);
                        data.addPortal(portalId, level.dimension(), pos.immutable(), be.getNetworkName(), portalPositions);
                        return;
                    }
                }
            }
        }
    }
}
