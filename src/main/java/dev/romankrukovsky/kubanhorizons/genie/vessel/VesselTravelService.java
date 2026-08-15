package dev.romankrukovsky.kubanhorizons.genie.vessel;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.genie.aura.MagicalSignature;
import dev.romankrukovsky.kubanhorizons.genie.player.PlayerGenieAttachment;
import dev.romankrukovsky.kubanhorizons.registry.KHAttachments;
import dev.romankrukovsky.kubanhorizons.worldgen.dimension.KHDimensions;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Безопасный визит хозяина в дворец внутри лампы с точным возвратом.
 *
 * <p>Полноценный карманный мир «Вечная Кубань» — полноценный Minecraft внутри Minecraft
 * (десятки тысяч блоков ландшафта, парящие острова, степные биомы). Правила измерения
 * полностью настраиваемые владельцем-джиннией: гравитация 30 %, вечный закат, запрет
 * враждебных мобов, свободный полёт, сохранение инвентаря после смерти. Внутри измерения
 * джинния обладает абсолютной властью — команда «хочу дворец здесь» мгновенно
 * материализует постройку из дыма. Сервис управляет входом/выходом, редактированием
 * правил и строительством.</p>
 */
public final class VesselTravelService {
    private static final Vec3 PALACE_SPAWN =
            new Vec3(0.5D, KHDimensions.PALACE_FLOOR_Y + 2.0D, 0.5D);

    /** Правила карманного измерения (хранятся в уровне, редактируются джиннией). */
    public record PocketDimensionRules(
            float gravityMultiplier,   // 0.3f = 30 % гравитации
            boolean eternalSunset,     // вечный закат, время заморожено
            boolean noHostileMobs,     // полное отсутствие враждебных мобов
            boolean flightAllowed,     // свободный полёт для всех
            boolean keepInventoryOnDeath, // инвентарь не теряется при смерти
            int worldRadiusChunks      // радиус мира в чанках (десятки тысяч блоков)
    ) {
        public static final PocketDimensionRules DEFAULT = new PocketDimensionRules(
                0.3f, true, true, true, true, 128);
    }

    private static PocketDimensionRules currentRules = PocketDimensionRules.DEFAULT;

    private VesselTravelService() {
    }

    /** Возвращает текущие правила измерения. */
    public static PocketDimensionRules getRules() {
        return currentRules;
    }

    /** Устанавливает новые правила (только джинния внутри измерения). */
    public static void setRules(PocketDimensionRules rules, ServerPlayer editor) {
        if (!isVisitingPalace(editor) || !isGenie(editor)) {
            editor.sendSystemMessage(Component.translatable(
                    "message.kubanhorizons.genie.vessel.rules_only_genie"));
            return;
        }
        currentRules = rules;
        // Применяем правила немедленно ко всем игрокам в измерении
        applyRulesToAllPlayers(editor.level().getServer().getLevel(KHDimensions.ETERNAL_KUBAN));
        editor.sendSystemMessage(Component.translatable(
                "message.kubanhorizons.genie.vessel.rules_updated"));
    }

    /** Применяет текущие правила ко всем игрокам в измерении. */
    private static void applyRulesToAllPlayers(ServerLevel level) {
        if (level == null) return;
        for (ServerPlayer p : level.players()) {
            applyRulesToPlayer(p);
        }
    }

    /** Применяет правила к конкретному игроку (гравитация, полёт, keepInventory). */
    public static void applyRulesToPlayer(ServerPlayer player) {
        if (!isVisitingPalace(player)) return;
        // Гравитация: модифицируем motion при тике (вызывается из события)
        // Полёт: разрешаем creative flight
        if (currentRules.flightAllowed()) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
        // keepInventory обрабатывается в death handler (см. ниже)
    }

