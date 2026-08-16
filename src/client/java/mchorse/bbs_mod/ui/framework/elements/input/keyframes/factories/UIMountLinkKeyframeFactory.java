package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.film.replays.MountLink;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class UIMountLinkKeyframeFactory extends UIKeyframeFactory<MountLink>
{
    private final boolean riddenMode;
    private UIToggle active;
    private UIButton actor;

    public UIMountLinkKeyframeFactory(Keyframe<MountLink> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        UIKeyframeSheet sheet = editor.getGraph().getSheet(keyframe);
        this.riddenMode = sheet != null && "ridden".equals(sheet.id);

        this.active = new UIToggle(this.riddenMode ? UIKeys.GENERIC_KEYFRAMES_MOUNT_HAS_RIDER : UIKeys.GENERIC_KEYFRAMES_MOUNT_SIT, (b) -> this.setActive(b.getValue()));
        this.active.setValue(keyframe.getValue().active);

        if (this.riddenMode)
        {
            this.actor = new UIButton(UIKeys.GENERIC_KEYFRAMES_MOUNT_PICK_RIDER, (b) -> this.displayActors());
            this.scroll.add(this.active, this.actor);
        }
        else
        {
            this.scroll.add(this.active);
        }
    }

    private void displayActors()
    {
        UIAnchorKeyframeFactory.displayActors(this.getContext(), this.getPanel().getController().getEntities(), this.keyframe.getValue().replay, this::setReplay);
    }

    private void setActive(boolean active)
    {
        BaseValue.edit(this.keyframe, (value) ->
        {
            value.getValue().active = active;
            value.getValue().replay = MountLink.NO_REPLAY;
        });
    }

    private void setReplay(int replay)
    {
        BaseValue.edit(this.keyframe, (value) ->
        {
            value.getValue().replay = replay;
            value.getValue().active = replay >= 0;
        });

        this.active.setValue(this.keyframe.getValue().active);
    }

    private UIFilmPanel getPanel()
    {
        return this.getParent(UIFilmPanel.class);
    }
}
