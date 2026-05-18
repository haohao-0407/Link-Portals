package com.haohao.link_portals.screen;

import com.haohao.link_portals.network.ChooseDestinationPayload;
import com.haohao.link_portals.network.OpenPortalScreenPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class PortalDestinationScreen extends Screen {
    private final OpenPortalScreenPayload payload;
    private final List<Button> destinationButtons = new ArrayList<>();

    public PortalDestinationScreen(OpenPortalScreenPayload payload) {
        super(Component.translatable("screen.link_portals.portal_destination"));
        this.payload = payload;
    }

    @Override
    protected void init() {
        super.init();
        this.destinationButtons.clear();

        List<OpenPortalScreenPayload.Destination> destinations = payload.destinations();
        int buttonWidth = Math.min(280, this.width - 40);
        int startY = 35;
        int buttonHeight = 20;
        int spacing = 4;

        for (int i = 0; i < destinations.size(); i++) {
            OpenPortalScreenPayload.Destination dest = destinations.get(i);
            String dimName = dest.dimension().identifier().getPath();
            String label = "[" + dimName + "] " + dest.corePos().toShortString();
            int y = startY + i * (buttonHeight + spacing);

            Button button = Button.builder(Component.literal(label), b -> {
                        ClientPacketDistributor.sendToServer(new ChooseDestinationPayload(
                                payload.portalId(), dest.portalId()));
                        this.onClose();
                    })
                    .pos((this.width - buttonWidth) / 2, y)
                    .width(buttonWidth)
                    .build();
            this.addRenderableWidget(button);
            this.destinationButtons.add(button);
        }

        this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), btn -> this.onClose())
                .pos(this.width / 2 - 50, this.height - 35)
                .width(100)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.text(this.font, this.title, this.width / 2 - this.font.width(this.title) / 2, 10, 0xFFFFFF);
    }
}
