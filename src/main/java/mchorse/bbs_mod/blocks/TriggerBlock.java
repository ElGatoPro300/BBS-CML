package mchorse.bbs_mod.blocks;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.blocks.entities.TriggerBlockEntity;
import mchorse.bbs_mod.network.ServerNetwork;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.joml.Vector3f;

import org.jetbrains.annotations.Nullable;

public class TriggerBlock extends Block implements EntityBlock
{
    public TriggerBlock(Properties settings)
    {
        super(settings);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData)
    {
        BlockEntity entity = world.getBlockEntity(pos);

        if (entity instanceof TriggerBlockEntity triggerBlock)
        {
            ItemStack stack = new ItemStack(this);
            stack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(BBSMod.TRIGGER_BLOCK_ENTITY, triggerBlock.saveWithoutMetadata(world.registryAccess())));

            return stack;
        }

        return super.getCloneItemStack(world, pos, state, includeData);
    }

    @Override
    public RenderShape getRenderShape(BlockState state)
    {
        return RenderShape.INVISIBLE;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter world, BlockPos pos)
    {
        return 1.0F;
    }

    public boolean isTransparent(BlockState state, BlockGetter world, BlockPos pos)
    {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new TriggerBlockEntity(pos, state);
    }

    @Override
    public void attack(BlockState state, Level world, BlockPos pos, Player player)
    {
        if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer && !player.isCreative())
        {
            BlockEntity be = world.getBlockEntity(pos);

            if (be instanceof TriggerBlockEntity triggerBlock)
            {
                triggerBlock.trigger(serverPlayer, false);
            }
        }

        super.attack(state, world, pos, player);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit)
    {
        if (player.getMainHandItem().isEmpty())
        {
            if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer)
            {
                if (!player.isCreative() || (player.isCreative() && player.isShiftKeyDown()))
                {
                    BlockEntity be = world.getBlockEntity(pos);

                    if (be instanceof TriggerBlockEntity triggerBlock)
                    {
                        triggerBlock.trigger(serverPlayer, true);

                        return InteractionResult.SUCCESS;
                    }
                }
                else
                {
                    ServerNetwork.sendClickedTriggerBlock(serverPlayer, pos);

                    return InteractionResult.SUCCESS;
                }
            }

            return InteractionResult.SUCCESS;
        }

        return super.useWithoutItem(state, world, pos, player, hit);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context)
    {
        return this.getShape(world, pos);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context)
    {
        try
        {
            BlockEntity be = world.getBlockEntity(pos);

            if (be instanceof TriggerBlockEntity trigger)
            {
                if (!trigger.collidable.get())
                {
                    return Shapes.empty();
                }

                return this.getShape(world, pos);
            }
        }
        catch (Exception e)
        {
        }

        /* Never fall back to a solid full cube for a non-solid block: an exception or
         * a missing block entity here must not eject the player through the world. */
        return Shapes.empty();
    }

    private VoxelShape getShape(BlockGetter world, BlockPos pos)
    {
        try
        {
            BlockEntity be = world.getBlockEntity(pos);

            if (be instanceof TriggerBlockEntity trigger)
            {
                Vector3f min = trigger.pos1.get();
                Vector3f max = trigger.pos2.get();

                if (min == null || max == null)
                {
                    return Shapes.empty();
                }

                double minX = Math.max(0D, Math.min(min.x, max.x));
                double minY = Math.max(0D, Math.min(min.y, max.y));
                double minZ = Math.max(0D, Math.min(min.z, max.z));
                double maxX = Math.min(1D, Math.max(min.x, max.x));
                double maxY = Math.min(1D, Math.max(min.y, max.y));
                double maxZ = Math.min(1D, Math.max(min.z, max.z));

                if (minX < maxX && minY < maxY && minZ < maxZ)
                {
                    return Shapes.box(minX, minY, minZ, maxX, maxY, maxZ);
                }
            }
        }
        catch (Exception e)
        {
        }

        return Shapes.empty();
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type)
    {
        return type == BBSMod.TRIGGER_BLOCK_ENTITY ? (BlockEntityTicker<T>) (BlockEntityTicker<TriggerBlockEntity>) (w, p, s, e) -> TriggerBlockEntity.tick(w, p, s, e) : null;
    }
}
