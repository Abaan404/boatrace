package com.abaan404.boatrace.maps;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.abaan404.boatrace.BoatRace;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
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
    private final Regions regions;
    private final Meta meta;
    private final MapTemplate template;

    private TrackMap(MapTemplate template) {
        this.template = template;

        this.meta = template.getMetadata()
                .getData()
                .decode(Meta.CODEC)
                .orElse(Meta.of());

        List<RespawnRegion> checkpoints = template.getMetadata()
                .getRegions("checkpoint")
                .filter(cp -> cp.getData().getInt("index").isPresent())
                .sorted(Comparator.comparingInt(cp -> cp.getData().getInt("index").orElseThrow()))
                .map(RespawnRegion::of)
                .toList();

        // make sure there is atleast one checkpoint in this track
        if (checkpoints.size() == 0) {
            checkpoints.add(RespawnRegion.of());
        }

        List<RespawnRegion> gridBoxes = template.getMetadata()
                .getRegions("grid_box")
                .filter(gb -> gb.getData().getInt("index").isPresent())
                .sorted(Comparator.comparingInt(gb -> gb.getData().getInt("index").orElseThrow()))
                .map(RespawnRegion::of)
                .toList();

        BlockBounds pitEntry = template.getMetadata()
                .getRegions("pit_entry")
                .map(TemplateRegion::getBounds)
                .findFirst()
                .orElse(BlockBounds.ofBlock(BlockPos.ORIGIN));

        BlockBounds pitExit = template.getMetadata()
                .getRegions("pit_exit")
                .map(TemplateRegion::getBounds)
                .findFirst()
                .orElse(BlockBounds.ofBlock(BlockPos.ORIGIN));

        List<RespawnRegion> pitBoxes = template.getMetadata()
                .getRegions("pit_box")
                .filter(pb -> pb.getData().getInt("index").isPresent())
                .sorted(Comparator.comparingInt(pb -> pb.getData().getInt("index").orElseThrow()))
                .map(RespawnRegion::of)
                .toList();

        this.regions = new Regions(
                checkpoints,
                gridBoxes,
                pitEntry,
                pitExit,
                pitBoxes);
    }

    /**
     * Represents a track loaded from a resource.
     *
     * @param server     The server to load from.
     * @param identifier The resource id of the track
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
    public Meta getMeta() {
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

    public record RespawnRegion(BlockBounds bounds, float respawnYaw, float respawnPitch) {
        public static RespawnRegion of() {
            return new RespawnRegion(BlockBounds.ofBlock(BlockPos.ORIGIN), 0.0f, 0.0f);
        }

        private static RespawnRegion of(TemplateRegion templateRegion) {
            return new RespawnRegion(
                    templateRegion.getBounds(),
                    templateRegion.getData().getFloat("respawnYaw", 0.0f),
                    templateRegion.getData().getFloat("respawnPitch", 0.0f));
        }
    }

    public record Regions(
            List<RespawnRegion> checkpoints,
            List<RespawnRegion> gridBoxes,
            BlockBounds pitEntry, BlockBounds pitExit, List<RespawnRegion> pitBoxes) {
    }

    public record Meta(
            String name, List<String> authors,
            Layout layout) {

        public static final MapCodec<Meta> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("name", "Unknown Track").forGetter(Meta::name),
                Codec.STRING.listOf().optionalFieldOf("authors", List.of()).forGetter(Meta::authors),
                Layout.CODEC.optionalFieldOf("layout", Layout.CIRCULAR).forGetter(Meta::layout))
                .apply(instance, Meta::new));

        public static Meta of() {
            return new Meta("Unknown Track", List.of(), Layout.CIRCULAR);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + ((authors == null) ? 0 : authors.hashCode());

            // use enum's names for serializing hashCodes
            result = prime * result + ((layout == null) ? 0 : layout.toString().hashCode());
            return result;
        }
    }

    public enum Layout implements StringIdentifiable {
        CIRCULAR("circular"),
        LINEAR("linear");

        private final String name;

        public static final Codec<Layout> CODEC = StringIdentifiable.createCodec(Layout::values);

        Layout(String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return this.name;
        }
    }
}
