package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIItemStack;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import net.minecraft.class_1799;

public class UIItemStackKeyframeFactory extends UIKeyframeFactory<class_1799>
{
    private UIItemStack editor;
    private UITrackpad count;

    public UIItemStackKeyframeFactory(Keyframe<class_1799> keyframe, UIKeyframes editor)
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
        class_1799 stack = this.keyframe.getValue();
        int clamped = Math.max(0, Math.min(999, count));

        if (stack == null || stack.method_7960())
        {
            this.count.setValue(clamped);

            return;
        }

        int appliedCount = Math.max(1, clamped);

        class_1799 copy = stack.method_7972();

        copy.method_7939(appliedCount);
        this.editor.setStack(copy);
        this.setValue(copy);
        this.count.setValue(clamped);
    }

    private void updateCountFromStack(class_1799 stack)
    {
        int value = stack == null || stack.method_7960() ? 0 : Math.max(0, Math.min(999, stack.method_7947()));

        this.count.setValue(value);
    }

    @Override
    public void update()
    {
        super.update();

        class_1799 stack = this.keyframe.getValue();

        this.editor.setStack(stack);
        this.updateCountFromStack(stack);
    }
}
