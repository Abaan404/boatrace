package com.abaan404.boatrace.boatrace.game.maps;

import java.io.IOException;
import java.util.Optional;

import com.abaan404.boatrace.boatrace.BoatRace;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.MapTemplateSerializer;
import xyz.nucleoid.plasmid.api.game.world.generator.TemplateChunkGenerator;

/**
 * A lobby.
 */
public class LobbyMap {
    public record Regions(
            BlockPos spawn,
            BlockPos trackSelect, BlockPos formatSelect, BlockPos modeSelect) {
    }

    private final MapTemplate template;
    private final Regions regions;

    private LobbyMap(MapTemplate template) {
        this.template = template;

        BlockPos spawn = template
                .getMetadata()
                .getRegions("spawn")
                .map(region -> region.getBounds())
                .findFirst()
                .orElse(new BlockBounds(BlockPos.ORIGIN, BlockPos.ORIGIN))
                .min();

        BlockPos trackSelect = template
                .getMetadata()
                .getRegions("trackSelect")
                .map(region -> region.getBounds())
                .findFirst()
                .orElse(new BlockBounds(BlockPos.ORIGIN, BlockPos.ORIGIN))
                .min();

        BlockPos formatSelect = template
                .getMetadata()
                .getRegions("formatSelect")
                .map(region -> region.getBounds())
                .findFirst()
                .orElse(new BlockBounds(BlockPos.ORIGIN, BlockPos.ORIGIN))
                .min();

        BlockPos modeSelect = template
                .getMetadata()
                .getRegions("modeSelect")
                .map(region -> region.getBounds())
                .findFirst()
                .orElse(new BlockBounds(BlockPos.ORIGIN, BlockPos.ORIGIN))
                .min();

        this.regions = new Regions(spawn, trackSelect, formatSelect, modeSelect);
    }

    /**
     * Load the lobby map
     *
     * @param server The server to load from
     * @param config The game's config
     * @return A lobby map
     */
    public static Optional<LobbyMap> load(MinecraftServer server, Identifier identifier) {
        MapTemplate template;

        try {
            template = MapTemplateSerializer.loadFromResource(server, identifier);
        } catch (IOException e) {
            BoatRace.LOGGER.error("Could not find the lobby map {}", identifier);
            return Optional.empty();
        }

        return Optional.of(new LobbyMap(template));
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
}
