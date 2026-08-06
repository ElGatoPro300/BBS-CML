package mchorse.bbs_mod.settings.values.numeric;

import mchorse.bbs_mod.settings.values.base.BaseKeyframeFactoryValue;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

public class ValueBoolean extends BaseKeyframeFactoryValue<Boolean>
{
    public ValueBoolean(String id)
    {
        this(id, false);
    }

    public ValueBoolean(String id, boolean defaultValue)
    {
        super(id, KeyframeFactories.BOOLEAN, defaultValue);
    }

    public void toggle()
    {
        this.set(!this.get());
    }

    @Override
    public Boolean get()
    {
        Object runtime = this.getRuntimeValue();
        Object stored = runtime != null ? runtime : this.getOriginalValue();

        if (stored instanceof Boolean)
        {
            return (Boolean) stored;
        }

        /* Legacy float runtime values (e.g. shaderShadow briefly stored as float). */
        if (stored instanceof Number)
        {
            return ((Number) stored).floatValue() > 0.001F;
        }

        Boolean original = this.getOriginalValue();

        return original != null ? original : Boolean.FALSE;
    }

    @Override
    public String toString()
    {
        return Boolean.toString(this.get());
    }
}
