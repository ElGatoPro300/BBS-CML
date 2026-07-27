package mchorse.bbs_mod.ui.film.controller;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.UIFilmPreview;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.context.UIContextMenu;
import mchorse.bbs_mod.ui.utils.UI;

public class UIViewportHideContextMenu extends UIContextMenu
{
    private final UIElement column;

    public UIViewportHideContextMenu(UIFilmPreview preview)
    {
        UIToggle hideAll = new UIToggle(UIKeys.FILM_HIDE_ALL, preview.isViewportButtonsHidden(), (b) ->
        {
            preview.setViewportButtonsHidden(b.getValue());
        });
        hideAll.w(1F);

        UIToggle hideCameraPreview = new UIToggle(UIKeys.FILM_HIDE_CAMERA_PREVIEW, !BBSSettings.recordingCameraPreview.get(), (b) ->
        {
            BBSSettings.recordingCameraPreview.set(!b.getValue());
        });
        hideCameraPreview.w(1F);

        this.column = UI.column(5, 8,
            UI.label(UIKeys.FILM_HIDE_MENU_TITLE),
            hideAll,
            hideCameraPreview
        );
        this.column.relative(this).w(180);

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
