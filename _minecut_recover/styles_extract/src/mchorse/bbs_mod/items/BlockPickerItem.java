package mchorse.bbs_mod.items;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.blocks.ModelBlock;
import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.blocks.entities.ModelProperties;
import mchorse.bbs_mod.forms.forms.BlockForm;
import net.minecraft.class_1269;
import net.minecraft.class_1794;
import net.minecraft.class_1799;
import net.minecraft.class_1834;
import net.minecraft.class_1838;
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_2487;
import net.minecraft.class_2586;
import net.minecraft.class_2680;
import net.minecraft.class_2741;
import net.minecraft.class_3612;

public class BlockPickerItem extends class_1794
{
    public BlockPickerItem(class_1793 settings)
    {
        super(class_1834.field_8922, settings);
    }

    @Override
    public boolean method_7886(class_1799 stack)
    {
        return true;
    }

    @Override
    public class_1269 method_7884(class_1838 context)
    {
        class_1937 world = context.method_8045();

        if (world.field_9236)
        {
            return class_1269.field_5812;
        }

        if (context.method_8036() == null)
        {
            return class_1269.field_5811;
        }

        class_2338 pos = context.method_8037();
        class_2680 sourceState = world.method_8320(pos);

        if (sourceState.method_27852(BBSMod.MODEL_BLOCK))
        {
            return class_1269.field_5811;
        }

        BlockForm form = createBlockForm(world, pos, sourceState);

        class_2680 modelState = BBSMod.MODEL_BLOCK.method_9564()
            .method_11657(class_2741.field_12508, world.method_8316(pos).method_39360(class_3612.field_15910))
            .method_11657(ModelBlock.LIGHT_LEVEL, 0);

        if (!world.method_8652(pos, modelState, 3))
        {
            return class_1269.field_5811;
        }

        class_2586 blockEntity = world.method_8321(pos);

        if (!(blockEntity instanceof ModelBlockEntity modelBlockEntity))
        {
            return class_1269.field_5811;
        }

        ModelProperties properties = modelBlockEntity.getProperties();

        properties.setForm(form);
        properties.setName(sourceState.method_26204().method_9518().getString());
        properties.setHitbox(true);

        float hardness = sourceState.method_26214(world, pos);

        if (hardness >= 0F)
        {
            properties.setHardness(hardness);
        }

        modelBlockEntity.method_5431();
        world.method_8413(pos, modelState, modelState, 3);

        return class_1269.field_5812;
    }

    public static BlockForm createBlockForm(class_1937 world, class_2338 pos, class_2680 state)
    {
        BlockForm form = new BlockForm();

        form.blockState.set(state);

        class_2586 sourceEntity = world.method_8321(pos);

        if (sourceEntity != null)
        {
            class_2487 nbt = sourceEntity.method_38243(world.method_30349());

            nbt.method_10569("x", 0);
            nbt.method_10569("y", 0);
            nbt.method_10569("z", 0);
            form.blockEntityNbt.set(nbt.toString());
        }

        return form;
    }
}
