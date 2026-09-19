package mchorse.bbs_mod.ui.forms.editors.forms;

import mchorse.bbs_mod.forms.forms.FluidForm;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.panels.UIFluidFormPanel;
import mchorse.bbs_mod.ui.utils.icons.Icons;

public class UIFluidForm extends UIForm<FluidForm>
{
    private UIFluidFormPanel fluidFormPanel;

    public UIFluidForm()
    {
        super();

        this.fluidFormPanel = new UIFluidFormPanel(this);
        this.defaultPanel = this.fluidFormPanel;

        this.registerPanel(this.defaultPanel, UIKeys.FORMS_EDITORS_FLUID_TITLE, Icons.MATERIAL);
        this.registerDefaultPanels();
    }
}
