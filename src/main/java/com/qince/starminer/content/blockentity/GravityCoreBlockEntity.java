package com.qince.starminer.content.blockentity;

import com.qince.starminer.Config;
import com.qince.starminer.api.BuildSpeed;
import com.qince.starminer.api.GravityConst;
import com.qince.starminer.api.GravityField;
import com.qince.starminer.content.menu.GravityCoreMenu;
import com.qince.starminer.registry.SMBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The Gravity Core block entity.
 *
 * <p>Holds the field parameters (gravity range, star radius, shape), a 28 slot inventory and a work
 * cursor used to build the star shell one shell cell at a time.
 *
 * <p>NBT keys keep the original {@code stmn.} prefix so a 1.7.10 save can in principle be migrated.
 */
public class GravityCoreBlockEntity extends BlockEntity implements Container, MenuProvider {

    public static final String NBT_GRAVITY_RANGE = "stmn.gravRange";
    public static final String NBT_STAR_RADIUS = "stmn.starRad";
    public static final String NBT_TYPE = "stmn.type";
    public static final String NBT_BUFFER = "stmn.usb";
    /** Cursor state of the star being built: layer, ring and index within the ring. */
    public static final String NBT_CURSOR_LAYER = "stmn.wkX";
    public static final String NBT_CURSOR_RING = "stmn.wkY";
    public static final String NBT_CURSOR_INDEX = "stmn.wkZ";
    public static final String NBT_PLACED = "stmn.placed";
    public static final String NBT_SPEED = "stmn.speed";

    /** Slots 0..26 accept terraforming blocks, slot 27 is the "gravitize" slot. */
    public static final int SLOT_COUNT = 28;
    public static final int TERRAFORM_SLOTS = SLOT_COUNT - 1;
    public static final int GRAVITIZE_SLOT = SLOT_COUNT - 1;
    public static final int STACK_LIMIT = 64;

    private final ItemStack[] items = new ItemStack[SLOT_COUNT];

    /** Defaults for a freshly placed core: usable straight away rather than a dead zero field. */
    public static final double DEFAULT_GRAVITY_RANGE = 24.0D;
    public static final double DEFAULT_STAR_RADIUS = 4.0D;

    private double gravityRange = DEFAULT_GRAVITY_RANGE;
    private double starRadius = DEFAULT_STAR_RADIUS;
    private int type = GravityConst.FIELD_TYPE_SPHERE;
    private boolean useBufferArea = true;
    private BuildSpeed buildSpeed = BuildSpeed.NORMAL;

    /** Walks the shell of the star being built, one cell per placed block. */
    private StarShellCursor cursor;
    /** The radius and shape the current cursor was built for; the cursor is rebuilt when they change. */
    private int cursorRadius = -1;
    private int cursorType = -1;

    private boolean working;
    private long placedBlocks;
    private int cachedShellKey = -1;
    private int cachedShellCount = -1;

    public GravityCoreBlockEntity(BlockPos pos, BlockState state) {
        super(SMBlockEntities.GRAVITY_CORE.get(), pos, state);
        java.util.Arrays.fill(this.items, ItemStack.EMPTY);
    }

    // ------------------------------------------------------------------ data

    public double getGravityRange() {
        return this.gravityRange;
    }

    public double getStarRadius() {
        return this.starRadius;
    }

    public int getFieldType() {
        return this.type;
    }

    public boolean isUsingBufferArea() {
        return this.useBufferArea;
    }

    public BuildSpeed getBuildSpeed() {
        return this.buildSpeed;
    }

    public void setBuildSpeed(BuildSpeed speed) {
        this.buildSpeed = speed == null ? BuildSpeed.NORMAL : speed;
    }

    public boolean isWorking() {
        return this.working;
    }

    /** How many blocks have been placed into the star shell so far. */
    public long getPlacedBlocks() {
        return this.placedBlocks;
    }

