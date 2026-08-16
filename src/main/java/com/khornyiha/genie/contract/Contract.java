package com.khornyiha.genie.contract;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.*;

/**
 * Individual contract between player and Genie.
 * Contains terms, conditions, and enforcement logic.
 */
public class Contract {

    private final String contractId;
    private final UUID ownerId;
    private final String ownerName;
    private final long creationTime;
    private final long expirationTime;

    private String terms;
    private String conditions;
    private String loopholes;
    private String enforcement;
    private ContractStatus status;

    private final Map<String, String> metadata = new HashMap<>();

    public enum ContractStatus {
        ACTIVE,
        COMPLETED,
        FAILED,
        CANCELLED,
        EXPIRED
    }

    public Contract(String contractId, UUID ownerId, String ownerName, String terms) {
        this.contractId = contractId;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.creationTime = System.currentTimeMillis();
        this.expirationTime = creationTime + 7 * 24 * 60 * 60 * 1000L; // 7 days default
        this.terms = terms;
        this.status = ContractStatus.ACTIVE;
        this.conditions = "";
        this.loopholes = "";
        this.enforcement = "";
    }

    /**
     * Check if contract conditions are met
     */
    public boolean evaluateConditions(Map<String, Object> context) {
        if (status != ContractStatus.ACTIVE) {
            return false;
        }

        // Simple condition evaluation
        if (conditions.isEmpty()) {
            return true; // No conditions = always true
        }

        // Parse conditions (simplified)
        String[] conditionParts = conditions.split("&&");
        for (String condition : conditionParts) {
            condition = condition.trim();
            if (!evaluateSingleCondition(condition, context)) {
                return false;
            }
        }

        return true;
    }

    private boolean evaluateSingleCondition(String condition, Map<String, Object> context) {
        // Format: "variable operator value"
        String[] parts = condition.split("\\s+");
        if (parts.length < 3) {
            return false;
        }

        String variable = parts[0];
        String operator = parts[1];
        String valueStr = String.join(" ", Arrays.copyOfRange(parts, 2, parts.length));

        Object value = context.get(variable);
        if (value == null) {
            return false;
        }

        try {
            return switch (operator) {
                case "==" -> value.toString().equals(valueStr);
                case "!=" -> !value.toString().equals(valueStr);
                case ">" -> Double.parseDouble(value.toString()) > Double.parseDouble(valueStr);
                case "<" -> Double.parseDouble(value.toString()) < Double.parseDouble(valueStr);
                case ">=" -> Double.parseDouble(value.toString()) >= Double.parseDouble(valueStr);
                case "<=" -> Double.parseDouble(value.toString()) <= Double.parseDouble(valueStr);
                case "contains" -> value.toString().contains(valueStr);
                default -> false;
            };
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Detect potential loopholes in contract
     */
    public List<String> detectLoopholes() {
        List<String> foundLoopholes = new ArrayList<>();

        if (loopholes.isEmpty()) {
            return foundLoopholes;
        }

        // Parse loopholes
        String[] loopholeParts = loopholes.split("\\n");
        for (String loophole : loopholeParts) {
            loophole = loophole.trim();
            if (!loophole.isEmpty()) {
                foundLoopholes.add(loophole);
            }
        }

        return foundLoopholes;
    }

    /**
     * Check if contract has been violated
     */
    public boolean checkViolation(Map<String, Object> context) {
        if (status != ContractStatus.ACTIVE) {
            return false;
        }

        // Check enforcement rules
        if (enforcement.isEmpty()) {
            return false; // No enforcement = no violation
        }

        // Parse enforcement rules
        String[] rules = enforcement.split("\\n");
        for (String rule : rules) {
            rule = rule.trim();
            if (rule.startsWith("violation:")) {
                String condition = rule.substring("violation:".length()).trim();
                if (evaluateSingleCondition(condition, context)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Execute contract enforcement
     */
    public ContractResult executeEnforcement(Map<String, Object> context) {
        if (!checkViolation(context)) {
            return new ContractResult(false, "No violation detected");
        }

        // Apply enforcement
        String[] actions = enforcement.split("\\n");
        for (String action : actions) {
            action = action.trim();
            if (action.startsWith("action:")) {
                String command = action.substring("action:".length()).trim();
                // Execute command (simplified)
                context.put("enforcementAction", command);
            }
        }

        return new ContractResult(true, "Enforcement executed");
    }

    /**
     * Complete the contract
     */
    public void complete() {
        this.status = ContractStatus.COMPLETED;
    }

    /**
     * Fail the contract
     */
    public void fail() {
        this.status = ContractStatus.FAILED;
    }

    /**
     * Cancel the contract
     */
    public void cancel() {
        this.status = ContractStatus.CANCELLED;
    }

    /**
     * Check if contract has expired
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expirationTime;
    }

    /**
     * Add metadata to contract
     */
    public void addMetadata(String key, String value) {
        metadata.put(key, value);
    }

    /**
     * Get metadata value
     */
    public String getMetadata(String key) {
        return metadata.get(key);
    }

    /**
     * Save contract to NBT
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.putString("contractId", contractId);
        tag.putUuid("ownerId", ownerId);
        tag.putString("ownerName", ownerName);
        tag.putLong("creationTime", creationTime);
        tag.putLong("expirationTime", expirationTime);
        tag.putString("terms", terms);
        tag.putString("conditions", conditions);
        tag.putString("loopholes", loopholes);
        tag.putString("enforcement", enforcement);
        tag.putString("status", status.name());

        // Save metadata
        CompoundTag metadataTag = new CompoundTag();
        metadata.forEach(metadataTag::putString);
        tag.put("metadata", metadataTag);

        return tag;
    }

    /**
     * Load contract from NBT
     */
    public static Contract load(CompoundTag tag) {
        Contract contract = new Contract(
            tag.getString("contractId"),
            tag.getUuid("ownerId"),
            tag.getString("ownerName"),
            tag.getString("terms")
        );

        contract.creationTime = tag.getLong("creationTime");
        contract.expirationTime = tag.getLong("expirationTime");
        contract.terms = tag.getString("terms");
        contract.conditions = tag.getString("conditions");
        contract.loopholes = tag.getString("loopholes");
        contract.enforcement = tag.getString("enforcement");
        contract.status = ContractStatus.valueOf(tag.getString("status"));

        // Load metadata
        CompoundTag metadataTag = tag.getCompound("metadata");
        metadataTag.getAllKeys().forEach(key ->
            contract.metadata.put(key, metadataTag.getString(key))
        );

        return contract;
    }

    // Getters
    public String getContractId() {
        return contractId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    public String getTerms() {
        return terms;
    }

    public void setTerms(String terms) {
        this.terms = terms;
    }

    public String getConditions() {
        return conditions;
    }

    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    public String getLoopholes() {
        return loopholes;
    }

    public void setLoopholes(String loopholes) {
        this.loopholes = loopholes;
    }

    public String getEnforcement() {
        return enforcement;
    }

    public void setEnforcement(String enforcement) {
        this.enforcement = enforcement;
    }

    public ContractStatus getStatus() {
        return status;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    /**
     * Contract execution result
     */
    public static class ContractResult {
        private final boolean success;
        private final String message;

        public ContractResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}
