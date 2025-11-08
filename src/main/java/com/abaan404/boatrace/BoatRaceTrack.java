package com.abaan404.boatrace;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
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
 * The track map to use loaded from a file.
 */
public class BoatRaceTrack {
    private final Regions regions;
    private final Meta meta;
    private final MapTemplate template;

    private BoatRaceTrack(MapTemplate template) {
        this.template = template;

        this.meta = Meta.CODEC
                .decode(NbtOps.INSTANCE, NbtOps.INSTANCE.getMap(template.getMetadata().getData()).getOrThrow())
                .resultOrPartial((error) -> BoatRace.LOGGER.error("Failed to read track meta: {}", error))
                .orElse(Meta.DEFAULT);

        List<RespawnRegion> checkpoints = template.getMetadata()
                .getRegions("checkpoint")
                .filter(cp -> cp.getData().contains("index", NbtElement.INT_TYPE))
                .sorted(Comparator.comparingInt(cp -> cp.getData().getInt("index")))
                .map(RespawnRegion::of)
                .toList();

        // make sure there is atleast one checkpoint in this track
        if (checkpoints.size() == 0) {
            checkpoints = List.of(RespawnRegion.DEFAULT);
        }

        RespawnRegion spawn = template.getMetadata()
                .getRegions("spawn")
                .map(RespawnRegion::of)
                .findFirst()
                .orElse(RespawnRegion.DEFAULT);

        List<RespawnRegion> gridBoxes = template.getMetadata()
                .getRegions("grid_box")
                .filter(cp -> cp.getData().contains("index", NbtElement.INT_TYPE))
                .sorted(Comparator.comparingInt(cp -> cp.getData().getInt("index")))
                .map(RespawnRegion::of)
                .toList();

        RespawnRegion pitEntry = template.getMetadata()
                .getRegions("pit_entry")
                .map(RespawnRegion::of)
                .findFirst()
                .orElse(RespawnRegion.DEFAULT);

        RespawnRegion pitExit = template.getMetadata()
                .getRegions("pit_exit")
                .map(RespawnRegion::of)
                .findFirst()
                .orElse(RespawnRegion.DEFAULT);

        List<RespawnRegion> pitBoxes = template.getMetadata()
                .getRegions("pit_box")
                .filter(cp -> cp.getData().contains("index", NbtElement.INT_TYPE))
                .sorted(Comparator.comparingInt(cp -> cp.getData().getInt("index")))
                .map(RespawnRegion::of)
                .toList();

        this.regions = new Regions(
                checkpoints,
                spawn,
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
    public static Optional<BoatRaceTrack> load(MinecraftServer server, Identifier identifier) {
        MapTemplate template;

        try {
            template = MapTemplateSerializer.loadFromResource(server, identifier);
        } catch (IOException e) {
            BoatRace.LOGGER.warn("No map with the resource \"{}\" found.", identifier);
            return Optional.empty();
        }

        BoatRaceTrack map = new BoatRaceTrack(template);

        return Optional.of(map);
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
        BoatRaceTrack other = (BoatRaceTrack) obj;
        if (meta == null) {
            if (other.meta != null)
                return false;
        } else if (!meta.equals(other.meta))
            return false;
        return true;
    }

    public record RespawnRegion(BlockBounds bounds, float respawnYaw, float respawnPitch) {

        public static RespawnRegion DEFAULT = new RespawnRegion(BlockBounds.ofBlock(BlockPos.ORIGIN), 0.0f, 0.0f);

        private static RespawnRegion of(TemplateRegion templateRegion) {
            float respawnYaw = DEFAULT.respawnYaw();
            float respawnPitch = DEFAULT.respawnPitch();

            if (templateRegion.getData().contains("respawnYaw", NbtElement.FLOAT_TYPE)) {
                respawnYaw = templateRegion.getData().getFloat("respawnYaw");
            }

            if (templateRegion.getData().contains("respawnPitch", NbtElement.FLOAT_TYPE)) {
                respawnPitch = templateRegion.getData().getFloat("respawnPitch");
            }

            return new RespawnRegion(
                    templateRegion.getBounds(),
                    respawnYaw,
                    respawnPitch);
        }
    }

    public record Regions(
            List<RespawnRegion> checkpoints,
            RespawnRegion spawn, List<RespawnRegion> gridBoxes,
            RespawnRegion pitEntry, RespawnRegion pitExit, List<RespawnRegion> pitBoxes) {
    }

    public record Meta(
            String name, List<String> authors, long version,
            Layout layout) {

        public static final Meta DEFAULT = new Meta("Unknown Track", ObjectArrayList.of(), 0, Layout.CIRCULAR);

        public static final MapCodec<Meta> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("name", DEFAULT.name()).forGetter(Meta::name),
                Codec.STRING.listOf().optionalFieldOf("authors", DEFAULT.authors()).forGetter(Meta::authors),
                Codec.LONG.optionalFieldOf("version", DEFAULT.version()).forGetter(Meta::version),
                Layout.CODEC.optionalFieldOf("layout", DEFAULT.layout()).forGetter(Meta::layout))
                .apply(instance, Meta::new));

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
