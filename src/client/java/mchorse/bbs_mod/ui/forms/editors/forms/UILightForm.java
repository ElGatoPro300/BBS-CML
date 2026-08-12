package mchorse.bbs_mod.ui.forms.editors.forms;

import mchorse.bbs_mod.forms.forms.LightForm;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.panels.UILightFormPanel;
import mchorse.bbs_mod.ui.utils.icons.Icons;

public class UILightForm extends UIForm<LightForm>
{
    public UILightForm()
    {
        super();

        this.defaultPanel = new UILightFormPanel(this);

        this.registerPanel(this.defaultPanel, UIKeys.RAW_LIGHT, Icons.LIGHT);
        this.registerDefaultPanels();
    }
}
