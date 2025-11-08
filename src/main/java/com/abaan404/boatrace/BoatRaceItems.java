package com.abaan404.boatrace;

import java.util.List;
import java.util.function.Function;

import com.abaan404.boatrace.game.race.RaceWidgets;

import eu.pb4.polymer.core.api.item.PolymerItemGroupUtils;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class BoatRaceItems {
    public static final RegistryKey<ItemGroup> ITEM_GROUP_KEY = RegistryKey.of(
            Registries.ITEM_GROUP.getKey(),
            Identifier.of(BoatRace.ID, "item_group"));

    public static final ItemGroup ITEM_GROUP = PolymerItemGroupUtils.builder()
            .icon(() -> new ItemStack(Items.OAK_BOAT))
            .displayName(Text.translatable("itemGroup.boatrace"))
            .build();

    public static final SimplePolymerItem RESET = register("reset", SimplePolymerItem::new,
            new SimplePolymerItem.Settings());

    public static final SimplePolymerItem RESPAWN = register("respawn", SimplePolymerItem::new,
            new SimplePolymerItem.Settings());

    public static final SimplePolymerItem CYCLE_LEADERBOARD = register("cycle_leaderboard", SimplePolymerItem::new,
            new SimplePolymerItem.Settings()
                    .component(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(
                            List.of(),
                            List.of(),
                            List.of(RaceWidgets.LeaderboardType.PLAYER.toString()),
                            List.of())));

    public static void initialize() {
        Registry.register(Registries.ITEM_GROUP, ITEM_GROUP_KEY, ITEM_GROUP);
        ItemGroupEvents.modifyEntriesEvent(ITEM_GROUP_KEY).register(itemGroup -> {
            itemGroup.add(RESET);
            itemGroup.add(RESPAWN);
            itemGroup.add(CYCLE_LEADERBOARD);
        });
    }

    public static SimplePolymerItem register(String name,
            Function<SimplePolymerItem.Settings, SimplePolymerItem> itemFactory, SimplePolymerItem.Settings settings) {
        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(BoatRace.ID, name));
        SimplePolymerItem item = itemFactory.apply(settings.registryKey(itemKey));
        Registry.register(Registries.ITEM, itemKey, item);

        return item;
    }
}
