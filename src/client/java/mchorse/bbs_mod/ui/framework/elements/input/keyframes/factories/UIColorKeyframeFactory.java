package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditorUtils;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

import java.util.function.Consumer;

/**
 * Generic color keyframe editor (clips, boss bar presets, etc.).
 * Form Color track uses {@link UIFormColorKeyframeFactory}.
 */
public class UIColorKeyframeFactory extends UIKeyframeFactory<Color>
{
    private UIColor color;
    private UIToggle spectrum;

    public UIColorKeyframeFactory(Keyframe<Color> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.color = new UIColor((c) -> this.applyColorEdit((color) ->
        {
            Color value = Color.rgba(c);

            color.set(value.r, value.g, value.b, value.a);
        }));
        this.color.setColor(keyframe.getValue().getARGBColor());
        this.color.withAlpha();

        this.spectrum = new UIToggle(UIKeys.GENERIC_KEYFRAMES_COLOR_SPECTRUM, (b) -> this.setSpectrum(b.getValue()));
        this.spectrum.tooltip(UIKeys.GENERIC_KEYFRAMES_COLOR_SPECTRUM_TOOLTIP);
        this.spectrum.setValue(keyframe.isSpectrum());

        this.scroll.add(this.color.marginTop(8));
        this.scroll.add(this.spectrum);

        this.update();
    }

    @Override
    public void update()
    {
        super.update();

        Color value = this.keyframe.getValue() == null ? Color.white() : this.keyframe.getValue();

        this.color.setColor(value.getARGBColor());
        this.spectrum.setValue(this.keyframe.isSpectrum());
    }

    protected void applyColorEdit(Consumer<Color> editor)
    {
        boolean[] applied = {false};

        UIReplaysEditorUtils.forEachSelectedKeyframe(this.editor, this.keyframe, (selected) ->
        {
            applied[0] = true;

            Color color = (Color) selected.getValue();

            if (color == null)
            {
                color = Color.white();
                selected.setValue(color);
            }

            selected.preNotify();
            editor.accept(color);
            selected.postNotify();
        });

        if (!applied[0])
        {
            Color color = this.keyframe.getValue();

            if (color == null)
            {
                color = Color.white();
                this.keyframe.setValue(color);
            }

            this.keyframe.preNotify();
            editor.accept(color);
            this.keyframe.postNotify();
        }
    }

    private void setSpectrum(boolean value)
    {
        boolean[] applied = {false};

        UIReplaysEditorUtils.forEachSelectedKeyframe(this.editor, this.keyframe, (selected) ->
        {
            applied[0] = true;
            selected.setSpectrum(value);
        });

        if (!applied[0])
        {
            this.keyframe.setSpectrum(value);
        }
    }
}
