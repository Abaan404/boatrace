package com.abaan404.boatrace.boatrace.game.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;

public class BoatRaceMapConfig {
    public static final Codec<BoatRaceMapConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.fieldOf("spawn_block").forGetter(map -> map.spawnBlock)
    ).apply(instance, BoatRaceMapConfig::new));

    public final BlockState spawnBlock;

    public BoatRaceMapConfig(BlockState spawnBlock) {
        this.spawnBlock = spawnBlock;
    }
}
