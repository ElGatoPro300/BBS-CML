package mchorse.bbs_mod.ui.forms.editors;

import mchorse.bbs_mod.ui.forms.IUIFormList;
import mchorse.bbs_mod.ui.forms.UIFormList;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.EventPropagation;
import mchorse.bbs_mod.utils.colors.Colors;

import org.lwjgl.glfw.GLFW;

public class UIFormEditorList extends UIFormList
{
    public UIFormEditorList(IUIFormList palette)
    {
        super(palette);

        this.edit.removeFromParent();
        this.mouseEventPropagataion(EventPropagation.BLOCK_INSIDE).keyboardEventPropagataion(EventPropagation.PASS).markContainer();
    }

    @Override
    public boolean subKeyPressed(UIContext context)
    {
        if (context.isPressed(GLFW.GLFW_KEY_ESCAPE))
        {
            this.palette.exit();

            return true;
        }

        return super.subKeyPressed(context);
    }

    @Override
    public void render(UIContext context)
    {
        this.area.render(context.batcher, Colors.A50);

        super.render(context);
    }
}