    /** {@link StarShellCursor#countCells} memoised for the GUI, which asks once per frame. */
    public int shellCellCountMemoised() {
        int key = ((int) this.starRadius << 4) | (this.type & 0xF);
        if (key != this.cachedShellKey) {
            this.cachedShellKey = key;
            int radius = (int) this.starRadius;
            this.cachedShellCount = radius <= 0 ? 0 : StarShellCursor.countCells(radius, this.type);
        }
        return this.cachedShellCount;
    }

    public void setGravityRange(double value) {
        this.gravityRange = Mth.clamp(value, 0.0D, Config.MAX_GRAVITY_RANGE.get());
    }

    public void setStarRadius(double value) {
        this.starRadius = Mth.clamp(value, 0.0D, Config.MAX_STAR_RADIUS.get());
        this.invalidateCursor();
    }

    public void setFieldType(int value) {
        this.type = GravityConst.clampFieldType(value);
        this.invalidateCursor();
    }

    /** Drops the build cursor so the next tick rebuilds it for the new radius or shape. */
    private void invalidateCursor() {
        this.cursor = null;
        this.cursorRadius = -1;
        this.cursorType = -1;
        this.placedBlocks = 0L;
    }

    public void setUseBufferArea(boolean value) {
        this.useBufferArea = value;
    }

    /**
     * Applies a settings change, clamping everything to the configured limits.
     *
     * <p>Called from the network handler, so the values may come from a client and must not be trusted.
     */
    public void applySettings(int gravityRange, int starRadius, int fieldType, boolean useBufferArea, int speedIndex) {
        boolean changed = false;
        if (gravityRange != (int) this.gravityRange) {
            setGravityRange(gravityRange);
            changed = true;
        }
        if (starRadius != (int) this.starRadius) {
            setStarRadius(starRadius);
            changed = true;
        }
        if (fieldType != this.type) {
            setFieldType(fieldType);
            changed = true;
        }
        if (useBufferArea != this.useBufferArea) {
            this.useBufferArea = useBufferArea;
            changed = true;
        }
        BuildSpeed requested = BuildSpeed.byIndex(speedIndex);
        if (requested != this.buildSpeed) {
            this.buildSpeed = requested;
            changed = true;
        }
        if (changed) {
            this.setChanged();
        }
    }

