package mchorse.bbs_mod.items;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.blocks.ModelBlock;
import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.blocks.entities.ModelProperties;
import mchorse.bbs_mod.forms.forms.BlockForm;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;

public class BlockPickerItem extends Item
{
    public BlockPickerItem(Properties settings)
    {
        super(settings);
    }

    @Override
    public boolean isFoil(ItemStack stack)
    {
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        Level world = context.getLevel();

        if (world.isClientSide())
        {
            return InteractionResult.SUCCESS;
        }

        if (context.getPlayer() == null)
        {
            return InteractionResult.PASS;
        }

        BlockPos pos = context.getClickedPos();
        BlockState sourceState = world.getBlockState(pos);

        if (sourceState.is(BBSMod.MODEL_BLOCK))
        {
            return InteractionResult.PASS;
        }

        BlockForm form = createBlockForm(world, pos, sourceState);

        BlockState modelState = BBSMod.MODEL_BLOCK.defaultBlockState()
            .setValue(BlockStateProperties.WATERLOGGED, world.getFluidState(pos).is(Fluids.WATER))
            .setValue(ModelBlock.LIGHT_LEVEL, 0);

        if (!world.setBlock(pos, modelState, 3))
        {
            return InteractionResult.PASS;
        }

        BlockEntity blockEntity = world.getBlockEntity(pos);

        if (!(blockEntity instanceof ModelBlockEntity modelBlockEntity))
        {
            return InteractionResult.PASS;
        }

        ModelProperties properties = modelBlockEntity.getProperties();

        properties.setForm(form);
        properties.setName(sourceState.getBlock().getName().getString());
        properties.setHitbox(true);

        float hardness = sourceState.getDestroySpeed(world, pos);

        if (hardness >= 0F)
        {
            properties.setHardness(hardness);
        }

        modelBlockEntity.setChanged();
        world.sendBlockUpdated(pos, modelState, modelState, 3);

        return InteractionResult.SUCCESS;
    }

    public static BlockForm createBlockForm(Level world, BlockPos pos, BlockState state)
    {
        BlockForm form = new BlockForm();

        form.blockState.set(state);

        BlockEntity sourceEntity = world.getBlockEntity(pos);

        if (sourceEntity != null)
        {
            CompoundTag nbt = sourceEntity.saveWithoutMetadata(world.registryAccess());

            nbt.putInt("x", 0);
            nbt.putInt("y", 0);
            nbt.putInt("z", 0);
            form.blockEntityNbt.set(nbt.toString());
        }

        return form;
    }
}
