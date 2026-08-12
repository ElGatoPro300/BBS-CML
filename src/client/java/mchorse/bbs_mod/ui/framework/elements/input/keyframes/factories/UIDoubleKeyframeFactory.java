package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.utils.UIBezierHandles;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class UIDoubleKeyframeFactory extends UIKeyframeFactory<Double>
{
    protected UITrackpad value;
    private UIBezierHandles handles;

    public UIDoubleKeyframeFactory(Keyframe<Double> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.value = new UITrackpad(this::setValue);
        this.applySheetLimits();
        this.value.setValue(keyframe.getValue());
        this.handles = new UIBezierHandles(keyframe);
        this.registerValueTrackpad(this.value);

        this.scroll.add(this.value, this.handles.createColumn());
    }

    private void applySheetLimits()
    {
        UIKeyframeSheet sheet = this.findSheet();

        if (sheet == null || (sheet.minValue == null && sheet.maxValue == null))
        {
            return;
        }

        double min = sheet.minValue != null ? sheet.minValue : Double.NEGATIVE_INFINITY;
        double max = sheet.maxValue != null ? sheet.maxValue : Double.POSITIVE_INFINITY;

        this.value.limit(min, max);
    }

    private UIKeyframeSheet findSheet()
    {
        for (UIKeyframeSheet sheet : this.editor.getGraph().getSheets())
        {
            if (sheet.channel == this.keyframe.getParent())
            {
                return sheet;
            }
        }

        return null;
    }

    @Override
    public void update()
    {
        super.update();

        if (!this.value.isActivelyEditing() && !this.value.isDragging())
        {
            this.value.setValue(this.keyframe.getValue());
        }

        this.handles.setKeyframe(this.keyframe);
        this.handles.update();
    }
}