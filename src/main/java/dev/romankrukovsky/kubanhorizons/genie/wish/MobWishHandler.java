package dev.romankrukovsky.kubanhorizons.genie.wish;

import dev.romankrukovsky.kubanhorizons.genie.entity.MobWishMemory;
import dev.romankrukovsky.kubanhorizons.genie.entity.MobWishMemory.MobWishRecord;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Диалоговый обработчик чтения и исполнения желаний мобов (корова, волк, голем, крипер).
 *
 * <p>Каждое исполненное желание записывается в {@link MobWishMemory}. У моба,
 * чьё желание уже исполнено, следующее обращение исполняет новый, более
 * требовательный квест: по три ступени на вид (корова: снег → золотое яблоко →
 * загон; волк: приручение → кость → страж; голем: цветок → слиток → исцеление;
 * крипер: спокойствие → салют → дар). Память переживает перезапуск мира.</p>
 */
public final class MobWishHandler {
    private MobWishHandler() {
    }

    public static boolean handleMobWish(ServerLevel level, Player player, LivingEntity target) {
        MobWishMemory memory = MobWishMemory.get(level);
        UUID mobId = target.getUUID();

        // Уже принятое, но ещё не закрытое желание повторно не исполняем.
        if (memory.pendingFor(mobId).isPresent()) {
            player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.mob_wish.pending"));
            return true;
        }

        int fulfilled = memory.fulfilledCountFor(mobId);
        String rewardKey = null;
        if (target.getType() == EntityTypes.COW) {
            rewardKey = grantCow(level, player, target, fulfilled);
        } else if (target.getType() == EntityTypes.WOLF && target instanceof TamableAnimal tamable) {
            rewardKey = grantWolf(level, player, tamable, fulfilled);
        } else if (target.getType() == EntityTypes.IRON_GOLEM) {
            rewardKey = grantGolem(level, player, target, fulfilled);
        } else if (target.getType() == EntityTypes.CREEPER && target instanceof Creeper creeper) {
            rewardKey = grantCreeper(level, player, creeper, fulfilled);
        }
        if (rewardKey == null) {
            return false;
        }

        String mobType = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
        String wishKey = wishKeyFor(target.getType(), rewardKey);
        Optional<MobWishRecord> previous = memory.lastFulfilledFor(mobId);
        long now = level.getGameTime();
        memory.record(new MobWishRecord(mobId, mobType, player.getUUID(), wishKey, now, 0L, false, rewardKey));
        memory.markFulfilled(mobId);
        // Ссылка на историю: у моба с прошлыми желаниями это продолжение квестовой линии.
        if (previous.isPresent()) {
            player.sendSystemMessage(Component.translatable(
                    "message.kubanhorizons.genie.mob_wish.follow_up",
                    Component.translatable(previous.get().wishText())));
        }
        return true;
    }

