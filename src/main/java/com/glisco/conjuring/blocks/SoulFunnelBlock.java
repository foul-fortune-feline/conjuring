package com.glisco.conjuring.blocks;

import com.glisco.conjuring.items.ConjuringFocus;
import com.glisco.conjuring.items.ConjuringScepter;
import io.wispforest.owo.ops.ItemOps;
import io.wispforest.owo.particles.ClientParticles;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.AbstractRandom;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SoulFunnelBlock extends BlockWithEntity {

    private static final VoxelShape PILLAR1 = Block.createCuboidShape(12, 0, 0, 16, 9, 4);

    private static final VoxelShape PILLAR2 = Block.createCuboidShape(12, 0, 12, 16, 9, 16);
    private static final VoxelShape PILLAR3 = Block.createCuboidShape(0, 0, 12, 4, 9, 16);
    private static final VoxelShape PILLAR4 = Block.createCuboidShape(0, 0, 0, 4, 9, 4);

    private static final VoxelShape WALL1 = Block.createCuboidShape(4, 0, 0, 12, 6, 4);
    private static final VoxelShape WALL2 = Block.createCuboidShape(12, 0, 4, 16, 6, 12);
    private static final VoxelShape WALL3 = Block.createCuboidShape(4, 0, 12, 12, 6, 16);
    private static final VoxelShape WALL4 = Block.createCuboidShape(0, 0, 4, 4, 6, 12);

    private static final VoxelShape SOUL_SAND = Block.createCuboidShape(4, 0, 4, 12, 5, 12);

    private static final VoxelShape SHAPE = VoxelShapes.union(SOUL_SAND, PILLAR1, PILLAR2, PILLAR3, PILLAR4, WALL1, WALL2, WALL3, WALL4);

    public static final BooleanProperty FILLED = BooleanProperty.of("filled");

    //Construction stuff
    public SoulFunnelBlock() {
        super(Settings.copy(Blocks.BLACKSTONE).nonOpaque());
        setDefaultState(getStateManager().getDefaultState().with(FILLED, false));
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SoulFunnelBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    //BlockState shit
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FILLED);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    //Actual Logic
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {

        //Pedestal highlighting logic
        final ItemStack playerStack = player.getStackInHand(hand);
        if (playerStack.isEmpty() && player.isSneaking()) {
            if (!world.isClient) return ActionResult.SUCCESS;

            List<BlockPos> possiblePedestals = new ArrayList<>();
            possiblePedestals.add(pos.add(3, 0, 0));
            possiblePedestals.add(pos.add(-3, 0, 0));
            possiblePedestals.add(pos.add(0, 0, 3));
            possiblePedestals.add(pos.add(0, 0, -3));

            ClientParticles.setParticleCount(50);
            ClientParticles.persist();

            for (BlockPos pedestal : possiblePedestals) {
                if (world.getBlockEntity(pedestal) instanceof BlackstonePedestalBlockEntity) continue;
                ClientParticles.spawnPrecise(ParticleTypes.DRIPPING_OBSIDIAN_TEAR, world, new Vec3d(pedestal.getX() + 0.5, pedestal.getY() + 0.75, pedestal.getZ() + 0.5), 0.5, 0.75, 0.5);
            }

            ClientParticles.reset();
            return ActionResult.SUCCESS;
        }

        //Filling logic
        if (playerStack.getItem().equals(Items.SOUL_SAND) && !state.get(FILLED)) {
            world.setBlockState(pos, state.with(FILLED, true));

            ItemOps.decrementPlayerHandItem(player, hand);


            if (!world.isClient()) {
                world.playSound(null, pos, SoundEvents.BLOCK_SOUL_SAND_PLACE, SoundCategory.BLOCKS, 1, 1);
            }
            return ActionResult.SUCCESS;
        }

        //Ritual logic
        if (playerStack.getItem() instanceof ConjuringScepter) {

            RitualCore core = (RitualCore) world.getBlockEntity(pos);
            if (core.tryStartRitual(player)) return ActionResult.SUCCESS;
        }

        //Focus placing logic
        if (!state.get(FILLED)) return ActionResult.PASS;

        SoulFunnelBlockEntity funnel = (SoulFunnelBlockEntity) world.getBlockEntity(pos);
        ItemStack funnelFocus = funnel.getItem();

        if (funnelFocus.isEmpty()) {
            if (!(playerStack.getItem() instanceof ConjuringFocus))
                return ActionResult.PASS;

            if (!world.isClient()) {
                funnel.setItem(playerStack.copy());
                player.setStackInHand(hand, ItemStack.EMPTY);
            }
        } else {
            if (!world.isClient() && !funnel.isRitualRunning()) {
                if (playerStack.isEmpty()) {
                    player.setStackInHand(hand, funnelFocus);
                } else {
                    ItemScatterer.spawn(world, pos.getX(), pos.getY() + 0.55d, pos.getZ(), funnelFocus);
                }
                funnel.setItem(ItemStack.EMPTY);
            }
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            if (state.get(FILLED)) ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.SOUL_SAND));

            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof SoulFunnelBlockEntity funnel) {
                funnel.onBroken();
            }
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, ConjuringBlocks.Entities.SOUL_FUNNEL, world.isClient ? SoulFunnelBlockEntity.CLIENT_TICKER : SoulFunnelBlockEntity.SERVER_TICKER);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, AbstractRandom random) {
        if (!(world.getBlockEntity(pos) instanceof SoulFunnelBlockEntity funnel)) return;

        for (BlockPos p : funnel.getPedestalPositions()) {
            if (random.nextDouble() > 0.5f) continue;
            if (!(world.getBlockEntity(p) instanceof BlackstonePedestalBlockEntity pedestal)) continue;

            if (pedestal.getLinkedFunnel() == null) continue;
            if (pedestal.getLinkedFunnel().compareTo(pos) != 0) return;

            ClientParticles.spawnEnchantParticles(world, Vec3d.of(p).add(0.5, 0.5, 0.5), Vec3d.of(pos).add(0.5, 1, 0.5), 0);
        }
    }
}
