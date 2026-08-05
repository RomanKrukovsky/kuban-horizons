package dev.romankrukovsky.kubanhorizons.menu;

import dev.romankrukovsky.kubanhorizons.blockentity.OilPressBlockEntity;
import dev.romankrukovsky.kubanhorizons.registry.KHMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Меню маслопресса.
 *
 * <p>Раскладка слотов: сырьё (44,22), бутылка (44,52), результат (116,26),
 * жмых (116,56); далее инвентарь игрока в стандартной раскладке.</p>
 *
 * <p>Прогресс синхронизируется через {@link ContainerData} — сервер
 * остаётся источником истины.</p>
 */
public class OilPressMenu extends AbstractContainerMenu {
    private static final int PLAYER_INV_START = OilPressBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 36;

    private final Container container;
    private final ContainerData data;

    /** Клиентский конструктор (создаётся из сетевого фактори). */
    public OilPressMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory,
                new SimpleContainer(OilPressBlockEntity.SLOT_COUNT),
                new SimpleContainerData(OilPressBlockEntity.DATA_COUNT));
    }

    /** Серверный конструктор — с реальным контейнером block entity. */
    public OilPressMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(KHMenus.OIL_PRESS.get(), containerId);
        checkContainerSize(container, OilPressBlockEntity.SLOT_COUNT);
        checkContainerDataCount(data, OilPressBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;

        this.addSlot(new Slot(container, OilPressBlockEntity.SLOT_INPUT, 44, 22));
        this.addSlot(new Slot(container, OilPressBlockEntity.SLOT_BOTTLE, 44, 52) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.GLASS_BOTTLE);
            }
        });
        this.addSlot(new OutputSlot(container, OilPressBlockEntity.SLOT_RESULT, 116, 26));
        this.addSlot(new OutputSlot(container, OilPressBlockEntity.SLOT_BYPRODUCT, 116, 56));

        // Инвентарь игрока: 3 ряда + hotbar.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        this.addDataSlots(data);

        container.startOpen(playerInventory.player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot.hasItem()) {
            ItemStack current = slot.getItem();
            moved = current.copy();
            if (slotIndex < PLAYER_INV_START) {
                // Из пресса в инвентарь игрока.
                if (!this.moveItemStackTo(current, PLAYER_INV_START, PLAYER_INV_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(current, moved);
            } else {
                // Из инвентаря в пресс: бутылки — в слот бутылок, остальное — в сырьё.
                if (current.is(Items.GLASS_BOTTLE)) {
                    if (!this.moveItemStackTo(current, OilPressBlockEntity.SLOT_BOTTLE,
                            OilPressBlockEntity.SLOT_BOTTLE + 1, false)
                            && !this.moveItemStackTo(current, OilPressBlockEntity.SLOT_INPUT,
                                    OilPressBlockEntity.SLOT_INPUT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(current, OilPressBlockEntity.SLOT_INPUT,
                        OilPressBlockEntity.SLOT_INPUT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (current.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (current.getCount() == moved.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, current);
        }
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }

    /** Прогресс отжима 0..1 для отрисовки стрелки. */
    public float progress() {
        int total = this.data.get(OilPressBlockEntity.DATA_TOTAL);
        if (total <= 0) {
            return 0.0F;
        }
        return Math.min(1.0F, (float) this.data.get(OilPressBlockEntity.DATA_PROGRESS) / total);
    }

    /** Выходной слот: класть нельзя, забирать можно. */
    private static final class OutputSlot extends Slot {
        OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
