package genie.meta;

import genie.KubanGenie;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for instant smelting operations that can be enabled via policies.
 * Provides methods to instantly smelt items in player inventory or at specific locations.
 */
@Mod.EventBusSubscriber(modid = KubanGenie.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class InstantSmeltService {

    private static final Map<UUID, Long> lastSmeltTime = new HashMap<>();
    private static final long SMOKE_COOLDOWN = 20 * 5; // 5 seconds

    static {
        MinecraftForge.EVENT_BUS.register(InstantSmeltService.class);
    }

    /**
     * Check if instant smelt is enabled for a player
     */
    public static boolean isInstantSmeltEnabled(ServerPlayer player) {
        return PolicyService.get(player.level()).isInstantSmeltEnabled(player.getUUID());
    }

    /**
     * Instantly smelt all smeltable items in player inventory
     */
    public static int instantSmeltInventory(ServerPlayer player) {
        if (!isInstantSmeltEnabled(player)) {
            return 0; // Instant smelt disabled
        }

        if (!canSmelt(player)) {
            return 0; // Cannot smelt right now
        }

        int smeltedCount = 0;
        Container inventory = player.getInventory();

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && isSmeltable(stack)) {
                ItemStack result = getSmeltResult(stack, player.level());
                if (!result.isEmpty()) {
                    inventory.setItem(i, result.copy());
                    smeltedCount++;
                }
            }
        }

        if (smeltedCount > 0) {
            recordSmelt(player);
            KubanGenie.LOGGER.info("Player {} instantly smelted {} items", player.getName().getString(), smeltedCount);
        }

        return smeltedCount;
    }

    /**
     * Instantly smelt items at a specific furnace location
     */
    public static boolean instantSmeltFurnace(ServerPlayer player, BlockPos pos) {
        if (!isInstantSmeltEnabled(player)) {
            return false;
        }

        Level level = player.level();
        if (!(level.getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity furnace)) {
            return false;
        }

        if (!canSmelt(player)) {
            return false;
        }

        ItemStack input = furnace.getItem(0);
        ItemStack fuel = furnace.getItem(1);

        if (input.isEmpty() || !isSmeltable(input)) {
            return false;
        }

        ItemStack result = getSmeltResult(input, level);
        if (result.isEmpty()) {
            return false;
        }

        // Instant smelt
        furnace.setItem(0, ItemStack.EMPTY);
        furnace.setItem(2, result.copy());
        furnace.setRecipeUsed(level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, furnace, level).orElse(null));

        recordSmelt(player);
        return true;
    }

    /**
     * Check if an item can be smelted
     */
    private static boolean isSmeltable(ItemStack stack) {
        return !getSmeltResult(stack, null).isEmpty();
    }

    /**
     * Get smelting result for an item
     */
    private static ItemStack getSmeltResult(ItemStack stack, Level level) {
        if (level != null) {
            var recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SimpleContainer(stack), level);
            if (recipe.isPresent()) {
                return recipe.get().assemble(new SimpleContainer(stack), level.registryAccess());
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Check if player can smelt (cooldown check)
     */
    private static boolean canSmelt(ServerPlayer player) {
        UUID playerId = player.getUUID();
        Long lastTime = lastSmeltTime.get(playerId);

        if (lastTime == null || System.currentTimeMillis() - lastTime > SMOKE_COOLDOWN) {
            return true;
        }

        return false;
    }

    /**
     * Record a smelt operation
     */
    private static void recordSmelt(ServerPlayer player) {
        lastSmeltTime.put(player.getUUID(), System.currentTimeMillis());
    }

    /**
     * Enable instant smelt for a player
     */
    public static void enableInstantSmelt(ServerPlayer player) {
        PolicyService.get(player.level()).setInstantSmeltPolicy(player.getUUID(), true);
        KubanGenie.LOGGER.info("Enabled instant smelt for player {}", player.getName().getString());
    }

    /**
     * Disable instant smelt for a player
     */
    public static void disableInstantSmelt(ServerPlayer player) {
        PolicyService.get(player.level()).setInstantSmeltPolicy(player.getUUID(), false);
        KubanGenie.LOGGER.info("Disabled instant smelt for player {}", player.getName().getString());
    }

    /**
     * Toggle instant smelt for a player
     */
    public static void toggleInstantSmelt(ServerPlayer player) {
        boolean currentlyEnabled = isInstantSmeltEnabled(player);
        PolicyService.get(player.level()).setInstantSmeltPolicy(player.getUUID(), !currentlyEnabled);

        String status = currentlyEnabled ? "disabled" : "enabled";
        KubanGenie.LOGGER.info("Toggled instant smelt {} for player {}", status, player.getName().getString());
    }

    // Event handlers

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Auto-enable instant smelt for players with appropriate permissions
            if (player.hasPermissions(2)) {
                enableInstantSmelt(player);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        // Cleanup on logout
        if (event.getEntity() instanceof ServerPlayer player) {
            lastSmeltTime.remove(player.getUUID());
        }
    }
}