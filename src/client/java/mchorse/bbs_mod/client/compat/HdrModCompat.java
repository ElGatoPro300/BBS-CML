package mchorse.bbs_mod.client.compat;

import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Soft compatibility with <a href="https://github.com/rrtt217/Minecraft-HDR-Mod">rrtt217's HDR Mod</a>.
 * Detects the mod and its {@code enableHDR} config without a hard dependency.
 * <p>
 * When HDR presentation is active, its {@code _blitToScreen} color-transform pass can leave
 * blend/FBO state wrong and present the film offscreen world full-screen — film editor UI
 * then loses panel backgrounds. BBS gates opaque chrome + GL restore on {@link #isHdrPresentationActive()}.
 */
public final class HdrModCompat
{
    private static final String MOD_ID = "hdr_mod";
    private static final String CONFIG_CLASS = "xyz.rrtt217.HDRMod.config.HDRModConfig";
    private static final String AUTO_CONFIG_CLASS = "me.shedaniel.autoconfig.AutoConfig";

    private static boolean modChecked;
    private static boolean modPresent;
    private static boolean configResolved;
    private static boolean configLookupFailed;
    private static Object configHolder;
    private static Method getConfigMethod;
    private static Field enableHdrField;

    private HdrModCompat()
    {}

    public static boolean isModPresent()
    {
        if (!modChecked)
        {
            modPresent = FabricLoader.getInstance().isModLoaded(MOD_ID);
            modChecked = true;
        }

        return modPresent;
    }

    /**
     * True when HDR Mod is loaded and its general {@code enableHDR} option is on
     * (or the option cannot be read — treat as on so the UI safety path still runs).
     */
    public static boolean isHdrPresentationActive()
    {
        if (!isModPresent())
        {
            return false;
        }

        Boolean enabled = readEnableHdr();

        return enabled == null || enabled.booleanValue();
    }

    private static Boolean readEnableHdr()
    {
        if (configLookupFailed)
        {
            return null;
        }

        try
        {
            resolveConfigAccess();

            if (configHolder == null || getConfigMethod == null || enableHdrField == null)
            {
                return null;
            }

            Object config = getConfigMethod.invoke(configHolder);

            if (config == null)
            {
                return null;
            }

            return enableHdrField.getBoolean(config);
        }
        catch (Throwable t)
        {
            configLookupFailed = true;

            return null;
        }
    }

    private static void resolveConfigAccess() throws Exception
    {
        if (configResolved)
        {
            return;
        }

        configResolved = true;

        Class<?> configClass = Class.forName(CONFIG_CLASS);
        Class<?> autoConfigClass = Class.forName(AUTO_CONFIG_CLASS);
        Method getConfigHolder = autoConfigClass.getMethod("getConfigHolder", Class.class);

        configHolder = getConfigHolder.invoke(null, configClass);
        getConfigMethod = configHolder.getClass().getMethod("getConfig");
        enableHdrField = configClass.getField("enableHDR");
    }
}
