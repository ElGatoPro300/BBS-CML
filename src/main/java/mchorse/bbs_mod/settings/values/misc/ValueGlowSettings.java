package mchorse.bbs_mod.settings.values.misc;

import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.settings.values.base.BaseKeyframeFactoryValue;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

public class ValueGlowSettings extends BaseKeyframeFactoryValue<GlowSettings>
{
    public ValueGlowSettings(String id, GlowSettings value)
    {
        super(id, KeyframeFactories.GLOW_SETTINGS, value);
    }
}
