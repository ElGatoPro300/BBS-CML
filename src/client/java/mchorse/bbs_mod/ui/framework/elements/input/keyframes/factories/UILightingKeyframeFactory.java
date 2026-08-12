package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditorUtils;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.utils.UIBezierHandles;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

/**
 * Form lighting float track: animatable world-lighting influence plus a step/hold
 * natural-world flag.
 * <p>
 * Stored/engine float: {@code 1} = world lighting, {@code 0} = full bright.
 * The trackpad edits brightness ({@code 1 - engine}) so raising the value brightens.
 */
public class UILightingKeyframeFactory extends UIKeyframeFactory<Float>
{
    private UITrackpad value;
    private UIToggle natural;
    private UIBezierHandles handles;

    public UILightingKeyframeFactory(Keyframe<Float> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.value = new UITrackpad((v) -> this.setEngineValue(fromBrightness(v.floatValue())));
        this.value.limit(0D, 1D);
        this.value.tooltip(UIKeys.FILM_REPLAY_TRACK_LIGHTING_VALUE_TOOLTIP);
        this.registerValueTrackpad(this.value);

        this.natural = new UIToggle(UIKeys.FILM_REPLAY_TRACK_LIGHTING_NATURAL, (b) ->
        {
            this.setNaturalLighting(b.getValue());
            this.updateValueEnabled();
        });
        this.natural.tooltip(UIKeys.FILM_REPLAY_TRACK_LIGHTING_NATURAL_TOOLTIP);
        this.natural.setValue(keyframe.isNaturalLighting());

        this.handles = new UIBezierHandles(keyframe);

        this.scroll.add(this.value, this.natural, this.handles.createColumn());
        this.syncValueFromKeyframe();
        this.updateValueEnabled();
    }

    @Override
    public void update()
    {
        super.update();

        if (!this.value.isActivelyEditing() && !this.value.isDragging())
        {
            this.syncValueFromKeyframe();
        }

        this.natural.setValue(this.keyframe.isNaturalLighting());
        this.updateValueEnabled();
        this.handles.setKeyframe(this.keyframe);
        this.handles.update();
    }

    private void syncValueFromKeyframe()
    {
        Float engine = this.keyframe.getValue();
        float amount = engine == null ? 1F : engine;

        this.value.setValue(toBrightness(amount));
    }

    private void updateValueEnabled()
    {
        this.value.setEnabled(!this.keyframe.isNaturalLighting());
    }

    private void setEngineValue(float engineAmount)
    {
        this.setValue(MathUtils.clamp(engineAmount, 0F, 1F));
    }

    private void setNaturalLighting(boolean value)
    {
        boolean[] applied = {false};

        UIReplaysEditorUtils.forEachSelectedKeyframe(this.editor, this.keyframe, (selected) ->
        {
            applied[0] = true;
            selected.setNaturalLighting(value);
        });

        if (!applied[0])
        {
            this.keyframe.setNaturalLighting(value);
        }
    }

    /** Engine world-lighting influence → UI brightness (higher = brighter). */
    private static float toBrightness(float engineAmount)
    {
        return 1F - MathUtils.clamp(engineAmount, 0F, 1F);
    }

    /** UI brightness → engine world-lighting influence. */
    private static float fromBrightness(float brightness)
    {
        return 1F - MathUtils.clamp(brightness, 0F, 1F);
    }
}
