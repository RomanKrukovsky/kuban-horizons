package dev.romankrukovsky.kubanhorizons.genie.vessel;

import dev.romankrukovsky.kubanhorizons.entity.KubanGenie;
import dev.romankrukovsky.kubanhorizons.genie.GenieAnchor;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Лампа единственной Кубанской Джиннии: привязка, призыв и вход во дворец. */
public final class GenieLampItem extends Item {
    private static final String GENIE_KEY = "Genie";
    private static final String OWNER_KEY = "Owner";

    public GenieLampItem(Properties properties) {
        super(properties);
    }

    /** Привязывает пустую лампу. Кража уже привязанной лампы владельца не меняет. */
    public static boolean bind(ItemStack stack, ServerPlayer player, KubanGenie genie) {
        if (!(stack.getItem() instanceof GenieLampItem) || !genie.isOwnedBy(player)) {
            return false;
        }
        Binding current = binding(stack);
        if (current != null) {
            return current.ownerId().equals(player.getUUID())
                    && current.genieId().equals(genie.getUUID());
        }
        CompoundTag data = new CompoundTag();
        data.putString(GENIE_KEY, genie.getUUID().toString());
        data.putString(OWNER_KEY, player.getUUID().toString());
        CustomData.set(DataComponents.CUSTOM_DATA, stack, data);
        player.sendSystemMessage(Component.translatable(
                "message.kubanhorizons.genie.lamp.bound", genie.getDisplayName()));
        return true;
    }

    public static @Nullable Binding binding(ItemStack stack) {
        CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String genie = data.getStringOr(GENIE_KEY, "");
        String owner = data.getStringOr(OWNER_KEY, "");
        try {
            return genie.isEmpty() || owner.isEmpty()
                    ? null
                    : new Binding(UUID.fromString(genie), UUID.fromString(owner));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static @Nullable ItemStack findBoundLamp(ServerPlayer player, UUID genieId) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            Binding binding = binding(stack);
            if (binding != null && binding.genieId().equals(genieId)
                    && binding.ownerId().equals(player.getUUID())) {
                return stack;
            }
        }
        return null;
    }

    /** Возвращает загруженную якорную джиннию к хозяину. */
    public static boolean summon(ServerPlayer player, Binding binding) {
        return summonResolved(player, binding,
                findGenie(player.level().getServer(), binding.genieId()));
    }

    /** Исполняет уже разрешённый призыв; отдельно проверяется без общего GameTest-якоря. */
    public static boolean summonResolved(ServerPlayer player, Binding binding,
                                         @Nullable KubanGenie genie) {
        if (!binding.ownerId().equals(player.getUUID())) {
            player.sendSystemMessage(Component.translatable(
                    "message.kubanhorizons.genie.lamp.not_owner"));
            return false;
        }
        if (genie == null || !binding.genieId().equals(genie.getUUID()) || !genie.isOwnedBy(player)) {
            player.sendSystemMessage(Component.translatable(
                    "message.kubanhorizons.genie.lamp.unavailable"));
            return false;
        }
        ServerLevel destination = (ServerLevel) player.level();
        Vec3 target = player.position().add(player.getLookAngle().scale(2.0D)).add(0.0D, 0.5D, 0.0D);
        if (genie.level().dimension() != destination.dimension()) {
            genie.teleportTo(destination, target.x, target.y, target.z,
                    java.util.EnumSet.noneOf(net.minecraft.world.entity.Relative.class),
                    player.getYRot(), 0.0F, false);
        } else {
            genie.snapTo(target.x, target.y, target.z, player.getYRot(), 0.0F);
        }
        genie.getNavigation().stop();
        genie.playCast();
        player.sendSystemMessage(Component.translatable(
                "message.kubanhorizons.genie.lamp.summoned"));
        return true;
    }

    public static @Nullable KubanGenie findGenie(MinecraftServer server, UUID genieId) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(genieId) instanceof KubanGenie genie && genie.isAlive()) {
                return genie;
            }
        }
        KubanGenie anchored = GenieAnchor.find(server);
        return anchored != null && anchored.getUUID().equals(genieId) ? anchored : null;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        Binding binding = binding(player.getItemInHand(hand));
        if (binding == null) {
            player.sendSystemMessage(Component.translatable(
                    "message.kubanhorizons.genie.lamp.unbound"));
            return InteractionResult.FAIL;
        }
        if (!binding.ownerId().equals(player.getUUID())) {
            player.sendSystemMessage(Component.translatable(
                    "message.kubanhorizons.genie.lamp.not_owner"));
            return InteractionResult.FAIL;
        }
        if (VesselTravelService.isVisitingPalace(serverPlayer)) {
            return VesselTravelService.leavePalace(serverPlayer)
                    ? InteractionResult.SUCCESS
                    : InteractionResult.FAIL;
        }
        if (player.isShiftKeyDown()) {
            return VesselTravelService.enterPalace(serverPlayer, binding.genieId())
                    ? InteractionResult.SUCCESS
                    : InteractionResult.FAIL;
        }
        return summon(serverPlayer, binding) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    public record Binding(UUID genieId, UUID ownerId) {
    }
}
