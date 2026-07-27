package mchorse.bbs_mod.settings;

/**
 * Optional UI skins registered by Fabric addons (e.g. Minecut).
 * Core CML stays Classic unless an addon calls {@link #enableMinecutStyle()}.
 */
public final class UiStyleCapabilities
{
    public static final int CLASSIC = 0;
    public static final int MINECUT = 1;

    private static boolean minecutStyleAvailable;

    private UiStyleCapabilities()
    {}

    public static void enableMinecutStyle()
    {
        minecutStyleAvailable = true;
    }

    public static boolean isMinecutStyleAvailable()
    {
        return minecutStyleAvailable;
    }
}
