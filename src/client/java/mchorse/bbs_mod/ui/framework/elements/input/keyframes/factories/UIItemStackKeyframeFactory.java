package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIItemStack;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

import net.minecraft.item.ItemStack;

public class UIItemStackKeyframeFactory extends UIKeyframeFactory<ItemStack>
{
    private UIItemStack editor;
    private UITrackpad count;

    public UIItemStackKeyframeFactory(Keyframe<ItemStack> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.editor = new UIItemStack((stack) ->
        {
            this.setValue(stack);
            this.updateCountFromStack(stack);
        });
        this.editor.setStack(keyframe.getValue());
        this.count = new UITrackpad((v) -> this.setCount(v.intValue()));
        this.count.limit(0, 999).integer().tooltip(UIKeys.ITEM_STACK_COUNT);
        this.updateCountFromStack(keyframe.getValue());

        this.scroll.add(this.editor, this.count);
    }

    private void setCount(int count)
    {
        ItemStack stack = this.keyframe.getValue();
        int clamped = Math.max(0, Math.min(999, count));

        if (stack == null || stack.isEmpty())
        {
            this.count.setValue(clamped);

            return;
        }

        int appliedCount = Math.max(1, clamped);

        ItemStack copy = stack.copy();

        copy.setCount(appliedCount);
        this.editor.setStack(copy);
        this.setValue(copy);
        this.count.setValue(clamped);
    }

    private void updateCountFromStack(ItemStack stack)
    {
        int value = stack == null || stack.isEmpty() ? 0 : Math.max(0, Math.min(999, stack.getCount()));

        this.count.setValue(value);
    }

    @Override
    public void update()
    {
        super.update();

        ItemStack stack = this.keyframe.getValue();

        this.editor.setStack(stack);
        this.updateCountFromStack(stack);
    }
}
