package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIConstants;
import mchorse.bbs_mod.utils.Axis;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.joml.Vectors;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Transform;

import org.joml.Vector3d;

import java.util.function.Consumer;

public class UIPoseTransformKeyframeFactory extends UIKeyframeFactory<PoseTransform>
{
    public UITrackpad fix;
    public UIColor color;
    public UIToggle lighting;
    public UIPropTransform transform;

    public UIPoseTransformKeyframeFactory(Keyframe<PoseTransform> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.transform = new UIPoseTransforms(this);
        this.transform.enableHotkeys();
        this.transform.setTransform(keyframe.getValue());

        this.keys().register(Keys.TRANSFORMATIONS_TOGGLE_FIX, this::toggleFix).category(UIKeys.TRANSFORMS_KEYS_CATEGORY);

        this.fix = new UITrackpad((v) ->
        {
            if (this.transform.getTransform() instanceof PoseTransform)
            {
                UIPoseTransforms.apply(editor, keyframe, (poseT) -> poseT.fix = v.floatValue());
            }
        });
        this.fix.limit(0D, 1D).increment(1D).values(0.1, 0.05D, 0.2D);
        this.fix.tooltip(UIKeys.POSE_CONTEXT_FIX_TOOLTIP);
        this.fix.setValue(keyframe.getValue().fix);

        this.color = new UIColor((c) ->
        {
            if (this.transform.getTransform() instanceof PoseTransform)
            {
                UIPoseTransforms.apply(editor, keyframe, (poseT) -> poseT.color.set(c));
            }
        });
        this.color.withAlpha();
        this.color.setColor(keyframe.getValue().color.getARGBColor());

        this.lighting = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_LIGHTING, (b) ->
        {
            if (this.transform.getTransform() instanceof PoseTransform)
            {
                UIPoseTransforms.apply(editor, keyframe, (poseT) -> poseT.lighting = b.getValue() ? 0F : 1F);
            }
        });
        this.lighting.h(UIConstants.CONTROL_HEIGHT);
        this.lighting.setValue(keyframe.getValue().lighting == 0F);

        this.scroll.add(UI.label(UIKeys.POSE_CONTEXT_FIX), this.fix, UI.row(this.color, this.lighting), this.transform.marginTop(4));
    }

    private void toggleFix()
    {
        if (!(this.transform.getTransform() instanceof PoseTransform))
        {
            return;
        }
        float next = this.fix.getValue() >= 0.5F ? 0F : 1F;
        this.fix.setValue(next);
        UIPoseTransforms.apply(this.editor, this.keyframe, (poseT) -> poseT.fix = next);
    }

    public static class UIPoseTransforms extends UIPropTransform
    {
        private UIPoseTransformKeyframeFactory editor;

        public UIPoseTransforms(UIPoseTransformKeyframeFactory editor)
        {
            this.editor = editor;
        }

        public static void apply(UIKeyframes editor, Keyframe keyframe, Consumer<PoseTransform> consumer)
        {
            for (UIKeyframeSheet sheet : editor.getGraph().getSheets())
            {
                if (sheet.channel.getFactory() != keyframe.getFactory())
                {
                    continue;
                }

                for (Keyframe kf : sheet.selection.getSelected())
                {
                    if (kf.getValue() instanceof PoseTransform transform)
                    {
                        kf.preNotify();
                        consumer.accept(transform);
                        kf.postNotify();
                    }
                }
            }
        }

        @Override
        public void pasteTranslation(Vector3d translation)
        {
            apply(this.editor.editor, this.editor.keyframe, (poseT) -> poseT.translate.set(translation));
            this.refillTransform();
        }

        @Override
        public void pasteScale(Vector3d scale)
        {
            apply(this.editor.editor, this.editor.keyframe, (poseT) -> poseT.scale.set(scale));
            this.refillTransform();
        }

        @Override
        public void pasteRotation(Vector3d rotation)
        {
            apply(this.editor.editor, this.editor.keyframe, (poseT) -> poseT.rotate.set(Vectors.toRad(rotation)));
            this.refillTransform();
        }

        @Override
        public void pasteRotation2(Vector3d rotation)
        {
            apply(this.editor.editor, this.editor.keyframe, (poseT) -> poseT.rotate2.set(Vectors.toRad(rotation)));
            this.refillTransform();
        }

        @Override
        public void setT(Axis axis, double x, double y, double z)
        {
            Transform transform = this.getTransform();
            float dx = (float) (x - transform.translate.x);
            float dy = (float) (y - transform.translate.y);
            float dz = (float) (z - transform.translate.z);

            apply(this.editor.editor, this.editor.keyframe, (poseT) ->
            {
                poseT.translate.x += dx;
                poseT.translate.y += dy;
                poseT.translate.z += dz;
            });
        }

        @Override
        public void setS(Axis axis, double x, double y, double z)
        {
            Transform transform = this.getTransform();
            float dx = (float) (x - transform.scale.x);
            float dy = (float) (y - transform.scale.y);
            float dz = (float) (z - transform.scale.z);

            apply(this.editor.editor, this.editor.keyframe, (poseT) ->
            {
                poseT.scale.x += dx;
                poseT.scale.y += dy;
                poseT.scale.z += dz;
            });
        }

        @Override
        public void setR(Axis axis, double x, double y, double z)
        {
            Transform transform = this.getTransform();
            float dx = MathUtils.toRad((float) x) - transform.rotate.x;
            float dy = MathUtils.toRad((float) y) - transform.rotate.y;
            float dz = MathUtils.toRad((float) z) - transform.rotate.z;

            apply(this.editor.editor, this.editor.keyframe, (poseT) ->
            {
                poseT.rotate.x += dx;
                poseT.rotate.y += dy;
                poseT.rotate.z += dz;
            });
        }

        @Override
        public void setR2(Axis axis, double x, double y, double z)
        {
            Transform transform = this.getTransform();
            float dx = MathUtils.toRad((float) x) - transform.rotate2.x;
            float dy = MathUtils.toRad((float) y) - transform.rotate2.y;
            float dz = MathUtils.toRad((float) z) - transform.rotate2.z;

            apply(this.editor.editor, this.editor.keyframe, (poseT) ->
            {
                poseT.rotate2.x += dx;
                poseT.rotate2.y += dy;
                poseT.rotate2.z += dz;
            });
        }
    }
}
