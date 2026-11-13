package com.kalabushkin;

import com.kalabushkin.item.CarpenterWeaponItem;
import com.kalabushkin.item.ShatuniItem;
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

	public static final Item VLAD_LOGUTOV_SPAWN_EGG = register("vlad_logutov_spawn_egg",
			new SpawnEggItem(ModEntities.VLAD_LOGUTOV, 0x537C7B, 0x16161A, new Item.Settings()));

	public static final Item CARPENTER_PLANE = register("carpenter_plane",
			new CarpenterWeaponItem(CarpenterWeaponItem.Kind.PLANE, new Item.Settings()));
	public static final Item WOODEN_MALLET = register("wooden_mallet",
			new CarpenterWeaponItem(CarpenterWeaponItem.Kind.MALLET, new Item.Settings()));

	public static final Item SHATUNI = register("shatuni", new ShatuniItem(new Item.Settings()));

	public static final ItemGroup GROUP = Registry.register(Registries.ITEM_GROUP, Kalabushkin.id("main"),
			FabricItemGroup.builder()
					.displayName(Text.translatable("itemGroup.kalabushkin"))
					.icon(() -> new ItemStack(SHATUNI))
					.entries((context, entries) -> {
						entries.add(KALABUSHKIN_SPAWN_EGG);
						entries.add(VLAD_LOGUTOV_SPAWN_EGG);
						entries.add(SHATUNI);
						entries.add(CARPENTER_PLANE);
						entries.add(WOODEN_MALLET);
					})
					.build());

	private ModItems() {
	}

	private static Item register(String name, Item item) {
		return Registry.register(Registries.ITEM, Kalabushkin.id(name), item);
	}

	public static void init() {
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS)
				.register(entries -> {
					entries.add(KALABUSHKIN_SPAWN_EGG);
					entries.add(VLAD_LOGUTOV_SPAWN_EGG);
				});
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
			entries.add(CARPENTER_PLANE);
			entries.add(WOODEN_MALLET);
			entries.add(SHATUNI);
		});
	}
}
