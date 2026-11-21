package ru.obabok.client.util.ObsidianPredictor;

import fi.dy.masa.malilib.util.data.Color4f;
import net.minecraft.core.BlockPos;
import ru.obabok.client.Config;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LavaMarker {
    public enum Type {
        // Alpha ~0.85 (0xDD) for waypoints and key targets
        LAVA_WAYPOINT,          // Red: Main waypoint / lava pool center
        ANCIENT_DEBRIS,         // Amber Gold
        TRIAL_SPAWNER_VAULT,    // Electric Cyan
        OBSIDIAN_BLOCK,         // Deep Purple
        TOUGH_BLOCK         // Slate Gray
    }

    public volatile BlockPos pos;
    public final Type type;
    public final Set<BlockPos> trackedLavaBlocks;
    public final Set<BlockPos> predictedObsidianBlocks;

    public LavaMarker(BlockPos pos, Type type, Set<BlockPos> trackedLavaBlocks, Set<BlockPos> predictedObsidianBlocks) {
        this.pos = pos;
        this.type = type;
        this.trackedLavaBlocks = ConcurrentHashMap.newKeySet();
        if (trackedLavaBlocks != null) {
            this.trackedLavaBlocks.addAll(trackedLavaBlocks);
        }
        this.predictedObsidianBlocks = predictedObsidianBlocks != null ? Collections.unmodifiableSet(new HashSet<>(predictedObsidianBlocks)) : Collections.emptySet();
    }

    public LavaMarker(BlockPos pos, Type type) {
        this(pos, type, Collections.emptySet(), Collections.emptySet());
    }

    public static Color4f getMarkerColor(LavaMarker.Type type) {
        return switch (type)
        {
            case LAVA_WAYPOINT -> Config.Generic.OBSIDIAN_PREDICTOR_LAVA_COLOR.getColor();
            case ANCIENT_DEBRIS -> Config.Generic.OBSIDIAN_PREDICTOR_ANCIENT_DEBRIS_COLOR.getColor();
            case TRIAL_SPAWNER_VAULT -> Config.Generic.OBSIDIAN_PREDICTOR_TRIAL_SPAWNER_VAULT_COLOR.getColor();
            case OBSIDIAN_BLOCK -> Config.Generic.OBSIDIAN_PREDICTOR_OBSIDIAN_COLOR.getColor();
            case TOUGH_BLOCK -> Config.Generic.OBSIDIAN_PREDICTOR_TOUGH_BLOCK_COLOR.getColor();
            case null -> Config.Generic.SELECTED_BLOCKS_COLOR.getColor();
        };

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LavaMarker that)) return false;
        return pos.equals(that.pos) && type == that.type;
    }

    @Override
    public int hashCode() {
        return 31 * pos.hashCode() + type.hashCode();
    }
}
