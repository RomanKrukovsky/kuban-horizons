package com.worldgenie.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.AttachmentType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * WorldGenieMemory - Persistent attachment storing wish history, contracts,
 * temperament drift, and role swap events for MC 1.20.6 (MC 26.2).
 *
 * Uses AttachmentType with Codec serialization (old API, pre-DataComponentType).
 */
public record WorldGenieMemory(
        List<Wish> wishHistory,
        List<Contract> contracts,
        List<TemperamentDrift> temperamentDrift,
        List<RoleSwapEvent> roleSwapEvents,
        CausalLedger causalLedger
) {

    public static final String MODID = "worldgenie";

    // Old ResourceLocation constructor (NOT the static method)
    public static final ResourceLocation ID =
            new ResourceLocation(MODID, "world_genie_memory");

    // Codec for the attachment (used by AttachmentType)
    public static final Codec<WorldGenieMemory> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Wish.CODEC.listOf().fieldOf("wish_history")
                            .forGetter(WorldGenieMemory::wishHistory),
                    Contract.CODEC.listOf().fieldOf("contracts")
                            .forGetter(WorldGenieMemory::contracts),
                    TemperamentDrift.CODEC.listOf().fieldOf("temperament_drift")
                            .forGetter(WorldGenieMemory::temperamentDrift),
                    RoleSwapEvent.CODEC.listOf().fieldOf("role_swap_events")
                            .forGetter(WorldGenieMemory::roleSwapEvents),
                    CausalLedger.CODEC.fieldOf("causal_ledger")
                            .forGetter(WorldGenieMemory::causalLedger)
            ).apply(instance, WorldGenieMemory::new)
    );

    // DeferredRegister for AttachmentType (old Forge API)
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);

    public static final RegistryObject<AttachmentType<WorldGenieMemory>> WORLD_GENIE_MEMORY =
            ATTACHMENT_TYPES.register("world_genie_memory", () ->
                    AttachmentType.builder(() -> new WorldGenieMemory(
                                    new ArrayList<>(),
                                    new ArrayList<>(),
                                    new ArrayList<>(),
                                    new ArrayList<>(),
                                    new CausalLedger(new ArrayList<>())
                            ))
                            .serialize(CODEC)
                            .copyOnDeath() // Persists through player death
                            .build()
            );

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    // ========== Core Methods ==========

    /**
     * Creates a new memory instance with an initial wish recorded.
     */
    public WorldGenieMemory withNewWish(Wish wish, CompoundTag beforeState, CompoundTag afterState) {
        List<Wish> newHistory = new ArrayList<>(this.wishHistory);
        newHistory.add(wish);

        CausalEntry entry = new CausalEntry(
                System.currentTimeMillis(),
                wish.id(),
                beforeState,
                afterState,
                RollbackChoice.NONE
        );

        CausalLedger newLedger = this.causalLedger.withEntry(entry);

        return new WorldGenieMemory(
                newHistory,
                this.contracts,
                this.temperamentDrift,
                this.roleSwapEvents,
                newLedger
        );
    }

    /**
     * Records a contract signing.
     */
    public WorldGenieMemory withContract(Contract contract) {
        List<Contract> newContracts = new ArrayList<>(this.contracts);
        newContracts.add(contract);
        return new WorldGenieMemory(
                this.wishHistory,
                newContracts,
                this.temperamentDrift,
                this.roleSwapEvents,
                this.causalLedger
        );
    }

    /**
     * Records a temperament drift event.
     */
    public WorldGenieMemory withTemperamentDrift(TemperamentDrift drift) {
        List<TemperamentDrift> newDrift = new ArrayList<>(this.temperamentDrift);
        newDrift.add(drift);
        return new WorldGenieMemory(
                this.wishHistory,
                this.contracts,
                newDrift,
                this.roleSwapEvents,
                this.causalLedger
        );
    }

    /**
     * Records a role swap event.
     */
    public WorldGenieMemory withRoleSwap(RoleSwapEvent event) {
        List<RoleSwapEvent> newSwaps = new ArrayList<>(this.roleSwapEvents);
        newSwaps.add(event);
        return new WorldGenieMemory(
                this.wishHistory,
                this.contracts,
                this.temperamentDrift,
                newSwaps,
                this.causalLedger
        );
    }

    /**
     * Performs rollback of the last wish using CausalLedger.
     * This is the implementation for ROLLBACK_LAST_WISH choice.
     */
    public Optional<WorldGenieMemory> rollbackLastWish() {
        Optional<CausalEntry> lastEntryOpt = this.causalLedger.getLastNonRolledBackEntry();

        if (lastEntryOpt.isEmpty()) {
            return Optional.empty();
        }

        CausalEntry lastEntry = lastEntryOpt.get();

        // Mark the entry as rolled back
        CausalLedger rolledBackLedger = this.causalLedger.markAsRolledBack(lastEntry.id());

        // Remove the last wish from history
        if (this.wishHistory.isEmpty()) {
            return Optional.empty();
        }

        List<Wish> newHistory = new ArrayList<>(this.wishHistory);
        Wish lastWish = newHistory.remove(newHistory.size() - 1);

        // Verify the wish ID matches
        if (!lastWish.id().equals(lastEntry.wishId())) {
            // Mismatch - do not rollback
            return Optional.empty();
        }

        return Optional.of(new WorldGenieMemory(
                newHistory,
                this.contracts,
                this.temperamentDrift,
                this.roleSwapEvents,
                rolledBackLedger
        ));
    }

    /**
     * Returns the most recent wish, if any.
     */
    public Optional<Wish> getLastWish() {
        if (wishHistory.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(wishHistory.get(wishHistory.size() - 1));
    }

    /**
     * Returns total number of wishes made.
     */
    public int getWishCount() {
        return wishHistory.size();
    }

    /**
     * Checks if a specific wish was rolled back.
     */
    public boolean wasWishRolledBack(UUID wishId) {
        return causalLedger.isWishRolledBack(wishId);
    }
}
