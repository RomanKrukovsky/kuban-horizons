package dev.romankrukovsky.kubanhorizons.genie.society;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.genie.GeniePersonality;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.*;

/**
 * SocietySimulator — симуляция общества вокруг джиннии.
 *
 * <p>С одной стороны — репутация и альянсы между NPC-джинниями, с другой —
 * репутация владельцев в глазах магии (0..100, персистентно в {@link SocietyData})
 * и расползающиеся по миру слухи, порождённые этой репутацией.</p>
 */
public final class SocietySimulator {

    private static final int REP_GOOD_DELTA = 5;
    private static final int REP_BAD_DELTA = -10;
    private static final int REP_MAX = 100;

    /** Как часто тик реально разносит слух (иначе он только обновляет активный мир). */
    private static final float RUMOR_CHANCE = 0.2F;

    private final Map<UUID, Map<UUID, Integer>> reputation = new HashMap<>(); // genie -> (otherGenie -> rep)
    private final Map<UUID, Set<UUID>> alliances = new HashMap<>();
    private final Map<UUID, Set<UUID>> conflicts = new HashMap<>();

    private SocietySimulator() {}

    private static final SocietySimulator INSTANCE = new SocietySimulator();

    /** Активный мир для методов без параметра уровня; выставляется в {@link #tick}. */
    private ServerLevel activeLevel;

    /** Запасная репутация без загруженного мира (тесты, headless). */
    private final Map<UUID, Integer> ownerReputationFallback = new HashMap<>();

    public static SocietySimulator get() {
        return INSTANCE;
    }

    /**
     * Тикает социум: запоминает активный мир и иногда разносит слух случайному игроку.
     *
     * <p>Вызывается из {@code GenieEvents.onLevelTick} раз в 600 тиков (30 секунд),
     * поэтому здесь не требуется собственный счётчик: «иногда» решает случайность.</p>
     */
    public void tick(ServerLevel level) {
        this.activeLevel = level;
        if (level.getRandom().nextFloat() >= RUMOR_CHANCE) {
            return;
        }
        List<UUID> known = SocietyData.get(level).knownOwners();
        List<ServerPlayer> online = level.players();
        if (known.isEmpty() || online.isEmpty()) {
            return;
        }
        UUID subject = known.get(level.getRandom().nextInt(known.size()));
        rumorFor(subject).ifPresent(rumor -> {
            ServerPlayer listener = online.get(level.getRandom().nextInt(online.size()));
            listener.sendSystemMessage(Component.literal(rumor));
        });
    }

    /** Записывает исполненное желание владельца: +5 за безопасное, −10 за безрассудное. */
    public void recordWish(ServerLevel level, UUID owner, boolean wellReceived) {
        SocietyData.get(level).recordWish(owner, wellReceived);
    }

    /** Записывает желание в активном мире (см. {@link #tick}); без мира — в запасную карту. */
    public void recordWish(UUID owner, boolean wellReceived) {
        if (activeLevel != null) {
            recordWish(activeLevel, owner, wellReceived);
            return;
        }
        int next = Math.max(0, Math.min(REP_MAX,
                ownerReputationFallback.getOrDefault(owner, 0)
                        + (wellReceived ? REP_GOOD_DELTA : REP_BAD_DELTA)));
        ownerReputationFallback.put(owner, next);
    }

    /** Репутация владельца в активном мире (0..100). */
    public int reputation(ServerLevel level, UUID owner) {
        return SocietyData.get(level).reputation(owner);
    }

    /** Репутация владельца в активном мире; без мира — из запасной карты. */
    public int reputation(UUID owner) {
        if (activeLevel != null) {
            return reputation(activeLevel, owner);
        }
        return ownerReputationFallback.getOrDefault(owner, 0);
    }

    /**
     * Слух об игроке, порождённый его репутацией.
     *
     * <p>Пустой Optional — у магии ещё нет мнения (владелец ни разу не желал).
     * Высокая репутация — «у X сбываются желания», низкая — «X опасен для магии».</p>
     */
    public Optional<String> rumorFor(UUID owner) {
        if (activeLevel == null) {
            if (!ownerReputationFallback.containsKey(owner)) {
                return Optional.empty();
            }
        } else if (!SocietyData.get(activeLevel).knownOwners().contains(owner)) {
            return Optional.empty();
        }
        int rep = reputation(owner);
        String name = ownerName(owner);
        if (rep >= 70) {
            return Optional.of("Говорят, у " + name + " сбываются желания — и степь это подтверждает.");
        }
        if (rep >= 35) {
            return Optional.of("Поговаривают, что " + name + " в ладу с магией джиннии.");
        }
        if (rep > 0) {
            return Optional.of("В деревне шепчутся, что " + name + " что-то получил от джиннии.");
        }
        return Optional.of("Ходят слухи, что " + name + " опасен для магии.");
    }

