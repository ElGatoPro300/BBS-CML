package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.framework.elements.input.UIDeltaPropTransform;
import mchorse.bbs_mod.utils.pose.Transform;

import java.util.function.Consumer;

/**
 * Delta transform editor for keyframe properties (anchor, etc.).
 */
public abstract class UIKeyframePropTransform extends UIDeltaPropTransform
{
    protected void applyDuringRecording(int tick, Consumer<Transform> consumer)
    {}

    protected Transform getRecordedTransform(int tick)
    {
        return null;
    }

    @Override
    protected void applyToTarget(Consumer<Transform> consumer)
    {
        this.applyToSelection(consumer);
    }
}
