package mchorse.bbs_mod.ui.forms.editors.forms;

import mchorse.bbs_mod.forms.forms.FramebufferForm;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.forms.editors.panels.UIFramebufferFormPanel;
import mchorse.bbs_mod.ui.utils.icons.Icons;

public class UIFramebufferForm extends UIForm<FramebufferForm>
{
    public UIFramebufferForm()
    {
        super();

        this.defaultPanel = new UIFramebufferFormPanel(this);

        this.registerPanel(this.defaultPanel, IKey.raw("Framebuffer options"), Icons.CAMERA);
        this.registerDefaultPanels();
    }
}