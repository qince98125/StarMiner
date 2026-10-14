package com.qince.starminer.content.menu;

import com.qince.starminer.client.GuiLayout;
import com.qince.starminer.content.blockentity.GravityCoreBlockEntity;
import com.qince.starminer.registry.SMMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Container menu for the Gravity Core.
 *
 * <p>All coordinates are taken from the original 1.7.10 GUI texture, whose slot outlines are baked into
 * the artwork: core slots occupy {@code x 8..167 / y 72..123}, the gravitize slot sits at
 * {@code x 8..23 / y 54..69}, the player inventory starts at {@code y 140} and the hotbar at
 * {@code y 198}. The artwork is 176 x 222 for the used area, so the menu uses the same size.
 */
public class GravityCoreMenu extends AbstractContainerMenu {

    public static final int DATA_GRAVITY_RANGE = 0;
    public static final int DATA_STAR_RADIUS = 1;
    public static final int DATA_FIELD_TYPE = 2;
    public static final int DATA_BUFFER = 3;
    public static final int DATA_WORKING = 4;
    /** The placed block count needs 64 bits, so it travels as two data slots. */
    public static final int DATA_PLACED_HIGH = 5;
    public static final int DATA_PLACED_LOW = 6;
    public static final int DATA_SPEED = 7;
    public static final int DATA_COUNT = 8;

    private final Container container;
    private final ContainerData data;

    /**
     * The client side constructor cannot set {@code corePos} after its {@code this(...)} call, so the
     * packet is decoded once into this holder and handed to the delegating constructor.
     */
    private record ClientPacket(BlockPos pos, Container container) {
    }

    /**
     * Position of the core, so the screen can address its settings packet to the right block.
     *
     * <p>Not final: the server side takes it from the block entity, while the client side overwrites it
     * with the position decoded from the open packet.
     */
    private BlockPos corePos = BlockPos.ZERO;

