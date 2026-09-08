package mchorse.bbs_mod.settings.values.base;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.utils.keyframes.factories.IKeyframeFactory;

public abstract class BaseValueNumber <T extends Number> extends BaseKeyframeFactoryValue<T>
{
    protected T min;
    protected T max;

    public BaseValueNumber(String id, IKeyframeFactory<T> factory, T defaultValue, T min, T max)
    {
        super(id, factory, defaultValue);

        this.min = min;
        this.max = max;
    }

    public T getMin()
    {
        return this.min;
    }

    public T getMax()
    {
        return this.max;
    }

    @Override
    public void set(T value, int flag)
    {
        if (this.min != null && this.max != null)
        {
            value = this.clamp(value);
        }

        super.set(value, flag);
    }

    /**
     * fromData used to assign raw factory output and skip {@link #set}/{@link #clamp}.
     * Oversized film JSON numbers (long → truncated int) then poisoned clip duration
     * and froze the camera timeline.
     */
    @Override
    public void fromData(BaseType data)
    {
        T value = this.getFactory().fromData(data);

        if (this.min != null && this.max != null)
        {
            value = this.clamp(value);
        }

        this.value = value;
    }

    protected abstract T clamp(T value);
}