package com.haohao.link_portals.screen;

import com.haohao.link_portals.network.ConfirmPortalNamePayload;
import com.haohao.link_portals.network.OpenNamingScreenPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class PortalNamingScreen extends Screen {
    private final OpenNamingScreenPayload payload;
    private EditBox networkNameField;
    private EditBox portalNameField;

    public PortalNamingScreen(OpenNamingScreenPayload payload) {
        super(Component.translatable("screen.link_portals.portal_naming"));
        this.payload = payload;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = this.height / 2 - 50;

        this.portalNameField = new EditBox(this.font, centerX - 100, startY, 200, 20,
                Component.translatable("screen.link_portals.portal_name"));
        this.portalNameField.setMaxLength(64);
        this.portalNameField.setHint(Component.translatable("screen.link_portals.portal_name"));
        this.addRenderableWidget(this.portalNameField);

        this.networkNameField = new EditBox(this.font, centerX - 100, startY + 30, 200, 20,
                Component.translatable("screen.link_portals.network_name"));
        this.networkNameField.setMaxLength(64);
        //this.networkNameField.setValue("default");
        this.networkNameField.setHint(Component.translatable("screen.link_portals.network_name"));
        this.addRenderableWidget(this.networkNameField);

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.done"), btn -> {
                    String network = this.networkNameField.getValue().trim();
                    if (network.isEmpty()) network = "default";
                    String name = this.portalNameField.getValue().trim();
                    ClientPacketDistributor.sendToServer(new ConfirmPortalNamePayload(
                            network, name, payload.axis(), payload.minCorner(), payload.width(), payload.height()));
                    this.onClose();
                })
                .pos(centerX - 105, startY + 60)
                .width(100)
                .build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.cancel"), btn -> this.onClose())
                .pos(centerX + 5, startY + 60)
                .width(100)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.text(this.font, this.title, this.width / 2 - this.font.width(this.title) / 2, this.height / 2 - 70, 0xFFFFFF);

    }
}
