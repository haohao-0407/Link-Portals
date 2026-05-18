package com.haohao.link_portals.network;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record ChooseDestinationPayload(UUID sourcePortalId, UUID targetPortalId)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ChooseDestinationPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("link_portals", "choose_destination"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChooseDestinationPayload> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC,
                    ChooseDestinationPayload::sourcePortalId,
                    UUIDUtil.STREAM_CODEC,
                    ChooseDestinationPayload::targetPortalId,
                    ChooseDestinationPayload::new
            );

    @Override
    public CustomPacketPayload.Type<ChooseDestinationPayload> type() {
        return TYPE;
    }
}
