package mchorse.bbs_mod.ui.dashboard.utils;

import mchorse.bbs_mod.camera.OrbitCamera;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.utils.Area;

import java.util.function.Supplier;

public class UIOrbitCamera implements IUIElement
{
    public OrbitCamera orbit = new OrbitCamera();
    private boolean control;
    private boolean enabled = true;
    private Supplier<Area> viewportArea;

    public boolean canControl()
    {
        return this.control;
    }

    public boolean getControl()
    {
        return this.control;
    }

    public void setControl(boolean control)
    {
        this.control = control;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     * Restrict click-drag rotate/roll/FOV to the given area (e.g. the active panel's 3D
     * viewport), so an otherwise-unhandled click elsewhere in the UI (menu bar, empty space
     * in a docked panel, etc.) can't start it. Pass {@code null} to remove the restriction.
     */
    public void setViewportArea(Supplier<Area> viewportArea)
    {
        this.viewportArea = viewportArea;
    }

    private boolean isInsideViewportArea(UIContext context)
    {
        if (this.viewportArea == null)
        {
            return true;
        }

        Area area = this.viewportArea.get();

        return area != null && area.isInside(context);
    }

    @Override
    public IUIElement mouseClicked(UIContext context)
    {
        if (!this.isInsideViewportArea(context))
        {
            return null;
        }

        int i = this.orbit.canStart(context);

        if (i >= 0)
        {
            this.orbit.start(i, context.mouseX, context.mouseY);

            return this;
        }

        return null;
    }

    @Override
    public IUIElement mouseScrolled(UIContext context)
    {
        if (!this.control)
        {
            return null;
        }

        return this.orbit.scroll((int) context.mouseWheel) ? this : null;
    }

    @Override
    public IUIElement mouseReleased(UIContext context)
    {
        this.orbit.release();

        return null;
    }

    @Override
    public void render(UIContext context)
    {
        if (!this.control)
        {
            this.orbit.cache(context.mouseX, context.mouseY);

            return;
        }

        /* Opening a form palette / modal overlay stops key events, so WASD velocity would
         * otherwise keep applying until a release is seen. Freeze and clear control. */
        if (context.hasOverlayPanel() || context.hasFormPaletteOpen())
        {
            this.orbit.reset();
            this.orbit.release();
            this.orbit.cache(context.mouseX, context.mouseY);

            return;
        }

        this.orbit.drag(context.mouseX, context.mouseY);
        this.orbit.update(context);
    }

    /* Unimplemented GUI element methods */

    @Override
    public void resize()
    {}

    @Override
    public boolean isEnabled()
    {
        return this.enabled;
    }

    @Override
    public boolean isVisible()
    {
        return true;
    }

    @Override
    public IUIElement keyPressed(UIContext context)
    {
        return null;
    }

    @Override
    public IUIElement textInput(UIContext context)
    {
        return null;
    }

    @Override
    public boolean canBeRendered(Area area)
    {
        return true;
    }
}
