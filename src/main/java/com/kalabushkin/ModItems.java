package com.kalabushkin;

import com.kalabushkin.item.ArchitectToolItem;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;

public final class ModItems {
	public static final Item KALABUSHKIN_SPAWN_EGG = register("kalabushkin_spawn_egg",
			new SpawnEggItem(ModEntities.KALABUSHKIN, 0x84223A, 0x2C86C4, new Item.Settings()));

	public static final Item ARCHITECT_RULER = tool("architect_ruler");
	public static final Item DRAFTING_COMPASS = tool("drafting_compass");
	public static final Item SET_SQUARE = tool("set_square");
	public static final Item ARCHITECT_PENCIL = tool("architect_pencil");
	public static final Item BLUEPRINT = tool("blueprint");

	public static final ItemGroup GROUP = Registry.register(Registries.ITEM_GROUP, Kalabushkin.id("main"),
			FabricItemGroup.builder()
					.displayName(Text.translatable("itemGroup.kalabushkin"))
					.icon(() -> new ItemStack(DRAFTING_COMPASS))
					.entries((context, entries) -> {
						entries.add(KALABUSHKIN_SPAWN_EGG);
						entries.add(ARCHITECT_RULER);
						entries.add(DRAFTING_COMPASS);
						entries.add(SET_SQUARE);
						entries.add(ARCHITECT_PENCIL);
						entries.add(BLUEPRINT);
					})
					.build());

	private ModItems() {
	}

	private static Item tool(String name) {
		return register(name, new ArchitectToolItem(new Item.Settings().maxCount(1)));
	}

	private static Item register(String name, Item item) {
		return Registry.register(Registries.ITEM, Kalabushkin.id(name), item);
	}

	public static void init() {
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS)
				.register(entries -> entries.add(KALABUSHKIN_SPAWN_EGG));
	}
}
