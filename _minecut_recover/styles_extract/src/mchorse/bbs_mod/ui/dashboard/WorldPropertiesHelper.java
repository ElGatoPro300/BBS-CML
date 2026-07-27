package mchorse.bbs_mod.ui.dashboard;

import mchorse.bbs_mod.BBSSettings;
import net.minecraft.class_1294;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_1928;
import net.minecraft.class_310;
import net.minecraft.class_3218;
import net.minecraft.class_638;
import net.minecraft.class_746;
import net.minecraft.server.MinecraftServer;
import java.util.function.IntConsumer;

/**
 * Applies world-property changes through the integrated-server API when available (no chat spam, no
 * command parsing lag). Falls back to silent {@code sendCommand} only on multiplayer without direct access.
 */
public class WorldPropertiesHelper
{
    private static volatile long clientTimeOverride = -1L;

    /** Gamma override (1.0 = vanilla 100%, 15.0 = 1500% full-bright), read by
     *  {@code SimpleOptionMixin} so it can exceed the vanilla 0..1 slider range. Negative
     *  means "no override" (vanilla setting applies). */
    private static volatile double gammaOverride = -1D;

    private WorldPropertiesHelper()
    {}

    /**
     * Client-side time override for smooth sun rotation while dragging the World Properties time slider
     * (same mechanism as the Sun Rotation curve via {@code ClientWorldPropertiesMixin}).
     */
    public static void setClientTimeOverride(long time)
    {
        clientTimeOverride = time % 24000L;

        if (clientTimeOverride < 0L)
        {
            clientTimeOverride += 24000L;
        }
    }

    public static Long getClientTimeOverride()
    {
        return clientTimeOverride >= 0L ? clientTimeOverride : null;
    }

    public static void clearClientTimeOverride()
    {
        clientTimeOverride = -1L;
    }

    public static void setGammaPercent(double percent)
    {
        gammaOverride = Math.max(0D, percent) / 100D;

        if (BBSSettings.worldGammaPercent != null)
        {
            BBSSettings.worldGammaPercent.set(percent);
        }
    }

    public static Double getGammaOverride()
    {
        return gammaOverride >= 0D ? gammaOverride : null;
    }

    public static double getGammaPercent()
    {
        if (gammaOverride >= 0D)
        {
            return gammaOverride * 100D;
        }

        class_310 mc = class_310.method_1551();

        return mc.field_1690 == null ? 100D : mc.field_1690.method_42473().method_41753() * 100D;
    }

    public static void setSunPathRotation(float degrees)
    {
        if (degrees > 180F)
        {
            degrees = 180F;
        }
        else if (degrees < -180F)
        {
            degrees = -180F;
        }

        if (BBSSettings.worldSunPathRotation != null)
        {
            BBSSettings.worldSunPathRotation.set(degrees);
        }
    }

    public static float getSunPathRotation()
    {
        if (BBSSettings.worldSunPathRotation != null)
        {
            return BBSSettings.worldSunPathRotation.get();
        }

        return 0F;
    }

    public static void setNightVision(boolean enabled)
    {
        executeCommand(enabled
            ? "effect give @a minecraft:night_vision infinite 1 true"
            : "effect clear @a minecraft:night_vision");
    }

    public static boolean hasNightVision()
    {
        class_746 player = class_310.method_1551().field_1724;

        return player != null && player.method_6059(class_1294.field_5925);
    }

    public static void setTimeOfDay(long time)
    {
        setClientTimeOverride(time);

        class_310 mc = class_310.method_1551();
        MinecraftServer server = mc.method_1576();

        if (server != null)
        {
            server.execute(() ->
            {
                class_3218 world = server.method_30002();

                if (world != null)
                {
                    world.method_29199(time);
                }
            });

            return;
        }

        sendSilentCommand("time set " + time);
    }

    public static void setGamerule(class_1928.class_4313<class_1928.class_4310> key, boolean value)
    {
        class_310 mc = class_310.method_1551();
        MinecraftServer server = mc.method_1576();

        if (server != null)
        {
            server.execute(() ->
            {
                class_3218 world = server.method_30002();

                if (world != null)
                {
                    world.method_8450().method_20746(key).method_20758(value, server);
                }
            });

            return;
        }

        String name = key.method_20771();

        sendSilentCommand("gamerule " + name + " " + value);
    }

    public static void setWeatherClear()
    {
        executeWeatherCommand("weather clear");
    }

    public static void setWeatherRain()
    {
        executeWeatherCommand("weather rain");
    }

    public static void setWeatherThunder()
    {
        executeWeatherCommand("weather thunder");
    }

    public static void killAllMobs(IntConsumer callback)
    {
        class_310 mc = class_310.method_1551();
        MinecraftServer server = mc.method_1576();

        if (server != null)
        {
            server.execute(() ->
            {
                class_3218 world = server.method_30002();
                int count = 0;

                if (world != null)
                {
                    for (class_1297 entity : world.method_27909())
                    {
                        if (!(entity instanceof class_1657))
                        {
                            count++;
                        }
                    }
                }

                sendSilentCommandOnServer(server, "kill @e[type=!minecraft:player]");

                if (callback != null)
                {
                    int finalCount = count;
                    mc.execute(() -> callback.accept(finalCount));
                }
            });

            return;
        }

        sendSilentCommand("kill @e[type=!minecraft:player]");

        if (callback != null)
        {
            callback.accept(-1);
        }
    }

    /** Runs any command silently, through the integrated server when available. */
    public static void executeCommand(String command)
    {
        executeWeatherCommand(command);
    }

    private static void executeWeatherCommand(String command)
    {
        class_310 mc = class_310.method_1551();
        MinecraftServer server = mc.method_1576();

        if (server != null)
        {
            server.execute(() -> sendSilentCommandOnServer(server, command));

            return;
        }

        sendSilentCommand(command);
    }

    public static boolean readGamerule(class_1928.class_4313<class_1928.class_4310> key, boolean fallback)
    {
        class_310 mc = class_310.method_1551();
        MinecraftServer server = mc.method_1576();

        if (server != null)
        {
            class_3218 world = server.method_30002();

            if (world != null)
            {
                try
                {
                    return world.method_8450().method_8355(key);
                }
                catch (Exception e)
                {
                    return fallback;
                }
            }
        }

        class_638 world = mc.field_1687;

        if (world == null)
        {
            return fallback;
        }

        try
        {
            return world.method_8450().method_8355(key);
        }
        catch (Exception e)
        {
            return fallback;
        }
    }

    private static void sendSilentCommandOnServer(MinecraftServer server, String command)
    {
        server.method_3734().method_44252(server.method_3739(), command);
    }

    private static void sendSilentCommand(String command)
    {
        class_746 player = class_310.method_1551().field_1724;

        if (player != null)
        {
            player.field_3944.method_45731(command);
        }
    }
}
