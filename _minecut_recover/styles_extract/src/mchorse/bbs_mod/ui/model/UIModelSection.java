package mchorse.bbs_mod.ui.model;

import mchorse.bbs_mod.cubic.model.ModelConfig;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.UIPoseSectionCollapse;
import mchorse.bbs_mod.utils.colors.Colors;

public abstract class UIModelSection extends UIElement
{
    public UIPoseSectionCollapse section;
    public UIPoseSectionCollapse.SectionHeader title;
    public UIElement fields;

    protected ModelConfig config;
    protected IUIModelPanelHost editor;

    public UIModelSection(IUIModelPanelHost editor)
    {
        super();

        this.editor = editor;
        this.fields = new UIElement();
        this.fields.column().stretch().vertical().height(20);

        this.section = new UIPoseSectionCollapse(this.getTitle(), Colors.ACTIVE, this.fields);
        this.title = this.section.getToggle();

        this.column().stretch().vertical();
        this.add(this.section);
        this.section.setExpanded(true);
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
}
