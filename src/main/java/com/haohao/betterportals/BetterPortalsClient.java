package com.haohao.betterportals;

import com.haohao.betterportals.network.OpenPortalScreenPayload;
import com.haohao.betterportals.screen.PortalDestinationScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(value = BetterPortals.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = BetterPortals.MODID, value = Dist.CLIENT)
public class BetterPortalsClient {
    public BetterPortalsClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        BetterPortals.LOGGER.info("HELLO FROM CLIENT SETUP");
        BetterPortals.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    @SubscribeEvent
    static void registerPayloads(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1.0");
        registrar.playToClient(
                OpenPortalScreenPayload.TYPE,
                OpenPortalScreenPayload.STREAM_CODEC,
                (payload, context) -> Minecraft.getInstance().setScreen(
                        new PortalDestinationScreen(payload))
        );
    }
}
