package com.haohao.link_portals.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ConfirmPortalNamePayload(String networkName, String portalName, Direction.Axis axis, BlockPos minCorner, int width, int height, Direction facing)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ConfirmPortalNamePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("link_portals", "confirm_portal_name"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfirmPortalNamePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    ConfirmPortalNamePayload::networkName,
                    ByteBufCodecs.STRING_UTF8,
                    ConfirmPortalNamePayload::portalName,
                    ByteBufCodecs.INT.map(i -> Direction.Axis.values()[i], Enum::ordinal),
                    ConfirmPortalNamePayload::axis,
                    BlockPos.STREAM_CODEC,
                    ConfirmPortalNamePayload::minCorner,
                    ByteBufCodecs.INT,
                    ConfirmPortalNamePayload::width,
                    ByteBufCodecs.INT,
                    ConfirmPortalNamePayload::height,
                    ByteBufCodecs.INT.map(Direction::from2DDataValue, Direction::get2DDataValue),
                    ConfirmPortalNamePayload::facing,
                    ConfirmPortalNamePayload::new
            );

    @Override
    public CustomPacketPayload.Type<ConfirmPortalNamePayload> type() {
        return TYPE;
    }
}
