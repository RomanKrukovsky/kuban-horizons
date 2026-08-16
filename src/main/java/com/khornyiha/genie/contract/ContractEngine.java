package com.khornyiha.genie.contract;

import com.khornyiha.genie.memory.WorldGenieMemory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contract system for Genie system.
 * Manages creation, enforcement, and tracking of contracts between players and genies.
 */
public class ContractEngine {

    private static final Map<String, Contract> activeContracts = new ConcurrentHashMap<>();
    private static final Map<UUID, List<Contract>> playerContracts = new ConcurrentHashMap<>();
    private static final Map<String, ContractTemplate> contractTemplates = new HashMap<>();

    /**
     * Create a new contract
     */
    public static Contract createContract(Player player, String terms, String conditions, String loopholes, String enforcement) {
        String contractId = UUID.randomUUID().toString();

        Contract contract = new Contract(
            contractId,
            player.getUUID(),
            player.getName().getString(),
            terms
        );

        contract.setConditions(conditions);
        contract.setLoopholes(loopholes);
        contract.setEnforcement(enforcement);

        // Add to active contracts
        activeContracts.put(contractId, contract);

        // Add to player's contract list
        playerContracts.computeIfAbsent(player.getUUID(), k -> new ArrayList<>()).add(contract);

        // Record in world memory
        WorldGenieMemory memory = WorldGenieMemory.get((ServerLevel) player.level());
        memory.recordContractCreation(player.getUUID(), contractId, terms);

        return contract;
    }

    /**
     * Create contract from template
     */
    public static Contract createContractFromTemplate(Player player, String templateId, Map<String, String> variables) {
        ContractTemplate template = contractTemplates.get(templateId);
        if (template == null) {
            throw new IllegalArgumentException("Unknown contract template: " + templateId);
        }

        String terms = template.render(variables);
        return createContract(player, terms, template.getDefaultConditions(), template.getDefaultLoopholes(), template.getDefaultEnforcement());
    }

    /**
     * Check if player has active contracts
     */
    public static boolean hasActiveContracts(Player player) {
        List<Contract> contracts = playerContracts.get(player.getUUID());
        if (contracts == null) {
            return false;
        }

        return contracts.stream().anyMatch(c -> c.getStatus() == Contract.ContractStatus.ACTIVE);
    }

    /**
     * Get active contracts for player
     */
    public static List<Contract> getActiveContracts(Player player) {
        List<Contract> contracts = playerContracts.get(player.getUUID());
        if (contracts == null) {
            return Collections.emptyList();
        }

        return contracts.stream()
            .filter(c -> c.getStatus() == Contract.ContractStatus.ACTIVE)
            .toList();
    }

    /**
     * Get all contracts for player
     */
    public static List<Contract> getPlayerContracts(Player player) {
        return playerContracts.getOrDefault(player.getUUID(), Collections.emptyList());
    }

    /**
     * Get contract by ID
     */
    public static Contract getContract(String contractId) {
        return activeContracts.get(contractId);
    }

    /**
     * Update contract status
     */
    public static void updateContractStatus(String contractId, Contract.ContractStatus newStatus) {
        Contract contract = activeContracts.get(contractId);
        if (contract != null) {
            contract.status = newStatus;

            // Update world memory
            WorldGenieMemory memory = WorldGenieMemory.getFromAnyLevel();
            if (memory != null) {
                memory.updateContractStatus(contractId, newStatus.name());
            }
        }
    }

    /**
     * Check and enforce contracts for player
     */
    public static void enforceContracts(Player player) {
        List<Contract> contracts = getActiveContracts(player);
        if (contracts.isEmpty()) {
            return;
        }

        Map<String, Object> context = new HashMap<>();
        context.put("player", player);
        context.put("world", player.level());
        context.put("time", player.level().getGameTime());

        for (Contract contract : contracts) {
            if (contract.checkViolation(context)) {
                Contract.ContractResult result = contract.executeEnforcement(context);
                if (result.isSuccess()) {
                    updateContractStatus(contract.getContractId(), Contract.ContractStatus.FAILED);
                }
            }
        }
    }

    /**
     * Complete a contract
     */
    public static void completeContract(String contractId) {
        Contract contract = activeContracts.get(contractId);
        if (contract != null) {
            contract.complete();
            updateContractStatus(contractId, Contract.ContractStatus.COMPLETED);
        }
    }

    /**
     * Cancel a contract
     */
    public static void cancelContract(String contractId) {
        Contract contract = activeContracts.get(contractId);
        if (contract != null) {
            contract.cancel();
            updateContractStatus(contractId, Contract.ContractStatus.CANCELLED);
        }
    }

    /**
     * Cleanup expired contracts
     */
    public static void cleanupExpiredContracts() {
        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, Contract> entry : activeContracts.entrySet()) {
            if (entry.getValue().isExpired()) {
                toRemove.add(entry.getKey());
            }
        }

        for (String contractId : toRemove) {
            activeContracts.remove(contractId);
        }
    }

    /**
     * Register a contract template
     */
    public static void registerTemplate(String templateId, ContractTemplate template) {
        contractTemplates.put(templateId, template);
    }

    /**
     * Get contract template
     */
    public static ContractTemplate getTemplate(String templateId) {
        return contractTemplates.get(templateId);
    }

    /**
     * Check if contract exists
     */
    public static boolean contractExists(String contractId) {
        return activeContracts.containsKey(contractId);
    }

    /**
     * Add metadata to contract
     */
    public static void addContractMetadata(String contractId, String key, String value) {
        Contract contract = activeContracts.get(contractId);
        if (contract != null) {
            contract.addMetadata(key, value);
        }
    }

    /**
     * Get contract metadata
     */
    public static String getContractMetadata(String contractId, String key) {
        Contract contract = activeContracts.get(contractId);
        if (contract != null) {
            return contract.getMetadata(key);
        }
        return null;
    }

    /**
     * Contract template for reusable contract patterns
     */
    public static class ContractTemplate {
        private final String templateId;
        private final String templateText;
        private final String defaultConditions;
        private final String defaultLoopholes;
        private final String defaultEnforcement;

        public ContractTemplate(String templateId, String templateText, String defaultConditions,
                              String defaultLoopholes, String defaultEnforcement) {
            this.templateId = templateId;
            this.templateText = templateText;
            this.defaultConditions = defaultConditions;
            this.defaultLoopholes = defaultLoopholes;
            this.defaultEnforcement = defaultEnforcement;
        }

        public String render(Map<String, String> variables) {
            String result = templateText;
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                result = result.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            return result;
        }

        public String getTemplateId() {
            return templateId;
        }

        public String getTemplateText() {
            return templateText;
        }

        public String getDefaultConditions() {
            return defaultConditions;
        }

        public String getDefaultLoopholes() {
            return defaultLoopholes;
        }

        public String getDefaultEnforcement() {
            return defaultEnforcement;
        }
    }
}
