package dev.romankrukovsky.kubanhorizons.registry;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.entity.Pheasant;
import dev.romankrukovsky.kubanhorizons.entity.Quail;
import dev.romankrukovsky.kubanhorizons.util.KHIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Регистрация региональной фауны. */
public final class KHEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, KubanHorizons.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<Pheasant>> PHEASANT =
            ENTITIES.register("pheasant", () -> EntityType.Builder
                    .of(Pheasant::new, MobCategory.CREATURE)
                    .sized(0.65F, 0.8F)
                    .eyeHeight(0.68F)
                    .clientTrackingRange(8)
                    .build(key("pheasant")));

    public static final DeferredHolder<EntityType<?>, EntityType<Quail>> QUAIL =
            ENTITIES.register("quail", () -> EntityType.Builder
                    .of(Quail::new, MobCategory.CREATURE)
                    .sized(0.45F, 0.48F)
                    .eyeHeight(0.4F)
                    .clientTrackingRange(8)
                    .build(key("quail")));

    private KHEntities() {
    }

    private static ResourceKey<EntityType<?>> key(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, KHIds.of(name));
    }

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
    }
}
