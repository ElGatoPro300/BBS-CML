package mchorse.bbs_mod.film;

/**
 * Pauses integrated-server ticking while recording setup overlays are open.
 */
public final class RecordingPauseHelper
{
    private static int depth;

    private RecordingPauseHelper()
    {}

    public static void push()
    {
        RecordingPauseHelper.depth++;
    }

    public static void pop()
    {
        if (RecordingPauseHelper.depth > 0)
        {
            RecordingPauseHelper.depth--;
        }
    }

    public static boolean isActive()
    {
        return RecordingPauseHelper.depth > 0;
    }
}
