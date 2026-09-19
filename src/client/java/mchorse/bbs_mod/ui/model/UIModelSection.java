package mchorse.bbs_mod.ui.model;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.cubic.model.ModelConfig;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.colors.Colors;

public abstract class UIModelSection extends UIElement
{
    public UILabel title;
    public UIElement fields;

    protected ModelConfig config;
    protected IUIModelPanelHost editor;

    public UIModelSection(IUIModelPanelHost editor)
    {
        super();

        this.editor = editor;
        this.title = UI.label(this.getTitle()).background(() -> Colors.A50 | BBSSettings.primaryColor.get());
        this.fields = new UIElement();
        this.fields.column().stretch().vertical().height(20);

        this.column().stretch().vertical();
        this.add(this.title, this.fields);
    }

    public abstract IKey getTitle();

    public void deselect()
    {}

    public void onBoneSelected(String bone)
    {}

    public void setConfig(ModelConfig config)
    {
        this.config = config;
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (this.title.area.isInside(context))
        {
            if (context.mouseButton == 0)
            {
                this.fields.toggleVisible();
                this.resize();
                this.getParent().resize();

                return true;
            }
        }

        return super.subMouseClicked(context);
    }
}
