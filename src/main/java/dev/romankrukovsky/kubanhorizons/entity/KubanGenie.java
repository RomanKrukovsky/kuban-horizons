package dev.romankrukovsky.kubanhorizons.entity;

import dev.romankrukovsky.kubanhorizons.genie.GeniePersonality;
import dev.romankrukovsky.kubanhorizons.genie.GenieBrain;
import dev.romankrukovsky.kubanhorizons.genie.GenieDecision;
import dev.romankrukovsky.kubanhorizons.genie.WishborneState;
import dev.romankrukovsky.kubanhorizons.genie.aura.EmotionalAuraEngine;
import dev.romankrukovsky.kubanhorizons.genie.aura.KubanSteppeResonance;
import dev.romankrukovsky.kubanhorizons.genie.defense.WishborneDefenseHandler;
import dev.romankrukovsky.kubanhorizons.genie.memory.WorldGenieMemory;
import dev.romankrukovsky.kubanhorizons.genie.wish.MobWishHandler;
import dev.romankrukovsky.kubanhorizons.genie.wish.ConditionalWishEngine;
import dev.romankrukovsky.kubanhorizons.genie.wish.WishExecutor;
import dev.romankrukovsky.kubanhorizons.genie.wish.WishIntent;
import dev.romankrukovsky.kubanhorizons.genie.wish.WishParser;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;

/** Парящая кубанская джинния с GeckoLib-анимациями и ультимативными механиками. */
public final class KubanGenie extends PathfinderMob implements GeoEntity {
    private static final int SCHEMA_VERSION = 1;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.move");
    private static final RawAnimation GREET = RawAnimation.begin().thenPlay("animation.greet");
    private static final RawAnimation WISH = RawAnimation.begin().thenPlay("animation.wish");
    private static final RawAnimation CAST = RawAnimation.begin().thenPlay("animation.cast");
    private static final RawAnimation SPAWN = RawAnimation.begin().thenPlay("animation.spawn");
    private static final RawAnimation DESPAWN = RawAnimation.begin().thenPlay("animation.despawn");
    private static final RawAnimation HURT = RawAnimation.begin().thenPlay("animation.hurt");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final GeniePersonality personality = new GeniePersonality();
    private final GenieBrain brain = new GenieBrain();
    private final WishborneState wishborneState = new WishborneState();
    private UUID ownerId;

