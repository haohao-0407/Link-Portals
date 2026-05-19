package com.haohao.link_portals.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenNamingScreenPayload(Direction.Axis axis, BlockPos minCorner, int width, int height)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenNamingScreenPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("link_portals", "open_naming_screen"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenNamingScreenPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT.map(i -> Direction.Axis.values()[i], Enum::ordinal),
                    OpenNamingScreenPayload::axis,
                    BlockPos.STREAM_CODEC,
                    OpenNamingScreenPayload::minCorner,
                    ByteBufCodecs.INT,
                    OpenNamingScreenPayload::width,
                    ByteBufCodecs.INT,
                    OpenNamingScreenPayload::height,
                    OpenNamingScreenPayload::new
            );

    @Override
    public CustomPacketPayload.Type<OpenNamingScreenPayload> type() {
        return TYPE;
    }
}
