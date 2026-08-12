package mchorse.bbs_mod.ui.framework.elements.input;

import mchorse.bbs_mod.utils.Axis;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.joml.Vectors;
import mchorse.bbs_mod.utils.pose.Transform;

import org.joml.Vector3d;

import java.util.function.Consumer;

/**
 * Transform editor that applies every edit as a per-channel delta to a whole
 * selection of transforms, instead of overwriting them.
 */
public abstract class UIDeltaPropTransform extends UIPropTransform
{
    protected abstract void applyToSelection(Consumer<Transform> consumer);

    protected Transform getTargetTransform()
    {
        return this.getTransform();
    }

    protected void applyToTarget(Consumer<Transform> consumer)
    {
        this.applyToSelection(consumer);
    }

    protected void syncTargetTransform()
    {
        Transform transform = this.getTargetTransform();

        if (transform != null)
        {
            this.setTransform(transform);
        }
    }

    @Override
    public void setT(Axis axis, double x, double y, double z)
    {
        Transform transform = this.getTargetTransform();

        if (transform == null)
        {
            return;
        }

        float dx = (float) (x - transform.translate.x);
        float dy = (float) (y - transform.translate.y);
        float dz = (float) (z - transform.translate.z);

        this.preCallback();
        this.applyToTarget((t) ->
        {
            t.translate.x += dx;
            t.translate.y += dy;
            t.translate.z += dz;
        });
        this.postCallback();

        this.syncTargetTransform();
    }

    @Override
    public void setS(Axis axis, double x, double y, double z)
    {
        Transform transform = this.getTargetTransform();

        if (transform == null)
        {
            return;
        }

        float dx = (float) (x - transform.scale.x);
        float dy = (float) (y - transform.scale.y);
        float dz = (float) (z - transform.scale.z);

        this.preCallback();
        this.applyToTarget((t) ->
        {
            t.scale.x += dx;
            t.scale.y += dy;
            t.scale.z += dz;
        });
        this.postCallback();

        this.syncTargetTransform();
    }

    @Override
    public void setR(Axis axis, double x, double y, double z)
    {
        Transform transform = this.getTargetTransform();

        if (transform == null)
        {
            return;
        }

        float dx = MathUtils.toRad((float) x) - transform.rotate.x;
        float dy = MathUtils.toRad((float) y) - transform.rotate.y;
        float dz = MathUtils.toRad((float) z) - transform.rotate.z;

        this.preCallback();
        this.applyToTarget((t) ->
        {
            t.rotate.x += dx;
            t.rotate.y += dy;
            t.rotate.z += dz;
        });
        this.postCallback();

        this.syncTargetTransform();
    }

    @Override
    public void pasteTranslation(Vector3d translation)
    {
        this.preCallback();
        this.applyToTarget((t) -> t.translate.set(translation));
        this.postCallback();

        this.syncTargetTransform();
    }

    @Override
    public void pasteScale(Vector3d scale)
    {
        this.preCallback();
        this.applyToTarget((t) -> t.scale.set(scale));
        this.postCallback();

        this.syncTargetTransform();
    }

    @Override
    public void pasteRotation(Vector3d rotation)
    {
        this.preCallback();
        this.applyToTarget((t) -> t.rotate.set(Vectors.toRad(rotation)));
        this.postCallback();

        this.syncTargetTransform();
    }
}
