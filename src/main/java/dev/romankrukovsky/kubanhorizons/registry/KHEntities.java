package dev.romankrukovsky.kubanhorizons.registry;

import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import dev.romankrukovsky.kubanhorizons.entity.Pheasant;
import dev.romankrukovsky.kubanhorizons.entity.Quail;
import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
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

    public static final DeferredHolder<EntityType<?>, EntityType<KubanGenie>> KUBAN_GENIE =
            ENTITIES.register("kuban_genie", () -> EntityType.Builder
                    .of(KubanGenie::new, MobCategory.CREATURE)
                    .sized(0.8F, 2.4F)
                    .eyeHeight(2.05F)
                    .clientTrackingRange(10)
                    .build(key("kuban_genie")));

    /**
     * Дикий кабан: давление на незащищённое хозяйство.
     *
     * <p>MONSTER, а не CREATURE: спавн должен подчиняться темноте и лимиту
     * враждебных, иначе кабаны заполнят степь днём и потрава станет постоянной,
     * а не ночным событием. Радиус трекинга больше птичьего — игрок должен
     * увидеть кабана до того, как тот дойдёт до грядок.</p>
     */
    public static final DeferredHolder<EntityType<?>, EntityType<dev.romankrukovsky.kubanhorizons.entity.WildBoar>> WILD_BOAR =
            ENTITIES.register("wild_boar", () -> EntityType.Builder
                    .of(dev.romankrukovsky.kubanhorizons.entity.WildBoar::new, MobCategory.MONSTER)
                    .sized(1.0F, 1.05F)
                    .eyeHeight(0.9F)
                    .clientTrackingRange(10)
                    .build(key("wild_boar")));

    /**
     * Нутрия: вредитель оросительной сети в плавнях и лиманах.
     *
     * <p>CREATURE, а не MONSTER: она не угроза игроку и должна спавниться днём
     * у воды, как обычное животное. Урон она наносит инфраструктуре, а не
     * здоровью.</p>
     */
    public static final DeferredHolder<EntityType<?>, EntityType<dev.romankrukovsky.kubanhorizons.entity.Nutria>> NUTRIA =
            ENTITIES.register("nutria", () -> EntityType.Builder
                    .of(dev.romankrukovsky.kubanhorizons.entity.Nutria::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.6F)
                    .eyeHeight(0.5F)
                    .clientTrackingRange(8)
                    .build(key("nutria")));

    /**
     * Саранча: сезонное давление на посевы с воздуха.
     *
     * <p>MONSTER: налёт должен быть событием, ограниченным лимитом враждебных,
     * а не постоянным фоном. Сама особь живёт около четырёх игровых минут —
     * популяция самоограничена по времени, а не по числу.</p>
     */
    public static final DeferredHolder<EntityType<?>, EntityType<dev.romankrukovsky.kubanhorizons.entity.Locust>> LOCUST =
            ENTITIES.register("locust", () -> EntityType.Builder
                    .of(dev.romankrukovsky.kubanhorizons.entity.Locust::new, MobCategory.MONSTER)
                    .sized(0.4F, 0.35F)
                    .eyeHeight(0.25F)
                    .clientTrackingRange(8)
                    .build(key("locust")));


    /**
     * Кавказская овчарка: ответ игрока на давление.
     *
     * <p>CREATURE и приручаемая: это не дикая фауна, а инструмент защиты.
     * Существует ровно для того, чтобы у кабана и нутрии был контр-приём кроме
     * ограды, иначе давление остаётся безответным налогом.</p>
     */
    public static final DeferredHolder<EntityType<?>, EntityType<dev.romankrukovsky.kubanhorizons.entity.CaucasianShepherd>> CAUCASIAN_SHEPHERD =
            ENTITIES.register("caucasian_shepherd", () -> EntityType.Builder
                    .of(dev.romankrukovsky.kubanhorizons.entity.CaucasianShepherd::new, MobCategory.CREATURE)
                    .sized(0.9F, 1.0F)
                    .eyeHeight(0.9F)
                    .clientTrackingRange(10)
                    .build(key("caucasian_shepherd")));

    /** Осётр: крупная речная рыба поймы и лиманов, сырьё коптильни. */
    public static final DeferredHolder<EntityType<?>, EntityType<dev.romankrukovsky.kubanhorizons.entity.Sturgeon>> STURGEON =
            ENTITIES.register("sturgeon", () -> EntityType.Builder
                    .of(dev.romankrukovsky.kubanhorizons.entity.Sturgeon::new, MobCategory.WATER_AMBIENT)
                    .sized(0.9F, 0.5F)
                    .eyeHeight(0.3F)
                    .clientTrackingRange(4)
                    .build(key("sturgeon")));

    /** Чайка: падальщик побережий и лиманов. */
    public static final DeferredHolder<EntityType<?>, EntityType<dev.romankrukovsky.kubanhorizons.entity.Gull>> GULL =
            ENTITIES.register("gull", () -> EntityType.Builder
                    .of(dev.romankrukovsky.kubanhorizons.entity.Gull::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.6F)
                    .eyeHeight(0.5F)
                    .clientTrackingRange(8)
                    .build(key("gull")));

    /** Цапля: хищник мелкой рыбы на отмелях плавней. */
    public static final DeferredHolder<EntityType<?>, EntityType<dev.romankrukovsky.kubanhorizons.entity.Heron>> HERON =
            ENTITIES.register("heron", () -> EntityType.Builder
                    .of(dev.romankrukovsky.kubanhorizons.entity.Heron::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.4F)
                    .eyeHeight(1.3F)
                    .clientTrackingRange(10)
                    .build(key("heron")));

    /** Магический клон-двойник игрока. */
    public static final DeferredHolder<EntityType<?>, EntityType<dev.romankrukovsky.kubanhorizons.entity.MagicDoppelgangerEntity>> MAGIC_DOPPELGANGER =
            ENTITIES.register("magic_doppelganger", () -> EntityType.Builder
                    .of(dev.romankrukovsky.kubanhorizons.entity.MagicDoppelgangerEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F)
                    .eyeHeight(1.6F)
                    .clientTrackingRange(10)
                    .build(key("magic_doppelganger")));

    /** Ошибка Реальности: концептуальная сущность, неуязвимая к урону. */
    public static final DeferredHolder<EntityType<?>, EntityType<dev.romankrukovsky.kubanhorizons.entity.RealityErrorEntity>> REALITY_ERROR =
            ENTITIES.register("reality_error", () -> EntityType.Builder
                    .of(dev.romankrukovsky.kubanhorizons.entity.RealityErrorEntity::new, MobCategory.MISC)
                    .sized(0.9F, 2.4F)
                    .eyeHeight(2.2F)
                    .clientTrackingRange(10)
                    .build(key("reality_error")));

    /** Желание, ставшее существом: светящийся спутник с сутью желания. */
    public static final DeferredHolder<EntityType<?>, EntityType<dev.romankrukovsky.kubanhorizons.entity.WishCreatureEntity>> WISH_CREATURE =
            ENTITIES.register("wish_creature", () -> EntityType.Builder
                    .of(dev.romankrukovsky.kubanhorizons.entity.WishCreatureEntity::new, MobCategory.CREATURE)
                    .sized(0.4F, 0.6F)
                    .eyeHeight(0.5F)
                    .clientTrackingRange(10)
                    .build(key("wish_creature")));

    /**
     * Кубанский манул: талисман мода.
     *
     * <p>CREATURE и приручаемый в смысле «признал участок», но не питомец:
     * доверие набирается днями, а следовать за игроком он не станет ни на
     * какой ступени. Радиус трекинга большой — встреча должна начинаться с
     * того, что зверь заметил игрока первым.</p>
     */
    public static final DeferredHolder<EntityType<?>, EntityType<dev.romankrukovsky.kubanhorizons.entity.Manul>> MANUL =
            ENTITIES.register("manul", () -> EntityType.Builder
                    .of(dev.romankrukovsky.kubanhorizons.entity.Manul::new, MobCategory.CREATURE)
                    .sized(0.7F, 0.6F)
                    .eyeHeight(0.5F)
                    .clientTrackingRange(10)
                    .build(key("manul")));

    private KHEntities() {
    }

    private static ResourceKey<EntityType<?>> key(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, KHIds.of(name));
    }

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
    }
}
