package com.haohao.betterportals.network;

import com.haohao.betterportals.world.PortalNetworkSavedData;
import com.haohao.betterportals.world.PortalNetworkSavedData.PortalInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

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
    }

    static void handleDestinationChoice(ServerPlayer player, ChooseDestinationPayload payload) {
        ServerLevel currentLevel = (ServerLevel) player.level();
        PortalNetworkSavedData data = currentLevel.getServer().overworld()
                .getDataStorage().computeIfAbsent(PortalNetworkSavedData.TYPE);

        PortalInfo target = data.getPortalInfo(payload.targetPortalId());
        if (target == null) return;

        ServerLevel targetLevel = currentLevel.getServer().getLevel(target.dimension());
        if (targetLevel == null) return;

        BlockPos spawnPos = data.getSpawnPos(target.id());
        BlockPos targetPos = spawnPos != null ? spawnPos : target.corePos();
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
