package com.abaan404.boatrace;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.MapTemplateSerializer;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.api.game.GameOpenException;
import xyz.nucleoid.plasmid.api.game.world.generator.TemplateChunkGenerator;

/**
 * The track map to use loaded from a file.
 */
public class BoatRaceTrack {
    private final Regions regions;
    private final Meta meta;
    private final Attributes attributes;

    private final MapTemplate template;

    private static final int CURRENT_TRACK_FORMAT = 2;

    private BoatRaceTrack(MapTemplate template) {
        this.template = template;

        int trackFormat = template.getMetadata()
                .getData()
                .getIntOr("track_format", 0);

        if (trackFormat < CURRENT_TRACK_FORMAT) {
            throw new GameOpenException(Component.nullToEmpty("This track was built for an earlier version of boatrace."));
        } else if (trackFormat > CURRENT_TRACK_FORMAT) {
            throw new GameOpenException(Component.nullToEmpty("This track was built for a future version of boatrace."));
        }

        this.meta = template.getMetadata()
                .getData()
                .read("meta", Meta.CODEC.codec())
                .orElse(Meta.DEFAULT);

        this.attributes = template.getMetadata()
                .getData()
                .read("attributes", Attributes.CODEC.codec())
                .orElse(Attributes.DEFAULT);

        List<Set<RespawnRegion>> checkpoints = template.getMetadata()
                .getRegions("checkpoint")
                .filter(cp -> cp.getData().getInt("index").isPresent())
                .collect(Collectors.groupingBy(
                        cp -> cp.getData().getInt("index").orElseThrow(),
                        Collectors.mapping(RespawnRegion::of, Collectors.toSet())))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .toList();

        RespawnRegion spawn = template.getMetadata()
                .getRegions("spawn")
                .map(RespawnRegion::of)
                .findFirst()
                .orElse(RespawnRegion.DEFAULT);

        List<RespawnRegion> gridBoxes = template.getMetadata()
                .getRegions("grid_box")
                .filter(gb -> gb.getData().getInt("index").isPresent())
                .sorted(Comparator.comparingInt(gb -> gb.getData().getInt("index").orElseThrow()))
                .map(RespawnRegion::of)
                .toList();

        Optional<RespawnRegion> pitLane = template.getMetadata()
                .getRegions("pit_lane")
                .map(RespawnRegion::of)
                .findFirst();

        this.regions = new Regions(
                checkpoints,
                spawn,
                gridBoxes,
                pitLane);
    }

    /**
     * Represents a track loaded from a resource.
     *
     * @param server     The server to load from.
     * @param identifier The resource id of the track
     * @return A loaded track.
     */
    public static BoatRaceTrack load(MinecraftServer server, Identifier identifier) {
        MapTemplate template;

        try {
            template = MapTemplateSerializer.loadFromResource(server, identifier);
        } catch (IOException e) {
            throw new GameOpenException(Component.nullToEmpty(String.format("Couldn't load track {}", identifier.toString())));
        }

        return new BoatRaceTrack(template);
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

    /**
     * Misc attributes loaded from its template.
     *
     * @return The attributes.
     */
    public Attributes getAttributes() {
        return attributes;
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

    public record RespawnRegion(BlockBounds bounds, float yaw, float pitch) {

        public static RespawnRegion DEFAULT = new RespawnRegion(BlockBounds.ofBlock(BlockPos.ZERO), 0.0f, 0.0f);

        private static RespawnRegion of(TemplateRegion templateRegion) {
            return new RespawnRegion(
                    templateRegion.getBounds(),
                    templateRegion.getData().getFloatOr("yaw", DEFAULT.yaw()),
                    templateRegion.getData().getFloatOr("pitch", DEFAULT.pitch()));
        }

        /**
         * Check if an entity's intersected the region bounds.
         *
         * @param pos     The entity's current pos.
         * @param lastPos The entity's pos last tick.
         * @return If they intersected.
         */
        public boolean intersect(Vec3 pos, Vec3 lastPos) {
            return this.bounds().asBox().intersects(pos, lastPos);
        }
    }

    public record Regions(
            List<Set<RespawnRegion>> checkpoints,
            RespawnRegion spawn, List<RespawnRegion> gridBoxes,
            Optional<RespawnRegion> pitLane) {
    }

    public record Attributes(
            int timeOfDay,
            Layout layout) {

        public static final Attributes DEFAULT = new Attributes(6000, Layout.CIRCULAR);

        public static final MapCodec<Attributes> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.INT.optionalFieldOf("time_of_day", DEFAULT.timeOfDay()).forGetter(Attributes::timeOfDay),
                Layout.CODEC.optionalFieldOf("layout", DEFAULT.layout()).forGetter(Attributes::layout))
                .apply(instance, Attributes::new));
    }

    public record Meta(
            String name, List<String> authors, Optional<String> description, Optional<String> url) {

        public static final Meta DEFAULT = new Meta("Unknown Track", List.of("Unknown Authors"), Optional.empty(),
                Optional.empty());

        public static final MapCodec<Meta> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("name", DEFAULT.name()).forGetter(Meta::name),
                Codec.STRING.listOf().optionalFieldOf("authors", DEFAULT.authors()).forGetter(Meta::authors),
                Codec.STRING.optionalFieldOf("description").forGetter(Meta::description),
                Codec.STRING.optionalFieldOf("url").forGetter(Meta::url))
                .apply(instance, Meta::new));

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
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
            Meta other = (Meta) obj;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            return true;
        }
    }

    public enum Layout implements StringRepresentable {
        CIRCULAR("circular"),
        LINEAR("linear");

        private final String name;

        public static final Codec<Layout> CODEC = StringRepresentable.fromEnum(Layout::values);

        Layout(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
