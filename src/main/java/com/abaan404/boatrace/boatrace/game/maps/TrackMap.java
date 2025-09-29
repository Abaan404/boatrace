package com.abaan404.boatrace.boatrace.game.maps;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.abaan404.boatrace.boatrace.BoatRace;

import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.MapTemplateSerializer;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.api.game.world.generator.TemplateChunkGenerator;

/**
 * A track.
 */
public class TrackMap {
    public record Regions(
            BlockBounds finish, List<BlockBounds> checkpoints,
            BlockBounds pitEntry, BlockBounds pitExit, List<BlockBounds> pitBoxes) {
    }

    public record Meta(
            String name, List<String> authors) {
    }

    private final Regions regions;
    private final Meta meta;
    private final MapTemplate template;

    private TrackMap(MapTemplate template) {
        this.template = template;

        BlockBounds finish = template.getMetadata()
                .getRegions("finish")
                .map(TemplateRegion::getBounds)
                .findFirst()
                .orElse(new BlockBounds(BlockPos.ORIGIN, BlockPos.ORIGIN));

        List<BlockBounds> checkpoints = template.getMetadata()
                .getRegions("checkpoints")
                .filter(cp -> cp.getData().getInt("index").isPresent())
                .sorted(Comparator.comparingInt(cp -> cp.getData().getInt("index").get()))
                .map(TemplateRegion::getBounds)
                .toList();

        BlockBounds pitEntry = template.getMetadata()
                .getRegions("pit_entry")
                .map(TemplateRegion::getBounds)
                .findFirst()
                .orElse(new BlockBounds(BlockPos.ORIGIN, BlockPos.ORIGIN));

        BlockBounds pitExit = template.getMetadata()
                .getRegions("pit_exit")
                .map(TemplateRegion::getBounds)
                .findFirst()
                .orElse(new BlockBounds(BlockPos.ORIGIN, BlockPos.ORIGIN));

        List<BlockBounds> pitBoxes = template.getMetadata()
                .getRegions("pit_boxes")
                .map(TemplateRegion::getBounds)
                .toList();

        this.regions = new Regions(
                finish,
                checkpoints,
                pitEntry,
                pitExit,
                pitBoxes);

        String name = template.getMetadata()
                .getData()
                .getString("name", "Unknown");

        List<String> authors = template.getMetadata()
                .getData()
                .getList("authors")
                .map(nbtList -> nbtList.stream()
                        .filter(e -> e instanceof NbtString)
                        .map(e -> ((NbtString) e).asString().orElse("Unknown"))
                        .collect(Collectors.toList()))
                .orElse(List.of());

        this.meta = new Meta(name, authors);
    }

    /**
     * Represents a track loaded from a resource.
     *
     * @param server    The server to load from.
     * @param trackName The name of the track.
     * @return A loaded track.
     */
    public static Optional<TrackMap> load(MinecraftServer server, Identifier identifier) {
        MapTemplate template;

        try {
            template = MapTemplateSerializer.loadFromResource(server, identifier);
        } catch (IOException e) {
            BoatRace.LOGGER.warn("No map with the resource \"{}\" found.", identifier);
            return Optional.empty();
        }

        TrackMap map = new TrackMap(template);

        return Optional.of(map);
    }

    /**
     * Loads every track under map_template/tracks.
     *
     * @param server The server to load from
     * @return A list of every track that successfully loaded.
     */
    public static List<TrackMap> loadAll(MinecraftServer server) {
        return server.getResourceManager().findResources("map_template/tracks",
                id -> id.getNamespace().equals(BoatRace.ID))
                .keySet().stream()
                .map(id -> TrackMap.load(server, id))
                .flatMap(Optional::stream)
                .toList();
    }

    /**
     * Get a check generator for this track.
     *
     * @param server The server to use.
     * @return The chunk generator.
     */
    public ChunkGenerator asGenerator(MinecraftServer server) {
        return new TemplateChunkGenerator(server, this.template);
    }

    /**
     * The regions for this track loaded from its template.
     *
     * @return The regions.
     */
    public Regions getRegions() {
        return this.regions;
    }

    /**
     * The metadata for this track loaded from this template.
     *
     * @return The regions.
     */
    public Meta getMetadata() {
        return this.meta;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((regions == null) ? 0 : regions.hashCode());
        result = prime * result + ((meta == null) ? 0 : meta.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TrackMap other = (TrackMap) obj;
        if (regions == null) {
            if (other.regions != null)
                return false;
        } else if (!regions.equals(other.regions))
            return false;
        if (meta == null) {
            if (other.meta != null)
                return false;
        } else if (!meta.equals(other.meta))
            return false;
        return true;
    }
}
