package mchorse.bbs_mod.ui.framework.elements.buttons;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.utils.UIUtils;

import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public abstract class UIClickable <T> extends UIElement
{
    public Consumer<T> callback;

    protected boolean hover;
    protected boolean pressed;

    public UIClickable(Consumer<T> callback)
    {
        super();

        this.callback = callback;
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (this.isAllowed(context.mouseButton) && this.area.isInside(context))
        {
            this.pressed = true;

            return true;
        }

        return super.subMouseClicked(context);
    }

    protected boolean isAllowed(int mouseButton)
    {
        return mouseButton == 0;
    }

    protected void click(int mouseButton)
    {
        if (this.callback != null)
        {
            this.callback.accept(this.get());
        }
    }

    protected abstract T get();

    @Override
    public boolean subMouseReleased(UIContext context)
    {
        /* Only fire when this element also received the matching mouse press. Mouse
           releases are broadcast to the whole element tree (to reliably end drags), so
           without this check a button would trigger even when the press was consumed
           by an element rendered on top of it (e.g. a floating panel window). */
        if (this.pressed && this.isAllowed(context.mouseButton) && this.area.isInside(context))
        {
            UIUtils.playClick();
            this.click(context.mouseButton);
            this.pressed = false;

            return true;
        }

        this.pressed = false;

        return super.subMouseReleased(context);
    }

    @Override
    public void render(UIContext context)
    {
        this.hover = this.area.isInside(context);

        if (this.hover)
        {
            context.requestCursor(GLFW.GLFW_HAND_CURSOR);
        }

        this.renderSkin(context);
        super.render(context);
    }

    protected abstract void renderSkin(UIContext context);
}