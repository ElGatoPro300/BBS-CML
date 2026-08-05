package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

public class UIVisibleKeyframeFactory extends UIKeyframeFactory<Boolean>
{
    private UIToggle visibleToggle;
    private UIToggle renderToggle;
    private KeyframeChannel<Boolean> renderChannel;
    private float lastSyncedTick = Float.NaN;

    public UIVisibleKeyframeFactory(Keyframe<Boolean> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        UIKeyframeSheet sheet = editor.getGraph().getSheet(keyframe);

        this.renderChannel = sheet == null
            ? null
            : UIVisibleRenderKeyframeUtils.getRenderChannel(sheet.channel, FormUtils.getForm(sheet.property));

        /* Visible/render are step tracks — duration/interp only shift getClosest() mid-segment. */
        this.interp.setVisible(false);
        this.duration.setVisible(false);
        this.sanitizeDiscreteKeyframe(keyframe);

        this.visibleToggle = new UIToggle(UIKeys.FILM_REPLAY_TRACK_VISIBLE_ENABLED, (b) -> this.setValue(b.getValue()));
        this.visibleToggle.tooltip(UIKeys.FILM_REPLAY_TRACK_VISIBLE_ENABLED_TOOLTIP);
        this.visibleToggle.setValue(keyframe.getValue() == null || keyframe.getValue());

        this.renderToggle = new UIToggle(UIKeys.FILM_REPLAY_TRACK_RENDER_ENABLED, (b) ->
        {
            UIVisibleRenderKeyframeUtils.setRenderValue(this.editor, this.renderChannel, this.keyframe.getTick(), b.getValue());
        });
        this.renderToggle.tooltip(UIKeys.FILM_REPLAY_TRACK_RENDER_ENABLED_TOOLTIP);
        this.renderToggle.setValue(UIVisibleRenderKeyframeUtils.getRenderValue(this.renderChannel, keyframe.getTick()));

        this.scroll.add(this.visibleToggle);

        if (this.renderChannel != null)
        {
            this.scroll.add(this.renderToggle);
        }

        this.lastSyncedTick = keyframe.getTick();
    }

    @Override
    public void update()
    {
        super.update();

        this.sanitizeDiscreteKeyframe(this.keyframe);

        Boolean value = this.keyframe.getValue();

        this.visibleToggle.setValue(value == null || value);

        float tick = this.keyframe.getTick();

        if (this.renderChannel != null)
        {
            if (!Float.isNaN(this.lastSyncedTick) && Math.abs(this.lastSyncedTick - tick) > 0.0001F)
            {
                UIVisibleRenderKeyframeUtils.moveRenderKeyframe(this.editor, this.renderChannel, this.lastSyncedTick, tick);
            }

            this.renderToggle.setValue(UIVisibleRenderKeyframeUtils.getRenderValue(this.renderChannel, tick));
            this.lastSyncedTick = tick;
        }
    }

    private void sanitizeDiscreteKeyframe(Keyframe<Boolean> keyframe)
    {
        if (keyframe == null)
        {
            return;
        }

        if (keyframe.getDuration() != 0F)
        {
            keyframe.setDuration(0F);
        }

        if (keyframe.getInterpolation().getInterp() != Interpolations.CONST)
        {
            keyframe.getInterpolation().setInterp(Interpolations.CONST);
        }
    }
}
