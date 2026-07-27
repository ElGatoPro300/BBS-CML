package mchorse.bbs_mod.ui.film.clips.actions;

import mchorse.bbs_mod.actions.types.ActionClip;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.clips.UIClip;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UI;

public abstract class UIActionClip <T extends ActionClip> extends UIClip<T>
{
    public UITrackpad frequency;

    public UIActionClip(T clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.frequency = new UITrackpad((v) -> this.editor.editMultiple(this.clip.frequency, (frequency) -> frequency.set(v.intValue())));
        this.frequency.limit(0).integer();
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(UI.label(UIKeys.ACTIONS_FREQUENCY).marginTop(6), this.frequency);
    }

    @Override
    protected void addEnvelopes()
    {}

    @Override
    public void fillData()
    {
        super.fillData();

        this.frequency.setValue(this.clip.frequency.get());
    }
}