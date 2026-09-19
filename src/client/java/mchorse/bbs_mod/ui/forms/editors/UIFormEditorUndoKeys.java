package mchorse.bbs_mod.ui.forms.editors;

import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;

/**
 * Invisible overlay that captures Ctrl+Z and Ctrl+Y in form editor
 * so undo/redo has priority over other focused controls.
 */
public class UIFormEditorUndoKeys extends UIElement
{
    public UIFormEditorUndoKeys(UIFormEditor editor)
    {
        this.keys().ignoreFocus();
        this.keys().register(Keys.UNDO, editor::undo).category(UIKeys.CAMERA_EDITOR_KEYS_EDITOR_TITLE);
        this.keys().register(Keys.REDO, editor::redo).category(UIKeys.CAMERA_EDITOR_KEYS_EDITOR_TITLE);
        this.noCulling();
    }
}
