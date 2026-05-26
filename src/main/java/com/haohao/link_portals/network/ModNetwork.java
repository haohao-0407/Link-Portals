package com.haohao.link_portals.network;

import com.haohao.link_portals.world.PortalActivationHelper;
import com.haohao.link_portals.world.PortalNetworkSavedData;
import com.haohao.link_portals.world.PortalNetworkSavedData.PortalInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;
import java.util.UUID;

public class ModNetwork {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1.0");

        registrar.playToServer(
                ChooseDestinationPayload.TYPE,
                ChooseDestinationPayload.STREAM_CODEC,
                (payload, context) -> {
                    ServerPlayer player = (ServerPlayer) context.player();
                    context.enqueueWork(() -> handleDestinationChoice(player, payload));
                }
        );

        registrar.playToServer(
                ConfirmPortalNamePayload.TYPE,
                ConfirmPortalNamePayload.STREAM_CODEC,
                (payload, context) -> {
                    ServerPlayer player = (ServerPlayer) context.player();
                    context.enqueueWork(() -> handlePortalNameConfirm(player, payload));
                }
        );

        registrar.playToClient(
                OpenNamingScreenPayload.TYPE,
                OpenNamingScreenPayload.STREAM_CODEC
        );

        registrar.playToClient(
                OpenPortalScreenPayload.TYPE,
                OpenPortalScreenPayload.STREAM_CODEC
        );
    }

    static void handlePortalNameConfirm(ServerPlayer player, ConfirmPortalNamePayload payload) {
        ServerLevel level = (ServerLevel) player.level();
        PortalActivationHelper.FrameResult frame = new PortalActivationHelper.FrameResult(
                payload.axis(), payload.minCorner(), payload.width(), payload.height());

        if (!verifyFrame(level, frame)) return;

        String networkName = payload.networkName();
        if (networkName == null || networkName.isBlank()) networkName = "default";

        String portalName = payload.portalName();
        if (portalName == null) portalName = "";

        PortalActivationHelper.fillPortal(level, frame, UUID.randomUUID(), networkName, portalName, payload.facing());
    }

    private static boolean isFrameBlock(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(com.haohao.link_portals.block.ModBlocks.PORTAL_FRAME)
                || state.is(net.minecraft.world.level.block.Blocks.CRYING_OBSIDIAN);
    }

    private static boolean verifyFrame(ServerLevel level, PortalActivationHelper.FrameResult frame) {
        net.minecraft.core.Direction right = frame.axis() == net.minecraft.core.Direction.Axis.X
                ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.SOUTH;
        BlockPos bl = frame.minCorner();
        for (int w = 0; w < frame.width() + 2; w++) {
            if (!isFrameBlock(level.getBlockState(bl.relative(right, w))))
                return false;
        }
        BlockPos tl = bl.above(frame.height() + 1);
        for (int w = 0; w < frame.width() + 2; w++) {
            if (!isFrameBlock(level.getBlockState(tl.relative(right, w))))
                return false;
        }
        for (int h = 1; h <= frame.height(); h++) {
            if (!isFrameBlock(level.getBlockState(bl.above(h))))
                return false;
            if (!isFrameBlock(level.getBlockState(bl.relative(right, frame.width() + 1).above(h))))
                return false;
        }
        return true;
    }

    public static void handleDestinationChoice(ServerPlayer player, ChooseDestinationPayload payload) {
        ServerLevel currentLevel = (ServerLevel) player.level();
        PortalNetworkSavedData data = currentLevel.getServer().overworld()
                .getDataStorage().computeIfAbsent(PortalNetworkSavedData.TYPE);

        PortalInfo source = data.getPortalInfo(payload.sourcePortalId());
        if (source == null) return;

        net.minecraft.core.Direction.Axis sourceAxis = getPortalAxis(currentLevel, source.spawnPos());
        teleportEntity(player, currentLevel, payload.sourcePortalId(), payload.targetPortalId(), sourceAxis);
    }

    public static void teleportEntity(net.minecraft.world.entity.Entity entity, ServerLevel currentLevel,
                                       UUID sourcePortalId, UUID targetPortalId,
                                       net.minecraft.core.Direction.Axis sourceAxis) {
        PortalNetworkSavedData data = currentLevel.getServer().overworld()
                .getDataStorage().computeIfAbsent(PortalNetworkSavedData.TYPE);

        PortalInfo source = data.getPortalInfo(sourcePortalId);
        PortalInfo target = data.getPortalInfo(targetPortalId);
        if (source == null || target == null) return;

        ServerLevel targetLevel = currentLevel.getServer().getLevel(target.dimension());
        if (targetLevel == null) return;

        List<BlockPos> sourceBlocks = data.getPortalBlockPositions(sourcePortalId);
        List<BlockPos> destBlocks = data.getPortalBlockPositions(targetPortalId);
        if (sourceBlocks.isEmpty() || destBlocks.isEmpty()) return;

        net.minecraft.core.Direction.Axis destAxis = getPortalAxis(targetLevel, target.spawnPos());
        net.minecraft.core.Direction sourceFacing = source.facing();
        net.minecraft.core.Direction destFacing = target.facing();

        int[] srcBounds = getPortalBounds(sourceBlocks, sourceAxis);
        int[] dstBounds = getPortalBounds(destBlocks, destAxis);

        Vec3 entityPos = entity.position();
        double srcH = getHorizontalCoord(entityPos, sourceAxis) - srcBounds[0];
        double srcV = entityPos.y - srcBounds[2];
        double srcWidth = srcBounds[1] - srcBounds[0] + 1.0;
        double srcHeight = srcBounds[3] - srcBounds[2] + 1.0;

        double ratioH = Math.clamp(srcH / srcWidth, 0.0, 1.0);
        double ratioV = Math.clamp(srcV / srcHeight, 0.0, 1.0);

        double dstWidth = dstBounds[1] - dstBounds[0] + 1.0;
        double dstHeight = dstBounds[3] - dstBounds[2] + 1.0;
        double destH = dstBounds[0] + ratioH * dstWidth;
        double destV = dstBounds[2] + ratioV * dstHeight;

        boolean enteredFromFront = isEntityOnFrontSide(entityPos, sourceBlocks, sourceFacing, sourceAxis);
        net.minecraft.core.Direction exitDirection = enteredFromFront ? destFacing : destFacing.getOpposite();

        Vec3 targetVec = buildTargetVecWithOffset(destH, destV, destAxis, destBlocks, exitDirection);

        float yawOffset = computeYawOffset(sourceFacing, destFacing, enteredFromFront);
        float yRot = entity.getYRot() + yawOffset;
        Vec3 momentum = rotateMomentum(entity.getDeltaMovement(), yawOffset);

        TeleportTransition transition = new TeleportTransition(
                targetLevel, targetVec, momentum, yRot, entity.getXRot(),
                java.util.Set.of(),
                TeleportTransition.DO_NOTHING
        );

        List<net.minecraft.world.entity.Entity> passengers = new java.util.ArrayList<>(entity.getPassengers());
        for (net.minecraft.world.entity.Entity passenger : passengers) {
            passenger.stopRiding();
        }

        entity.teleport(transition);
        entity.setDeltaMovement(momentum);

        for (net.minecraft.world.entity.Entity passenger : passengers) {
            TeleportTransition passengerTransition = new TeleportTransition(
                    targetLevel, targetVec, momentum, passenger.getYRot() + yawOffset,
                    passenger.getXRot(),
                    java.util.Set.of(),
                    TeleportTransition.DO_NOTHING
            );
            passenger.teleport(passengerTransition);
            passenger.startRiding(entity);
            passenger.setDeltaMovement(momentum);
            if (passenger instanceof ServerPlayer sp) {
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));
            }
        }

        if (entity instanceof ServerPlayer sp) {
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));
        }
    }

    private static boolean isEntityOnFrontSide(Vec3 entityPos, List<BlockPos> portalBlocks,
                                                  net.minecraft.core.Direction facing,
                                                  net.minecraft.core.Direction.Axis axis) {
        double portalCoord;
        double entityCoord;
        if (axis == net.minecraft.core.Direction.Axis.X) {
            portalCoord = portalBlocks.getFirst().getZ() + 0.5;
            entityCoord = entityPos.z;
        } else {
            portalCoord = portalBlocks.getFirst().getX() + 0.5;
            entityCoord = entityPos.x;
        }
        double diff = entityCoord - portalCoord;
        int facingSign = facing.getAxisDirection() == net.minecraft.core.Direction.AxisDirection.POSITIVE ? 1 : -1;
        return (diff * facingSign) > 0;
    }

    private static Vec3 buildTargetVecWithOffset(double h, double v, net.minecraft.core.Direction.Axis axis,
                                                   List<BlockPos> destBlocks, net.minecraft.core.Direction exitDir) {
        double offset = 0.5;
        if (axis == net.minecraft.core.Direction.Axis.X) {
            int fixedZ = destBlocks.getFirst().getZ();
            double z = fixedZ + 0.5 + exitDir.getStepZ() * offset;
            return new Vec3(h, v, z);
        } else {
            int fixedX = destBlocks.getFirst().getX();
            double x = fixedX + 0.5 + exitDir.getStepX() * offset;
            return new Vec3(x, v, h);
        }
    }

    private static float computeYawOffset(net.minecraft.core.Direction sourceFacing,
                                            net.minecraft.core.Direction destFacing,
                                            boolean enteredFromFront) {
        net.minecraft.core.Direction entryDir = enteredFromFront
                ? sourceFacing.getOpposite()
                : sourceFacing;
        net.minecraft.core.Direction exitDir = enteredFromFront
                ? destFacing
                : destFacing.getOpposite();
        return directionToYaw(exitDir) - directionToYaw(entryDir);
    }

    private static float directionToYaw(net.minecraft.core.Direction dir) {
        return switch (dir) {
            case SOUTH -> 0f;
            case WEST -> 90f;
            case NORTH -> 180f;
            case EAST -> -90f;
            default -> 0f;
        };
    }

    private static Vec3 rotateMomentum(Vec3 momentum, float yawOffset) {
        double rad = Math.toRadians(yawOffset);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double x = momentum.x * cos + momentum.z * sin;
        double z = -momentum.x * sin + momentum.z * cos;
        return new Vec3(x, momentum.y, z);
    }

    private static double getHorizontalCoord(Vec3 pos, net.minecraft.core.Direction.Axis axis) {
        return axis == net.minecraft.core.Direction.Axis.X ? pos.x : pos.z;
    }

    private static int[] getPortalBounds(List<BlockPos> blocks, net.minecraft.core.Direction.Axis axis) {
        int minH = Integer.MAX_VALUE, maxH = Integer.MIN_VALUE;
        int minV = Integer.MAX_VALUE, maxV = Integer.MIN_VALUE;
        for (BlockPos bp : blocks) {
            int h = axis == net.minecraft.core.Direction.Axis.X ? bp.getX() : bp.getZ();
            int v = bp.getY();
            if (h < minH) minH = h;
            if (h > maxH) maxH = h;
            if (v < minV) minV = v;
            if (v > maxV) maxV = v;
        }
        return new int[]{minH, maxH, minV, maxV};
    }

    private static net.minecraft.core.Direction.Axis getPortalAxis(ServerLevel level, BlockPos pos) {
        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
        if (state.is(com.haohao.link_portals.block.ModBlocks.PORTAL)) {
            return state.getValue(com.haohao.link_portals.block.PortalBlock.AXIS);
        }
        for (BlockPos neighbor : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 2, 1))) {
            state = level.getBlockState(neighbor);
            if (state.is(com.haohao.link_portals.block.ModBlocks.PORTAL)) {
                return state.getValue(com.haohao.link_portals.block.PortalBlock.AXIS);
            }
        }
        return net.minecraft.core.Direction.Axis.X;
    }
}