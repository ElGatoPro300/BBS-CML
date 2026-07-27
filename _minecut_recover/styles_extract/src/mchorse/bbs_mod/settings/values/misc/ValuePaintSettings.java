package mchorse.bbs_mod.settings.values.misc;

import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.settings.values.base.BaseKeyframeFactoryValue;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

public class ValuePaintSettings extends BaseKeyframeFactoryValue<PaintSettings>
{
    public ValuePaintSettings(String id, PaintSettings value)
    {
        super(id, KeyframeFactories.PAINT_SETTINGS, value);
    }
}
