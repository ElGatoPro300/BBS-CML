package mchorse.bbs_mod.ui.forms.editors.forms;

import mchorse.bbs_mod.forms.forms.ShapeForm;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.panels.UIShapeFormPanel;
import mchorse.bbs_mod.ui.utils.icons.Icons;

public class UIShapeForm extends UIForm<ShapeForm>
{
    private UIShapeFormPanel shapeFormPanel;

    public UIShapeForm()
    {
        super();

        this.shapeFormPanel = new UIShapeFormPanel(this);
        this.defaultPanel = this.shapeFormPanel;

        this.registerPanel(this.defaultPanel, UIKeys.FORMS_EDITORS_BILLBOARD_TITLE, Icons.GEAR);
        this.registerDefaultPanels();
    }
}
