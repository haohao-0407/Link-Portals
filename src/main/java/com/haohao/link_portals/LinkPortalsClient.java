package com.haohao.link_portals;

import com.haohao.link_portals.network.ClientPayloadHandler;
import com.haohao.link_portals.network.OpenNamingScreenPayload;
import com.haohao.link_portals.network.OpenPortalScreenPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@Mod(value = LinkPortals.MODID, dist = Dist.CLIENT)
public class LinkPortalsClient {
    public LinkPortalsClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(this::registerClientPayloadHandlers);
    }

    private void registerClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(OpenNamingScreenPayload.TYPE, ClientPayloadHandler::handleOpenNaming);
        event.register(OpenPortalScreenPayload.TYPE, ClientPayloadHandler::handleOpenPortalScreen);
    }
}
