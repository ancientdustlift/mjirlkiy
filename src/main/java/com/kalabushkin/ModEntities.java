package com.kalabushkin;

import com.kalabushkin.entity.KalabushkinEntity;
import com.kalabushkin.entity.KalabushkinSpiritEntity;
import com.kalabushkin.entity.LittleKochEntity;
import com.kalabushkin.entity.VasilyEntity;
import com.kalabushkin.entity.VladLogutovEntity;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.BiomeKeys;

public final class ModEntities {
	public static final EntityType<KalabushkinEntity> KALABUSHKIN = Registry.register(
			Registries.ENTITY_TYPE,
			Kalabushkin.id("kalabushkin"),
			EntityType.Builder.create(KalabushkinEntity::new, SpawnGroup.CREATURE)
					.dimensions(0.6f, 1.95f)
					.eyeHeight(1.7f)
					.maxTrackingRange(10)
					.build());

	// Never saved: the spirit is re-created next to its owner from the player's attachment.
	public static final EntityType<KalabushkinSpiritEntity> SPIRIT = Registry.register(
			Registries.ENTITY_TYPE,
			Kalabushkin.id("kalabushkin_spirit"),
			EntityType.Builder.create(KalabushkinSpiritEntity::new, SpawnGroup.MISC)
					.dimensions(0.6f, 1.8f)
					.eyeHeight(1.5f)
					.makeFireImmune()
					.disableSaving()
					.disableSummon()
					.maxTrackingRange(10)
					.build());

	// Hitbox covers the rider; the bike's wheels stick out of it front and back.
	public static final EntityType<VladLogutovEntity> VLAD_LOGUTOV = Registry.register(
			Registries.ENTITY_TYPE,
			Kalabushkin.id("vlad_logutov"),
			EntityType.Builder.create(VladLogutovEntity::new, SpawnGroup.CREATURE)
					.dimensions(0.9f, 2.05f)
					.eyeHeight(1.8f)
					.maxTrackingRange(10)
					.build());

	// No natural spawn of his own: he arrives together with Vlad Logutov.
	public static final EntityType<LittleKochEntity> LITTLE_KOCH = Registry.register(
			Registries.ENTITY_TYPE,
			Kalabushkin.id("little_koch"),
			EntityType.Builder.create(LittleKochEntity::new, SpawnGroup.CREATURE)
					.dimensions(0.8f, 1.4f)
					.eyeHeight(1.15f)
					.maxTrackingRange(10)
					.build());

	public static final EntityType<VasilyEntity> VASILY = Registry.register(
			Registries.ENTITY_TYPE,
			Kalabushkin.id("vasily"),
			EntityType.Builder.create(VasilyEntity::new, SpawnGroup.CREATURE)
					.dimensions(0.6f, 1.95f)
					.eyeHeight(1.7f)
					.maxTrackingRange(10)
					.build());

	private ModEntities() {
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(KALABUSHKIN, KalabushkinEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(SPIRIT, KalabushkinSpiritEntity.createAttributes());

		FabricDefaultAttributeRegistry.register(VLAD_LOGUTOV, VladLogutovEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(LITTLE_KOCH, LittleKochEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(VASILY, VasilyEntity.createAttributes());

		SpawnRestriction.register(KALABUSHKIN, SpawnLocationTypes.ON_GROUND,
				Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, KalabushkinEntity::canSpawnHere);

		BiomeModifications.addSpawn(
				BiomeSelectors.includeByKey(BiomeKeys.PLAINS, BiomeKeys.SUNFLOWER_PLAINS, BiomeKeys.MEADOW,
						BiomeKeys.FOREST, BiomeKeys.BIRCH_FOREST, BiomeKeys.FLOWER_FOREST, BiomeKeys.TAIGA,
						BiomeKeys.SAVANNA, BiomeKeys.CHERRY_GROVE),
				SpawnGroup.CREATURE, KALABUSHKIN, 4, 1, 1);

		SpawnRestriction.register(VLAD_LOGUTOV, SpawnLocationTypes.ON_GROUND,
				Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, VladLogutovEntity::canSpawnHere);
		BiomeModifications.addSpawn(
				BiomeSelectors.includeByKey(BiomeKeys.PLAINS, BiomeKeys.SUNFLOWER_PLAINS, BiomeKeys.MEADOW,
						BiomeKeys.SAVANNA),
				SpawnGroup.CREATURE, VLAD_LOGUTOV, 4, 1, 1);

		SpawnRestriction.register(VASILY, SpawnLocationTypes.ON_GROUND,
				Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, VasilyEntity::canSpawnHere);
		BiomeModifications.addSpawn(
				BiomeSelectors.includeByKey(BiomeKeys.PLAINS, BiomeKeys.FOREST, BiomeKeys.BIRCH_FOREST,
						BiomeKeys.TAIGA),
				SpawnGroup.CREATURE, VASILY, 3, 1, 1);
	}
}