    private static String grantCow(ServerLevel level, Player player, LivingEntity cow, int tier) {
        switch (tier) {
            case 0 -> {
                level.sendParticles(ParticleTypes.SNOWFLAKE, cow.getX(), cow.getY() + 1.0D, cow.getZ(),
                        40, 0.6D, 0.6D, 0.6D, 0.05D);
                level.setBlockAndUpdate(cow.blockPosition(), Blocks.SNOW.defaultBlockState());
                player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.mob_wish.cow"));
                return "snow";
            }
            case 1 -> {
                cow.spawnAtLocation(level, new ItemStack(Items.GOLDEN_APPLE));
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, cow.getX(), cow.getY() + 1.2D, cow.getZ(),
                        20, 0.5D, 0.5D, 0.5D, 0.02D);
                player.sendSystemMessage(Component.translatable(
                        "message.kubanhorizons.genie.mob_wish.cow.golden_apple"));
                return "golden_apple";
            }
            default -> {
                // Загончик: обводим корову оградой, не трогая занятые блоки.
                BlockPos center = cow.blockPosition();
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                            BlockPos fencePos = center.offset(dx, 0, dz);
                            if (level.getBlockState(fencePos).isAir()) {
                                level.setBlockAndUpdate(fencePos, Blocks.OAK_FENCE.defaultBlockState());
                            }
                        }
                    }
                }
                player.sendSystemMessage(Component.translatable(
                        "message.kubanhorizons.genie.mob_wish.cow.pen"));
                return "pen";
            }
        }
    }

    private static String grantWolf(ServerLevel level, Player player, TamableAnimal wolf, int tier) {
        switch (tier) {
            case 0 -> {
                wolf.tame(player);
                level.sendParticles(ParticleTypes.HEART, wolf.getX(), wolf.getY() + 0.8D, wolf.getZ(),
                        10, 0.3D, 0.3D, 0.3D, 0.0D);
                player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.mob_wish.wolf"));
                return "tame";
            }
            case 1 -> {
                wolf.spawnAtLocation(level, new ItemStack(Items.BONE));
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, wolf.getX(), wolf.getY() + 0.8D, wolf.getZ(),
                        15, 0.4D, 0.4D, 0.4D, 0.02D);
                player.sendSystemMessage(Component.translatable(
                        "message.kubanhorizons.genie.mob_wish.wolf.bone"));
                return "bone";
            }
            default -> {
                wolf.setHealth(wolf.getMaxHealth());
                level.sendParticles(ParticleTypes.HEART, wolf.getX(), wolf.getY() + 0.8D, wolf.getZ(),
                        20, 0.5D, 0.5D, 0.5D, 0.05D);
                player.sendSystemMessage(Component.translatable(
                        "message.kubanhorizons.genie.mob_wish.wolf.guard"));
                return "guard";
            }
        }
    }

    private static String grantGolem(ServerLevel level, Player player, LivingEntity golem, int tier) {
        switch (tier) {
            case 0 -> {
                golem.spawnAtLocation(level, new ItemStack(Items.POPPY));
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, golem.getX(), golem.getY() + 2.0D, golem.getZ(),
                        20, 0.5D, 0.5D, 0.5D, 0.02D);
                player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.mob_wish.golem"));
                return "poppy";
            }
            case 1 -> {
                golem.spawnAtLocation(level, new ItemStack(Items.IRON_INGOT));
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, golem.getX(), golem.getY() + 2.0D, golem.getZ(),
                        20, 0.5D, 0.5D, 0.5D, 0.02D);
                player.sendSystemMessage(Component.translatable(
                        "message.kubanhorizons.genie.mob_wish.golem.iron"));
                return "iron";
            }
            default -> {
                golem.setHealth(golem.getMaxHealth());
                level.sendParticles(ParticleTypes.ENCHANT, golem.getX(), golem.getY() + 2.0D, golem.getZ(),
                        30, 0.5D, 0.6D, 0.5D, 0.3D);
                player.sendSystemMessage(Component.translatable(
                        "message.kubanhorizons.genie.mob_wish.golem.heal"));
                return "heal";
            }
        }
    }

    private static String grantCreeper(ServerLevel level, Player player, Creeper creeper, int tier) {
        switch (tier) {
            case 0 -> {
                creeper.setSwellDir(-1);
                level.sendParticles(ParticleTypes.FIREWORK, creeper.getX(), creeper.getY() + 1.2D, creeper.getZ(),
                        30, 0.4D, 0.5D, 0.4D, 0.1D);
                player.sendSystemMessage(Component.translatable("message.kubanhorizons.genie.mob_wish.creeper"));
                return "calm";
            }
            case 1 -> {
                creeper.spawnAtLocation(level, new ItemStack(Items.FIREWORK_ROCKET));
                level.sendParticles(ParticleTypes.FIREWORK, creeper.getX(), creeper.getY() + 1.2D, creeper.getZ(),
                        40, 0.4D, 0.6D, 0.4D, 0.15D);
                player.sendSystemMessage(Component.translatable(
                        "message.kubanhorizons.genie.mob_wish.creeper.firework"));
                return "firework";
            }
            default -> {
                creeper.setHealth(creeper.getMaxHealth());
                creeper.spawnAtLocation(level, new ItemStack(Items.GOLDEN_APPLE));
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, creeper.getX(), creeper.getY() + 1.2D, creeper.getZ(),
                        20, 0.4D, 0.5D, 0.4D, 0.05D);
                player.sendSystemMessage(Component.translatable(
                        "message.kubanhorizons.genie.mob_wish.creeper.gift"));
                return "gift";
            }
        }
    }

    private static String wishKeyFor(EntityType<?> type, String rewardKey) {
        if (type == EntityTypes.COW) {
            return switch (rewardKey) {
                case "snow" -> "message.kubanhorizons.genie.mob_wish.cow";
                case "golden_apple" -> "message.kubanhorizons.genie.mob_wish.cow.golden_apple";
                default -> "message.kubanhorizons.genie.mob_wish.cow.pen";
            };
        }
        if (type == EntityTypes.WOLF) {
            return switch (rewardKey) {
                case "tame" -> "message.kubanhorizons.genie.mob_wish.wolf";
                case "bone" -> "message.kubanhorizons.genie.mob_wish.wolf.bone";
                default -> "message.kubanhorizons.genie.mob_wish.wolf.guard";
            };
        }
        if (type == EntityTypes.IRON_GOLEM) {
            return switch (rewardKey) {
                case "poppy" -> "message.kubanhorizons.genie.mob_wish.golem";
                case "iron" -> "message.kubanhorizons.genie.mob_wish.golem.iron";
                default -> "message.kubanhorizons.genie.mob_wish.golem.heal";
            };
        }
        return switch (rewardKey) {
            case "calm" -> "message.kubanhorizons.genie.mob_wish.creeper";
            case "firework" -> "message.kubanhorizons.genie.mob_wish.creeper.firework";
            default -> "message.kubanhorizons.genie.mob_wish.creeper.gift";
        };
    }
}
