package com.qince.starminer.content.block;

import com.mojang.serialization.MapCodec;
import com.qince.starminer.content.blockentity.GravityCoreBlockEntity;
import com.qince.starminer.content.menu.GravityCoreMenu;
import com.qince.starminer.registry.SMBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** The Gravity Core - the block that projects a gravity field and terraforms a star shell. */
public class GravityCoreBlock extends BaseEntityBlock {
    public static final MapCodec<GravityCoreBlock> CODEC = simpleCodec(GravityCoreBlock::new);

    public GravityCoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GravityCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, SMBlockEntities.GRAVITY_CORE.get(), GravityCoreBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof GravityCoreBlockEntity core && player instanceof ServerPlayer serverPlayer) {
            // The provider writes the core position itself, so no extra data writer is passed here. That
            // keeps the packet payload to exactly one BlockPos.
            serverPlayer.openMenu(new GravityCoreMenu.Provider(core));
        }
        return InteractionResult.CONSUME;
    }
}
