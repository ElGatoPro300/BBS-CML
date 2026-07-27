package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.elements.UIElement;

/**
 * Applies flex layout for a timeline toolbar and its content siblings based on
 * the active dock edge.
 */
public final class TimelineToolbarDockLayout
{
    public static final String PANEL_CAMERA = "cameraTimeline";
    public static final String PANEL_ACTION = "actionTimeline";
    public static final String PANEL_REPLAY = "replayTimeline";
    public static final String PANEL_SCREEN_NODE_VIEW = "screenNodeView";

    public static void setup(UIElement host, TimelineToolbar toolbar, String panelId, UIElement... contents)
    {
        TimelineToolbarDock dock = BBSSettings.timelineToolbarDocks.getDock(panelId);

        toolbar.configureDockHost(host, panelId, () -> apply(host, toolbar, toolbar.getDock(), contents));
        apply(host, toolbar, dock, contents);
    }

    public static void apply(UIElement host, TimelineToolbar toolbar, TimelineToolbarDock dock, UIElement... contents)
    {
        apply(host, toolbar, dock, 0, contents);
    }

    public static void apply(UIElement host, TimelineToolbar toolbar, TimelineToolbarDock dock, int extraRightInset, UIElement... contents)
    {
        if (!TimelineToolbarSettings.isEnabled())
        {
            toolbar.closeOpenMenu();
            toolbar.cancelDockDrag();
            toolbar.setVisible(false);

            for (UIElement content : contents)
            {
                if (content == null)
                {
                    continue;
                }

                content.resetFlex().relative(host);
                content.x(0).y(0).w(1F, -extraRightInset).h(1F);
            }

            host.resize();

            return;
        }

        toolbar.setVisible(true);
        toolbar.setDock(dock);

        int thickness = TimelineToolbarSettings.getThickness(dock);

        toolbar.resetFlex().relative(host);

        switch (dock)
        {
            case TOP:
                toolbar.x(0).y(0).w(1F).h(thickness);
                break;
            case BOTTOM:
                toolbar.x(0).y(1F, -thickness).w(1F).h(thickness);
                break;
            case LEFT:
                toolbar.x(0).y(0).w(thickness).h(1F);
                break;
            case RIGHT:
                toolbar.x(1F, -thickness).y(0).w(thickness).h(1F);
                break;
        }

        for (UIElement content : contents)
        {
            if (content == null)
            {
                continue;
            }

            content.resetFlex().relative(host);

            switch (dock)
            {
                case TOP:
                    content.x(0).y(thickness).w(1F, -extraRightInset).h(1F, -thickness);
                    break;
                case BOTTOM:
                    content.x(0).y(0).w(1F, -extraRightInset).h(1F, -thickness);
                    break;
                case LEFT:
                    content.x(thickness).y(0).w(1F, -thickness - extraRightInset).h(1F);
                    break;
                case RIGHT:
                    content.x(0).y(0).w(1F, -thickness - extraRightInset).h(1F);
                    break;
            }
        }

        toolbar.ensureDockOverlayOnTop();
        host.resize();
    }

    public static TimelineToolbarDock findDockFor(UIElement element)
    {
        UIElement current = element;

        while (current != null)
        {
            for (TimelineToolbar toolbar : current.getChildren(TimelineToolbar.class))
            {
                return toolbar.getDock();
            }

            current = current.getParent();
        }

        return TimelineToolbarDock.BOTTOM;
    }

    private TimelineToolbarDockLayout()
    {}
}
