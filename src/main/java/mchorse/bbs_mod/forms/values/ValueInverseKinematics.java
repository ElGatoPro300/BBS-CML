package mchorse.bbs_mod.forms.values;

import mchorse.bbs_mod.forms.forms.utils.InverseKinematics;
import mchorse.bbs_mod.settings.values.base.BaseKeyframeFactoryValue;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

public class ValueInverseKinematics extends BaseKeyframeFactoryValue<InverseKinematics>
{
    public ValueInverseKinematics(String id, InverseKinematics value)
    {
        super(id, KeyframeFactories.INVERSE_KINEMATICS, value);
    }
}
