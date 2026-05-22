package com.abaan404.boatrace;

import java.util.List;
import java.util.function.Function;

import com.abaan404.boatrace.game.race.RaceWidgets;

import eu.pb4.polymer.core.api.item.PolymerCreativeModeTabUtils;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;

public class BoatRaceItems {
    public static final ResourceKey<CreativeModeTab> ITEM_GROUP_KEY = ResourceKey.create(
            BuiltInRegistries.CREATIVE_MODE_TAB.key(),
            Identifier.fromNamespaceAndPath(BoatRace.ID, "item_group"));

    public static final CreativeModeTab ITEM_GROUP = PolymerCreativeModeTabUtils.builder()
            .icon(() -> new ItemStack(Items.OAK_BOAT))
            .title(Component.translatable("itemGroup.boatrace"))
            .build();

    public static final SimplePolymerItem RESET = register("reset", SimplePolymerItem::new,
            new Item.Properties());

    public static final SimplePolymerItem RESPAWN = register("respawn", SimplePolymerItem::new,
            new Item.Properties());

    public static final SimplePolymerItem CYCLE_LEADERBOARD = register("cycle_leaderboard", SimplePolymerItem::new,
            new Item.Properties()
                    .component(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
                            List.of(),
                            List.of(),
                            List.of(RaceWidgets.LeaderboardType.PLAYER.toString()),
                            List.of())));

    public static void initialize() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ITEM_GROUP_KEY, ITEM_GROUP);
        CreativeModeTabEvents.modifyOutputEvent(ITEM_GROUP_KEY).register(itemGroup -> {
            itemGroup.accept(RESET);
            itemGroup.accept(RESPAWN);
            itemGroup.accept(CYCLE_LEADERBOARD);
        });
    }

    public static SimplePolymerItem register(String name,
            Function<SimplePolymerItem.Properties, SimplePolymerItem> itemFactory, Item.Properties settings) {
        ResourceKey<Item> itemKey = ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(BoatRace.ID, name));
        SimplePolymerItem item = itemFactory.apply(settings.setId(itemKey));
        Registry.register(BuiltInRegistries.ITEM, itemKey, item);

        return item;
    }
}
