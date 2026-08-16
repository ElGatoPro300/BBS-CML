package mchorse.bbs_mod.ui.forms.editors.forms;

import mchorse.bbs_mod.forms.forms.AnchorForm;

public class UIAnchorForm extends UIForm<AnchorForm>
{
    public UIAnchorForm()
    {
        super();

        this.registerDefaultPanels();

        this.defaultPanel = this.panels.get(0);

        if (this.generalPanel != null)
        {
            this.generalPanel.transform.setLocalMode(true);
        }
    }
}