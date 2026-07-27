package mchorse.bbs_mod.ui.film.clips.modules;

import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.framework.elements.UIElement;

public abstract class UIAbstractModule extends UIElement
{
    protected IUIClipsDelegate editor;

    public UIAbstractModule(IUIClipsDelegate editor)
    {
        super();

        this.editor = editor;
    }
}