    /**
     * Client side constructor. The open packet carries exactly one block position, written by
     * {@link Provider#writeClientSideData} — this mod supplies no extra data writer, so the position is
     * the entire payload.
     */
    public GravityCoreMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, decodeClientPacket(extraData));
    }

    private GravityCoreMenu(int containerId, Inventory playerInventory, ClientPacket packet) {
        this(containerId, playerInventory, packet.container(), new SimpleContainerData(DATA_COUNT));
        this.corePos = packet.pos();
    }

    /**
     * Reads the position and leaves the buffer cursor untouched for anything that might read after us.
     *
     * <p>The container is deliberately empty: vanilla syncs every slot's contents with
     * {@code ClientboundContainerSetContentPacket} right after the screen opens, so pre-filling slots
     * from this packet would duplicate that work and risk a desync.
     */
    private static ClientPacket decodeClientPacket(RegistryFriendlyByteBuf extraData) {
        BlockPos pos = BlockPos.ZERO;
        if (extraData.readableBytes() > 0) {
            int saved = extraData.readerIndex();
            pos = extraData.readBlockPos();
            extraData.readerIndex(saved);
        }
        return new ClientPacket(pos, new SimpleContainer(GravityCoreBlockEntity.SLOT_COUNT));
    }

    public GravityCoreMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(SMMenus.GRAVITY_CORE.get(), containerId);
        checkContainerSize(container, GravityCoreBlockEntity.SLOT_COUNT);
        checkContainerDataCount(data, DATA_COUNT);
        this.container = container;
        this.data = data;
        this.corePos = container instanceof GravityCoreBlockEntity core ? core.getBlockPos() : BlockPos.ZERO;

        container.startOpen(playerInventory.player);

        // Core terraforming slots 0..26, laid out in the three rows of nine the artwork provides.
        for (int i = 0; i < GravityCoreBlockEntity.TERRAFORM_SLOTS; i++) {
            int row = i / GuiLayout.SLOT_COLUMNS;
            int col = i % GuiLayout.SLOT_COLUMNS;
            this.addSlot(new Slot(container, i, GuiLayout.slotX(col),
                    GuiLayout.CORE_ROW_0_Y + row * GuiLayout.SLOT_SIZE));
        }

        // The gravitize slot occupies the small square above the grid.
        this.addSlot(new Slot(container, GravityCoreBlockEntity.GRAVITIZE_SLOT,
                GuiLayout.SLOT_GRID_X, GuiLayout.GRAVITIZE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return container instanceof GravityCoreBlockEntity core
                        ? core.isAllowedInGravitizeSlot(stack)
                        : GravityCoreBlockEntity.isGravitizableItem(stack);
            }
        });

        // Player inventory.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < GuiLayout.SLOT_COLUMNS; col++) {
                this.addSlot(new Slot(playerInventory, col + row * GuiLayout.SLOT_COLUMNS + GuiLayout.SLOT_COLUMNS,
                        GuiLayout.slotX(col), GuiLayout.PLAYER_INVENTORY_Y + row * GuiLayout.SLOT_SIZE));
            }
        }
        for (int col = 0; col < GuiLayout.SLOT_COLUMNS; col++) {
            this.addSlot(new Slot(playerInventory, col, GuiLayout.slotX(col), GuiLayout.HOTBAR_Y));
        }

        this.addDataSlots(data);
    }

    /** Position of the core this menu belongs to. */
    public BlockPos getCorePos() {
        return this.corePos;
    }

    /**
     * Menu provider that ships the core's position to the client.
     *
     * <p>The position is written in {@link #writeClientSideData}, and the {@code openMenu} overload used
     * by {@link com.qince.starminer.content.block.GravityCoreBlock} supplies no extra writer, so the
     * packet payload is exactly one {@code BlockPos}.
     */
    public static final class Provider implements net.minecraft.world.MenuProvider,
            net.neoforged.neoforge.client.extensions.IMenuProviderExtension {

        private final GravityCoreBlockEntity core;

        public Provider(GravityCoreBlockEntity core) {
            this.core = core;
        }

        @Override
        public net.minecraft.network.chat.Component getDisplayName() {
            return net.minecraft.network.chat.Component.translatable("block.starminer.gravitycore");
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new GravityCoreMenu(containerId, playerInventory, this.core, this.core.dataAccess);
        }

        @Override
        public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
            buffer.writeBlockPos(this.core.getBlockPos());
        }
    }

    public int getGravityRange() {
        return this.data.get(DATA_GRAVITY_RANGE);
    }

    public int getStarRadius() {
        return this.data.get(DATA_STAR_RADIUS);
    }

    public int getFieldType() {
        return this.data.get(DATA_FIELD_TYPE);
    }

    public boolean isUsingBufferArea() {
        return this.data.get(DATA_BUFFER) != 0;
    }

    public boolean isWorking() {
        return this.data.get(DATA_WORKING) != 0;
    }

    public int getSpeedIndex() {
        return this.data.get(DATA_SPEED);
    }

    public com.qince.starminer.api.BuildSpeed getBuildSpeed() {
        return com.qince.starminer.api.BuildSpeed.byIndex(getSpeedIndex());
    }

    /** How many blocks the core has placed into the current star shell. */
    public long getPlacedBlocks() {
        return ((long) this.data.get(DATA_PLACED_HIGH) << 32) | (this.data.get(DATA_PLACED_LOW) & 0xFFFFFFFFL);
    }

    /** Total shell cells for the configured star, computed on demand for the progress readout. */
    public int getShellCellCount() {
        int radius = getStarRadius();
        return radius <= 0 ? 0 : com.qince.starminer.content.blockentity.StarShellCursor.countCells(radius, getFieldType());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        int coreEnd = GravityCoreBlockEntity.SLOT_COUNT;
        if (index < coreEnd) {
            if (!this.moveItemStackTo(stack, coreEnd, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, 0, GravityCoreBlockEntity.TERRAFORM_SLOTS, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
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
}
