package mchorse.bbs_mod.forms.values;

import mchorse.bbs_mod.forms.forms.utils.LookAt;
import mchorse.bbs_mod.settings.values.base.BaseKeyframeFactoryValue;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

public class ValueLookAt extends BaseKeyframeFactoryValue<LookAt>
{
    public ValueLookAt(String id, LookAt value)
    {
        super(id, KeyframeFactories.LOOK_AT, value);
    }
}
