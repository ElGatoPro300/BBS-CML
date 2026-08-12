package mchorse.bbs_mod.blocks;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.network.ServerNetwork;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.joml.Vector3f;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

public class ModelBlock extends Block implements EntityBlock, SimpleWaterloggedBlock
{
    public static final IntegerProperty LIGHT_LEVEL = IntegerProperty.create("light_level", 0, 15);

    public static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> validateTicker(BlockEntityType<A> givenType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker)
    {
        return expectedType == givenType ? (BlockEntityTicker<A>) ticker : null;
    }

    public ModelBlock(Properties settings)
    {
        super(settings);

        this.registerDefaultState(defaultBlockState()
            .setValue(BlockStateProperties.WATERLOGGED, false)
            .setValue(LIGHT_LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(BlockStateProperties.WATERLOGGED, LIGHT_LEVEL);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx)
    {
        return this.defaultBlockState()
            .setValue(BlockStateProperties.WATERLOGGED, ctx.getLevel().getFluidState(ctx.getClickedPos()).is(Fluids.WATER));
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData)
    {
        BlockEntity entity = world.getBlockEntity(pos);

        if (entity instanceof ModelBlockEntity modelBlock)
        {
            ItemStack stack = new ItemStack(this);
            stack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(BBSMod.MODEL_BLOCK_ENTITY, modelBlock.saveWithoutMetadata(world.registryAccess())));
            
            stack.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(Map.of("light_level", String.valueOf(modelBlock.getProperties().getLightLevel()))));

            return stack;
        }

        return super.getCloneItemStack(world, pos, state, includeData);
    }

    @Override
    public RenderShape getRenderShape(BlockState state)
    {
        return RenderShape.INVISIBLE;
    }

    protected boolean isTransparent(BlockState state, BlockGetter world, BlockPos pos)
    {
        return true;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type)
    {
        return validateTicker(type, BBSMod.MODEL_BLOCK_ENTITY, (w, p, s, e) -> ModelBlockEntity.tick(w, p, s, e));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new ModelBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context)
    {
        try
        {
            if (world instanceof Level w)
            {
                BlockEntity be = w.getBlockEntity(pos);

                if (be instanceof ModelBlockEntity model && model.getProperties().isHitbox())
                {
                    Form form = model.getProperties().getForm();

                    if (form != null && form.hitbox.get())
                    {
                        float width = form.hitboxWidth.get();
                        float height = form.hitboxHeight.get();

                        if (width > 0F && height > 0F)
                        {
                            float halfWidth = width / 2F;

                            double minX = 0.5D - halfWidth;
                            double maxX = 0.5D + halfWidth;
                            double minZ = 0.5D - halfWidth;
                            double maxZ = 0.5D + halfWidth;
                            double minY = 0D;
                            double maxY = height;

                            minX = Math.max(0D, minX);
                            minZ = Math.max(0D, minZ);
                            maxX = Math.min(1D, maxX);
                            maxZ = Math.min(1D, maxZ);
                            maxY = Math.min(1D, maxY);

                            if (minX < maxX && minZ < maxZ && maxY > minY)
                            {
                                return Shapes.box(minX, minY, minZ, maxX, maxY, maxZ);
                            }
                        }
                    }

                    return Shapes.block();
                }
            }
        }
        catch (Exception e)
        {

        }

        return Shapes.empty();
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            ServerNetwork.sendClickedModelBlock(serverPlayer, pos);
        }

        return InteractionResult.SUCCESS;
    }

    /* Waterloggable implementation */

    @Override
    public FluidState getFluidState(BlockState state)
    {
        return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public void playerDestroy(Level world, Player player, BlockPos pos, BlockState state, BlockEntity be, ItemStack tool)
    {
        if (!world.isClientSide() && !player.getAbilities().instabuild)
        {
            if (be instanceof ModelBlockEntity model)
            {
                ItemStack stack = new ItemStack(this);
                stack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(BBSMod.MODEL_BLOCK_ENTITY, model.saveWithoutMetadata(world.registryAccess())));
                
                stack.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(Map.of("light_level", String.valueOf(model.getProperties().getLightLevel()))));

                Containers.dropContents(world, pos, NonNullList.withSize(1, stack));
            }
        }

        super.playerDestroy(world, player, pos, state, be, tool);
    }
}
