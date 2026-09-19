package mchorse.bbs_mod.text;

/**
 * Converts Arabic-script text from logical Unicode order into contextual
 * presentation forms so letters connect properly when rendered.
 */
public final class ArabicShaper
{
    private static final char ZWNJ = '\u200C';

    private static final char ZWJ = '\u200D';

    private static final char TATWEEL = '\u0640';

    /* Covers U+0621..U+06FF — [isolated, final, initial, medial]; 0 means no form */
    private static final int BASE = 0x0621;

    private static final int SPAN = 0x06FF - BASE + 1;

    private static final int[][] FORMS = new int[SPAN][];

    static
    {
        map(0x0621, 0xFE80, 0, 0, 0);           /* hamza */
        map(0x0622, 0xFE81, 0xFE82, 0, 0);       /* alef madda */
        map(0x0623, 0xFE83, 0xFE84, 0, 0);       /* alef hamza above */
        map(0x0624, 0xFE85, 0xFE86, 0, 0);       /* waw hamza */
        map(0x0625, 0xFE87, 0xFE88, 0, 0);       /* alef hamza below */
        map(0x0626, 0xFE89, 0xFE8A, 0xFE8B, 0xFE8C); /* yeh hamza */
        map(0x0627, 0xFE8D, 0xFE8E, 0, 0);       /* alef */
        map(0x0628, 0xFE8F, 0xFE90, 0xFE91, 0xFE92); /* beh */
        map(0x0629, 0xFE93, 0xFE94, 0, 0);       /* teh marbuta */
        map(0x062A, 0xFE95, 0xFE96, 0xFE97, 0xFE98); /* teh */
        map(0x062B, 0xFE99, 0xFE9A, 0xFE9B, 0xFE9C); /* theh */
        map(0x062C, 0xFE9D, 0xFE9E, 0xFE9F, 0xFEA0); /* jeem */
        map(0x062D, 0xFEA1, 0xFEA2, 0xFEA3, 0xFEA4); /* hah */
        map(0x062E, 0xFEA5, 0xFEA6, 0xFEA7, 0xFEA8); /* khah */
        map(0x062F, 0xFEA9, 0xFEAA, 0, 0);       /* dal */
        map(0x0630, 0xFEAB, 0xFEAC, 0, 0);       /* thal */
        map(0x0631, 0xFEAD, 0xFEAE, 0, 0);       /* reh */
        map(0x0632, 0xFEAF, 0xFEB0, 0, 0);       /* zain */
        map(0x0633, 0xFEB1, 0xFEB2, 0xFEB3, 0xFEB4); /* seen */
        map(0x0634, 0xFEB5, 0xFEB6, 0xFEB7, 0xFEB8); /* sheen */
        map(0x0635, 0xFEB9, 0xFEBA, 0xFEBB, 0xFEBC); /* sad */
        map(0x0636, 0xFEBD, 0xFEBE, 0xFEBF, 0xFEC0); /* dad */
        map(0x0637, 0xFEC1, 0xFEC2, 0xFEC3, 0xFEC4); /* tah */
        map(0x0638, 0xFEC5, 0xFEC6, 0xFEC7, 0xFEC8); /* zah */
        map(0x0639, 0xFEC9, 0xFECA, 0xFECB, 0xFECC); /* ain */
        map(0x063A, 0xFECD, 0xFECE, 0xFECF, 0xFED0); /* ghain */
        map(0x0641, 0xFED1, 0xFED2, 0xFED3, 0xFED4); /* feh */
        map(0x0642, 0xFED5, 0xFED6, 0xFED7, 0xFED8); /* qaf */
        map(0x0643, 0xFED9, 0xFEDA, 0xFEDB, 0xFEDC); /* kaf */
        map(0x0644, 0xFEDD, 0xFEDE, 0xFEDF, 0xFEE0); /* lam */
        map(0x0645, 0xFEE1, 0xFEE2, 0xFEE3, 0xFEE4); /* meem */
        map(0x0646, 0xFEE5, 0xFEE6, 0xFEE7, 0xFEE8); /* noon */
        map(0x0647, 0xFEE9, 0xFEEA, 0xFEEB, 0xFEEC); /* heh */
        map(0x0648, 0xFEED, 0xFEEE, 0, 0);       /* waw */
        map(0x0649, 0xFEEF, 0xFEF0, 0, 0);       /* alef maksura */
        map(0x064A, 0xFEF1, 0xFEF2, 0xFEF3, 0xFEF4); /* yeh */

        /* Persian / extended Arabic letters */
        map(0x0679, 0xFB66, 0xFB67, 0xFB68, 0xFB69); /* tteh */
        map(0x067E, 0xFB56, 0xFB57, 0xFB58, 0xFB59); /* peh */
        map(0x0686, 0xFB7A, 0xFB7B, 0xFB7C, 0xFB7D); /* tcheh */
        map(0x0688, 0xFB88, 0xFB89, 0, 0);       /* ddah */
        map(0x0691, 0xFB8C, 0xFB8D, 0, 0);       /* rreh */
        map(0x0698, 0xFB8A, 0xFB8B, 0, 0);       /* jeh */
        map(0x06A4, 0xFB6A, 0xFB6B, 0xFB6C, 0xFB6D); /* veh */
        map(0x06A9, 0xFB8E, 0xFB8F, 0xFB90, 0xFB91); /* keheh */
        map(0x06AF, 0xFB92, 0xFB93, 0xFB94, 0xFB95); /* gaf */
        map(0x06BA, 0xFB9E, 0xFB9F, 0, 0);       /* noon ghunna */
        map(0x06BE, 0xFBAA, 0xFBAB, 0xFBAC, 0xFBAD); /* heh doachashmee */
        map(0x06C1, 0xFBA6, 0xFBA7, 0xFBA8, 0xFBA9); /* heh goal */
        map(0x06CC, 0xFBFC, 0xFBFD, 0xFBFE, 0xFBFF); /* farsi yeh */
        map(0x06D2, 0xFBAE, 0xFBAF, 0, 0);       /* yeh barree */
    }

