package mchorse.bbs_mod.ui.framework.elements.input;

import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.EffectTransformMath;
import mchorse.bbs_mod.forms.forms.utils.PaintMaskShape;
import mchorse.bbs_mod.ui.framework.elements.events.UITrackpadDragEndEvent;
import mchorse.bbs_mod.ui.framework.elements.events.UITrackpadDragStartEvent;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.pose.Transform;

import java.util.function.Consumer;

/**
 * Standard {@link UIPropTransform} widget wired to {@link EffectTransform} for paint keyframes.
 */
public class UIEffectKeyframeTransform extends UIPropTransform
{
    private final Consumer<Consumer<EffectTransform>> apply;
    private boolean filling;

    public UIEffectKeyframeTransform(Consumer<Consumer<EffectTransform>> apply)
    {
        super();

        this.apply = apply;
        this.callbacks(null, this::commit);
        this.w(1F);
        this.setEffectTransform(new EffectTransform());
    }

    public void registerUndo(UIKeyframes editor)
    {
        if (editor == null)
        {
            return;
        }

        for (UITrackpad trackpad : this.getChildren(UITrackpad.class))
        {
            trackpad.getEvents().register(UITrackpadDragStartEvent.class, (e) -> editor.cacheKeyframes());
            trackpad.getEvents().register(UITrackpadDragEndEvent.class, (e) -> editor.submitKeyframes());
        }
    }

    public void setEffectTransform(EffectTransform transform)
    {
        EffectTransform value = transform == null ? new EffectTransform() : transform;
        Transform display = new Transform();

        display.translate.set(value.offsetX, value.offsetY, value.offsetZ);
        display.scale.set(value.scaleX, value.scaleY, value.scaleZ);
        display.rotate.set(
            MathUtils.toRad(value.rotateX),
            MathUtils.toRad(value.rotateY),
            MathUtils.toRad(value.rotateZ)
        );

        this.filling = true;

        try
        {
            this.setTransform(display);
        }
        finally
        {
            this.filling = false;
        }
    }

    public void setShape(PaintMaskShape shape)
    {
        if (this.filling || this.apply == null)
        {
            return;
        }

        this.apply.accept((effect) -> effect.shape = shape == null ? PaintMaskShape.BOX : shape);
    }

    private void commit()
    {
        if (this.filling || this.apply == null)
        {
            return;
        }

        Transform display = this.getTransform();

        if (display == null)
        {
            return;
        }

        this.apply.accept((effect) ->
        {
            effect.offsetX = display.translate.x;
            effect.offsetY = display.translate.y;
            effect.offsetZ = display.translate.z;
            effect.scaleX = this.sanitizeScale(display.scale.x);
            effect.scaleY = this.sanitizeScale(display.scale.y);
            effect.scaleZ = this.sanitizeScale(display.scale.z);
            effect.rotateX = MathUtils.toDeg(display.rotate.x);
            effect.rotateY = MathUtils.toDeg(display.rotate.y);
            effect.rotateZ = MathUtils.toDeg(display.rotate.z);
        });
    }

    private float sanitizeScale(float value)
    {
        if (Math.abs(value) < EffectTransformMath.EPSILON)
        {
            return 0F;
        }

        return value;
    }
}
