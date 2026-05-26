package com.haohao.link_portals.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

public class PortalNetworkSavedData extends SavedData {
    public static final Codec<PortalNetworkSavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, PortalInfo.CODEC)
                    .fieldOf("portalInfo")
                    .forGetter(d -> {
                        Map<String, PortalInfo> m = new HashMap<>();
                        d.portalInfoMap.forEach((id, info) -> m.put(id.toString(), info));
                        return m;
                    }),
            Codec.unboundedMap(Codec.STRING, Codec.STRING)
                    .fieldOf("portalBlocks")
                    .forGetter(d -> {
                        Map<String, String> m = new HashMap<>();
                        d.portalBlockMap.forEach((pos, id) -> m.put(pos.toShortString(), id.toString()));
                        return m;
                    })
    ).apply(i, PortalNetworkSavedData::new));

    public static final SavedDataType<PortalNetworkSavedData> TYPE =
            new SavedDataType<>(net.minecraft.resources.Identifier.fromNamespaceAndPath("link_portals", "portal_network"),
                    PortalNetworkSavedData::new, CODEC);

    private final Map<UUID, PortalInfo> portalInfoMap = new HashMap<>();
    private final Map<BlockPos, UUID> portalBlockMap = new HashMap<>();

    public PortalNetworkSavedData() {
    }

    private PortalNetworkSavedData(Map<String, PortalInfo> infoMap, Map<String, String> blockMap) {
        infoMap.forEach((k, v) -> this.portalInfoMap.put(UUID.fromString(k), v));
        blockMap.forEach((k, v) -> {
            String[] parts = k.split(", ");
            BlockPos pos = new BlockPos(
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2])
            );
            this.portalBlockMap.put(pos, UUID.fromString(v));
        });
    }

    public void addPortal(UUID portalId, ResourceKey<Level> dimension, String networkName, String portalName, Direction facing, Set<BlockPos> portalBlockPositions) {
        BlockPos spawnPos = portalBlockPositions.stream()
                .min((a, b) -> Integer.compare(a.getY(), b.getY()))
                .orElseThrow();
        portalInfoMap.put(portalId, new PortalInfo(portalId, dimension, spawnPos, networkName, portalName, facing));
        portalBlockPositions.forEach(pos -> portalBlockMap.put(pos, portalId));
        setDirty();
    }

    public void removePortal(UUID portalId) {
        portalInfoMap.remove(portalId);
        portalBlockMap.values().removeIf(portalId::equals);
        setDirty();
    }

    public PortalInfo getPortalInfo(UUID portalId) {
        return portalInfoMap.get(portalId);
    }

    public PortalInfo getPortalAtBlock(BlockPos portalBlockPos) {
        UUID id = portalBlockMap.get(portalBlockPos);
        return id != null ? portalInfoMap.get(id) : null;
    }

    public List<PortalInfo> getNetworkPortals(String networkName, UUID excludeId) {
        return portalInfoMap.values().stream()
                .filter(p -> p.networkName().equals(networkName) && !p.id().equals(excludeId))
                .toList();
    }

    public List<BlockPos> getPortalBlockPositions(UUID portalId) {
        List<BlockPos> positions = new ArrayList<>();
        for (Map.Entry<BlockPos, UUID> entry : portalBlockMap.entrySet()) {
            if (entry.getValue().equals(portalId)) {
                positions.add(entry.getKey());
            }
        }
        return positions;
    }

    public BlockPos getSpawnPos(UUID portalId) {
        PortalInfo info = portalInfoMap.get(portalId);
        return info != null ? info.spawnPos() : null;
    }

    public record PortalInfo(UUID id, ResourceKey<Level> dimension, BlockPos spawnPos, String networkName, String portalName, Direction facing) {
        public static final Codec<PortalInfo> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("id").forGetter(PortalInfo::id),
                Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(PortalInfo::dimension),
                BlockPos.CODEC.fieldOf("spawnPos").forGetter(PortalInfo::spawnPos),
                Codec.STRING.fieldOf("networkName").forGetter(PortalInfo::networkName),
                Codec.STRING.optionalFieldOf("portalName", "").forGetter(PortalInfo::portalName),
                Codec.INT.optionalFieldOf("facing", 0).xmap(
                        idx -> Direction.from2DDataValue(idx),
                        dir -> dir.get2DDataValue()
                ).forGetter(PortalInfo::facing)
        ).apply(i, PortalInfo::new));
    }
}
