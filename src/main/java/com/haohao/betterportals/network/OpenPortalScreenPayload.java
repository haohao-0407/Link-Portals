package com.haohao.betterportals.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.UUID;

public record OpenPortalScreenPayload(UUID portalId, String networkName, List<Destination> destinations)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenPortalScreenPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("betterportals", "open_portal_screen"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenPortalScreenPayload> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC,
                    OpenPortalScreenPayload::portalId,
                    ByteBufCodecs.STRING_UTF8,
                    OpenPortalScreenPayload::networkName,
                    Destination.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    OpenPortalScreenPayload::destinations,
                    OpenPortalScreenPayload::new
            );

    @Override
    public CustomPacketPayload.@NonNull Type<OpenPortalScreenPayload> type() {
        return TYPE;
    }

    public record Destination(UUID portalId, ResourceKey<Level> dimension, BlockPos corePos, String networkName) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Destination> STREAM_CODEC =
                StreamCodec.composite(
                        UUIDUtil.STREAM_CODEC,
                        Destination::portalId,
                        ResourceKey.streamCodec(net.minecraft.core.registries.Registries.DIMENSION),
                        Destination::dimension,
                        BlockPos.STREAM_CODEC,
                        Destination::corePos,
                        ByteBufCodecs.STRING_UTF8,
                        Destination::networkName,
                        Destination::new
                );
    }
}
