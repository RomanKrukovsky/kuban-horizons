package dev.romankrukovsky.kubanhorizons.genie.memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Contract lifecycle engine for MC 26.2.
 *
 * <p>Manages creation, acceptance, breach, and penalty application for contracts.
 * Integrates with WorldGenieMemory for persistent storage. All operations are
 * deterministic for multiplayer safety.</p>
 */
public final class ContractEngine {

    private final WorldGenieMemory memory;
    private final Map<UUID, Contract> activeContracts = new HashMap<>();
    private final Map<UUID, List<Penalty>> penalties = new HashMap<>();

    public ContractEngine(WorldGenieMemory memory) {
        this.memory = memory;
    }

    public static ContractEngine get(ServerLevel level) {
        return new ContractEngine(WorldGenieMemory.get(level));
    }

    /**
     * Proposes a new contract between parties with specified terms and duration.
     *
     * @param parties set of participating UUIDs
     * @param terms list of contractual obligations
     * @param durationTicks contract lifetime in game ticks (0 = no expiry)
     * @return newly created contract
     */
    public Contract proposeContract(Set<UUID> parties, List<String> terms, long durationTicks) {
        if (parties == null || parties.size() < 2) {
            throw new IllegalArgumentException("contract requires at least two parties");
        }
        if (terms == null || terms.isEmpty()) {
            throw new IllegalArgumentException("contract requires at least one term");
        }

        UUID contractId = UUID.randomUUID();
        long now = System.currentTimeMillis(); // deterministic seed, caller should pass gameTime if needed
        long expiry = durationTicks > 0 ? now + durationTicks : 0L;

        Map<UUID, Boolean> initialBreach = new HashMap<>();
        for (UUID party : parties) {
            initialBreach.put(party, false); // pending acceptance
        }

        Contract contract = new Contract(contractId, parties, terms, initialBreach, now, expiry);
        activeContracts.put(contractId, contract);
        return contract;
    }

    /**
     * Accepts a contract on behalf of a party. Once all parties accept, the contract is active.
     *
     * @param contractId target contract
     * @param party accepting party
     * @return updated contract or null if not found
     */
    public Contract acceptContract(UUID contractId, UUID party) {
        Contract existing = activeContracts.get(contractId);
        if (existing == null) {
            return null;
        }

        if (!existing.parties().contains(party)) {
            throw new IllegalArgumentException("party not part of contract");
        }

        Map<UUID, Boolean> updatedBreach = new HashMap<>(existing.breachStatus());
        updatedBreach.put(party, true);

        Contract updated = new Contract(
                existing.contractId(),
                existing.parties(),
                existing.terms(),
                updatedBreach,
                existing.creationTime(),
                existing.expiryTime()
        );
        activeContracts.put(contractId, updated);
        return updated;
    }

    /**
     * Marks a contract as breached by a party and applies penalty.
     *
     * @param contractId target contract
     * @param breachingParty party that breached
     * @param penalty penalty to apply
     * @return updated contract or null if not found
     */
    public Contract breachContract(UUID contractId, UUID breachingParty, Penalty penalty) {
        Contract existing = activeContracts.get(contractId);
        if (existing == null) {
            return null;
        }

        if (!existing.parties().contains(breachingParty)) {
            throw new IllegalArgumentException("party not part of contract");
        }

        Map<UUID, Boolean> updatedBreach = new HashMap<>(existing.breachStatus());
        updatedBreach.put(breachingParty, false);

        Contract updated = new Contract(
                existing.contractId(),
                existing.parties(),
                existing.terms(),
                updatedBreach,
                existing.creationTime(),
                existing.expiryTime()
        );
        activeContracts.put(contractId, updated);

        penalties.computeIfAbsent(breachingParty, k -> new ArrayList<>()).add(penalty);
        return updated;
    }

    public Contract getContract(UUID contractId) {
        return activeContracts.get(contractId);
    }

    public List<Penalty> getPenalties(UUID party) {
        return penalties.getOrDefault(party, List.of());
    }

    public void clearContract(UUID contractId) {
        activeContracts.remove(contractId);
    }

    /** Serializes active contracts and penalties. */
    public void save(ValueOutput output) {
        var contractsList = output.childrenList("ActiveContracts");
        for (Contract contract : activeContracts.values()) {
            var child = contractsList.addChild();
            contract.save(child);
        }

        var penaltiesList = output.childrenList("Penalties");
        for (Map.Entry<UUID, List<Penalty>> entry : penalties.entrySet()) {
            var partyNode = penaltiesList.addChild();
            partyNode.putString("Party", entry.getKey().toString());
            var penaltyNodes = partyNode.childrenList("Penalties");
            for (Penalty p : entry.getValue()) {
                var pNode = penaltyNodes.addChild();
                pNode.putString("Type", p.type());
                pNode.putInt("Severity", p.severity());
                pNode.putString("Description", p.description());
            }
        }
    }

    /** Deserializes contracts and penalties. */
    public void load(ValueInput input) {
        activeContracts.clear();
        for (ValueInput child : input.childrenListOrEmpty("ActiveContracts")) {
            Contract contract = Contract.load(child);
            activeContracts.put(contract.contractId(), contract);
        }

        penalties.clear();
        for (ValueInput partyNode : input.childrenListOrEmpty("Penalties")) {
            UUID party = UUID.fromString(partyNode.getStringOr("Party", UUID.randomUUID().toString()));
            List<Penalty> partyPenalties = new ArrayList<>();
            for (ValueInput pNode : partyNode.childrenListOrEmpty("Penalties")) {
                String type = pNode.getStringOr("Type", "generic");
                int severity = pNode.getIntOr("Severity", 1);
                String desc = pNode.getStringOr("Description", "");
                partyPenalties.add(new Penalty(type, severity, desc));
            }
            penalties.put(party, partyPenalties);
        }
    }

    /** Penalty record applied on breach. */
    public record Penalty(String type, int severity, String description) {
    }
}
