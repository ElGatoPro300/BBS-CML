package mchorse.bbs_mod.ui.dashboard.utils;

import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.utils.Area;

import java.util.function.Supplier;

public class UIOrbitCameraKeys implements IUIElement
{
    private UIDashboard dashboard;
    private Supplier<Boolean> enabled;

    public UIOrbitCameraKeys(UIDashboard dashboard)
    {
        this.dashboard = dashboard;
    }

    public void setEnabled(Supplier<Boolean> enabled)
    {
        this.enabled = enabled;
    }

    @Override
    public void resize()
    {}

    @Override
    public boolean isEnabled()
    {
        boolean enabled = true;

        if (this.enabled != null)
        {
            enabled = this.enabled.get();
        }

        return enabled && this.dashboard.orbitUI.isEnabled();
    }

    @Override
    public boolean isVisible()
    {
        return true;
    }

    @Override
    public IUIElement mouseClicked(UIContext context)
    {
        return null;
    }

    @Override
    public IUIElement mouseScrolled(UIContext context)
    {
        return null;
    }

    @Override
    public IUIElement mouseReleased(UIContext context)
    {
        return null;
    }

    @Override
    public IUIElement keyPressed(UIContext context)
    {
        if (context.isFocused())
        {
            return null;
        }

        if (this.dashboard.getPanels().panel instanceof IUIOrbitKeysHandler handler && handler.handleKeyPressed(context))
        {
            return this;
        }

        return this.dashboard.orbitUI.getControl() && this.dashboard.orbit.keyPressed(context) ? this : null;
    }

    @Override
    public IUIElement textInput(UIContext context)
    {
        return null;
    }

    @Override
    public boolean canBeRendered(Area viewport)
    {
        return false;
    }

    @Override
    public void render(UIContext context)
    {}
}