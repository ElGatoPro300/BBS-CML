package mchorse.bbs_mod.film;

/**
 * Pauses integrated-server ticking while recording setup overlays are open.
 * <p>
 * Must be released synchronously when recording starts or the UI screen is
 * replaced. Deferred overlay close animations can otherwise leave {@code depth}
 * stuck and freeze the integrated server for the rest of the session.
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

    public static void reset()
    {
        RecordingPauseHelper.depth = 0;
    }

    public static boolean isActive()
    {
        return RecordingPauseHelper.depth > 0;
    }
}
