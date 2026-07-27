package mchorse.bbs_mod.ui.utils;

import mchorse.bbs_mod.BBSSettings;

public class TimelineRuler
{
    private static final int MIN_LABEL_SPACING = 70;
    private static final int[] SECOND_STEPS = {1, 2, 5, 10, 20, 100, 200, 600, 1200, 6000};

    public static Step steps(Scale scale)
    {
        if (BBSSettings.editorTimeMode != null && BBSSettings.editorTimeMode.get() == 1)
        {
            return secondsSteps(scale);
        }

        int minor = Math.max(1, scale.getMult());

        return new Step(minor, minor * 5);
    }

    private static Step secondsSteps(Scale scale)
    {
        int major = SECOND_STEPS[SECOND_STEPS.length - 1];

        for (int step : SECOND_STEPS)
        {
            if (step * scale.getZoom() >= MIN_LABEL_SPACING)
            {
                major = step;

                break;
            }
        }

        int minor = Math.max(1, major / 5);

        return new Step(minor, major);
    }

    public static class Step
    {
        public final int minor;
        public final int major;

        public Step(int minor, int major)
        {
            this.minor = minor;
            this.major = major;
        }
    }
}
