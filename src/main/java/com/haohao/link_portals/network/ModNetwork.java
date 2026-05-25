package com.haohao.link_portals.network;

import com.haohao.link_portals.world.PortalActivationHelper;
import com.haohao.link_portals.world.PortalNetworkSavedData;
import com.haohao.link_portals.world.PortalNetworkSavedData.PortalInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
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

        PortalActivationHelper.fillPortal(level, frame, UUID.randomUUID(), networkName, portalName);
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

        PortalInfo target = data.getPortalInfo(payload.targetPortalId());
        if (target == null) return;

        ServerLevel targetLevel = currentLevel.getServer().getLevel(target.dimension());
        if (targetLevel == null) return;

        BlockPos targetPos = target.spawnPos();
        Vec3 targetVec = new Vec3(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);

        if (currentLevel.dimension() == target.dimension()) {
            player.teleportTo(targetLevel, targetVec.x, targetVec.y, targetVec.z,
                    Relative.union(Relative.DELTA, Relative.ROTATION), player.getYRot(), player.getXRot(), true);
        } else {
            TeleportTransition transition = new TeleportTransition(
                    targetLevel, targetVec, Vec3.ZERO, player.getYRot(), player.getXRot(),
                    Relative.union(Relative.DELTA, Relative.ROTATION),
                    TeleportTransition.DO_NOTHING
            );
            player.teleport(transition);
        }
    }

    public static void teleportEntity(net.minecraft.world.entity.Entity entity, ServerLevel currentLevel,
                                       UUID sourcePortalId, UUID targetPortalId,
                                       net.minecraft.core.Direction.Axis sourceAxis) {
        PortalNetworkSavedData data = currentLevel.getServer().overworld()
                .getDataStorage().computeIfAbsent(PortalNetworkSavedData.TYPE);

        PortalInfo target = data.getPortalInfo(targetPortalId);
        if (target == null) return;

        ServerLevel targetLevel = currentLevel.getServer().getLevel(target.dimension());
        if (targetLevel == null) return;

        List<BlockPos> sourceBlocks = data.getPortalBlockPositions(sourcePortalId);
        List<BlockPos> destBlocks = data.getPortalBlockPositions(targetPortalId);
        if (sourceBlocks.isEmpty() || destBlocks.isEmpty()) return;

        net.minecraft.core.Direction.Axis destAxis = getPortalAxis(targetLevel, target.spawnPos());

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

        Vec3 targetVec = buildTargetVec(destH, destV, destAxis, destBlocks);

        Vec3 momentum = entity.getDeltaMovement();
        float yRot = entity.getYRot();
        if (sourceAxis != destAxis) {
            momentum = new Vec3(-momentum.z, momentum.y, momentum.x);
            yRot += 90.0f;
        }

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
                    targetLevel, targetVec, momentum, passenger.getYRot() + (sourceAxis != destAxis ? 90.0f : 0.0f),
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

    private static double getHorizontalCoord(Vec3 pos, net.minecraft.core.Direction.Axis axis) {
        return axis == net.minecraft.core.Direction.Axis.X ? pos.x : pos.z;
    }

    private static Vec3 buildTargetVec(double h, double v, net.minecraft.core.Direction.Axis axis,
                                         List<BlockPos> destBlocks) {
        if (axis == net.minecraft.core.Direction.Axis.X) {
            int fixedZ = destBlocks.getFirst().getZ();
            return new Vec3(h, v, fixedZ + 0.5);
        } else {
            int fixedX = destBlocks.getFirst().getX();
            return new Vec3(fixedX + 0.5, v, h);
        }
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