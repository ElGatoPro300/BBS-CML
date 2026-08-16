package mchorse.bbs_mod.ui.film.controller;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.context.UIContextMenu;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UI;

/**
 * Pop-up for viewport gizmo line thickness ({@link BBSSettings#axesThickness}):
 * arrows, axes and rotation rings. Distance-based size still applies via {@code Gizmo}.
 */
public class UIGizmoThicknessContextMenu extends UIContextMenu
{
    public UITrackpad thickness;

    private UIElement column;

    public UIGizmoThicknessContextMenu()
    {
        this.thickness = new UITrackpad((v) -> BBSSettings.axesThickness.set(v.floatValue()));
        this.thickness.limit(BBSSettings.axesThickness).increment(0.1D).values(0.1D, 0.05D, 1D);
        this.thickness.setValue(BBSSettings.axesThickness.get());

        this.column = UI.column(5, 10,
            UI.label(UIKeys.FILM_GIZMO_THICKNESS),
            this.thickness
        );
        this.column.relative(this).w(140);

        this.add(this.column);
        this.column.resize();
    }

    @Override
    public boolean isEmpty()
    {
        return false;
    }

    @Override
    public void setMouse(UIContext context)
    {
        this.xy(context.mouseX(), context.mouseY())
            .wh(this.column.area.w, this.column.area.h)
            .bounds(context.menu.overlay, 5);
    }
}
