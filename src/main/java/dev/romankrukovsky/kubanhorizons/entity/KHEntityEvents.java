package dev.romankrukovsky.kubanhorizons.entity;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.config.KHServerConfig;
import dev.romankrukovsky.kubanhorizons.registry.KHEntities;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/** Атрибуты, физические ограничения и конфигурация естественного спавна. */
@EventBusSubscriber(modid = KubanHorizons.MOD_ID)
public final class KHEntityEvents {
    private KHEntityEvents() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(KHEntities.PHEASANT.get(), Pheasant.createAttributes().build());
        event.put(KHEntities.QUAIL.get(), Quail.createAttributes().build());
        event.put(KHEntities.WILD_BOAR.get(), WildBoar.createAttributes().build());
        event.put(KHEntities.NUTRIA.get(), Nutria.createAttributes().build());
        event.put(KHEntities.LOCUST.get(), Locust.createAttributes().build());
        event.put(KHEntities.CAUCASIAN_SHEPHERD.get(), CaucasianShepherd.createAttributes().build());
        event.put(KHEntities.STURGEON.get(), Sturgeon.createAttributes().build());
        event.put(KHEntities.GULL.get(), Gull.createAttributes().build());
        event.put(KHEntities.HERON.get(), Heron.createAttributes().build());
        event.put(KHEntities.MANUL.get(), Manul.createAttributes().build());
        event.put(KHEntities.KUBAN_GENIE.get(), KubanGenie.createAttributes().build());
        event.put(KHEntities.MAGIC_DOPPELGANGER.get(), MagicDoppelgangerEntity.createAttributes().build());
        event.put(KHEntities.REALITY_ERROR.get(), RealityErrorEntity.createAttributes().build());
        event.put(KHEntities.WISH_CREATURE.get(), WishCreatureEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(KHEntities.PHEASANT.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(KHEntities.QUAIL.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(KHEntities.WILD_BOAR.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(KHEntities.NUTRIA.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(KHEntities.CAUCASIAN_SHEPHERD.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(KHEntities.GULL.get(), SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(KHEntities.HERON.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(KHEntities.MANUL.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Manul::checkManulSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // Осётр — водная рыба: ванильные правила спавна для WaterAnimal.
        event.register(KHEntities.STURGEON.get(), SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.animal.fish.WaterAnimal::checkSurfaceWaterAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // Саранча приходит налётом через PressureScheduler, а не естественным
        // спавном: событие должно быть управляемым, иначе рой станет фоном.
    }

    @SubscribeEvent
    public static void limitNaturalSpawns(FinalizeSpawnEvent event) {
        if (!KHServerConfig.groundBirdSpawnsEnabled()
                && event.getEntity() instanceof AbstractGroundBird
                && event.getSpawnType() == EntitySpawnReason.NATURAL) {
            event.setSpawnCancelled(true);
        }
        // Манул выключается отдельно от птиц: он талисман, а не давление, и
        // сервер может убрать его, не трогая остальную фауну.
        if (!KHServerConfig.manulSpawnsEnabled()
                && event.getEntity() instanceof Manul
                && event.getSpawnType() == EntitySpawnReason.NATURAL) {
            event.setSpawnCancelled(true);
        }
    }
}
