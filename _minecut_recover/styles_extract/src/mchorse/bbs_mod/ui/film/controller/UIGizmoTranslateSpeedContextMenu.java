package mchorse.bbs_mod.ui.film.controller;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.context.UIContextMenu;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UI;

/**
 * Small pop-up menu for {@link BBSSettings#gizmoTranslateSpeed}: how fast objects move when
 * dragging gizmo translate handles.
 */
public class UIGizmoTranslateSpeedContextMenu extends UIContextMenu
{
    public UITrackpad speed;

    private UIElement column;

    public UIGizmoTranslateSpeedContextMenu()
    {
        this.speed = new UITrackpad((v) -> BBSSettings.gizmoTranslateSpeed.set(v.intValue()));
        this.speed.limit(BBSSettings.gizmoTranslateSpeed).integer().increment(1D).values(1D, 1D, 5D);
        this.speed.setValue(BBSSettings.gizmoTranslateSpeed.get());

        this.column = UI.column(5, 10,
            UI.label(UIKeys.FILM_GIZMO_TRANSLATE_SPEED),
            this.speed
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
