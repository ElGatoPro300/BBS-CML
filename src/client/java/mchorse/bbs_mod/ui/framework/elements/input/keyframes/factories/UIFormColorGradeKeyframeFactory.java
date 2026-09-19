package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditorUtils;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIFormColorAdjustments;
import mchorse.bbs_mod.ui.framework.elements.events.UITrackpadDragEndEvent;
import mchorse.bbs_mod.ui.framework.elements.events.UITrackpadDragStartEvent;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import java.util.List;
import java.util.function.Consumer;

/**
 * Nested Color grade track: brightness / contrast / hue / saturation (+ transforms)
 * on an independent {@code color_grade} channel (sibling of Color).
 */
public class UIFormColorGradeKeyframeFactory extends UIKeyframeFactory<Color>
{
    private UIFormColorAdjustments blendAdjustments;

    public UIFormColorGradeKeyframeFactory(Keyframe<Color> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.blendAdjustments = new UIFormColorAdjustments(
            () -> this.getOrCreateColor(this.keyframe.getValue()),
            (color) -> this.applyColorEdit((target) ->
            {
                target.brightness = color.brightness;
                target.contrast = color.contrast;
                target.hue = color.hue;
                target.saturation = color.saturation;
                target.brightnessTransform = color.brightnessTransform == null ? new EffectTransform() : color.brightnessTransform.copy();
                target.contrastTransform = color.contrastTransform == null ? new EffectTransform() : color.contrastTransform.copy();
                target.hueTransform = color.hueTransform == null ? new EffectTransform() : color.hueTransform.copy();
                target.saturationTransform = color.saturationTransform == null ? new EffectTransform() : color.saturationTransform.copy();
            }),
            false
        );
        this.wireUndo(this.blendAdjustments.brightness);
        this.wireUndo(this.blendAdjustments.contrast);
        this.wireUndo(this.blendAdjustments.hue);
        this.wireUndo(this.blendAdjustments.saturation);
        this.blendAdjustments.registerUndo(editor);
        this.blendAdjustments.wireResetThisValue(this::wireResetThisValue);

        this.scroll.add(this.blendAdjustments);

        this.context((menu) ->
        {
            menu.action(Icons.REFRESH, UIKeys.FORMS_EDITORS_COLOR_RESET_GRADE, this::resetColorGrade);
        });

        this.update();
    }

    private void wireUndo(UITrackpad trackpad)
    {
        trackpad.getEvents().register(UITrackpadDragStartEvent.class, (e) -> this.editor.cacheKeyframes());
        trackpad.getEvents().register(UITrackpadDragEndEvent.class, (e) -> this.editor.submitKeyframes());
    }

    private void wireResetThisValue(UITrackpad trackpad, Runnable reset)
    {
        trackpad.context((menu) -> menu.action(Icons.REFRESH, UIKeys.FORMS_EDITORS_COLOR_RESET_THIS_VALUE, () ->
        {
            if (this.editor != null)
            {
                this.editor.cacheKeyframes();
            }

            reset.run();

            if (this.editor != null)
            {
                this.editor.submitKeyframes();
            }

            this.update();
        }));
    }

    private void resetColorGrade()
    {
        this.blendAdjustments.resetGrade();
        this.update();
    }

    @Override
    public void update()
    {
        super.update();

        this.syncLiveColorKeyframe();
        this.blendAdjustments.syncFromForm();
    }

    @SuppressWarnings("unchecked")
    private void syncLiveColorKeyframe()
    {
        if (this.editor == null || this.keyframe == null)
        {
            return;
        }

        KeyframeChannel channel = this.keyframe.getParent() instanceof KeyframeChannel
            ? (KeyframeChannel) this.keyframe.getParent()
            : null;
        UIKeyframeSheet gradeSheet = null;

        for (UIKeyframeSheet sheet : this.editor.getGraph().getSheets())
        {
            if (channel != null && sheet.channel != channel)
            {
                continue;
            }

            if (sheet.channel.getFactory() != KeyframeFactories.COLOR)
            {
                continue;
            }

            String name = StringUtils.fileName(sheet.id);

            if (name.equals("color_grade"))
            {
                gradeSheet = sheet;
                break;
            }

            if (gradeSheet == null)
            {
                gradeSheet = sheet;
            }
        }

        if (gradeSheet == null)
        {
            return;
        }

        List selected = gradeSheet.selection.getSelected();

        if (!selected.isEmpty())
        {
            this.keyframe = (Keyframe<Color>) selected.get(0);

            return;
        }

        float tick = this.keyframe.getTick();

        for (Object kfObj : gradeSheet.channel.getKeyframes())
        {
            Keyframe<?> kf = (Keyframe<?>) kfObj;

            if (Math.abs(kf.getTick() - tick) < 0.001F && kf.getValue() instanceof Color)
            {
                this.keyframe = (Keyframe<Color>) kf;

                return;
            }
        }
    }

    private Color getOrCreateColor(Color color)
    {
        if (color == null)
        {
            color = Color.white();
        }

        if (color.transform == null)
        {
            color.transform = new EffectTransform();
        }

        return color;
    }

    @SuppressWarnings("unchecked")
    private void applyColorEdit(Consumer<Color> editor)
    {
        this.syncLiveColorKeyframe();

        boolean[] applied = {false};

        UIReplaysEditorUtils.forEachSelectedKeyframe(this.editor, this.keyframe, (selected) ->
        {
            applied[0] = true;
            this.keyframe = (Keyframe<Color>) (Keyframe<?>) selected;

            Color color = this.getOrCreateColor((Color) selected.getValue());

            selected.preNotify();
            editor.accept(color);
            selected.postNotify();
        });

        if (!applied[0])
        {
            Color color = this.getOrCreateColor(this.keyframe.getValue());

            this.keyframe.preNotify();
            editor.accept(color);
            this.keyframe.postNotify();
        }
    }
}
