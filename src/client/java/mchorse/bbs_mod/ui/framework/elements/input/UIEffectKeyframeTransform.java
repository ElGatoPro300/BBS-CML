package mchorse.bbs_mod.ui.framework.elements.input;

import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.EffectTransformMath;
import mchorse.bbs_mod.forms.forms.utils.PaintMaskShape;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.events.UITrackpadDragEndEvent;
import mchorse.bbs_mod.ui.framework.elements.events.UITrackpadDragStartEvent;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.UIConstants;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.pose.Transform;

import java.util.function.Consumer;

/**
 * {@link UIPropTransform} wired to {@link EffectTransform} for color / paint / glow masks.
 * Hides rotate2 (unused for masks). Pivot is the local rotation center of the mask volume.
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
        this.removeUnusedEffectRows();
        this.setEffectTransform(new EffectTransform());
    }

    /**
     * Effect masks use translate / scale / rotate / pivot. Drop rotate2 only —
     * that field belongs to pose-limb transforms, not paint masks.
     */
    private void removeUnusedEffectRows()
    {
        UIElement rotate2Row = this.iconR2.getParent();

        if (rotate2Row != null && rotate2Row != this)
        {
            rotate2Row.removeFromParent();
        }

        /* Four rows: translate, scale, rotate, pivot. */
        this.h(UIConstants.CONTROL_HEIGHT * 4);
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
        display.pivot.set(value.pivotX, value.pivotY, value.pivotZ);

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
            effect.pivotX = display.pivot.x;
            effect.pivotY = display.pivot.y;
            effect.pivotZ = display.pivot.z;
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
