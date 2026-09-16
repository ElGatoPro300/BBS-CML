package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.utils.UI;

public abstract class UIFormPanel <T extends Form> extends UIElement
{
    protected UIForm editor;
    protected T form;

    public UIScrollView options;

    public UIFormPanel(UIForm editor)
    {
        this.editor = editor;

        this.options = UI.scrollView(5, 10);
        this.options.scroll.cancelScrolling();
        this.options.full(this);

        this.add(this.options);
    }

    public void startEdit(T form)
    {
        this.form = form;
    }

    public void finishEdit()
    {}

    public void pickBone(String bone)
    {}
}