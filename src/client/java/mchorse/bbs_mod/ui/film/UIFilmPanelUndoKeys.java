package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;

/**
 * Invisible overlay that captures Ctrl+Z and Ctrl+Y for undo/redo
 * so they work on top of all other keybinds in the film panel (e.g. Y for body fix toggle).
 */
public class UIFilmPanelUndoKeys extends UIElement
{
    public UIFilmPanelUndoKeys(UIFilmPanel panel)
    {
        this.keys().ignoreFocus();
        this.keys().register(Keys.UNDO, panel::undo).category(UIKeys.CAMERA_EDITOR_KEYS_EDITOR_TITLE);
        this.keys().register(Keys.REDO, panel::redo).category(UIKeys.CAMERA_EDITOR_KEYS_EDITOR_TITLE);
        this.noCulling();
    }
}
