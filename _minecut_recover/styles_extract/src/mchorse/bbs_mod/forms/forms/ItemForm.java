package mchorse.bbs_mod.forms.forms;

import mchorse.bbs_mod.forms.values.ValueModelTransformationMode;
import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.settings.values.mc.ValueItemStack;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueDouble;
import mchorse.bbs_mod.utils.colors.Color;
import net.minecraft.class_7923;
import net.minecraft.class_811;

public class ItemForm extends Form
{
    public final ValueItemStack stack = new ValueItemStack("item_stack");
    public final ValueModelTransformationMode modelTransform = new ValueModelTransformationMode("modelTransform", class_811.field_4315);
    public final ValueBoolean sameAnimationWhenDropped = new ValueBoolean("same_animation_when_dropped", false);
    public final ValueColor color = new ValueColor("color", new Color(1F, 1F, 1F, 0F));
    public final ValueDouble usingItem = new ValueDouble("using_item", 0D, 0D, 1D);
    public final ValueDouble itemUseTime = new ValueDouble("item_use_time", 0D, 0D, Double.POSITIVE_INFINITY);

    public ItemForm()
    {
        this.add(this.stack);
        this.add(this.modelTransform);
        this.add(this.sameAnimationWhenDropped);
        this.add(this.color);
        this.registerColorOverlays();
        this.add(this.usingItem);
        this.add(this.itemUseTime);
    }

    @Override
    protected String getDefaultDisplayName()
    {
        return class_7923.field_41178.method_10221(this.stack.get().method_7909()).toString();
    }
}
