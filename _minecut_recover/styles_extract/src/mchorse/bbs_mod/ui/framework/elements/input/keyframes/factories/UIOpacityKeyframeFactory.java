package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditorUtils;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.events.UITrackpadDragEndEvent;
import mchorse.bbs_mod.ui.framework.elements.events.UITrackpadDragStartEvent;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

/**
 * Opacity track: numeric AA byte (0–255) mapped to internal {@code #AAffffff} color-code alpha,
 * plus an on/off No shading toggle.
 */
public class UIOpacityKeyframeFactory extends UIKeyframeFactory<Float>
{
    private UITrackpad opacityByte;
    private UIToggle noShading;
    private boolean filling;

    public UIOpacityKeyframeFactory(Keyframe<Float> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.opacityByte = new UITrackpad((v) ->
        {
            if (this.filling)
            {
                return;
            }

            this.setOpacityByte(MathUtils.clamp(v.intValue(), 0, 255));
        });
        this.opacityByte.integer().limit(0, 255).plainFormat().values(1D, 1D, 16D);
        this.opacityByte.tooltip(UIKeys.FILM_REPLAY_TRACK_OPACITY);
        this.opacityByte.getEvents().register(UITrackpadDragStartEvent.class, (e) -> this.editor.cacheKeyframes());
        this.opacityByte.getEvents().register(UITrackpadDragEndEvent.class, (e) -> this.editor.submitKeyframes());

        this.noShading = new UIToggle(UIKeys.FILM_REPLAY_OPACITY_NO_SHADING, (b) ->
        {
            if (this.filling)
            {
                return;
            }

            this.setNoshadingOpacity(b.getValue());
        });
        this.noShading.tooltip(UIKeys.FORMS_EDITORS_COLOR_NOSHADING_OPACITY_TOOLTIP);

        this.scroll.add(UI.label(UIKeys.FILM_REPLAY_TRACK_OPACITY).marginTop(4));
        this.scroll.add(this.opacityByte);
        this.scroll.add(this.noShading.marginTop(8));

        this.update();
    }

    @Override
    public void update()
    {
        super.update();

        int aa = MathUtils.clamp(Math.round(this.getOpacity() * 255F), 0, 255);

        this.filling = true;

        try
        {
            this.opacityByte.setValue(aa);
            this.noShading.setValue(this.keyframe.isNoshadingOpacity());
        }
        finally
        {
            this.filling = false;
        }
    }

    private float getOpacity()
    {
        Float value = this.keyframe.getValue();

        return value == null ? 1F : MathUtils.clamp(value, 0F, 1F);
    }

    private void setOpacityByte(int aa)
    {
        /* Internal color-code alpha (#AAffffff); UI only shows the AA byte. 0 = fully invisible. */
        float snapped = MathUtils.clamp(aa, 0, 255) / 255F;
        boolean[] applied = {false};

        UIReplaysEditorUtils.forEachSelectedKeyframe(this.editor, this.keyframe, (selected) ->
        {
            applied[0] = true;
            selected.preNotify();
            selected.setValue(snapped);
            selected.postNotify();
        });

        if (!applied[0])
        {
            this.keyframe.preNotify();
            this.keyframe.setValue(snapped);
            this.keyframe.postNotify();
        }

        this.filling = true;

        try
        {
            this.opacityByte.setValue(aa);
        }
        finally
        {
            this.filling = false;
        }
    }

    private void setNoshadingOpacity(boolean value)
    {
        boolean[] applied = {false};

        UIReplaysEditorUtils.forEachSelectedKeyframe(this.editor, this.keyframe, (selected) ->
        {
            applied[0] = true;
            selected.setNoshadingOpacity(value);
        });

        if (!applied[0])
        {
            this.keyframe.setNoshadingOpacity(value);
        }
    }
}