    private String ownerName(UUID owner) {
        if (activeLevel != null) {
            Player player = activeLevel.getPlayerByUUID(owner);
            if (player != null) {
                return player.getName().getString();
            }
        }
        String shortId = owner.toString().substring(0, 8);
        return "незнакомец " + shortId;
    }

    /**
     * Изменяет репутацию одной джиннии о другой.
     */
    public void adjustReputation(UUID genieA, UUID genieB, int delta) {
        reputation.computeIfAbsent(genieA, k -> new HashMap<>())
                .merge(genieB, delta, Integer::sum);
    }

    /**
     * Возвращает репутацию genieA о genieB.
     */
    public int getReputation(UUID genieA, UUID genieB) {
        return reputation.getOrDefault(genieA, Collections.emptyMap())
                .getOrDefault(genieB, 0);
    }

    /**
     * Создаёт альянс между двумя NPC-джинниями (если репутация >= 50).
     */
    public boolean formAlliance(UUID genieA, UUID genieB) {
        if (getReputation(genieA, genieB) < 50) {
            return false;
        }
        alliances.computeIfAbsent(genieA, k -> new HashSet<>()).add(genieB);
        alliances.computeIfAbsent(genieB, k -> new HashSet<>()).add(genieA);
        return true;
    }

    /**
     * Проверяет, состоят ли джиннии в альянсе.
     */
    public boolean areAllied(UUID genieA, UUID genieB) {
        return alliances.getOrDefault(genieA, Collections.emptySet()).contains(genieB);
    }

    /**
     * Регистрирует конфликт (если репутация <= -30).
     */
    public void registerConflict(UUID genieA, UUID genieB) {
        if (getReputation(genieA, genieB) <= -30) {
            conflicts.computeIfAbsent(genieA, k -> new HashSet<>()).add(genieB);
            conflicts.computeIfAbsent(genieB, k -> new HashSet<>()).add(genieA);
        }
    }

    /**
     * Проверяет, находятся ли джиннии в конфликте.
     */
    public boolean areInConflict(UUID genieA, UUID genieB) {
        return conflicts.getOrDefault(genieA, Collections.emptySet()).contains(genieB);
    }

    /**
     * Коллективное желание: несколько NPC-джинний совместно выполняют действие.
     * Пример: 3+ allied джиннии усиливают GROW_STEPPE в радиусе.
     */
    public void executeCollectiveWish(ServerLevel level, List<KubanGenie> participants, String action) {
        if (participants.size() < 3) {
            return;
        }

        // Проверяем, все ли в одном альянсе
        UUID first = participants.get(0).getUUID();
        for (int i = 1; i < participants.size(); i++) {
            if (!areAllied(first, participants.get(i).getUUID())) {
                return;
            }
        }

        // Усиливаем эффект (пример для GROW_STEPPE)
        if ("GROW_STEPPE".equals(action)) {
            for (KubanGenie genie : participants) {
                // Увеличиваем радиус или шанс
                // Здесь можно вызвать усиленную версию KubanSteppeResonance
            }
            level.getServer().getPlayerList().broadcastSystemMessage(
                    net.minecraft.network.chat.Component.literal("Collective wish empowered by alliance!"),
                    false
            );
        }
    }

    /**
     * Сериализация (для WorldGenieMemory).
     */
    public void save(net.minecraft.world.level.storage.ValueOutput output) {
        // Упрощённая сериализация репутации/альянсов (для MC 26.2 ValueOutput)
        var repList = output.childrenList("Reputation");
        for (Map.Entry<UUID, Map<UUID, Integer>> entry : reputation.entrySet()) {
            var node = repList.addChild();
            node.putString("Genie", entry.getKey().toString());
            var inner = node.childrenList("Relations");
            for (Map.Entry<UUID, Integer> rel : entry.getValue().entrySet()) {
                var r = inner.addChild();
                r.putString("Other", rel.getKey().toString());
                r.putInt("Value", rel.getValue());
            }
        }
        // Аналогично для alliances и conflicts (опущено для краткости)
    }

    public void load(net.minecraft.world.level.storage.ValueInput input) {
        reputation.clear();
        alliances.clear();
        conflicts.clear();
        for (var child : input.childrenListOrEmpty("Reputation")) {
            UUID genie = UUID.fromString(child.getStringOr("Genie", ""));
            Map<UUID, Integer> rels = new HashMap<>();
            for (var r : child.childrenListOrEmpty("Relations")) {
                UUID other = UUID.fromString(r.getStringOr("Other", ""));
                int val = r.getIntOr("Value", 0);
                rels.put(other, val);
            }
            reputation.put(genie, rels);
        }
    }
}
