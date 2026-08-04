package mchorse.bbs_mod.ui.dashboard.textures;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.color.UIColorPicker;

import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/**
 * Inline-only color picker for texture editor side panel.
 * Unlike popup pickers, this widget never removes itself from parent.
 */
public class UITextureInlineColorPicker extends UIColorPicker
{
    public UITextureInlineColorPicker(Consumer<Integer> callback)
    {
        super(callback);

        this.favorite.setVisible(false);
        this.recent.setVisible(false);
        this.setupSize();
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (!this.area.isInside(context))
        {
            return false;
        }

        return super.subMouseClicked(context);
    }

    @Override
    protected boolean subKeyPressed(UIContext context)
    {
        if (context.isPressed(GLFW.GLFW_KEY_ESCAPE))
        {
            return true;
        }

        return super.subKeyPressed(context);
    }
}
