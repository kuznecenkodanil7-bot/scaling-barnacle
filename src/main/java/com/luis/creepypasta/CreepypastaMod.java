package com.luis.creepypasta;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public final class CreepypastaMod implements ModInitializer {
    public static final String MOD_ID = "creepypasta";

    public static final RegistryKey<World> NIGHTMARE_WORLD = RegistryKey.of(RegistryKeys.WORLD, id("nightmare"));

    public static final SoundEvent SCREAMER_SOUND = registerSound("entity.haunter.screamer");
    public static final SoundEvent BREATH_SOUND = registerSound("entity.haunter.breath");
    public static final SoundEvent AMBIENT_SOUND = registerSound("entity.haunter.ambient");

    public static final Block DREAD_PORTAL = registerBlock("dread_portal",
            new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, id("dread_portal")))
                    .mapColor(MapColor.BLACK)
                    .strength(-1.0F, 3600000.0F)
                    .luminance(state -> 8)
                    .noCollision()
                    .sounds(BlockSoundGroup.GLASS)));

    public static final EntityType<HaunterEntity> HAUNTER = registerHaunter();

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    private static SoundEvent registerSound(String path) {
        Identifier id = id(path);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    private static Block registerBlock(String path, Block block) {
        return Registry.register(Registries.BLOCK, id(path), block);
    }

    private static EntityType<HaunterEntity> registerHaunter() {
        RegistryKey<EntityType<?>> key = RegistryKey.of(RegistryKeys.ENTITY_TYPE, id("haunter"));
        EntityType<HaunterEntity> type = EntityType.Builder.create(HaunterEntity::new, SpawnGroup.MONSTER)
                .dimensions(0.6F, 1.95F)
                .eyeHeight(1.74F)
                .build(key);
        return Registry.register(Registries.ENTITY_TYPE, key, type);
    }

    @Override
    public void onInitialize() {
        // Touch vanilla class so missing imports fail early in dev, not in gameplay.
        Blocks.STONE.getDefaultState();
        FabricDefaultAttributeRegistry.register(HAUNTER, HaunterEntity.createHaunterAttributes());
        ServerTickEvents.END_SERVER_TICK.register(HorrorManager::onServerTick);
    }
}
