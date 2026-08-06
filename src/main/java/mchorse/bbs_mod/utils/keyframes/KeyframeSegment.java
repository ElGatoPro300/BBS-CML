package mchorse.bbs_mod.utils.keyframes;

import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.keyframes.factories.IKeyframeFactory;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

/**
 * Keyframe segment.
 *
 * This structure contains all the necessary data to create an interpolated
 * value.
 */
public class KeyframeSegment <T>
{
    public Keyframe<T> a;
    public Keyframe<T> b;

    public Keyframe<T> preA;
    public Keyframe<T> postB;
    public float duration;
    public float offset;
    public float x;

    public KeyframeSegment()
    {}

    public KeyframeSegment(Keyframe<T> a, Keyframe<T> b)
    {
        this.fill(a, b);
    }

    public void setup(Keyframe<T> a, Keyframe<T> b, float ticks)
    {
        this.fill(a, b);
        this.setup(ticks);
    }

    public void fill(Keyframe<T> a, Keyframe<T> b)
    {
        this.a = a;
        this.b = b;

        KeyframeChannel<T> channel = (KeyframeChannel<T>) a.getParent();
        int index = channel.getKeyframes().indexOf(a);

        if (index >= 0)
        {
            Keyframe<T> preA = channel.get(index - 1);
            Keyframe<T> postB = channel.get(index + 2);

            this.preA = preA == null ? a : preA;
            this.postB = postB == null ? b : postB;
        }
        else
        {
            this.preA = a;
            this.postB = b;
        }
    }

    public void setup(float ticks)
    {
        float forcedDuration = this.a.getDuration();

        /* Boolean tracks are step/hold. Forced duration only moves segment.x (used by
         * getClosest and some discrete factories) and would flip “closest” at mid-duration
         * — that looks like ghost visible/render keyframes. Ignore it for booleans. */
        if (this.a.getFactory() == KeyframeFactories.BOOLEAN)
        {
            forcedDuration = 0F;
        }

        this.duration = forcedDuration > 0 ? forcedDuration : this.b.getTick() - this.a.getTick();
        this.offset = ticks - this.a.getTick();
        this.x = MathUtils.clamp(this.duration == 0 ? 0F : this.offset / this.duration, 0F, 1F);
    }

    public T createInterpolated()
    {
        IKeyframeFactory<T> factory = this.a.getFactory();

        if (this.isSame())
        {
            return factory.copy(this.a.getValue());
        }

        return factory.copy(factory.interpolate(this.preA, this.a, this.b, this.postB, this.a.getInterpolation(), this.x));
    }

    public boolean isSame()
    {
        return this.a == this.b;
    }

    public Keyframe<T> getClosest()
    {
        return this.x > 0.5F ? this.b : this.a;
    }
}