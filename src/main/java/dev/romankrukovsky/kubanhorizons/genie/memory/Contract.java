package dev.romankrukovsky.kubanhorizons.genie.memory;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Immutable contract record for MC 26.2.
 *
 * <p>Represents a binding agreement between multiple parties with defined terms,
 * breach tracking, and temporal bounds. Serialized via ValueInput/ValueOutput
 * for strict MC 26.2 compatibility.</p>
 */
public record Contract(
        UUID contractId,
        Set<UUID> parties,
        List<String> terms,
        Map<UUID, Boolean> breachStatus,
        long creationTime,
        long expiryTime
) {

    public Contract {
        parties = Collections.unmodifiableSet(new HashSet<>(parties));
        terms = List.copyOf(terms);
        breachStatus = Collections.unmodifiableMap(new HashMap<>(breachStatus));
    }

    /** Returns true if all parties have accepted (no pending breaches marked false). */
    public boolean isFullyAccepted() {
        return parties.stream().allMatch(p -> breachStatus.getOrDefault(p, false));
    }

    /** Returns true if the contract has expired based on the provided game time. */
    public boolean isExpired(long currentGameTime) {
        return expiryTime > 0 && currentGameTime >= expiryTime;
    }

    /** Returns true if any party has breached. */
    public boolean hasBreaches() {
        return breachStatus.values().stream().anyMatch(Boolean.FALSE::equals);
    }

    /** Serializes this contract to ValueOutput. */
    public void save(ValueOutput output) {
        output.putString("ContractId", contractId.toString());
        output.putLong("CreationTime", creationTime);
        output.putLong("ExpiryTime", expiryTime);

        var partiesList = output.childrenList("Parties");
        for (UUID party : parties) {
            partiesList.addChild().putString("Id", party.toString());
        }

        var termsList = output.childrenList("Terms");
        for (String term : terms) {
            termsList.addChild().putString("Text", term);
        }

        var breachList = output.childrenList("BreachStatus");
        for (Map.Entry<UUID, Boolean> entry : breachStatus.entrySet()) {
            var child = breachList.addChild();
            child.putString("Party", entry.getKey().toString());
            child.putBoolean("Accepted", entry.getValue());
        }
    }

    /** Deserializes a contract from ValueInput. */
    public static Contract load(ValueInput input) {
        UUID id = UUID.fromString(input.getStringOr("ContractId", UUID.randomUUID().toString()));
        long creation = input.getLongOr("CreationTime", 0L);
        long expiry = input.getLongOr("ExpiryTime", 0L);

        Set<UUID> loadedParties = new HashSet<>();
        for (ValueInput child : input.childrenListOrEmpty("Parties")) {
            String raw = child.getStringOr("Id", "");
            if (!raw.isEmpty()) {
                try {
                    loadedParties.add(UUID.fromString(raw));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        List<String> loadedTerms = new java.util.ArrayList<>();
        for (ValueInput child : input.childrenListOrEmpty("Terms")) {
            String text = child.getStringOr("Text", "");
            if (!text.isBlank()) {
                loadedTerms.add(text);
            }
        }

        Map<UUID, Boolean> loadedBreach = new HashMap<>();
        for (ValueInput child : input.childrenListOrEmpty("BreachStatus")) {
            String raw = child.getStringOr("Party", "");
            if (!raw.isEmpty()) {
                try {
                    UUID party = UUID.fromString(raw);
                    boolean accepted = child.getBooleanOr("Accepted", false);
                    loadedBreach.put(party, accepted);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        return new Contract(id, loadedParties, loadedTerms, loadedBreach, creation, expiry);
    }
}
