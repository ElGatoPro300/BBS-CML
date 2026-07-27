package mchorse.bbs_mod.text;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;

import java.text.Bidi;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Prepares UI strings for right-to-left Arabic-script languages (Persian, Arabic, Urdu).
 * Applies contextual letter shaping and bidirectional reordering so Minecraft's
 * left-to-right text renderer displays connected, readable script.
 */
public final class RtlTextEngine
{
    private static final int CACHE_LIMIT = 512;

    private static final Map<String, String> CACHE = new LinkedHashMap<>(CACHE_LIMIT, 0.75F, true)
    {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest)
        {
            return this.size() > CACHE_LIMIT;
        }
    };

    private RtlTextEngine()
    {}

    public static boolean isActive()
    {
        return isRtlLanguage(BBSModClient.getLanguageKey());
    }

    public static boolean isRtlLanguage(String language)
    {
        if (language == null || language.isEmpty())
        {
            return false;
        }

        String lang = language.toLowerCase();

        return lang.equals("fa_ir") || lang.equals("ar_ar") || lang.equals("ur_pk");
    }

    public static void clearCache()
    {
        CACHE.clear();
    }

    /**
     * Shape Arabic letters and reorder for visual left-to-right drawing.
     */
    public static String prepare(String text)
    {
        if (text == null || text.isEmpty() || !isActive())
        {
            return text;
        }

        if (!ArabicShaper.containsArabicScript(text))
        {
            return text;
        }

        synchronized (CACHE)
        {
            String cached = CACHE.get(text);

            if (cached != null)
            {
                return cached;
            }

            String shaped = ArabicShaper.shape(text);
            String visual = toVisualOrder(shaped);

            CACHE.put(text, visual);

            return visual;
        }
    }

    private static String toVisualOrder(String text)
    {
        Bidi bidi = new Bidi(text, Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT);

        if (bidi.isLeftToRight())
        {
            return text;
        }

        int runCount = bidi.getRunCount();

        if (runCount <= 1)
        {
            return reverseRun(text);
        }

        byte[] levels = new byte[runCount];
        Object[] runs = new Object[runCount];

        for (int run = 0; run < runCount; run++)
        {
            levels[run] = (byte) bidi.getRunLevel(run);
            runs[run] = run;
        }

        Bidi.reorderVisually(levels, 0, runs, 0, runCount);

        StringBuilder visual = new StringBuilder(text.length());

        for (Object entry : runs)
        {
            int run = (Integer) entry;
            int start = bidi.getRunStart(run);
            int limit = bidi.getRunLimit(run);
            String runText = text.substring(start, limit);

            if ((bidi.getRunLevel(run) & 1) == 1)
            {
                visual.append(reverseRun(runText));
            }
            else
            {
                visual.append(runText);
            }
        }

        return visual.toString();
    }

    private static String reverseRun(String text)
    {
        return new StringBuilder(text).reverse().toString();
    }
}
