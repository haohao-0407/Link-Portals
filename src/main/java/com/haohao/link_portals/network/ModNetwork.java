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

    static void handleDestinationChoice(ServerPlayer player, ChooseDestinationPayload payload) {
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
}