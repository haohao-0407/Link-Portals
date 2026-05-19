package com.haohao.link_portals.network;

import com.haohao.link_portals.screen.PortalDestinationScreen;
import com.haohao.link_portals.screen.PortalNamingScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler {
    public static void handleOpenNaming(OpenNamingScreenPayload payload, IPayloadContext context) {
        Minecraft.getInstance().setScreen(new PortalNamingScreen(payload));
    }

    public static void handleOpenPortalScreen(OpenPortalScreenPayload payload, IPayloadContext context) {
        Minecraft.getInstance().setScreen(new PortalDestinationScreen(payload));
    }
}
