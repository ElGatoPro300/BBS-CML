package mchorse.bbs_mod.ui.dashboard.panels;

import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.UIRenderable;
import mchorse.bbs_mod.utils.colors.Colors;

public abstract class UISidebarDashboardPanel extends UIDashboardPanel
{
    public UIElement iconBar;
    public UIElement editor;

    protected boolean update;

    public UISidebarDashboardPanel(UIDashboard dashboard)
    {
        super(dashboard);

        this.iconBar = new UIElement();
        this.iconBar.relative(this).x(1F, -20).w(20).h(1F).column(0).stretch();

        this.editor = new UIElement();
        this.editor.relative(this).wTo(this.iconBar.area).h(1F);

        this.add(new UIRenderable(this::renderBackground), this.iconBar, this.editor);
    }

    @Override
    public void open()
    {
        super.open();

        this.update = true;
    }

    @Override
    public void appear()
    {
        super.appear();

        if (this.update)
        {
            this.update = false;

            this.requestNames();
        }
    }

    public abstract void requestNames();

    protected void renderBackground(UIContext context)
    {
        if (this.iconBar.isVisible())
        {
            this.iconBar.area.render(context.batcher, Colors.A50);
            context.batcher.gradientHBox(this.iconBar.area.x - 6, this.iconBar.area.y, this.iconBar.area.x, this.iconBar.area.ey(), 0, 0x29000000);
        }
    }
}