    /** The field this core currently projects. */
    public GravityField field() {
        return new GravityField(this.worldPosition, this.gravityRange, this.type, this.useBufferArea);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide())
            com.qince.starminer.gravity.GravityFieldManager.of(this.level).add(this.worldPosition);
    }

    @Override
    public void setRemoved() {
        if (this.level != null && !this.level.isClientSide())
            com.qince.starminer.gravity.GravityFieldManager.of(this.level).remove(this.worldPosition);
        super.setRemoved();
    }

    public final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case GravityCoreMenu.DATA_GRAVITY_RANGE -> (int) Math.round(GravityCoreBlockEntity.this.gravityRange);
                case GravityCoreMenu.DATA_STAR_RADIUS -> (int) Math.round(GravityCoreBlockEntity.this.starRadius);
                case GravityCoreMenu.DATA_FIELD_TYPE -> GravityCoreBlockEntity.this.type;
                case GravityCoreMenu.DATA_BUFFER -> GravityCoreBlockEntity.this.useBufferArea ? 1 : 0;
                case GravityCoreMenu.DATA_WORKING -> GravityCoreBlockEntity.this.working ? 1 : 0;
                case GravityCoreMenu.DATA_PLACED_HIGH -> (int) (GravityCoreBlockEntity.this.placedBlocks >>> 32);
                case GravityCoreMenu.DATA_PLACED_LOW -> (int) GravityCoreBlockEntity.this.placedBlocks;
                case GravityCoreMenu.DATA_SPEED -> GravityCoreBlockEntity.this.buildSpeed.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case GravityCoreMenu.DATA_GRAVITY_RANGE -> GravityCoreBlockEntity.this.gravityRange = Mth.clamp(value, 0, 999);
                case GravityCoreMenu.DATA_STAR_RADIUS -> GravityCoreBlockEntity.this.starRadius = Mth.clamp(value, 0, 999);
                case GravityCoreMenu.DATA_FIELD_TYPE -> GravityCoreBlockEntity.this.type = GravityConst.clampFieldType(value);
                case GravityCoreMenu.DATA_BUFFER -> GravityCoreBlockEntity.this.useBufferArea = value != 0;
                case GravityCoreMenu.DATA_WORKING -> GravityCoreBlockEntity.this.working = value != 0;
                case GravityCoreMenu.DATA_SPEED -> GravityCoreBlockEntity.this.buildSpeed = BuildSpeed.byIndex(value);
                case GravityCoreMenu.DATA_PLACED_HIGH ->
                        GravityCoreBlockEntity.this.placedBlocks =
                                ((long) value << 32) | (GravityCoreBlockEntity.this.placedBlocks & 0xFFFFFFFFL);
                case GravityCoreMenu.DATA_PLACED_LOW ->
                        GravityCoreBlockEntity.this.placedBlocks =
                                (GravityCoreBlockEntity.this.placedBlocks & 0xFFFFFFFF00000000L) | (value & 0xFFFFFFFFL);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return GravityCoreMenu.DATA_COUNT;
        }
    };

    // ------------------------------------------------------------------ ticking

    public static void serverTick(Level level, BlockPos pos, BlockState state, GravityCoreBlockEntity core) {
        BuildSpeed speed = core.buildSpeed;
        if (level.getGameTime() % speed.cycleTicks() != 0L) {
            return;
        }

        int budget = speed.blocksPerCycle();
        boolean didWork = false;
        while (budget-- > 0 && core.tryPlaceOneShellBlock(level)) {
            didWork = true;
        }

        if (core.working != didWork) {
            core.working = didWork;
            core.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        } else if (didWork) {
            core.setChanged();
        }
    }

    /**
     * Places the next block of the star shell.
     *
     * <p>The cursor walks the shell layer by layer, so the cost per placed block is proportional to the
     * radius rather than the volume of the bounding cube, and arbitrarily large stars are supported.
     *
     * @return true when a block was actually placed
     */
    private boolean tryPlaceOneShellBlock(Level level) {
        int radius = (int) this.starRadius;
        if (radius <= 0) {
            return false;
        }

        if (this.cursor == null || this.cursorRadius != radius || this.cursorType != this.type) {
            this.cursor = new StarShellCursor(radius, this.type);
            this.cursorRadius = radius;
            this.cursorType = this.type;
        }

        for (BlockPos offset = this.cursor.advance(); offset != null; offset = this.cursor.advance()) {
            BlockPos target = this.worldPosition.offset(offset);
            if (!level.isLoaded(target) || level.isOutsideBuildHeight(target)) {
                continue;
            }
            if (!level.getBlockState(target).canBeReplaced()) {
                continue;
            }

            int slot = this.findTerraformBlockSlot();
            if (slot < 0) {
                // Nothing left to build with.
                return false;
            }
            ItemStack stack = this.items[slot];
            if (!(stack.getItem() instanceof BlockItem blockItem)) {
                continue;
            }

            level.setBlock(target, blockItem.getBlock().defaultBlockState(), Block.UPDATE_ALL);
            stack.shrink(1);
            if (stack.isEmpty()) {
                this.items[slot] = ItemStack.EMPTY;
            }
            this.placedBlocks++;
            return true;
        }

        // The shell is complete, or nothing in it can be replaced: start a fresh pass so a star that was
        // mined out can be topped up again.
        this.cursor = new StarShellCursor(radius, this.type);
        return false;
    }


    /** The first slot holding a block that may be placed, or -1. */
    private int findTerraformBlockSlot() {
        for (int i = 0; i < TERRAFORM_SLOTS; i++) {
            ItemStack stack = this.items[i];
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
                return i;
            }
        }
        return -1;
    }

    /** Slot 27 only accepts items the core can turn into a gravitized variant. */
    public boolean isAllowedInGravitizeSlot(ItemStack stack) {
        return isGravitizableItem(stack);
    }

    /** Shared item rule for the gravitize slot, safe to call on both sides. */
    public static boolean isGravitizableItem(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BlockItem;
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble(NBT_GRAVITY_RANGE, this.gravityRange);
        tag.putDouble(NBT_STAR_RADIUS, this.starRadius);
        tag.putInt(NBT_TYPE, this.type);
        tag.putBoolean(NBT_BUFFER, this.useBufferArea);
        StarShellCursor cursor = this.cursor;
        if (cursor != null) {
            tag.putInt(NBT_CURSOR_LAYER, cursor.layer());
            tag.putInt(NBT_CURSOR_RING, cursor.ring());
            tag.putInt(NBT_CURSOR_INDEX, cursor.index());
        }
        tag.putLong(NBT_PLACED, this.placedBlocks);
        tag.putString(NBT_SPEED, this.buildSpeed.id());

        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = this.items[i];
            if (!stack.isEmpty()) {
                CompoundTag entry = new CompoundTag();
                entry.putByte("Slot", (byte) i);
                entry.put("Item", stack.save(registries));
                list.add(entry);
            }
        }
        tag.put("Items", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.gravityRange = Mth.clamp(tag.getDouble(NBT_GRAVITY_RANGE), 0.0D, Config.MAX_GRAVITY_RANGE.get());
        // A save without the key predates the settings controls; fall back to a usable default.
        this.starRadius = Mth.clamp(
                tag.contains(NBT_STAR_RADIUS) ? tag.getDouble(NBT_STAR_RADIUS) : DEFAULT_STAR_RADIUS,
                0.0D, Config.MAX_STAR_RADIUS.get());
        this.type = GravityConst.clampFieldType(tag.getInt(NBT_TYPE));
        this.useBufferArea = !tag.contains(NBT_BUFFER) || tag.getBoolean(NBT_BUFFER);
        this.buildSpeed = tag.contains(NBT_SPEED) ? BuildSpeed.byId(tag.getString(NBT_SPEED)) : BuildSpeed.NORMAL;
        this.placedBlocks = tag.getLong(NBT_PLACED);

        // Rebuild the walk cursor at the persisted position. An unfinished star therefore resumes where
        // it left off instead of starting over.
        if (this.starRadius > 0.0D) {
            StarShellCursor rebuilt = new StarShellCursor((int) this.starRadius, this.type);
            rebuilt.restore(tag.getInt(NBT_CURSOR_LAYER), tag.getInt(NBT_CURSOR_RING), tag.getInt(NBT_CURSOR_INDEX));
            this.cursor = rebuilt;
            this.cursorRadius = (int) this.starRadius;
            this.cursorType = this.type;
        } else {
            this.cursor = null;
        }

        java.util.Arrays.fill(this.items, ItemStack.EMPTY);
        net.minecraft.nbt.ListTag list = tag.getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getByte("Slot") & 0xFF;
            if (slot < SLOT_COUNT) {
                ItemStack.parse(registries, entry.getCompound("Item")).ifPresent(stack -> this.items[slot] = stack);
            }
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    // ------------------------------------------------------------------ Container

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items[slot];
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = this.items[slot];
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack split = stack.split(amount);
        if (stack.isEmpty()) {
            this.items[slot] = ItemStack.EMPTY;
        }
        this.setChanged();
        return split;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = this.items[slot];
        this.items[slot] = ItemStack.EMPTY;
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items[slot] = stack;
        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }
        this.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        java.util.Arrays.fill(this.items, ItemStack.EMPTY);
        this.setChanged();
    }

    public int getMaxStackSize() {
        return STACK_LIMIT;
    }

    // ------------------------------------------------------------------ MenuProvider

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.starminer.gravitycore");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new GravityCoreMenu(containerId, playerInventory, this, this.dataAccess);
    }
}
