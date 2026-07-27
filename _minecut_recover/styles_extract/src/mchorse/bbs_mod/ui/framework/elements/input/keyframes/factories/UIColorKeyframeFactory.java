package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.film.replays.UIReplaysEditorUtils;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

import java.util.function.Consumer;

/**
 * Simple color keyframe editor (clips such as Letterbox / Eye / Vignette).
 * Form Color track uses {@link UIFormColorKeyframeFactory}.
 */
public class UIColorKeyframeFactory extends UIKeyframeFactory<Color>
{
    private UIColor color;

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

        this.scroll.add(this.color.marginTop(8));

        this.update();
    }

    @Override
    public void update()
    {
        super.update();

        Color value = this.keyframe.getValue() == null ? Color.white() : this.keyframe.getValue();

        this.color.setColor(value.getARGBColor());
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
}
