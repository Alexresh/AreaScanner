package ru.obabok.client.util.Lavobsidian;

import fi.dy.masa.malilib.util.data.Color4f;
import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LavaMarker {
    public enum Type {
        // Alpha ~0.85 (0xDD) for waypoints and key targets
        LAVA_WAYPOINT(Color4f.fromColor(0x80FF2222)),          // Red: Main waypoint / lava pool center
        PREDICTED_OBSIDIAN(Color4f.fromColor(0x80FF6600)),     // Fiery Orange: Exact block turning into obsidian
        ANCIENT_DEBRIS(Color4f.fromColor(0x80E5A00D)),         // Amber Gold
        TRIAL_SPAWNER_VAULT(Color4f.fromColor(0x8000E5FF)),    // Electric Cyan
        OBSIDIAN_BLOCK(Color4f.fromColor(0x807A18D1)),         // Deep Purple
        TOUGH_BLOCK(Color4f.fromColor(0x80708090));            // Slate Gray

        public final Color4f defaultColor;

        Type(Color4f defaultColor) {
            this.defaultColor = defaultColor;
        }
    }

    public volatile BlockPos pos;
    public final Type type;
    public final Color4f color;
    public final Set<BlockPos> trackedLavaBlocks;
    public final Set<BlockPos> predictedObsidianBlocks;

    public LavaMarker(BlockPos pos, Type type, Color4f color, Set<BlockPos> trackedLavaBlocks, Set<BlockPos> predictedObsidianBlocks) {
        this.pos = pos;
        this.type = type;
        this.color = color != null ? color : type.defaultColor;
        this.trackedLavaBlocks = ConcurrentHashMap.newKeySet();
        if (trackedLavaBlocks != null) {
            this.trackedLavaBlocks.addAll(trackedLavaBlocks);
        }
        this.predictedObsidianBlocks = predictedObsidianBlocks != null ? Collections.unmodifiableSet(new HashSet<>(predictedObsidianBlocks)) : Collections.emptySet();
    }

    public LavaMarker(BlockPos pos, Type type) {
        this(pos, type, type.defaultColor, Collections.emptySet(), Collections.emptySet());
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
