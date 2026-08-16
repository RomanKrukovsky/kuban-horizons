package genie.meta;

import genie.KubanGenie;
import genie.events.GenieEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global rule engine for Kuban Genie that controls world-level rules
 * and provides a unified interface for managing game rules through genie mechanics.
 */
@Mod.EventBusSubscriber(modid = KubanGenie.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MetaRuleEngine {

    // Global rule categories
    public enum RuleCategory {
        MOB_GRIEFING,
        WEATHER_CONTROL,
        WORLD_CLOCK,
        TIME_FREEZE,
        STRUCTURE_GROWTH,
        PARTICLE_LIMITS,
        SOUND_LIMITS
    }

    // Rule storage
    private static final Map<RuleCategory, Boolean> globalRules = new HashMap<>();
    private static final Map<UUID, Map<RuleCategory, Boolean>> playerRules = new HashMap<>();
    private static final Map<UUID, Map<RuleCategory, Long>> ruleCooldowns = new HashMap<>();

    // Rule cooldowns in ticks (20 ticks = 1 second)
    private static final long DEFAULT_COOLDOWN = 20 * 60 * 20; // 20 minutes
    private static final long WORLD_CLOCK_COOLDOWN = 20 * 60 * 60; // 1 hour

    static {
        // Initialize default rules
        globalRules.put(RuleCategory.MOB_GRIEFING, true);
        globalRules.put(RuleCategory.WEATHER_CONTROL, false);
        globalRules.put(RuleCategory.WORLD_CLOCK, false);
        globalRules.put(RuleCategory.TIME_FREEZE, false);
        globalRules.put(RuleCategory.STRUCTURE_GROWTH, true);
        globalRules.put(RuleCategory.PARTICLE_LIMITS, true);
        globalRules.put(RuleCategory.SOUND_LIMITS, true);
    }

    /**
     * Get the current global rule value
     */
    public static boolean getGlobalRule(RuleCategory category) {
        return globalRules.getOrDefault(category, true);
    }

    /**
     * Set a global rule (requires admin permissions)
     */
    public static boolean setGlobalRule(RuleCategory category, boolean value, ServerPlayer player) {
        if (!player.hasPermissions(4)) {
            return false; // Not admin
        }

        globalRules.put(category, value);
        GenieEvents.META_RULE_CHANGED.post(new MetaRuleChangedEvent(category, value, player.getUUID()));
        return true;
    }

    /**
     * Get player-specific rule override
     */
    public static boolean getPlayerRuleOverride(UUID playerId, RuleCategory category) {
        return playerRules.getOrDefault(playerId, Map.of()).getOrDefault(category, globalRules.getOrDefault(category, true));
    }

    /**
     * Set player-specific rule override
     */
    public static boolean setPlayerRuleOverride(UUID playerId, RuleCategory category, boolean value, ServerPlayer player) {
        if (!player.getUUID().equals(playerId) && !player.hasPermissions(4)) {
            return false; // Can only override own rules unless admin
        }

        playerRules.computeIfAbsent(playerId, k -> new HashMap<>()).put(category, value);
        GenieEvents.PLAYER_RULE_CHANGED.post(new PlayerRuleChangedEvent(playerId, category, value));
        return true;
    }

    /**
     * Check if player can change rules (cooldown check)
     */
    public static boolean canChangeRule(UUID playerId, RuleCategory category) {
        long cooldown = category == RuleCategory.WORLD_CLOCK ? WORLD_CLOCK_COOLDOWN : DEFAULT_COOLDOWN;
        long currentTime = System.currentTimeMillis();

        if (!ruleCooldowns.containsKey(playerId)) {
            ruleCooldowns.put(playerId, new HashMap<>());
        }

        Long lastChange = ruleCooldowns.get(playerId).get(category);
        if (lastChange == null || currentTime - lastChange > cooldown) {
            ruleCooldowns.get(playerId).put(category, currentTime);
            return true;
        }

        return false;
    }

    /**
     * Apply mob griefing rule to a specific level
     */
    public static boolean shouldAllowMobGriefing(ServerLevel level) {
        boolean globalSetting = getGlobalRule(RuleCategory.MOB_GRIEFING);

        // Check for player overrides in this dimension
        if (level.players().stream().anyMatch(p -> getPlayerRuleOverride(p.getUUID(), RuleCategory.MOB_GRIEFING))) {
            return false; // Player override disables griefing
        }

        return globalSetting;
    }

    /**
     * Control weather in a dimension
     */
    public static void setWeather(ServerLevel level, boolean raining, boolean thunder) {
        if (!getGlobalRule(RuleCategory.WEATHER_CONTROL)) {
            return; // Weather control disabled globally
        }

        level.setWeatherParameters(raining ? 1 : 0, thunder ? 1 : 0, false, false);
    }

    /**
     * Freeze time in a dimension
     */
    public static void setTimeFrozen(ServerLevel level, boolean frozen) {
        if (frozen && !getGlobalRule(RuleCategory.TIME_FREEZE)) {
            return; // Time freeze disabled globally
        }

        level.setDayTime(frozen ? level.getDayTime() : System.currentTimeMillis() % 24000);
    }

    /**
     * Get current world time
     */
    public static long getWorldTime(ServerLevel level) {
        if (getGlobalRule(RuleCategory.WORLD_CLOCK)) {
            return level.getDayTime();
        }
        return System.currentTimeMillis() % 24000;
    }

    /**
     * Load rules from disk
     */
    public static void loadRules() {
        // TODO: Implement persistent storage
    }

    /**
     * Save rules to disk
     */
    public static void saveRules() {
        // TODO: Implement persistent storage
    }

    // Event classes
    public static class MetaRuleChangedEvent {
        public final RuleCategory category;
        public final boolean newValue;
        public final UUID changerId;

        public MetaRuleChangedEvent(RuleCategory category, boolean newValue, UUID changerId) {
            this.category = category;
            this.newValue = newValue;
            this.changerId = changerId;
        }
    }

    public static class PlayerRuleChangedEvent {
        public final UUID playerId;
        public final RuleCategory category;
        public final boolean newValue;

        public PlayerRuleChangedEvent(UUID playerId, RuleCategory category, boolean newValue) {
            this.playerId = playerId;
            this.category = category;
            this.newValue = newValue;
        }
    }
}