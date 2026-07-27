package mchorse.bbs_mod.ui.model;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.colors.Colors;

public final class UIModelUIStyles
{
    public static final int STRIP_WIDTH = 20;
    public static final int STRIP_BACKGROUND = Colors.A75;

    /** Darkest: left icon strip in the embedded form model editor */
    public static final int FORM_STRIP_BACKGROUND = 0xFF000000;

    public static int panelBackground()
    {
        return BBSSettings.deepSurface();
    }

    public static int viewportBackground()
    {
        return BBSSettings.deepSurface();
    }

    public static int formPanelBackground()
    {
        return BBSSettings.deepSurface();
    }

    public static int formViewportBackground()
    {
        return BBSSettings.deepSurface();
    }

    public static void renderWorkspaceSidePanels(UIContext context, int x, int y, int ey, int areaW, int sideMargin, int leftWidth, int rightWidth)
    {
        context.batcher.box(x + sideMargin - 2, y + 6, x + sideMargin + leftWidth + 2, ey - 6, formPanelBackground());

        int rx = x + areaW - sideMargin - rightWidth;
        context.batcher.box(rx - 2, y + 6, rx + rightWidth + 2, ey - 6, formPanelBackground());
    }

    private UIModelUIStyles()
    {}
}
