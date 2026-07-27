package mchorse.bbs_mod.ui.forms;

import mchorse.bbs_mod.ui.framework.elements.events.UIEvent;

public class UIToggleEditorEvent extends UIEvent<UIFormPalette>
{
    /**
     * Whether form will be edited
     */
    public final boolean editing;

    public UIToggleEditorEvent(UIFormPalette element, boolean editing)
    {
        super(element);

        this.editing = editing;
    }
}