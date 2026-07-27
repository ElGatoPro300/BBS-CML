package mchorse.bbs_mod.ui.forms.editors.panels.widgets;

import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.settings.values.IValueListener;
import mchorse.bbs_mod.cubic.IModel;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.settings.values.core.ValuePose;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.utils.pose.UIPoseEditor;
import mchorse.bbs_mod.utils.pose.PoseTransform;

public class UIModelPoseEditor extends UIPoseEditor
{
    private ValuePose valuePose;

    @Override
    protected boolean useModelGizmoDrag()
    {
        /* General transform uses a plain UIPropTransform; match that for pose trackball drag. */
        return false;
    }

    @Override
    protected float getGizmoTranslationScale()
    {
        return ModelFormRenderer.isBobjModel(this.model) ? 1F : 16F;
    }

    public void setValuePose(ValuePose valuePose)
    {
        this.valuePose = valuePose;
    }

    @Override
    protected UIPropTransform createTransformEditor()
    {
        UIPropTransform editor = super.createTransformEditor()
            .callbacks(() -> this.valuePose);

        /* Same signs as FilmPoseGizmoDrag / UIPickableFormRenderer pose-bone prepare. */
        if (ModelFormRenderer.isBobjModel(this.model))
        {
            editor.bobjPoseGizmoTuning();
        }
        else
        {
            editor.configurePoseRingTuning(false);
            editor.setAxisProjectedTranslation(false);
        }

        editor.setInvertTrackballDragY(false);
        editor.clearTrackballEulerInverts();

        return editor;
    }

    @Override
    public void fillGroups(IModel model, java.util.Map<String, String> flippedParts, boolean reset)
    {
        super.fillGroups(model, flippedParts, reset);

        if (this.transform != null)
        {
            if (ModelFormRenderer.isBobjModel(model))
            {
                this.transform.translationScale(1F);
                this.transform.configurePoseRingTuning(true);
                this.transform.setAxisProjectedTranslation(true);
            }
            else
            {
                this.transform.translationScale(16F);
                this.transform.configurePoseRingTuning(false);
                this.transform.setAxisProjectedTranslation(false);
                this.transform.clearTrackballEulerInverts();
            }
        }
    }

    @Override
    protected void pastePose(MapType data)
    {
        this.valuePose.preNotify(IValueListener.FLAG_UNMERGEABLE);
        super.pastePose(data);
        this.valuePose.postNotify(IValueListener.FLAG_UNMERGEABLE);
    }

    @Override
    protected void flipPose()
    {
        this.valuePose.preNotify(IValueListener.FLAG_UNMERGEABLE);
        super.flipPose();
        this.valuePose.postNotify(IValueListener.FLAG_UNMERGEABLE);
    }

    @Override
    protected void setFix(PoseTransform transform, float value)
    {
        this.valuePose.preNotify(IValueListener.FLAG_UNMERGEABLE);
        super.setFix(transform, value);
        this.valuePose.postNotify(IValueListener.FLAG_UNMERGEABLE);
    }

    @Override
    protected void setColor(PoseTransform transform, int value)
    {
        this.valuePose.preNotify(IValueListener.FLAG_UNMERGEABLE);
        super.setColor(transform, value);
        this.valuePose.postNotify(IValueListener.FLAG_UNMERGEABLE);
    }

    @Override
    protected void setPaintColor(PoseTransform transform, int value)
    {
        this.valuePose.preNotify(IValueListener.FLAG_UNMERGEABLE);
        super.setPaintColor(transform, value);
        this.valuePose.postNotify(IValueListener.FLAG_UNMERGEABLE);
    }

    @Override
    protected void setLighting(PoseTransform transform, boolean value)
    {
        this.valuePose.preNotify(IValueListener.FLAG_UNMERGEABLE);
        super.setLighting(transform, value);
        this.valuePose.postNotify(IValueListener.FLAG_UNMERGEABLE);
    }

    @Override
    protected void setOpacity(PoseTransform transform, float value)
    {
        this.valuePose.preNotify(IValueListener.FLAG_UNMERGEABLE);
        super.setOpacity(transform, value);
        this.valuePose.postNotify(IValueListener.FLAG_UNMERGEABLE);
    }

    @Override
    protected void setTextureBlend(PoseTransform transform, float value)
    {
        this.valuePose.preNotify(IValueListener.FLAG_UNMERGEABLE);
        super.setTextureBlend(transform, value);
        this.valuePose.postNotify(IValueListener.FLAG_UNMERGEABLE);
    }
}