package dev.romankrukovsky.kubanhorizons.genie.wish;

import dev.romankrukovsky.kubanhorizons.genie.memory.ProvenanceJournal;
import dev.romankrukovsky.kubanhorizons.genie.memory.UnfulfilledWishRoom;
import dev.romankrukovsky.kubanhorizons.genie.memory.WorldGenieMemory;
import dev.romankrukovsky.kubanhorizons.genie.history.AlternativeCausalityEngine;
import dev.romankrukovsky.kubanhorizons.genie.social.GenieMythSystem;
import dev.romankrukovsky.kubanhorizons.genie.society.SocietySimulator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.Optional;

/** Единый серверный исполнитель всех категорий желаний. */
public final class WishExecutor {
    private WishExecutor() {
    }

    public static Result execute(ServerLevel level, Player player, WishIntent intent) {
        // LLM_DELEGATED wishes are handled directly without recording to memory
        if (intent.target() == WishIntent.Target.LLM_DELEGATED) {
            return LLMWishExecutor.execute(level, player, intent.detailParam());
        }

        Result result = switch (intent.category()) {
            case META_RULE -> MetaRuleEngine.execute(level, player, intent);
            case GIGANTISM -> GigantismScaleEngine.execute(level, player, intent);
            case MATERIAL -> switch (intent.target()) {
                case WORD_MATERIALIZATION -> executeWordMaterialization(level, player, intent);
                case DRAWING -> executeDrawing(level, player);
                case MAGIC_PHOTO -> executeMagicPhoto(level, player);
                default -> executeMaterialWish(level, player, intent);
            };
            case CIVILIZATION -> switch (intent.target()) {
                case BIOME_REWRITE -> executeBiomeRewrite(level, player);
                case NPC_PERSONALITY -> executeNpcPersonality(level, player, intent);
                case LIVING_PAINTING -> executeLivingPainting(level, player);
                case FLYING_HOUSE -> GeneralWishEngine.execute(level, player, intent.detailParam());
                case MAGIC_DOPPELGANGER -> executeDoppelganger(level, player);
                case MATERIALIZE_BRIDGE -> executeBridge(level, player);
                case RAISE_GROUND -> executeRaiseGround(level, player);
                case TEMP_ARMY -> executeTempArmy(level, player);
                case CONTEXTUAL_DOOR -> executeContextualDoor(level, player);
                case UNSPOKEN_WISH -> executeUnspokenWish(level, player);
                case MAKE_CONTRACT -> executeContract(level, player);
                case WISH_CREATURE -> executeWishCreature(level, player, intent);
                case GENIE_TITLE -> executeGenieTitle(level, player);
                case GENIE_OWN_WISH -> executeGenieOwnWish(level, player, intent);
                case WISH_CHAIN -> executeWishChain(level, player, intent);
                default -> executeCivilizationWish(level, player, intent);
            };
            case PROVENANCE -> switch (intent.target()) {
                case BLOCK_WHISPER -> executeBlockWhisper(level, player, intent);
                case ITEM_MEMORY -> executeItemMemory(level, player, intent);
                default -> executeProvenanceQuery(level, player, intent);
            };
            case HISTORY -> switch (intent.target()) {
                case WHAT_IF -> executeWhatIf(level, player, intent);
                case THEATER_REENACTMENT -> executeTheater(level, player);
                default -> executeWhatIf(level, player, intent);
            };
            case MUSIC -> executeMusicSpell(level, player, intent);
            case DISTORTED_HIGHER_WISH -> (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
                    ? switch (intent.target()) {
                        case REALITY_ERROR -> executeRealityError(level, serverPlayer);
                        default -> DistortedWishEngine.execute(level, serverPlayer, intent);
                    }
                    : LLMWishExecutor.execute(level, player, intent.detailParam());
            default -> GeneralWishEngine.execute(level, player, intent.detailParam().isBlank() ? "general wish" : intent.detailParam());
        };
        if (result.executed()) {
            WorldGenieMemory.get(level).recordWish(player.blockPosition(), intent.target().name(),
                    intent.precision(), level.getGameTime());
            SocietySimulator.get().recordWish(level, player.getUUID(), intent.safe());
        }
        return result;
    }

    private static Result executeMaterialWish(ServerLevel level, Player player, WishIntent intent) {
        if (intent.isPreciseAndSafe()) {
            return placeDiamondChest(level, player)
                    ? new Result(true, "message.kubanhorizons.genie.wish.safe")
                    : new Result(false, "message.kubanhorizons.genie.wish.no_space");
        }

        BlockPos source = player.blockPosition().above(3);
        while (!level.isEmptyBlock(source) && source.getY() < level.getMaxY() - 1) {
            source = source.above();
        }
        if (!level.isEmptyBlock(source)) {
            return new Result(false, "message.kubanhorizons.genie.wish.no_space");
        }
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                FallingBlockEntity falling = FallingBlockEntity.fall(level, source.offset(x, 0, z),
                        Blocks.DIAMOND_ORE.defaultBlockState());
                falling.setHurtsEntities(8.0F, 40);
            }
        }
        level.sendParticles(ParticleTypes.PORTAL, source.getX() + 0.5D, source.getY() + 0.5D,
                source.getZ() + 0.5D, 36, 0.7D, 0.7D, 0.7D, 0.1D);
        return new Result(true, "message.kubanhorizons.genie.wish.literal");
    }

    private static Result executeCivilizationWish(ServerLevel level, Player player, WishIntent intent) {
        if (intent.target() == WishIntent.Target.GENIE_FESTIVAL) {
            return executeSocietyWish(level, player, intent);
        }
        BlockPos target = player.blockPosition().relative(player.getDirection(), 2);
        if (level.isEmptyBlock(target)) {
            level.setBlockAndUpdate(target, Blocks.CHEST.defaultBlockState());
            if (level.getBlockEntity(target) instanceof Container chest) {
                for (int i = 0; i < chest.getContainerSize(); i++) {
                    chest.setItem(i, new ItemStack(Items.EMERALD, 64));
                }
                chest.setChanged();
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX() + 0.5D, target.getY() + 0.5D,
                        target.getZ() + 0.5D, 40, 0.8D, 0.8D, 0.8D, 0.1D);
                memoryRecordVillage(level, target);
                return new Result(true, "message.kubanhorizons.genie.wish.village_wealth");
            }
        }
        return new Result(false, "message.kubanhorizons.genie.wish.no_space");
    }

    private static void memoryRecordVillage(ServerLevel level, BlockPos pos) {
        WorldGenieMemory memory = WorldGenieMemory.get(level);
        memory.recordVillageSaved(pos, level.getGameTime());
    }

    /** Социум: ежегодный праздник джиннии прямо у игрока. */
    private static Result executeSocietyWish(ServerLevel level, Player player, WishIntent intent) {
        GenieMythSystem.celebrateAnnualFestival(level, player.blockPosition());
        return new Result(true, "message.kubanhorizons.genie.wish.festival");
    }

    /**
     * Магическая музыка: «сыграй песню» исполняется как танец-заклинание.
     *
     * <p>Без названной песни звучит Песня дождя — первая и самая простая;
     * названные рост/покой/огонь дают свою песню. Каст идёт через
     * {@link DanceEngine}, чтобы эффект продолжался всю длительность песни.</p>
     */
    private static Result executeMusicSpell(ServerLevel level, Player player, WishIntent intent) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return new Result(false, "wish.kubanhorizons.music.rain");
        }
        dev.romankrukovsky.kubanhorizons.genie.music.MusicSpell spell = switch (intent.detailParam()) {
            case "GROWTH" -> dev.romankrukovsky.kubanhorizons.genie.music.MusicSpell.GROWTH_MELODY;
            case "PEACE" -> dev.romankrukovsky.kubanhorizons.genie.music.MusicSpell.PEACE_LULLABY;
            case "FIRE" -> dev.romankrukovsky.kubanhorizons.genie.music.MusicSpell.DANCE_OF_FIRE;
            default -> dev.romankrukovsky.kubanhorizons.genie.music.MusicSpell.RAIN_SONG;
        };
        dev.romankrukovsky.kubanhorizons.genie.music.DanceEngine.cast(level, serverPlayer, spell);
        String messageKey = switch (spell) {
            case RAIN_SONG -> "wish.kubanhorizons.music.rain";
            case GROWTH_MELODY -> "wish.kubanhorizons.music.growth";
            case PEACE_LULLABY -> "wish.kubanhorizons.music.peace";
            case DANCE_OF_FIRE -> "wish.kubanhorizons.music.fire";
        };
        return new Result(true, messageKey);
    }

    private static Result executeProvenanceQuery(ServerLevel level, Player player, WishIntent intent) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            player.sendSystemMessage(Component.translatable("wish.kubanhorizons.provenance.empty_hand"));
            return new Result(false, "wish.kubanhorizons.provenance.empty_hand");
        }
        String id = held.getItem().builtInRegistryHolder().unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse(held.getItem().toString());
        ProvenanceJournal journal = ProvenanceJournal.get(level);
        List<ProvenanceJournal.ProvenanceRecord> hits = journal.queryById(id);
        if (hits.isEmpty()) {
            player.sendSystemMessage(Component.translatable("wish.kubanhorizons.provenance.empty", id));
            return new Result(false, "wish.kubanhorizons.provenance.empty");
        }
        ProvenanceJournal.ProvenanceRecord last = hits.get(hits.size() - 1);
        player.sendSystemMessage(Component.translatable("wish.kubanhorizons.provenance.entry",
                id, last.action(), last.wishText()));
        return new Result(true, "wish.kubanhorizons.provenance.entry");
    }

    /** Память блоков: джинния переводит шёпот блока, на который смотрит игрок. */
    private static Result executeBlockWhisper(ServerLevel level, Player player, WishIntent intent) {
        var hit = player.pick(8.0D, 0.0F, false);
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK
                || !(hit instanceof net.minecraft.world.phys.BlockHitResult blockHit)) {
            player.sendSystemMessage(Component.translatable("wish.kubanhorizons.whisper.empty"));
            return new Result(false, "wish.kubanhorizons.whisper.empty");
        }
        boolean heard = dev.romankrukovsky.kubanhorizons.genie.memory.BlockWhispersEngine
                .listenToBlock(null, level, player, blockHit.getBlockPos());
        if (heard) {
            return new Result(true, "message.kubanhorizons.genie.whisper.bell");
        }
        player.sendSystemMessage(Component.translatable("wish.kubanhorizons.whisper.empty"));
        return new Result(false, "wish.kubanhorizons.whisper.empty");
    }

    /** Память предмета: джинния читает накопленные воспоминания предмета в руке. */
    private static Result executeItemMemory(ServerLevel level, Player player, WishIntent intent) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            player.sendSystemMessage(Component.translatable("wish.kubanhorizons.provenance.empty_hand"));
            return new Result(false, "wish.kubanhorizons.provenance.empty_hand");
        }
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return new Result(false, "wish.kubanhorizons.provenance.empty_hand");
        }
        dev.romankrukovsky.kubanhorizons.genie.memory.ItemMemoryReader
                .readItemMemory(null, serverPlayer, held);
        return new Result(true, "message.kubanhorizons.genie.memory.item_read");
    }

    /** История: «А что если?» — описательный отчёт об альтернативной версии мира. */
    private static Result executeWhatIf(ServerLevel level, Player player, WishIntent intent) {
        Optional<AlternativeCausalityEngine.WhatIfResult> alternative =
                AlternativeCausalityEngine.whatIf(level, player.getUUID(), intent.detailParam());
        if (alternative.isEmpty()) {
            player.sendSystemMessage(Component.translatable("wish.kubanhorizons.whatif.empty"));
            return new Result(false, "wish.kubanhorizons.whatif.empty");
        }
        AlternativeCausalityEngine.WhatIfResult result = alternative.get();
        player.sendSystemMessage(Component.translatable("wish.kubanhorizons.whatif.result",
                result.wishText(), result.actualOutcome(), result.changedBlocks(),
                result.alternativeOutcome()));
        return new Result(true, "wish.kubanhorizons.whatif.result");
    }

    private static Result executeTheater(ServerLevel level, Player player) {
        boolean shown = dev.romankrukovsky.kubanhorizons.genie.dimension.VisualReenactmentEngine
                .reenactPastEvent(level, player.blockPosition(), player);
        return shown
                ? new Result(true, "message.kubanhorizons.genie.theater_reenactment")
                : new Result(false, "message.kubanhorizons.genie.theater_empty");
    }

    /** Материализация слова: «напиши слово Х» — блочный шрифт в воздухе. */
    private static Result executeWordMaterialization(ServerLevel level, Player player, WishIntent intent) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return new Result(false, "message.kubanhorizons.genie.wish.no_space");
        }
        String word = extractWord(intent.detailParam());
        if (word.isBlank() || word.length() > 12) {
            player.sendSystemMessage(Component.translatable("wish.kubanhorizons.word.bad"));
            return new Result(false, "wish.kubanhorizons.word.bad");
        }
        try {
            var runtime = dev.romankrukovsky.kubanhorizons.genie.runtime.WishRuntime.get(level.getServer());
            if (!runtime.ready()) runtime.recover();
            var preview = runtime.previewWord(serverPlayer, word);
            var report = runtime.executeWord(serverPlayer,
                    runtime.confirmWord(player.getUUID(), preview));
            boolean ok = report.outcome()
                    == dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome.COMPLETED;
            return new Result(ok, ok ? "wish.kubanhorizons.word.written" : "message.kubanhorizons.genie.wish.no_space");
        } catch (java.io.IOException | RuntimeException exception) {
            return new Result(false, "message.kubanhorizons.genie.runtime.failed");
        }
    }

    /** Рисунок-линия: «нарисуй» — линия блоков по выделению (или перед игроком). */
    private static Result executeDrawing(ServerLevel level, Player player) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return new Result(false, "message.kubanhorizons.genie.wish.no_space");
        }
        try {
            var runtime = dev.romankrukovsky.kubanhorizons.genie.runtime.WishRuntime.get(level.getServer());
            if (!runtime.ready()) runtime.recover();
            var preview = runtime.previewSelectedDrawing(serverPlayer);
            var report = runtime.executeDrawing(serverPlayer,
                    runtime.confirmDrawing(player.getUUID(), preview));
            boolean ok = report.outcome()
                    == dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome.COMPLETED;
            return new Result(ok, ok ? "wish.kubanhorizons.drawing.drawn" : "message.kubanhorizons.genie.wish.no_space");
        } catch (java.io.IOException | RuntimeException exception) {
            return new Result(false, "message.kubanhorizons.genie.runtime.failed");
        }
    }

    /** Магическая фотография: джинния сохраняет вид сцены в фото-предмет. */
    private static Result executeMagicPhoto(ServerLevel level, Player player) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return new Result(false, "message.kubanhorizons.genie.wish.no_space");
        }
        ItemStack photo = dev.romankrukovsky.kubanhorizons.genie.memory.MagicPhotoEngine
                .capture(level, serverPlayer);
        ItemStack main = player.getMainHandItem();
        if (main.isEmpty()) {
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, photo);
        } else {
            if (!player.getInventory().add(photo)) {
                player.drop(photo, false);
            }
        }
        player.sendSystemMessage(Component.translatable("wish.kubanhorizons.photo.captured"));
        return new Result(true, "wish.kubanhorizons.photo.captured");
    }

    /** Переписывание биома: область вокруг игрока становится кубанской степью. */
    private static Result executeBiomeRewrite(ServerLevel level, Player player) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return new Result(false, "message.kubanhorizons.genie.wish.no_space");
        }
        try {
            var runtime = dev.romankrukovsky.kubanhorizons.genie.runtime.WishRuntime.get(level.getServer());
            if (!runtime.ready()) runtime.recover();
            var preview = runtime.previewBiomeRewrite(serverPlayer, player.blockPosition());
            var report = runtime.executeBiomeRewrite(serverPlayer,
                    runtime.confirmBiomeRewrite(player.getUUID(), preview));
            boolean ok = report.outcome()
                    == dev.romankrukovsky.kubanhorizons.genie.runtime.transaction.TransactionOutcome.COMPLETED;
            return new Result(ok, ok ? "wish.kubanhorizons.biome.rewritten" : "message.kubanhorizons.genie.wish.no_space");
        } catch (java.io.IOException | RuntimeException exception) {
            return new Result(false, "message.kubanhorizons.genie.runtime.failed");
        }
    }

    /** Склонности NPC: джинния меняет характер ближайшего моба. */
    private static Result executeNpcPersonality(ServerLevel level, Player player, WishIntent intent) {
        var mobs = level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                player.getBoundingBox().inflate(8.0D),
                e -> e != player && e.isAlive());
        if (mobs.isEmpty()) {
            player.sendSystemMessage(Component.translatable("wish.kubanhorizons.npc.none"));
            return new Result(false, "wish.kubanhorizons.npc.none");
        }
        String text = intent.detailParam() == null ? "" : intent.detailParam().toLowerCase(java.util.Locale.ROOT);
        String trait = text.contains("спокой") || text.contains("мирн") || text.contains("peace") || text.contains("calm")
                ? "calm" : "active";
        net.minecraft.world.entity.LivingEntity target = mobs.getFirst();
        dev.romankrukovsky.kubanhorizons.genie.entity.NPCPersonalityEngine
                .modifyPersonality(level, target, trait);
        player.sendSystemMessage(Component.translatable("wish.kubanhorizons.npc.modified", trait));
        return new Result(true, "wish.kubanhorizons.npc.modified");
    }

    /** Живая картина: вход в зеркальный мир (MIRROR_WORLD). */
    private static Result executeLivingPainting(ServerLevel level, Player player) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return new Result(false, "message.kubanhorizons.genie.wish.no_space");
        }
        var mirror = serverPlayer.level().getServer()
                .getLevel(dev.romankrukovsky.kubanhorizons.worldgen.dimension.KHMagicDimensions.MIRROR_WORLD);
        if (mirror == null) {
            player.sendSystemMessage(Component.translatable("wish.kubanhorizons.painting.missing"));
            return new Result(false, "wish.kubanhorizons.painting.missing");
        }
        boolean entered = dev.romankrukovsky.kubanhorizons.genie.dimension.LivingPaintingEngine
                .enterDimension(level, player.blockPosition(), serverPlayer,
                        dev.romankrukovsky.kubanhorizons.worldgen.dimension.KHMagicDimensions.MIRROR_WORLD,
                        new net.minecraft.world.phys.Vec3(0.5D, 64.0D, 0.5D));
        return entered
                ? new Result(true, "wish.kubanhorizons.painting.entered")
                : new Result(false, "wish.kubanhorizons.painting.missing");
    }

    /** Магический двойник: джинния создаёт копию игрока рядом. */
    private static Result executeDoppelganger(ServerLevel level, Player player) {
        var doppelganger = dev.romankrukovsky.kubanhorizons.registry.KHEntities.MAGIC_DOPPELGANGER
                .get().create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (doppelganger == null) {
            return new Result(false, "message.kubanhorizons.genie.wish.no_space");
        }
        BlockPos pos = player.blockPosition().relative(player.getDirection(), 3);
        doppelganger.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        level.addFreshEntity(doppelganger);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
                pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                40, 0.5D, 1.0D, 0.5D, 0.1D);
        player.sendSystemMessage(Component.translatable("wish.kubanhorizons.doppelganger.created"));
        return new Result(true, "wish.kubanhorizons.doppelganger.created");
    }

    /** Материализация намерения: джинния строит мост в направлении взгляда. */
    private static Result executeBridge(ServerLevel level, Player player) {
        int built = dev.romankrukovsky.kubanhorizons.genie.spatial.BridgeMaterializerEngine
                .buildBridge(level, player.blockPosition(), player.getDirection());
        if (built == 0) {
            player.sendSystemMessage(Component.translatable("wish.kubanhorizons.bridge.none"));
            return new Result(false, "wish.kubanhorizons.bridge.none");
        }
        player.sendSystemMessage(Component.translatable("wish.kubanhorizons.bridge.built", built));
        return new Result(true, "wish.kubanhorizons.bridge.built");
    }

    /** Материализация намерения: джинния поднимает земляную колонну. */
    private static Result executeRaiseGround(ServerLevel level, Player player) {
        int raised = dev.romankrukovsky.kubanhorizons.genie.spatial.BridgeMaterializerEngine
                .raiseGround(level, player.blockPosition(), player.getDirection());
        if (raised == 0) {
            player.sendSystemMessage(Component.translatable("wish.kubanhorizons.ground.none"));
            return new Result(false, "wish.kubanhorizons.ground.none");
        }
        player.sendSystemMessage(Component.translatable("wish.kubanhorizons.ground.raised", raised));
        return new Result(true, "wish.kubanhorizons.ground.raised");
    }

    /** Временная армия: джинния призывает големов-защитников. */
    private static Result executeTempArmy(ServerLevel level, Player player) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return new Result(false, "message.kubanhorizons.genie.wish.no_space");
        }
        int count = dev.romankrukovsky.kubanhorizons.genie.entity.TemporaryArmyEngine
                .summonArmy(level, serverPlayer);
        if (count == 0) {
            return new Result(false, "message.kubanhorizons.genie.wish.no_space");
        }
        player.sendSystemMessage(Component.translatable("wish.kubanhorizons.army.summoned", count));
        return new Result(true, "wish.kubanhorizons.army.summoned");
    }

    /** Ошибка Реальности: опасное желание призывает неуязвимый парадокс. */
    private static Result executeRealityError(ServerLevel level, net.minecraft.server.level.ServerPlayer player) {
        var error = dev.romankrukovsky.kubanhorizons.registry.KHEntities.REALITY_ERROR
                .get().create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (error == null) {
            return new Result(false, "message.kubanhorizons.genie.wish.no_space");
        }
        BlockPos pos = player.blockPosition().relative(player.getDirection(), 5);
        error.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        level.addFreshEntity(error);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                60, 1.0D, 2.0D, 1.0D, 0.15D);
        player.sendSystemMessage(Component.translatable("wish.kubanhorizons.reality_error.summoned"));
        return new Result(true, "wish.kubanhorizons.reality_error.summoned");
    }

    /** Дверь с контекстным выходом: джинния ставит дверь-портал перед игроком. */
    private static Result executeContextualDoor(ServerLevel level, Player player) {
        BlockPos pos = player.blockPosition().relative(player.getDirection(), 3);
        if (!level.isEmptyBlock(pos)) {
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.wish.no_space"));
            return new Result(false, "message.kubanhorizons.genie.wish.no_space");
        }
        level.setBlockAndUpdate(pos, dev.romankrukovsky.kubanhorizons.registry.KHBlocks.CONTEXTUAL_DOOR
                .get().defaultBlockState());
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
                pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                40, 0.4D, 0.8D, 0.4D, 0.1D);
        player.sendSystemMessage(Component.translatable("wish.kubanhorizons.door.created"));
        return new Result(true, "wish.kubanhorizons.door.created");
    }

    /** Невысказанное желание: джинния угадывает намерение по контексту. */
    private static Result executeUnspokenWish(ServerLevel level, Player player) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return new Result(false, "wish.kubanhorizons.unspoken.none");
        }
        boolean guessed = dev.romankrukovsky.kubanhorizons.genie.wish.UnspokenWishEngine
                .guess(level, serverPlayer);
        if (!guessed) {
            player.sendSystemMessage(Component.translatable("wish.kubanhorizons.unspoken.none"));
            return new Result(false, "wish.kubanhorizons.unspoken.none");
        }
        return new Result(true, "wish.kubanhorizons.unspoken.guessed");
    }

    /** Контракт: джинния предлагает договор между игроком и собой. */
    private static Result executeContract(ServerLevel level, Player player) {
        var engine = dev.romankrukovsky.kubanhorizons.genie.memory.ContractEngine.get(level);
        try {
            java.util.Set<java.util.UUID> parties = java.util.Set.of(
                    player.getUUID(),
                    java.util.UUID.nameUUIDFromBytes("kuban_genie".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            var contract = engine.proposeContract(parties,
                    java.util.List.of("играть честно", "не злоупотреблять желаниями"), 12000L);
            engine.acceptContract(contract.contractId(), player.getUUID());
            player.sendSystemMessage(Component.translatable("wish.kubanhorizons.contract.made",
                    contract.contractId().toString().substring(0, 8)));
            return new Result(true, "wish.kubanhorizons.contract.made");
        } catch (RuntimeException exception) {
            return new Result(false, "wish.kubanhorizons.contract.failed");
        }
    }

    /** Желание, ставшее существом: материализация сути желания в спутника. */
    private static Result executeWishCreature(ServerLevel level, Player player, WishIntent intent) {
        var creature = dev.romankrukovsky.kubanhorizons.registry.KHEntities.WISH_CREATURE
                .get().create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (creature == null) {
            return new Result(false, "message.kubanhorizons.genie.wish.no_space");
        }
        String wish = intent.detailParam() == null ? "" : intent.detailParam().trim();
        BlockPos pos = player.blockPosition().relative(player.getDirection(), 2);
        creature.setPos(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
        creature.setWish(wish, player.getUUID());
        level.addFreshEntity(creature);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                40, 0.5D, 0.8D, 0.5D, 0.1D);
        player.sendSystemMessage(Component.translatable("wish.kubanhorizons.creature.made",
                wish.isBlank() ? "?" : wish));
        return new Result(true, "wish.kubanhorizons.creature.made");
    }

    /** Титул мира: джинния называет свой титул по делам в этом мире. */
    private static Result executeGenieTitle(ServerLevel level, Player player) {
        String titleKey = dev.romankrukovsky.kubanhorizons.genie.GenieTitleSystem.titleKey(level);
        player.sendSystemMessage(Component.translatable("wish.kubanhorizons.title.announces",
                Component.translatable(titleKey)));
        return new Result(true, "wish.kubanhorizons.title.announces");
    }

    /** Самостоятельное желание джиннии: она дарит маленький подарок по своему выбору. */
    private static Result executeGenieOwnWish(ServerLevel level, Player player, WishIntent intent) {
        boolean polite = intent.polite();
        ItemStack gift;
        if (polite) {
            gift = new ItemStack(net.minecraft.world.item.Items.GOLDEN_CARROT, 4);
        } else if (level.getRandom().nextBoolean()) {
            gift = new ItemStack(net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE, 1);
        } else {
            gift = new ItemStack(net.minecraft.world.item.Items.EMERALD, 8);
        }
        if (!player.getInventory().add(gift)) {
            player.drop(gift, false);
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                player.getX(), player.getY() + 1.5D, player.getZ(),
                30, 0.5D, 0.6D, 0.5D, 0.05D);
        player.sendSystemMessage(Component.translatable("wish.kubanhorizons.genie_own.gift"));
        return new Result(true, "wish.kubanhorizons.genie_own.gift");
    }

    /** Цепное желание: исполняет одно и то же желание несколько раз подряд. */
    private static Result executeWishChain(ServerLevel level, Player player, WishIntent intent) {
        int repeats = 3;
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(\\d+)\\s*(раз|раза|times)").matcher(
                        intent.detailParam() == null ? "" : intent.detailParam());
        if (matcher.find()) {
            repeats = Math.min(10, Math.max(1, Integer.parseInt(matcher.group(1))));
        }
        // Цепь из простых исполнений: каждый повтор материализует слиток.
        int given = 0;
        for (int i = 0; i < repeats; i++) {
            if (player.getInventory().add(new ItemStack(net.minecraft.world.item.Items.GOLD_INGOT, 1))) {
                given++;
            }
        }
        if (given == 0) {
            return new Result(false, "wish.kubanhorizons.chain.none");
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                player.getX(), player.getY() + 1.5D, player.getZ(),
                20 * given, 0.5D, 0.6D, 0.5D, 0.08D);
        player.sendSystemMessage(Component.translatable("wish.kubanhorizons.chain.done", given));
        return new Result(true, "wish.kubanhorizons.chain.done");
    }

    /** Достаёт слово из «напиши слово X»: берёт первый подряд латиницей/кириллицей токен после «слово». */
    private static String extractWord(String detailParam) {
        String text = detailParam == null ? "" : detailParam.toLowerCase(java.util.Locale.ROOT);
        int marker = text.indexOf("слово");
        String after = marker >= 0 ? text.substring(marker + 5) : text;
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("[а-яёa-z]{1,12}").matcher(after);
        return matcher.find() ? matcher.group() : "";
    }

    private static boolean placeDiamondChest(ServerLevel level, Player player) {
        Direction facing = player.getDirection();
        BlockPos origin = player.blockPosition().relative(facing, 2);
        BlockPos target = origin;
        for (int dy = 0; dy <= 2; dy++) {
            BlockPos candidate = origin.above(dy);
            if (level.isEmptyBlock(candidate) && level.getBlockState(candidate.below()).isSolid()) {
                target = candidate;
                break;
            }
        }
        if (!level.isEmptyBlock(target) || !level.getBlockState(target.below()).isSolid()) {
            return false;
        }

        level.setBlockAndUpdate(target, Blocks.CHEST.defaultBlockState());
        if (!(level.getBlockEntity(target) instanceof Container chest)) {
            return false;
        }
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            chest.setItem(slot, new ItemStack(Items.DIAMOND, 64));
        }
        chest.setChanged();
        level.sendParticles(ParticleTypes.ENCHANT, target.getX() + 0.5D, target.getY() + 0.8D,
                target.getZ() + 0.5D, 48, 0.8D, 0.6D, 0.8D, 0.1D);
        return true;
    }

    public record Result(boolean executed, String messageKey) {
        public Component message(int precision) {
            return Component.translatable(messageKey, precision);
        }
    }
}