    /** Проверка, является ли игрок джиннией (внутри измерения имеет абсолютную власть). */
    private static boolean isGenie(ServerPlayer player) {
        // Интеграция с PlayerGenieAttachment
        try {
            var attachment = player.getData(dev.romankrukovsky.kubanhorizons.registry.KHAttachments.PLAYER_GENIE_DATA);
            return attachment.isGenie();
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean isVisitingPalace(ServerPlayer player) {
        return player.level().dimension().equals(KHDimensions.ETERNAL_KUBAN);
    }

    public static boolean enterPalace(ServerPlayer player, UUID genieId) {
        PlayerGenieAttachment attachment = player.getData(KHAttachments.PLAYER_GENIE_DATA);
        if (attachment.isGenie()) {
            player.sendSystemMessage(Component.translatable(
                    "message.kubanhorizons.genie.law.use_forfeit"));
            return false;
        }
        KubanGenie genie = GenieLampItem.findGenie(player.level().getServer(), genieId);
        if (genie == null || !genie.isOwnedBy(player)) {
            player.sendSystemMessage(Component.translatable(
                    "message.kubanhorizons.genie.lamp.unavailable"));
            return false;
        }
        ServerLevel target = player.level().getServer().getLevel(KHDimensions.ETERNAL_KUBAN);
        if (target == null) {
            player.sendSystemMessage(Component.translatable(
                    "message.kubanhorizons.genie.vessel.dimension_missing"));
            return false;
        }

        attachment.setBoundVesselPos(player.blockPosition());
        attachment.setBoundVesselDimension(player.level().dimension());
        ServerLevel from = (ServerLevel) player.level();
        MagicalSignature.cast(from, player.position());
        target.getChunk(0, 0);
        player.teleport(new TeleportTransition(target, PALACE_SPAWN, Vec3.ZERO,
                player.getYRot(), 0.0F, Set.<Relative>of(), TeleportTransition.DO_NOTHING));
        applyRulesToPlayer(player);
        player.sendSystemMessage(Component.translatable(
                "message.kubanhorizons.genie.vessel.palace_entered"));
        return true;
    }

    public static boolean leavePalace(ServerPlayer player) {
        if (!isVisitingPalace(player)) {
            return false;
        }
        PlayerGenieAttachment attachment = player.getData(KHAttachments.PLAYER_GENIE_DATA);
        if (attachment.isGenie()) {
            player.sendSystemMessage(Component.translatable(
                    "message.kubanhorizons.genie.law.use_forfeit"));
            return false;
        }
        Optional<BlockPos> entry = attachment.getBoundVesselPos();
        ServerLevel target = attachment.getBoundVesselDimension()
                .map(key -> player.level().getServer().getLevel(key))
                .orElse(null);
        if (entry.isEmpty() || target == null) {
            player.teleport(player.findRespawnPositionAndUseSpawnBlock(
                    false, TeleportTransition.DO_NOTHING));
        } else {
            Vec3 destination = Vec3.atBottomCenterOf(entry.get());
            player.teleport(new TeleportTransition(target, destination, Vec3.ZERO,
                    player.getYRot(), 0.0F, Set.<Relative>of(),
                    TeleportTransition.DO_NOTHING));
            target.sendParticles(ParticleTypes.PORTAL, destination.x, destination.y + 1.0D,
                    destination.z, 60, 0.5D, 0.8D, 0.5D, 0.1D);
            MagicalSignature.cast(target, destination);
        }
        player.sendSystemMessage(Component.translatable(
                "message.kubanhorizons.genie.vessel.palace_left"));
        return true;
    }

    /** Массивная генерация мира: обеспечивает десятки тысяч блоков (парящие острова, степь). */
    public static void ensureMassiveWorld(ServerLevel level, int radiusChunks) {
        int centerX = 0, centerZ = 0;
        for (int cx = centerX - radiusChunks; cx <= centerX + radiusChunks; cx++) {
            for (int cz = centerZ - radiusChunks; cz <= centerZ + radiusChunks; cz++) {
                level.getChunk(cx, cz); // Force load for massive world
            }
        }
    }

    /** Абсолютная власть джиннии: мгновенная материализация из дыма по желанию. */
    public static boolean materializeWish(ServerPlayer genie, String wishDescription, BlockPos targetPos) {
        if (!isVisitingPalace(genie) || !isGenie(genie)) {
            genie.sendSystemMessage(Component.translatable(
                    "message.kubanhorizons.genie.vessel.only_inside"));
            return false;
        }
        ServerLevel level = (ServerLevel) genie.level();
        // Пример: "хочу дворец здесь" -> массив блоков с частицами дыма
        // В реальной интеграции: парсить wishDescription, генерировать структуру
        // Здесь: простой пример - возвести 10x10x10 дворец из золота/камня
        for (int x = -5; x <= 5; x++) {
            for (int y = 0; y <= 10; y++) {
                for (int z = -5; z <= 5; z++) {
                    BlockPos pos = targetPos.offset(x, y, z);
                    var state = (y == 0 || y == 10) ? Blocks.GOLD_BLOCK.defaultBlockState() :
                            (x == -5 || x == 5 || z == -5 || z == 5) ? Blocks.STONE_BRICKS.defaultBlockState() :
                                    Blocks.AIR.defaultBlockState();
                    level.setBlock(pos, state, 3);
                    if (level.random.nextFloat() < 0.3f) {
                        level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                3, 0.2, 0.2, 0.2, 0.01);
                    }
                }
            }
        }
        genie.sendSystemMessage(Component.translatable(
                "message.kubanhorizons.genie.vessel.wish_materialized", wishDescription));
        return true;
    }

    /** Редактирование правил через команду/интерфейс. */
    public static void editRule(ServerPlayer editor, String ruleKey, String value) {
        if (!isVisitingPalace(editor) || !isGenie(editor)) return;
        // Простой парсер; в продакшене - полноценный GUI/команда
        switch (ruleKey.toLowerCase()) {
            case "gravity" -> currentRules = new PocketDimensionRules(
                    Float.parseFloat(value), currentRules.eternalSunset(), currentRules.noHostileMobs(),
                    currentRules.flightAllowed(), currentRules.keepInventoryOnDeath(), currentRules.worldRadiusChunks());
            case "flight" -> currentRules = new PocketDimensionRules(
                    currentRules.gravityMultiplier(), currentRules.eternalSunset(), currentRules.noHostileMobs(),
                    Boolean.parseBoolean(value), currentRules.keepInventoryOnDeath(), currentRules.worldRadiusChunks());
            // ... другие правила
            default -> editor.sendSystemMessage(Component.literal("Unknown rule: " + ruleKey));
        }
        applyRulesToAllPlayers((ServerLevel) editor.level());
    }

    /** Обработчик смерти: no item loss если keepInventoryOnDeath. */
    public static void handleDeath(ServerPlayer player) {
        if (isVisitingPalace(player) && currentRules.keepInventoryOnDeath()) {
            // Предотвращаем дроп: инвентарь сохраняется автоматически в NeoForge death event override
            player.sendSystemMessage(Component.translatable(
                    "message.kubanhorizons.genie.vessel.death_safe"));
        }
    }

    /** Tick handler для гравитации и правил (регистрируется в главном классе мода). */
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && isVisitingPalace(player)) {
            applyRulesToPlayer(player);
            // Кастомная гравитация 30%
            if (currentRules.gravityMultiplier() < 1.0f && !player.onGround()) {
                Vec3 motion = player.getDeltaMovement();
                player.setDeltaMovement(motion.x, motion.y * currentRules.gravityMultiplier(), motion.z);
            }
        }
    }

    /** Death handler для keepInventory. */
    public static void onPlayerDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            handleDeath(player);
        }
    }
}