    private ArabicShaper()
    {}

    private static void map(int base, int isolated, int fin, int init, int med)
    {
        int index = base - BASE;

        if (index < 0 || index >= FORMS.length)
        {
            return;
        }

        FORMS[index] = new int[] {isolated, fin, init, med};
    }

    public static boolean isArabicLetter(int codePoint)
    {
        if (codePoint >= BASE && codePoint <= 0x06FF)
        {
            return FORMS[codePoint - BASE] != null;
        }

        if (codePoint >= 0xFB50 && codePoint <= 0xFDFF)
        {
            return true;
        }

        if (codePoint >= 0xFE70 && codePoint <= 0xFEFF)
        {
            return true;
        }

        return false;
    }

    public static boolean containsArabicScript(String text)
    {
        if (text == null || text.isEmpty())
        {
            return false;
        }

        for (int i = 0; i < text.length();)
        {
            int cp = text.codePointAt(i);

            if (isArabicLetter(cp) || isArabicDiacritic(cp))
            {
                return true;
            }

            i += Character.charCount(cp);
        }

        return false;
    }

    private static boolean isArabicDiacritic(int codePoint)
    {
        return codePoint >= 0x064B && codePoint <= 0x065F;
    }

    private static boolean isTransparent(int codePoint)
    {
        return codePoint == ZWNJ || codePoint == ZWJ || isArabicDiacritic(codePoint);
    }

    private static boolean connectsBefore(int codePoint)
    {
        int[] forms = getForms(codePoint);

        return forms != null && (forms[2] != 0 || forms[3] != 0);
    }

    private static boolean connectsAfter(int codePoint)
    {
        int[] forms = getForms(codePoint);

        return forms != null && (forms[1] != 0 || forms[3] != 0);
    }

    private static int[] getForms(int codePoint)
    {
        if (codePoint >= BASE && codePoint <= 0x06FF)
        {
            return FORMS[codePoint - BASE];
        }

        return null;
    }

    private static int previousLetter(String text, int index)
    {
        for (int i = index - 1; i >= 0;)
        {
            int cp = text.codePointBefore(i + 1);

            if (!isTransparent(cp))
            {
                return cp;
            }

            if (cp == ZWNJ)
            {
                return -1;
            }

            i -= Character.charCount(cp);
        }

        return -1;
    }

    private static int nextLetter(String text, int index)
    {
        for (int i = index + 1; i < text.length();)
        {
            int cp = text.codePointAt(i);

            if (!isTransparent(cp))
            {
                return cp;
            }

            if (cp == ZWNJ)
            {
                return -1;
            }

            i += Character.charCount(cp);
        }

        return -1;
    }

    private static int lamAlef(int lam, int alef)
    {
        if (lam != 0x0644)
        {
            return 0;
        }

        switch (alef)
        {
            case 0x0622: return 0xFEF5;
            case 0x0623: return 0xFEF7;
            case 0x0625: return 0xFEF9;
            case 0x0627: return 0xFEFB;
            default: return 0;
        }
    }

    private static int lamAlefIsolated(int lam, int alef)
    {
        if (lam != 0x0644)
        {
            return 0;
        }

        switch (alef)
        {
            case 0x0622: return 0xFEF6;
            case 0x0623: return 0xFEF8;
            case 0x0625: return 0xFEFA;
            case 0x0627: return 0xFEFC;
            default: return 0;
        }
    }

    public static String shape(String text)
    {
        if (text == null || text.isEmpty() || !containsArabicScript(text))
        {
            return text;
        }

        StringBuilder out = new StringBuilder(text.length());

        for (int i = 0; i < text.length();)
        {
            int cp = text.codePointAt(i);

            if (cp == 0x00A7)
            {
                /* Minecraft formatting code — copy § and the next char untouched */
                out.appendCodePoint(cp);

                int next = i + Character.charCount(cp);

                if (next < text.length())
                {
                    out.appendCodePoint(text.codePointAt(next));
                    i = next + Character.charCount(text.codePointAt(next));
                }
                else
                {
                    i = next;
                }

                continue;
            }

            int[] forms = getForms(cp);

            if (forms == null)
            {
                out.appendCodePoint(cp);
                i += Character.charCount(cp);

                continue;
            }

            int nextIndex = i + Character.charCount(cp);
            int nextCp = nextIndex < text.length() ? text.codePointAt(nextIndex) : -1;
            int ligature = lamAlef(cp, nextCp);

            if (ligature != 0)
            {
                int prev = previousLetter(text, i);
                boolean prevJoins = prev != -1 && connectsAfter(prev);

                out.appendCodePoint(prevJoins ? ligature : lamAlefIsolated(cp, nextCp));
                i = nextIndex + Character.charCount(nextCp);

                continue;
            }

            int prev = previousLetter(text, i);
            int next = nextLetter(text, i);
            boolean prevJoins = prev != -1 && connectsAfter(prev);
            boolean nextJoins = next != -1 && connectsBefore(next);
            int shaped;

            if (prevJoins && nextJoins && forms[3] != 0)
            {
                shaped = forms[3];
            }
            else if (prevJoins && forms[1] != 0)
            {
                shaped = forms[1];
            }
            else if (nextJoins && forms[2] != 0)
            {
                shaped = forms[2];
            }
            else
            {
                shaped = forms[0];
            }

            out.appendCodePoint(shaped != 0 ? shaped : cp);
            i += Character.charCount(cp);
        }

        return out.toString();
    }
}
