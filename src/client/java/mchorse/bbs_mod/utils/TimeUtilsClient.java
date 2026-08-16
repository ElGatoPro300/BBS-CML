package mchorse.bbs_mod.utils;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;

public class TimeUtilsClient
{
    public static void configure(UITrackpad element, int defaultValue)
    {
        if (BBSSettings.editorSeconds.get())
        {
            element.values(0.1D, 0.05D, 0.25D).limit(defaultValue / 20D, Double.POSITIVE_INFINITY, false);
        }
        else
        {
            element.values(1.0D).limit(defaultValue, Double.POSITIVE_INFINITY, true);
        }
    }
}