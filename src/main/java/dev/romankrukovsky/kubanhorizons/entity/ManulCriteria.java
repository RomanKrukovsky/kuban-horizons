package dev.romankrukovsky.kubanhorizons.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.romankrukovsky.kubanhorizons.KubanHorizons;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContextSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;

/**
 * Собственные триггеры достижений про манула.
 *
 * <h2>Зачем свои триггеры</h2>
 *
 * <p>Все четыре достижения про манула описывают состояния <em>зверя</em>
 * (доверие, окрас, поселился ли он) и <em>поведение игрока</em> (сколько
 * он наблюдал, не подходя). Ванильные триггеры этого не видят:
 * {@code inventory_changed} требует предмет, а манул не предмет;
 * {@code tame_animal} срабатывает на ванильное приручение, которого у
 * манула нет по замыслу — он не становится домашним котом ни на одной
 * ступени доверия.</p>
 *
 * <p>Поэтому триггеры свои, но минимальные: у каждого — только предикат
 * игрока, а всё условие проверено на стороне вызывающего кода. Это
 * сознательный выбор: условие «доверие максимально» уже проверено там, где
 * доверие меняется, и дублировать его в кодеке предиката значило бы
 * держать одно правило в двух местах.</p>
 *
 * <p>Каждый триггер вызывается ровно из одного места, и это место указано
 * в его javadoc — если триггер не вызывается, достижение недостижимо, а
 * недостижимое достижение хуже отсутствующего.</p>
 */
public final class ManulCriteria {
    private static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, KubanHorizons.MOD_ID);

    /**
     * «Манул тебя терпит»: доверие зверя достигло максимума
     * ({@link ManulTrust#RESIDENT}).
     *
     * <p>Вызывается из {@code ManulWorldEvents.checkSettlement}, который
     * раз в пять секунд смотрит на доверие манулов у укрытий игрока.</p>
     */
    public static final DeferredHolder<CriterionTrigger<?>, SimpleTrigger> MANUL_TRUSTED =
            TRIGGERS.register("manul_trusted", SimpleTrigger::new);

    /**
     * «Опора станицы»: манул с максимальным доверием поселился в укрытии
     * рядом с усадьбой игрока.
     *
     * <p>Вызывается из {@code ManulWorldEvents.checkSettlement} — там же,
     * где проверяется занятость укрытия.</p>
     */
    public static final DeferredHolder<CriterionTrigger<?>, SimpleTrigger> MANUL_SETTLED =
            TRIGGERS.register("manul_settled", SimpleTrigger::new);

    /**
     * «Не трогай кота»: игрок долго наблюдал за диким манулом, не приближаясь.
     *
     * <p>Вызывается из {@code ManulObservation.onPlayerTick} по накоплении
     * порога выдержки.</p>
     */
    public static final DeferredHolder<CriterionTrigger<?>, SimpleTrigger> MANUL_OBSERVED =
            TRIGGERS.register("manul_observed", SimpleTrigger::new);

    /**
     * Секретное «Кубанский»: встречен серебристый манул — самый редкий окрас
     * ({@link ManulCoat#SILVER}).
     *
     * <p>Вызывается из {@code ManulObservation.onPlayerTick}: достаточно
     * увидеть зверя рядом, ловить его не нужно.</p>
     */
    public static final DeferredHolder<CriterionTrigger<?>, SimpleTrigger> MANUL_SILVER =
            TRIGGERS.register("manul_silver", SimpleTrigger::new);

    private ManulCriteria() {
    }

    public static void register(IEventBus modEventBus) {
        TRIGGERS.register(modEventBus);
    }

    /**
     * Триггер без собственных условий: всё решает вызывающая сторона.
     *
     * <p>Один класс на все четыре критерия — экземпляры реестра различны,
     * поэтому и критерии различны, а копировать кодек четыре раза не нужно.</p>
     */
    public static final class SimpleTrigger extends SimpleCriterionTrigger<SimpleTrigger.Instance> {
        @Override
        public Codec<Instance> codec() {
            return Instance.CODEC;
        }

        /** Выдаёт критерий игроку. */
        public void trigger(ServerPlayer player) {
            trigger(player, instance -> true);
        }

        public record Instance(Optional<ContextAwarePredicate> player)
                implements SimpleCriterionTrigger.SimpleInstance {
            public static final Codec<Instance> CODEC = RecordCodecBuilder.create(
                    builder -> builder.group(
                                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player")
                                            .forGetter(Instance::player))
                            .apply(builder, Instance::new));

            @Override
            public void validate(ValidationContextSource validator) {
                Validatable.validate(validator.entityContext(), "player", player());
            }
        }
    }

    /** Пустой экземпляр критерия — условие проверяется в коде, не в данных. */
    public static Criterion<SimpleTrigger.Instance> criterion(
            DeferredHolder<CriterionTrigger<?>, SimpleTrigger> trigger) {
        return trigger.get().createCriterion(new SimpleTrigger.Instance(Optional.empty()));
    }
}
