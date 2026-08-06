package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIItemStack;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import net.minecraft.item.ItemStack;

public class UIItemStackKeyframeFactory extends UIKeyframeFactory<ItemStack>
{
    private UIItemStack editor;

    public UIItemStackKeyframeFactory(Keyframe<ItemStack> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.editor = new UIItemStack(this::setValue);
        this.editor.setStack(keyframe.getValue());

        this.scroll.add(this.editor);
    }
}