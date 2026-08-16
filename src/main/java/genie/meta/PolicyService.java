package genie.meta;

import genie.KubanGenie;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent policy service that stores and manages various game policies
 * that persist across server restarts and player sessions.
 */
public class PolicyService extends SavedData {

    private static final String DATA_NAME = KubanGenie.MODID + "_Policies";

    // Policy storage
    private final Map<String, CompoundTag> policies = new HashMap<>();
    private final Map<UUID, CompoundTag> playerPolicies = new HashMap<>();

    public PolicyService() {
        super();
    }

    public static PolicyService get(Level level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
            PolicyService::load,
            PolicyService::new,
            DATA_NAME
        );
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag policiesTag = new CompoundTag();

        // Save global policies
        CompoundTag globalPolicies = new CompoundTag();
        policies.forEach((key, value) -> globalPolicies.put(key, value));
        policiesTag.put("global_policies", globalPolicies);

        // Save player policies
        CompoundTag playerPoliciesTag = new CompoundTag();
        playerPolicies.forEach((uuid, policy) -> playerPoliciesTag.put(uuid.toString(), policy));
        policiesTag.put("player_policies", playerPoliciesTag);

        return policiesTag;
    }

    public static PolicyService load(CompoundTag tag) {
        PolicyService service = new PolicyService();

        if (tag.contains("global_policies")) {
            CompoundTag globalPolicies = tag.getCompound("global_policies");
            globalPolicies.getAllKeys().forEach(key -> service.policies.put(key, globalPolicies.getCompound(key)));
        }

        if (tag.contains("player_policies")) {
            CompoundTag playerPoliciesTag = tag.getCompound("player_policies");
            playerPoliciesTag.getAllKeys().forEach(key -> {
                UUID uuid = UUID.fromString(key);
                service.playerPolicies.put(uuid, playerPoliciesTag.getCompound(key));
            });
        }

        return service;
    }

    // Global policy methods

    /**
     * Set a global policy
     */
    public void setGlobalPolicy(String policyName, CompoundTag policyData) {
        policies.put(policyName, policyData);
        setDirty();
    }

    /**
     * Get a global policy
     */
    public CompoundTag getGlobalPolicy(String policyName) {
        return policies.get(policyName);
    }

    /**
     * Remove a global policy
     */
    public void removeGlobalPolicy(String policyName) {
        policies.remove(policyName);
        setDirty();
    }

    // Player policy methods

    /**
     * Set a player-specific policy
     */
    public void setPlayerPolicy(UUID playerId, String policyName, CompoundTag policyData) {
        playerPolicies.computeIfAbsent(playerId, k -> new CompoundTag())
            .put(policyName, policyData);
        setDirty();
    }

    /**
     * Get a player-specific policy
     */
    public CompoundTag getPlayerPolicy(UUID playerId, String policyName) {
        CompoundTag playerData = playerPolicies.get(playerId);
        return playerData != null ? playerData.getCompound(policyName) : null;
    }

    /**
     * Remove a player-specific policy
     */
    public void removePlayerPolicy(UUID playerId, String policyName) {
        CompoundTag playerData = playerPolicies.get(playerId);
        if (playerData != null) {
            playerData.remove(policyName);
            setDirty();
        }
    }

    /**
     * Check if player has a specific policy
     */
    public boolean hasPlayerPolicy(UUID playerId, String policyName) {
        CompoundTag playerData = playerPolicies.get(playerId);
        return playerData != null && playerData.contains(policyName);
    }

    // Common policy types

    /**
     * Set instant smelt policy for a player
     */
    public void setInstantSmeltPolicy(UUID playerId, boolean enabled) {
        CompoundTag policyData = new CompoundTag();
        policyData.putBoolean("enabled", enabled);
        policyData.putLong("timestamp", System.currentTimeMillis());

        setPlayerPolicy(playerId, "instant_smelt", policyData);
    }

    /**
     * Get instant smelt policy for a player
     */
    public boolean isInstantSmeltEnabled(UUID playerId) {
        CompoundTag policyData = getPlayerPolicy(playerId, "instant_smelt");
        return policyData != null && policyData.getBoolean("enabled");
    }

    /**
     * Set creative mode restrictions for a player
     */
    public void setCreativeRestrictions(UUID playerId, boolean restricted) {
        CompoundTag policyData = new CompoundTag();
        policyData.putBoolean("restricted", restricted);
        policyData.putInt("max_creative_items", restricted ? 64 : -1);

        setPlayerPolicy(playerId, "creative_restrictions", policyData);
    }

    /**
     * Check if player has creative restrictions
     */
    public boolean isCreativeRestricted(UUID playerId) {
        CompoundTag policyData = getPlayerPolicy(playerId, "creative_restrictions");
        return policyData != null && policyData.getBoolean("restricted");
    }

    /**
     * Set world generation rules
     */
    public void setWorldGenRules(String dimension, boolean generateStructures, boolean generateOres) {
        CompoundTag dimensionRules = new CompoundTag();
        dimensionRules.putBoolean("generate_structures", generateStructures);
        dimensionRules.putBoolean("generate_ores", generateOres);

        setGlobalPolicy("world_gen_" + dimension, dimensionRules);
    }

    /**
     * Get world generation rules for a dimension
     */
    public Map<String, Boolean> getWorldGenRules(String dimension) {
        CompoundTag policyData = getGlobalPolicy("world_gen_" + dimension);
        Map<String, Boolean> rules = new HashMap<>();

        if (policyData != null) {
            rules.put("generate_structures", policyData.getBoolean("generate_structures"));
            rules.put("generate_ores", policyData.getBoolean("generate_ores"));
        }

        return rules;
    }
}