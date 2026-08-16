package genie.vessel;

import genie.genie.KubanGenie;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Defines what happens to a vessel and its contained genie when the owner dies.
 * Four possible options for handling owner death.
 */
public enum OwnerDeathProtocol {

    /**
     * Genie is released and becomes wild.
     * Vessel becomes inactive but can be reclaimed.
     */
    RELEASE_GENIE {
        @Override
        public void handleOwnerDeath(Player owner, KubanGenie genie, VesselTracker tracker, UUID vesselId) {
            if (genie != null) {
                genie.setWild(true);
                genie.setOwner(null);
                owner.sendSystemMessage(Component.translatable("message.kuban_horizon.genie_released_on_death"));
            }
        }
    },

    /**
     * Genie is destroyed along with the vessel.
     * Both are permanently removed from the world.
     */
    DESTROY_VESSEL {
        @Override
        public void handleOwnerDeath(Player owner, KubanGenie genie, VesselTracker tracker, UUID vesselId) {
            if (genie != null) {
                genie.remove();
            }
            tracker.removeVessel(vesselId);
            owner.sendSystemMessage(Component.translatable("message.kuban_horizon.vessel_destroyed_on_death"));
        }
    },

    /**
     * Genie is transferred to a random nearby player.
     */
    TRANSFER_RANDOM {
        @Override
        public void handleOwnerDeath(Player owner, KubanGenie genie, VesselTracker tracker, UUID vesselId) {
            if (genie != null) {
                // Find nearby players
                java.util.List<Player> nearbyPlayers = owner.level().players().stream()
                    .filter(p -> p.distanceToSqr(owner) < 100) // Within 10 blocks
                    .toList();

                if (!nearbyPlayers.isEmpty()) {
                    Player newOwner = nearbyPlayers.get(0);
                    genie.setOwner(newOwner);
                    tracker.setVesselOwner(vesselId, newOwner.getUUID());
                    newOwner.sendSystemMessage(Component.translatable("message.kuban_horizon.genie_transferred_to", owner.getName()));
                    owner.sendSystemMessage(Component.translatable("message.kuban_horizon.genie_transferred_on_death", newOwner.getName()));
                } else {
                    // No nearby players, release genie
                    genie.setWild(true);
                    genie.setOwner(null);
                    owner.sendSystemMessage(Component.translatable("message.kuban_horizon.genie_released_no_owner"));
                }
            }
        }
    },

    /**
     * Genie becomes wild and vessel becomes a normal block.
     * Vessel can be broken and looted.
     */
    BECOME_WILD {
        @Override
        public void handleOwnerDeath(Player owner, KubanGenie genie, VesselTracker tracker, UUID vesselId) {
            if (genie != null) {
                genie.setWild(true);
                genie.setOwner(null);
            }
            owner.sendSystemMessage(Component.translatable("message.kuban_horizon.genie_wild_on_death"));
        }
    };

    /**
     * Handle owner death according to this protocol
     */
    public abstract void handleOwnerDeath(Player owner, @Nullable KubanGenie genie, VesselTracker tracker, UUID vesselId);

    /**
     * Get the protocol from its name
     */
    @Nullable
    public static OwnerDeathProtocol fromName(String name) {
        try {
            return OwnerDeathProtocol.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Get the default protocol
     */
    public static OwnerDeathProtocol getDefault() {
        return RELEASE_GENIE;
    }

    /**
     * Save protocol to string
     */
    public String save() {
        return this.name();
    }

    /**
     * Load protocol from string
     */
    @Nullable
    public static OwnerDeathProtocol load(String name) {
        return fromName(name);
    }
}