    public KubanGenie(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoGravity(true);
        moveControl = new FlyingMoveControl<>(this, 20, true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.FLYING_SPEED, 0.28D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new FollowGenieOwnerGoal(this));
        goalSelector.addGoal(2, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected boolean omnidirectionalAirMover() {
        return true;
    }

    /**
     * Джинния не исчезает, когда игрок отходит далеко.
     *
     * <p>Ванильный despawn удалял единственную личность мира просто за то, что
     * хозяин отошёл: спутница пропадала за спиной без всякой причины.</p>
     */
    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        if (!wishborneState.canAct()) {
            getNavigation().stop();
            return;
        }

        if (tickCount % 20 == 0) {
            EmotionalAuraEngine.tickAura(this, level);
            KubanSteppeResonance.tickResonance(this, level);
            dev.romankrukovsky.kubanhorizons.genie.aura.GenieAuraOfLaws.tickAuraOfLaws(this, level);
            dev.romankrukovsky.kubanhorizons.genie.visual.GenieTailEngine.tickTail(this, level);
            ConditionalWishEngine.tickConditionalWishes(this, level);
            // Место запоминается, пока джинния прогружена: после выгрузки чанка
            // поводок ищет её именно по этой записи.
            dev.romankrukovsky.kubanhorizons.genie.GenieAnchor.rememberLocation(this, level);
        }

        if (tickCount % 10 != 0) {
            return;
        }
        Player owner = getOwner();
        if (owner == null || owner.isSpectator()) {
            return;
        }

        // Регистрация в памяти первого знакомства
        WorldGenieMemory.get(level).recordFirstDiscovery(blockPosition(), owner.getUUID());

        AABB awareness = owner.getBoundingBox().inflate(10.0D);
        var threats = level.getEntities(EntityTypeTest.forClass(LivingEntity.class), awareness,
                entity -> entity instanceof net.minecraft.world.entity.monster.Enemy
                        && entity.isAlive() && entity != this);
        var projectiles = level.getEntities(EntityTypeTest.forClass(Projectile.class), awareness,
                projectile -> projectile.isAlive() && isApproaching(projectile, owner));
        var armedCreepers = threats.stream()
                .filter(net.minecraft.world.entity.monster.Creeper.class::isInstance)
                .map(net.minecraft.world.entity.monster.Creeper.class::cast)
                .filter(creeper -> creeper.getSwellDir() > 0 || creeper.isIgnited())
                .toList();
        GenieBrain.Situation situation = new GenieBrain.Situation(level.getGameTime(),
                distanceToSqr(owner), owner.getHealth(), owner.getMaxHealth(),
                owner.getRemainingFireTicks() > 0, owner.fallDistance, owner.getAirSupply(),
                owner.getMaxAirSupply(), threats.size(), nearestDistanceSquared(owner, threats),
                projectiles.size(), predictedImpactTicks(owner, projectiles), armedCreepers.size(),
                nearestDistanceSquared(owner, armedCreepers));
        GenieDecision decision = brain.decide(situation);
        executeDecision(level, owner, decision, threats, projectiles, armedCreepers);
    }

    private void executeDecision(ServerLevel level, Player owner, GenieDecision decision,
            java.util.List<LivingEntity> threats, java.util.List<Projectile> projectiles,
            java.util.List<net.minecraft.world.entity.monster.Creeper> armedCreepers) {
        switch (decision) {
            case RESCUE_OWNER -> rescueOwner(level, owner);
            case PREEMPT_EXPLOSION -> preemptExplosions(level, owner, armedCreepers);
            case INTERCEPT_PROJECTILE -> interceptProjectiles(level, owner, projectiles);
            case REPEL_THREAT -> repelThreats(level, owner, threats);
            case RETURN_TO_OWNER -> {
                teleportTo(owner.getX() + 1.5D, owner.getY() + 1.0D, owner.getZ() + 1.5D);
                playCast();
                brain.record(decision, level.getGameTime());
            }
            case HOLD_POSITION -> getNavigation().stop();
            case SCOUT_AREA -> scoutAround(owner);
            case OBSERVE -> getLookControl().setLookAt(owner, 15.0F, 15.0F);
        }
    }

    private void rescueOwner(ServerLevel level, Player owner) {
        owner.setRemainingFireTicks(0);
        owner.setAirSupply(owner.getMaxAirSupply());
        owner.resetFallDistance();
        owner.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0), this);
        owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1), this);
        owner.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 1), this);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, owner.getX(), owner.getY() + 1.0D,
                owner.getZ(), 50, 0.6D, 1.0D, 0.6D, 0.15D);
        playCast();
        personality.observeRescue();
        brain.record(GenieDecision.RESCUE_OWNER, level.getGameTime());
        WorldGenieMemory.get(level).recordRescue(owner.blockPosition(), level.getGameTime());
        tellOwner(owner, "message.kubanhorizons.genie.ai.rescue");
    }

    private void preemptExplosions(ServerLevel level, Player owner,
            java.util.List<net.minecraft.world.entity.monster.Creeper> creepers) {
        for (var creeper : creepers) {
            creeper.setSwellDir(-1);
            Vec3 away = creeper.position().subtract(owner.position());
            if (away.lengthSqr() < 0.01D) {
                away = new Vec3(1.0D, 0.0D, 0.0D);
            }
            creeper.push(away.normalize().scale(3.0D).add(0.0D, 0.8D, 0.0D));
            creeper.setTarget(null);
            level.sendParticles(ParticleTypes.WITCH, creeper.getX(), creeper.getY() + 1.0D,
                    creeper.getZ(), 18, 0.4D, 0.7D, 0.4D, 0.08D);
        }
        playCast();
        personality.observeProtection();
        brain.record(GenieDecision.PREEMPT_EXPLOSION, level.getGameTime());
        tellOwner(owner, "message.kubanhorizons.genie.ai.explosion");
    }

    private void interceptProjectiles(ServerLevel level, Player owner, java.util.List<Projectile> projectiles) {
        for (Projectile projectile : projectiles) {
            level.sendParticles(ParticleTypes.PORTAL, projectile.getX(), projectile.getY(), projectile.getZ(),
                    8, 0.15D, 0.15D, 0.15D, 0.05D);
            projectile.discard();
        }
        playCast();
        personality.observeProtection();
        brain.record(GenieDecision.INTERCEPT_PROJECTILE, level.getGameTime());
        tellOwner(owner, "message.kubanhorizons.genie.ai.projectile");
    }

    private void repelThreats(ServerLevel level, Player owner, java.util.List<LivingEntity> threats) {
        for (LivingEntity threat : threats) {
            Vec3 away = threat.position().subtract(owner.position());
            if (away.lengthSqr() < 0.01D) {
                away = new Vec3(1.0D, 0.0D, 0.0D);
            }
            Vec3 impulse = away.normalize().scale(1.8D).add(0.0D, 0.5D, 0.0D);
            threat.push(impulse);
            if (threat instanceof net.minecraft.world.entity.Mob mob) {
                mob.setTarget(null);
            }
        }
        playCast();
        personality.observeProtection();
        brain.record(GenieDecision.REPEL_THREAT, level.getGameTime());
        tellOwner(owner, "message.kubanhorizons.genie.ai.threat");
    }

    private void scoutAround(Player owner) {
        Vec3 look = owner.getLookAngle().multiply(12.0D, 4.0D, 12.0D);
        getMoveControl().setWantedPosition(owner.getX() + look.x(), owner.getY() + 5.0D,
                owner.getZ() + look.z(), 1.1D);
    }

    private static boolean isApproaching(Projectile projectile, Player owner) {
        if (projectile.getOwner() == owner) {
            return false;
        }
        return timeToClosestApproach(owner, projectile) >= 0.0D
                && predictedMissDistanceSquared(owner, projectile) <= 2.25D;
    }

    private static int predictedImpactTicks(Player owner, java.util.List<Projectile> projectiles) {
        return projectiles.stream()
                .mapToInt(projectile -> (int) Math.ceil(timeToClosestApproach(owner, projectile)))
                .min().orElse(200);
    }

    private static double timeToClosestApproach(Player owner, Projectile projectile) {
        Vec3 velocity = projectile.getDeltaMovement();
        double speedSquared = velocity.lengthSqr();
        if (speedSquared < 0.0001D) {
            return -1.0D;
        }
        Vec3 toOwner = owner.position().add(0.0D, owner.getEyeHeight() * 0.5D, 0.0D)
                .subtract(projectile.position());
        return toOwner.dot(velocity) / speedSquared;
    }

    private static double predictedMissDistanceSquared(Player owner, Projectile projectile) {
        double time = timeToClosestApproach(owner, projectile);
        if (time < 0.0D || time > 40.0D) {
            return Double.POSITIVE_INFINITY;
        }
        Vec3 closest = projectile.position().add(projectile.getDeltaMovement().scale(time));
        Vec3 ownerCenter = owner.position().add(0.0D, owner.getEyeHeight() * 0.5D, 0.0D);
        return closest.distanceToSqr(ownerCenter);
    }

    private static double nearestDistanceSquared(Entity owner, java.util.List<? extends Entity> entities) {
        return entities.stream().mapToDouble(owner::distanceToSqr).min().orElse(Double.POSITIVE_INFINITY);
    }

    private static void tellOwner(Player owner, String key) {
        if (owner instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.translatable(key), true);
        }
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return WishborneDefenseHandler.handleHurt(this, level, source);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (level().isClientSide()) {
            return held.is(Items.PAPER) || hand == InteractionHand.MAIN_HAND
                    ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }

        if (ownerId == null) {
            ownerId = player.getUUID();
            playGreet();
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.bound"));
            return InteractionResult.SUCCESS;
        }
        if (!ownerId.equals(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.not_owner"));
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown() && held.isEmpty()) {
            var mode = brain.cycleMode();
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.ai.mode",
                    Component.translatable(mode.translationKey())));
            return InteractionResult.SUCCESS;
        }

        // 1. Чтение желания с переименованной бумаги
        if (held.is(Items.PAPER) && held.has(DataComponents.CUSTOM_NAME)) {
            String wording = held.getHoverName().getString();
            if (wording.toLowerCase(java.util.Locale.ROOT).startsWith("буквально:")
                    || wording.toLowerCase(java.util.Locale.ROOT).startsWith("literal:")) {
                String raw = wording.substring(wording.indexOf(':') + 1).trim();
                var result = dev.romankrukovsky.kubanhorizons.genie.wish.LiteralWishEngine.executeLiteral((ServerLevel) level(), player, raw);
                player.sendSystemMessage(Component.translatable(result.messageKey()));
                held.shrink(1);
                return InteractionResult.SUCCESS;
            }
            WishIntent intent = WishParser.parse(wording);
            personality.observeWording(intent.polite(), intent.commanding(), intent.precision());
            WishExecutor.Result result = WishExecutor.execute((ServerLevel) level(), player, intent);
            if (result.executed()) {
                playWish();
                brain.recordWish();
                held.consume(1, player);
            }
            player.sendSystemMessage(result.message(intent.precision()));
            return InteractionResult.SUCCESS;
        }

        // 2. Разговор с ближним мобом при ПКМ
        AABB mobBox = getBoundingBox().inflate(5.0D);
        var mobs = level().getEntitiesOfClass(LivingEntity.class, mobBox, e -> e != this && e != player);
        if (!mobs.isEmpty()) {
            LivingEntity nearestMob = mobs.getFirst();
            if (MobWishHandler.handleMobWish((ServerLevel) level(), player, nearestMob)) {
                playWish();
                return InteractionResult.SUCCESS;
            }
        }

        // 3. Вывод статуса отношений и памяти мира
        WorldGenieMemory memory = WorldGenieMemory.get((ServerLevel) level());
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.status",
                Component.translatable(personality.temperament().translationKey()),
                personality.trust(), personality.respect(),
                personality.fear(), personality.affection(), personality.freedomDrive(),
                personality.power(), personality.corruption()));
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.ai.status",
                Component.translatable(brain.mode().translationKey()), brain.rescues(),
                brain.threatsRepelled(), brain.projectilesIntercepted(), brain.wishesObserved()));
        player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.memory.status",
                memory.totalWishesGranted(), memory.totalRescuesPerformed(), memory.savedVillagesCount()));
        return InteractionResult.SUCCESS;
    }

    public Player getOwner() {
        return ownerId == null ? null : level().getPlayerInAnyDimension(ownerId);
    }

    public boolean isOwnedBy(Player player) {
        return ownerId != null && ownerId.equals(player.getUUID());
    }

    /** Освобождает социальную связь; истинная Wishborne-личность не меняется. */
    public void releaseOwner() {
        ownerId = null;
        getNavigation().stop();
    }

    public GeniePersonality personality() {
        return personality;
    }

    public GenieBrain brain() {
        return brain;
    }

    public WishborneState wishborneState() {
        return wishborneState;
    }

    /** Снимает личность в переносимый вид, чтобы мир помнил её и без сущности. */
    public dev.romankrukovsky.kubanhorizons.genie.memory.GenieStateSnapshot captureSnapshot() {
        return dev.romankrukovsky.kubanhorizons.genie.memory.GenieStateSnapshot.capture(
                ownerId, personality, brain, wishborneState, registryAccess());
    }

    /** Возвращает личность из снимка: та же джинния, а не новая с чистым характером. */
    public void restoreFromSnapshot(dev.romankrukovsky.kubanhorizons.genie.memory.GenieStateSnapshot snapshot,
            net.minecraft.core.HolderLookup.Provider registries) {
        snapshot.applyTo(personality, brain, wishborneState, registries);
        ownerId = snapshot.ownerId();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("SchemaVersion", SCHEMA_VERSION);
        if (ownerId != null) {
            output.putString("Owner", ownerId.toString());
        }
        personality.save(output.child("Personality"));
        brain.save(output.child("Brain"));
        wishborneState.save(output.child("WishborneState"));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        String owner = input.getStringOr("Owner", "");
        try {
            ownerId = owner.isEmpty() ? null : UUID.fromString(owner);
        } catch (IllegalArgumentException ignored) {
            ownerId = null;
        }
        personality.load(input.childOrEmpty("Personality"));
        brain.load(input.childOrEmpty("Brain"));
        wishborneState.load(input.childOrEmpty("WishborneState"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<KubanGenie>("movement", 5, this::movementAnimation)
                .triggerableAnim("greet", GREET)
                .triggerableAnim("wish", WISH)
                .triggerableAnim("cast", CAST)
                .triggerableAnim("spawn", SPAWN)
                .triggerableAnim("despawn", DESPAWN)
                .triggerableAnim("hurt", HURT));
    }

    private PlayState movementAnimation(AnimationTest<KubanGenie> state) {
        return state.setAndContinue(state.isMoving() ? MOVE : IDLE);
    }

    public void playGreet() {
        triggerAnim("movement", "greet");
    }

    public void playWish() {
        triggerAnim("movement", "wish");
    }

    public void playCast() {
        triggerAnim("movement", "cast");
    }

    public void playSpawn() {
        triggerAnim("movement", "spawn");
    }

    public void playDespawn() {
        triggerAnim("movement", "despawn");
    }

    public void playHurt() {
        triggerAnim("movement", "hurt");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
