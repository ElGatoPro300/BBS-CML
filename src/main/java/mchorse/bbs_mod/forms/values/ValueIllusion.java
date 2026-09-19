package mchorse.bbs_mod.forms.values;

import mchorse.bbs_mod.forms.forms.utils.Illusion;
import mchorse.bbs_mod.settings.values.base.BaseKeyframeFactoryValue;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

public class ValueIllusion extends BaseKeyframeFactoryValue<Illusion>
{
    public ValueIllusion(String id, Illusion value)
    {
        super(id, KeyframeFactories.ILLUSION, value);
    }
}
