package dev.romankrukovsky.kubanhorizons.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import dev.romankrukovsky.kubanhorizons.registry.KHEntities;
import dev.romankrukovsky.kubanhorizons.registry.KHSounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * Кубанский манул — талисман мода.
 *
 * <p>Устроен так, чтобы не быть «котом с другой текстурой». Ванильный кот
 * приручается одной рыбой и после этого ходит за игроком; манул не делает ни
 * того, ни другого. Отличий три, и все механические:</p>
 *
 * <ol>
 *   <li><b>Доверие вместо приручения.</b> Подношения принимаются не чаще раза
 *   в игровой день ({@link #OFFER_COOLDOWN_TICKS}), поэтому знакомство
 *   растягивается на несколько дней и его нельзя ускорить рыбой из инвентаря.
 *   Резкие движения и вред окрестным животным доверие отнимают.</li>
 *   <li><b>Независимость навсегда.</b> Даже на высшей ступени манул не следует
 *   за игроком: он уходит по своим делам ({@link #ROAM_INTERVAL_TICKS}) и
 *   возвращается сам. Ошейника и команды «сидеть» для дикого зверя нет.</li>
 *   <li><b>Скрытый характер.</b> {@link ManulPersonality} меняет дистанцию
 *   побега, скорость доверия и склонность подходить, поэтому две особи
 *   ощущаются разными существами.</li>
 * </ol>
 *
 * <p>Наследует {@link TamableAnimal} ради готовой связи «владелец — зверь»,
 * которая нужна для расселения у усадьбы, но приручение переопределено: ванильный
 * путь через {@code tame()} одной рыбой здесь недоступен.</p>
 */
public final class Manul extends TamableAnimal implements GeoEntity {
    /** Подношения: сырое мясо, рыба и региональная еда. */
    public static final TagKey<net.minecraft.world.item.Item> OFFERINGS =
            ItemTags.create(dev.romankrukovsky.kubanhorizons.util.KHIds.of("manul_offerings"));

    private static final int SCHEMA_VERSION = 1;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.manul.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.manul.walk");
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.manul.sit");
    private static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("animation.manul.sleep");
    private static final RawAnimation HISS = RawAnimation.begin().thenLoop("animation.manul.hiss");
    /**
     * Пауза между зачётными подношениями — игровые сутки.
     *
     * <p>Именно это делает доверие многодневным. Без паузы игрок скормил бы
     * стопку рыбы за минуту, и вся механика свелась бы к ванильному приручению.
     */
    public static final int OFFER_COOLDOWN_TICKS = 24000;
    /** Как часто зверь уходит по своим делам. */
    private static final int ROAM_INTERVAL_TICKS = 6000;
    /** Тиков шипения после испуга. */
    private static final int HISS_TICKS = 30;
    /** Тиков неподвижного разглядывания игрока перед отходом. */
    private static final int FREEZE_TICKS = 40;
    /** Скорость, выше которой приближение считается резким. */
    private static final double RUSH_SPEED_SQR = 0.0225D;
    /** Радиус, в котором манул замечает вред окрестным животным. */
    private static final double WITNESS_RADIUS = 12.0D;

    private static final EntityDataAccessor<Integer> DATA_COAT =
            SynchedEntityData.defineId(Manul.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TRUST =
            SynchedEntityData.defineId(Manul.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_HISSING =
            SynchedEntityData.defineId(Manul.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_FROZEN =
            SynchedEntityData.defineId(Manul.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_LOAFING =
            SynchedEntityData.defineId(Manul.class, EntityDataSerializers.BOOLEAN);
    /** Спит ли зверь днём в укрытии; синхронизируется ради позы на клиенте. */
    private static final EntityDataAccessor<Boolean> DATA_DOZING =
            SynchedEntityData.defineId(Manul.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private ManulPersonality personality = ManulPersonality.CAUTIOUS;
    private long lastOfferTick = Long.MIN_VALUE;
    private int hissTicks;
    private int freezeTicks;
    private int roamTicks;
    /** Тиков активной злости; гаснет сама — манул огрызается и уходит. */
    private int retaliationTicks;
    /** Тиков подряд, что побег не удаётся: основа состояния «загнан в угол». */
    private int blockedRetreatTicks;
    /**
     * Тиков запрета на новую провокацию после остывания.
     *
     * <p>Без него зверь попадал в цикл: загнан → бьёт → остыл → всё ещё
     * загнан → бьёт снова. Тест это и поймал: злость формально гасла, но
     * включалась в тот же тик, и снаружи манул выглядел вечно агрессивным.</p>
     */
    private int provokeCooldownTicks;

    public Manul(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 18.0D);
    }

    /**
     * Правило спавна: манул выходит в сумерках и ночью, а не в полдень.
     *
     * <p>Ванильное {@code checkAnimalSpawnRules} требует освещения выше 8, то
     * есть дневного света, и с ним манул спавнился бы ровно наоборот своей
     * природе. Здесь порог перевёрнут: нужен полумрак. Днём зверь не исчезает —
     * просто новые особи не появляются, поэтому найденный утром манул досидит
     * до вечера.</p>
     *
     * <p>Спавн-яйцо и разведение проходят мимо этой проверки: ограничение
     * относится к естественному появлению, иначе игрок не смог бы поставить
     * зверя днём в творческом режиме.</p>
     */
    public static boolean checkManulSpawnRules(EntityType<Manul> type,
            net.minecraft.world.level.LevelAccessor level, EntitySpawnReason reason,
            BlockPos pos, net.minecraft.util.RandomSource random) {
        if (!level.getBlockState(pos.below())
                .is(net.minecraft.tags.BlockTags.ANIMALS_SPAWNABLE_ON)) {
            return false;
        }
        // Окно суток ограничивает только естественный спавн. Яйцо, команда,
        // разведение и структуры обязаны работать всегда: игрок попросил зверя
        // явно, и отказ по часам выглядел бы поломкой, а не замыслом.
        // Проверяется именно NATURAL, а не ignoresLightRequirements(): в 26.2
        // последний возвращает true только для trial spawner, и яйцо призыва
        // под него не попадает — на этом и упал первый вариант правила.
        if (reason != EntitySpawnReason.NATURAL) {
            return true;
        }
        if (!dev.romankrukovsky.kubanhorizons.config.KHServerConfig.manulNocturnalSpawns()) {
            return true;
        }
        // Сумерки и ночь: свет ниже дневного порога.
        return level.getRawBrightness(pos, 0) <= 8;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        // Отвечает лапами: от удара, при побудке вплотную и когда загнан в
        // угол. Цель выбирается в targetSelector — здесь только сама драка.
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.4D, false));
        goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        // Овчарка гонит дикого зверя, и манул это знает: от неё он уходит
        // всегда и безусловно, даже если игроку уже доверяет. Приоритет выше
        // отхода от игрока — собака опаснее человека.
        goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.AvoidEntityGoal<>(
                this, CaucasianShepherd.class, 10.0F, 1.2D, 1.6D));
        // Своя цель отхода вместо AvoidEntityGoal: нужен порядок
        // «замер — посмотрел — отошёл», которого у ванильной цели нет.
        goalSelector.addGoal(4, new ManulRetreatGoal(this));
        goalSelector.addGoal(5, new ManulOfferingGoal(this));
        // Днём зверь уходит спать в укрытие: манул ночной, и бодрый полдень
        // выдавал бы в нём перекрашенную кошку. Выше любопытства — сон
        // сильнее интереса, — но ниже страха и подношений.
        goalSelector.addGoal(6, new ManulSleepGoal(this));
        // Любопытство: подойти и разглядывать. Ниже отхода — страх сильнее
        // интереса, — но выше прогулки, иначе характер никогда бы не
        // проявился и curiosity() остался бы мёртвым числом.
        goalSelector.addGoal(7, new ManulObserveGoal(this));
        goalSelector.addGoal(8, new BreedGoal(this, 1.0D));
        // Греется и сидит неподвижно: то, за что манула и запоминают.
        goalSelector.addGoal(9, new ManulLoafGoal(this));
        goalSelector.addGoal(10, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(12, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Своя агрессия по причине: побудка вплотную, загнанность в угол или
        // уже испорченная репутация. Именно это отличает манула от зомби —
        // он бросается не на игрока как такового, а на конкретный поступок.
        targetSelector.addGoal(2, new ManulProvokedGoal(this));
        // Охота на мелкую живность: саранча — то, что реально есть в моде.
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Locust.class, true));
    }


    /**
     * Сумерки, ночь или рассвет.
     *
     * <p>Считается по освещённости неба ({@code getSkyDarken}), а не по
     * времени суток: это единственная величина, доступная из
     * {@link net.minecraft.world.level.LevelAccessor} на этапе спавна, и она
     * же учитывает грозу — в пасмурный день манул тоже выходит.</p>
     */
    private static boolean isDuskOrNight(net.minecraft.world.level.LevelAccessor level) {
        // 4 — тот же порог, по которому ванильный Level.isBrightOutside()
        // отделяет день от ночи; берём его, чтобы «день» у манула совпадал с
        // днём в остальной игре.
        return level.getSkyDarken() >= 4;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_COAT, ManulCoat.STEPPE.ordinal());
        builder.define(DATA_TRUST, 0);
        builder.define(DATA_HISSING, false);
        builder.define(DATA_FROZEN, false);
        builder.define(DATA_LOAFING, false);
        builder.define(DATA_DOZING, false);
    }

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(
            net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty, EntitySpawnReason reason,
            net.minecraft.world.entity.SpawnGroupData data) {
        setCoat(ManulCoat.random(random));
        personality = ManulPersonality.random(random);
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    // --- Окрас, характер, доверие ---

    public ManulCoat coat() {
        return ManulCoat.byIndex(entityData.get(DATA_COAT));
    }

    public void setCoat(ManulCoat coat) {
        entityData.set(DATA_COAT, coat.ordinal());
    }

    public ManulPersonality personality() {
        return personality;
    }

    /**
     * Задаёт характер напрямую.
     *
     * <p>Нужно тестам и командам отладки: иначе проверить «осторожный уходит
     * раньше храброго» можно было бы только перебором спавнов до нужного
     * нрава, то есть недетерминированно.</p>
     */
    public void setPersonality(ManulPersonality value) {
        this.personality = value == null ? ManulPersonality.CAUTIOUS : value;
    }

    public int trustPoints() {
        return entityData.get(DATA_TRUST);
    }

    public ManulTrust trust() {
        return ManulTrust.ofPoints(trustPoints());
    }

    /** Клиентский флаг для анимации шипения (прижатые уши). */
    public boolean isHissing() {
        return entityData.get(DATA_HISSING);
    }

    /** Клиентский флаг для позы неподвижного разглядывания. */
    public boolean isFrozen() {
        return entityData.get(DATA_FROZEN);
    }

    /**
     * Сидит ли зверь неподвижно ({@link ManulLoafGoal}).
     *
     * <p>Синхронизируется на клиент, чтобы рендерер мог показать сидячую позу:
     * без флага «манул сидит на плетне» существовало бы только в логике
     * сервера, а игрок видел бы стоящего зверя.</p>
     */
    public boolean isLoafing() {
        return entityData.get(DATA_LOAFING);
    }

    void setLoafing(boolean loafing) {
        entityData.set(DATA_LOAFING, loafing);
    }

    /**
     * Спит ли зверь днём в укрытии ({@link ManulSleepGoal}).
     *
     * <p>Синхронизируется на клиент по той же причине, что и сидячая поза:
     * иначе спящий манул выглядел бы бодрствующим.</p>
     */
    public boolean isDozing() {
        return entityData.get(DATA_DOZING);
    }

    void setSleeping(boolean sleeping) {
        entityData.set(DATA_DOZING, sleeping);
    }

    /**
     * Изменяет доверие на дельту с ограничением шкалы.
     *
     * @return доверие после изменения
     */
    public int adjustTrust(int delta) {
        int updated = Mth.clamp(trustPoints() + delta, 0, ManulTrust.maxPoints());
        entityData.set(DATA_TRUST, updated);
        return updated;
    }

    /** Готов ли зверь зачесть новое подношение (не чаще раза в игровые сутки). */
    public boolean canAcceptOffering() {
        return lastOfferTick == Long.MIN_VALUE
                || level().getGameTime() - lastOfferTick >= OFFER_COOLDOWN_TICKS;
    }

    /**
     * Принимает подношение: растит доверие с учётом характера и аппетита.
     *
     * @return true, если подношение зачтено
     */
    public boolean acceptOffering() {
        if (!canAcceptOffering()) {
            return false;
        }
        lastOfferTick = level().getGameTime();
        int gain = Math.max(1, Math.round(2.0F * personality.trustRate()
                * personality.appetite()));
        adjustTrust(gain);
        playSound(KHSounds.MANUL_PURR.get(), 0.6F, 1.0F);
        return true;
    }

    /** Дистанция отхода с учётом характера и уже заработанного доверия. */
    public double retreatDistance() {
        double base = personality.fleeDistance();
        // Чем выше доверие, тем ближе подпускает: это и есть видимый прогресс.
        return switch (trust()) {
            case WILD -> base;
            case WARY -> base * 0.7D;
            case ACCEPTING -> base * 0.4D;
            default -> 0.0D;
        };
    }

    // --- Поведение ---

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (hissTicks > 0 && --hissTicks == 0) {
            entityData.set(DATA_HISSING, false);
        }
        if (freezeTicks > 0 && --freezeTicks == 0) {
            entityData.set(DATA_FROZEN, false);
        }
        // Злость гаснет сама. Это и отделяет манула от враждебного моба: он
        // огрызнулся и пошёл своей дорогой, а не гонится через полкарты.
        if (retaliationTicks > 0 && --retaliationTicks == 0) {
            setTarget(null);
            setLastHurtByMob(null);
            blockedRetreatTicks = 0;
            // Остыв, зверь получает право спокойно уйти: иначе он снова
            // окажется «загнанным» в том же углу и бросится опять.
            provokeCooldownTicks = PROVOKE_COOLDOWN_TICKS;
        }
        if (provokeCooldownTicks > 0) {
            provokeCooldownTicks--;
        }
        // Независимость: даже полностью доверяющий зверь уходит по своим делам.
        if (trust().atLeast(ManulTrust.FRIENDLY) && ++roamTicks > ROAM_INTERVAL_TICKS) {
            roamTicks = 0;
            getNavigation().stop();
            setOrderedToSit(false);
        }
    }

    /**
     * Длительность ответной агрессии — около десяти секунд.
     *
     * <p>Ограничение и есть характер зверя. Без него ударенный манул гнался бы
     * за игроком до потери из виду, то есть вёл себя как мстительный хищник,
     * а по замыслу он огрызается и уходит.</p>
     */
    public static final int RETALIATION_TICKS = 200;

    /** Пауза после остывания: столько тиков зверь не бросается снова. */
    public static final int PROVOKE_COOLDOWN_TICKS = 200;

    /** Тиков безуспешного побега, после которых зверь считается загнанным. */
    private static final int CORNERED_TICKS = 30;

    /**
     * Удар запускает то же окно остывания, что и провокация.
     *
     * <p>Без этого {@code HurtByTargetGoal} держал бы цель бессрочно, и
     * ударенный зверь преследовал бы игрока до потери из виду — ровно то
     * поведение, от которого отделяет {@link #RETALIATION_TICKS}.</p>
     */
    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level,
            DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && source.getEntity() instanceof Player) {
            startRetaliation();
            hiss();
            // Удар — это тоже поступок: доверие падает заметно сильнее, чем от
            // вида чужой смерти, потому что бьют лично его.
            adjustTrust(-6);
        }
        return hurt;
    }

    /** Можно ли сейчас спровоцировать зверя (после остывания — нельзя). */
    public boolean canBeProvoked() {
        return provokeCooldownTicks <= 0;
    }

    /** Идёт ли сейчас ответная агрессия. */
    public boolean isRetaliating() {
        return retaliationTicks > 0;
    }

    /** Запускает (или продлевает) окно ответной агрессии. */
    public void startRetaliation() {
        retaliationTicks = RETALIATION_TICKS;
    }

    /**
     * Загнан ли зверь в угол: побег не удаётся дольше {@link #CORNERED_TICKS}.
     *
     * <p>Считается по факту неудачи, а не по геометрии вокруг: проверять стены
     * лучами дорого и всё равно врёт на лестницах, заборах и в воде. Здесь
     * ровно то, что важно игроку — зверь пытался уйти и не смог.</p>
     */
    public boolean isCornered() {
        return blockedRetreatTicks >= CORNERED_TICKS;
    }

    /**
     * Отмечает такт побега: удался он или нет.
     *
     * <p>Вызывается из {@link ManulRetreatGoal}: только цель отхода знает,
     * действительно ли зверь сейчас пытается уйти.</p>
     */
    public void noteRetreatProgress(boolean moved) {
        if (moved) {
            blockedRetreatTicks = 0;
        } else if (blockedRetreatTicks < CORNERED_TICKS) {
            blockedRetreatTicks++;
        }
    }

    /**
     * Доверие именно к этому игроку.
     *
     * <p>Пока доверие в моде одно на особь, поэтому владелец получает текущее
     * значение, а посторонний — тоже. Метод существует, чтобы провокация не
     * зависела от этой детали: когда доверие станет персональным, менять
     * придётся одно место, а не логику агрессии.</p>
     */
    public int trustToward(Player player) {
        return trustPoints();
    }

    /** Шипение: предупреждение, а не атака. */
    public void hiss() {        if (hissTicks > 0) {
            return;
        }
        hissTicks = HISS_TICKS;
        entityData.set(DATA_HISSING, true);
        playSound(KHSounds.MANUL_HISS.get(), 0.9F, 0.95F + random.nextFloat() * 0.1F);
    }

    /** Замирание с разглядыванием игрока — первая реакция на встречу. */
    public void freezeAndStare() {
        if (freezeTicks > 0) {
            return;
        }
        freezeTicks = FREEZE_TICKS;
        entityData.set(DATA_FROZEN, true);
        getNavigation().stop();
    }

    /** Резкое ли приближение: по скорости игрока, а не по расстоянию. */
    public static boolean isRushing(Player player) {
        return player.getDeltaMovement().horizontalDistanceSqr() > RUSH_SPEED_SQR
                || player.isSprinting();
    }

    /**
     * Приручение одной рыбой недоступно: подношение растит доверие, а не
     * превращает зверя в питомца. Это главное отличие от ванильного кота.
     */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(OFFERINGS)) {
            if (!level().isClientSide()) {
                // Из рук берёт только уже доверяющий зверь; дикому корм кладут
                // на землю, и подбирает он его сам.
                if (!trust().atLeast(ManulTrust.ACCEPTING)) {
                    hiss();
                    return InteractionResult.SUCCESS;
                }
                if (acceptOffering()) {
                    held.consume(1, player);
                    if (trust().atLeast(ManulTrust.RESIDENT) && getOwnerReference() == null) {
                        // Признал участок: владелец нужен для расселения у усадьбы.
                        setOwner(player);
                        setTame(true, false);
                    }
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    /** Подбирает подношения с земли — основной путь знакомства для дикого зверя. */
    @Override
    protected void pickUpItem(ServerLevel level, ItemEntity item) {
        ItemStack stack = item.getItem();
        if (!stack.is(OFFERINGS) || !canAcceptOffering()) {
            return;
        }
        if (acceptOffering()) {
            stack.shrink(1);
            if (stack.isEmpty()) {
                item.discard();
            }
        }
    }

    @Override
    public boolean canPickUpLoot() {
        return true;
    }

    /**
     * Вред окрестной живности снижает доверие.
     *
     * <p>Проверяется здесь, а не событием на игрока: манул реагирует на то, что
     * видит сам, поэтому нужен радиус вокруг зверя, а не глобальный слушатель.</p>
     */
    public void witnessHarm(LivingEntity victim, LivingEntity attacker) {
        if (attacker instanceof Player && victim != this
                && distanceToSqr(victim) <= WITNESS_RADIUS * WITNESS_RADIUS) {
            adjustTrust(-3);
            hiss();
        }
    }

    /** Все манулы вокруг точки — для реакции на вред живности. */
    public static List<Manul> nearby(ServerLevel level, BlockPos pos) {
        AABB area = new AABB(pos).inflate(WITNESS_RADIUS);
        return level.getEntitiesOfClass(Manul.class, area, Manul::isAlive);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(OFFERINGS);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        Manul child = KHEntities.MANUL.get().create(level, EntitySpawnReason.BREEDING);
        if (child != null) {
            // Котёнок наследует окрас одного из родителей, а характер — свой:
            // окрас в природе наследуется, повадки складываются заново.
            ManulCoat inherited = partner instanceof Manul other && random.nextBoolean()
                    ? other.coat() : coat();
            child.setCoat(inherited);
            child.personality = ManulPersonality.random(random);
        }
        return child;
    }

    // --- Сохранение ---

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("SchemaVersion", SCHEMA_VERSION);
        output.putInt("Coat", coat().ordinal());
        output.putInt("Trust", trustPoints());
        output.putString("Personality", personality.key());
        output.putLong("LastOffer", lastOfferTick);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setCoat(ManulCoat.byIndex(input.getIntOr("Coat", ManulCoat.STEPPE.ordinal())));
        entityData.set(DATA_TRUST,
                Mth.clamp(input.getIntOr("Trust", 0), 0, ManulTrust.maxPoints()));
        personality = ManulPersonality.byKey(
                input.getStringOr("Personality", ManulPersonality.CAUTIOUS.key()));
        lastOfferTick = input.getLongOr("LastOffer", Long.MIN_VALUE);
        hissTicks = 0;
        freezeTicks = 0;
        entityData.set(DATA_HISSING, false);
        entityData.set(DATA_FROZEN, false);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<Manul>("movement", 5, this::movementAnimation));
    }

    private PlayState movementAnimation(AnimationTest<Manul> state) {
        if (isDozing()) {
            return state.setAndContinue(SLEEP);
        }
        if (isInSittingPose() || isLoafing()) {
            return state.setAndContinue(SIT);
        }
        if (isHissing()) {
            return state.setAndContinue(HISS);
        }
        return state.setAndContinue(state.isMoving() ? WALK : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    // --- Звук ---

    @Override
    protected SoundEvent getAmbientSound() {
        return KHSounds.MANUL_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return KHSounds.MANUL_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return KHSounds.MANUL_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.5F;
    }
}
