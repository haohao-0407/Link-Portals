package com.haohao.betterportals.block.entity;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.UUID;

public class PortalCoreBlockEntity extends BlockEntity {
    private UUID portalId = UUID.randomUUID();
    private String networkName = "default";

    public PortalCoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public UUID getPortalId() {
        return portalId;
    }

    public void setPortalId(UUID portalId) {
        this.portalId = portalId;
        setChanged();
    }

    public String getNetworkName() {
        return networkName;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
        setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.portalId = input.read("PortalId", Codec.STRING)
                .map(UUID::fromString)
                .orElse(UUID.randomUUID());
        this.networkName = input.read("NetworkName", Codec.STRING).orElse("default");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("PortalId", Codec.STRING, portalId.toString());
        output.store("NetworkName", Codec.STRING, networkName);
    }
}
