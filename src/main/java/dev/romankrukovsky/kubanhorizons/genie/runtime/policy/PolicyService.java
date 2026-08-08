package dev.romankrukovsky.kubanhorizons.genie.runtime.policy;

import dev.romankrukovsky.kubanhorizons.genie.runtime.confirmation.ConfirmedPolicy;
import dev.romankrukovsky.kubanhorizons.genie.runtime.preview.PolicyPreview;
import dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome;
import dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionReport;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.gamerules.GameRules;

/** Reversible policy runtime для глобального mobGriefing. */
public final class PolicyService {
    public static final String MOB_GRIEFING = "minecraft:mob_griefing";
    public static final String WEATHER = "minecraft:weather";
    private static final Duration PREVIEW_TTL = Duration.ofMinutes(2);
    private final PolicyManifestStore store;
    private final Set<UUID> issued = new HashSet<>();

    public PolicyService(PolicyManifestStore store) {
        this.store = store;
    }

    public PolicyPreview previewMobGriefing(UUID actor, MinecraftServer server, boolean target) {
        return new PolicyPreview(UUID.randomUUID(), actor, MOB_GRIEFING,
                Boolean.toString(server.getGameRules().get(GameRules.MOB_GRIEFING)),
                Boolean.toString(target),
                Instant.now().plus(PREVIEW_TTL));
    }

    public PolicyPreview previewWeather(UUID actor, MinecraftServer server,
                                        float rain, float thunder) {
        return new PolicyPreview(UUID.randomUUID(), actor, WEATHER,
                encodeWeather(server.overworld().getRainLevel(1.0F),
                        server.overworld().getThunderLevel(1.0F)),
                encodeWeather(rain, thunder), Instant.now().plus(PREVIEW_TTL));
    }

    public synchronized ConfirmedPolicy confirm(UUID actor, PolicyPreview preview) {
        if (!preview.actorId().equals(actor) || !preview.expiresAt().isAfter(Instant.now())) {
            throw new IllegalArgumentException("policy preview is stale or belongs to another actor");
        }
        UUID id = UUID.randomUUID();
        issued.add(id);
        return new ConfirmedPolicy(id, preview, Instant.now());
    }

    public TransactionReport execute(UUID actor, MinecraftServer server, ConfirmedPolicy confirmed)
            throws IOException {
        synchronized (this) {
            if (!confirmed.preview().actorId().equals(actor)
                    || !confirmed.preview().expiresAt().isAfter(Instant.now())
                    || !issued.remove(confirmed.confirmationId())) {
                throw new IllegalArgumentException("policy confirmation is invalid or already used");
            }
        }
        if (!read(server, confirmed.preview().ruleId()).equals(confirmed.preview().beforeValue())) {
            return new TransactionReport(UUID.randomUUID(), TransactionOutcome.STALE_PREVIEW, 0,
                    "policy changed after preview");
        }
        retireCommitted(actor, server);
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        PolicyManifest prepared = new PolicyManifest(id, actor, confirmed.preview().ruleId(),
                confirmed.preview().beforeValue(), confirmed.preview().targetValue(), now,
                PolicyManifest.PolicyState.PREPARED);
        store.save(prepared);
        apply(server, prepared.ruleId(), prepared.targetValue());
        if (!read(server, prepared.ruleId()).equals(prepared.targetValue())) {
            apply(server, prepared.ruleId(), prepared.beforeValue());
            store.save(new PolicyManifest(id, actor, prepared.ruleId(), prepared.beforeValue(),
                    prepared.targetValue(), now, PolicyManifest.PolicyState.ROLLED_BACK));
            return new TransactionReport(id, TransactionOutcome.ROLLED_BACK, 0,
                    "gamerule verification failed; prior value restored");
        }
        store.save(new PolicyManifest(id, actor, prepared.ruleId(), prepared.beforeValue(),
                prepared.targetValue(), now, PolicyManifest.PolicyState.COMMITTED));
        return new TransactionReport(id, TransactionOutcome.COMPLETED, 0,
                "mobGriefing policy committed");
    }

    public TransactionReport undo(UUID actor, MinecraftServer server, UUID transactionId)
            throws IOException {
        PolicyManifest manifest = store.load(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("policy transaction not found"));
        if (!manifest.actorId().equals(actor) || manifest.state() != PolicyManifest.PolicyState.COMMITTED) {
            throw new IllegalArgumentException("policy transaction is not undoable by this actor");
        }
        apply(server, manifest.ruleId(), manifest.beforeValue());
        store.save(new PolicyManifest(manifest.transactionId(), manifest.actorId(), manifest.ruleId(),
                manifest.beforeValue(), manifest.targetValue(), manifest.createdAt(),
                PolicyManifest.PolicyState.RETIRED));
        return new TransactionReport(transactionId, TransactionOutcome.COMPLETED, 0,
                "mobGriefing policy reverted");
    }

    public void recover(MinecraftServer server) throws IOException {
        for (PolicyManifest manifest : store.list()) {
            if (manifest.state() == PolicyManifest.PolicyState.PREPARED) {
                apply(server, manifest.ruleId(), manifest.beforeValue());
                store.save(new PolicyManifest(manifest.transactionId(), manifest.actorId(), manifest.ruleId(),
                        manifest.beforeValue(), manifest.targetValue(), manifest.createdAt(),
                        PolicyManifest.PolicyState.ROLLED_BACK));
            }
        }
    }

    public List<PolicyManifest> available(UUID actor) throws IOException {
        return store.list().stream().filter(policy -> policy.actorId().equals(actor)
                && policy.state() == PolicyManifest.PolicyState.COMMITTED).toList();
    }

    private void retireCommitted(UUID actor, MinecraftServer server) throws IOException {
        for (PolicyManifest manifest : available(actor)) {
            apply(server, manifest.ruleId(), manifest.beforeValue());
            store.save(new PolicyManifest(manifest.transactionId(), manifest.actorId(), manifest.ruleId(),
                    manifest.beforeValue(), manifest.targetValue(), manifest.createdAt(),
                    PolicyManifest.PolicyState.RETIRED));
        }
    }

    private static String read(MinecraftServer server, String ruleId) {
        return switch (ruleId) {
            case MOB_GRIEFING -> Boolean.toString(server.getGameRules().get(GameRules.MOB_GRIEFING));
            case WEATHER -> encodeWeather(server.overworld().getRainLevel(1.0F),
                    server.overworld().getThunderLevel(1.0F));
            default -> throw new IllegalArgumentException("unknown policy " + ruleId);
        };
    }

    private static void apply(MinecraftServer server, String ruleId, String value) {
        switch (ruleId) {
            case MOB_GRIEFING -> server.getGameRules().set(GameRules.MOB_GRIEFING,
                    Boolean.parseBoolean(value), server);
            case WEATHER -> {
                String[] parts = value.split(",", -1);
                if (parts.length != 2) throw new IllegalArgumentException("malformed weather policy");
                float rain = Float.parseFloat(parts[0]);
                float thunder = Float.parseFloat(parts[1]);
                for (var level : server.getAllLevels()) {
                    level.setRainLevel(rain);
                    level.setThunderLevel(thunder);
                }
            }
            default -> throw new IllegalArgumentException("unknown policy " + ruleId);
        }
    }

    private static String encodeWeather(float rain, float thunder) {
        return Float.toString(rain) + "," + Float.toString(thunder);
    }
}
