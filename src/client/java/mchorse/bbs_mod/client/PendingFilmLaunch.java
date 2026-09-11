package mchorse.bbs_mod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;

public class PendingFilmLaunch
{
    private static final int OPEN_DELAY_TICKS = 8;
    private static final int WORLD_MATCH_GRACE_TICKS = 40;
    private static final int FAILSAFE_TICKS = 600;

    private static String worldFolder;
    private static String filmId;
    private static int waitTicks;
    private static boolean joinedSinceSchedule;

    public static void schedule(String world, String film)
    {
        PendingFilmLaunch.worldFolder = world;
        PendingFilmLaunch.filmId = film;
        PendingFilmLaunch.waitTicks = 0;
        PendingFilmLaunch.joinedSinceSchedule = false;
    }

    public static void clear()
    {
        PendingFilmLaunch.worldFolder = null;
        PendingFilmLaunch.filmId = null;
        PendingFilmLaunch.waitTicks = 0;
        PendingFilmLaunch.joinedSinceSchedule = false;
    }

    public static boolean hasPending()
    {
        return PendingFilmLaunch.worldFolder != null && PendingFilmLaunch.filmId != null;
    }

    public static void onJoin()
    {
        if (PendingFilmLaunch.hasPending() && !PendingFilmLaunch.joinedSinceSchedule)
        {
            PendingFilmLaunch.joinedSinceSchedule = true;
            PendingFilmLaunch.waitTicks = 0;
        }
    }

    public static void tick(Minecraft client)
    {
        if (!PendingFilmLaunch.hasPending())
        {
            return;
        }

        PendingFilmLaunch.waitTicks += 1;

        if (PendingFilmLaunch.waitTicks > PendingFilmLaunch.FAILSAFE_TICKS)
        {
            PendingFilmLaunch.tryOpen(client);

            return;
        }

        if (!PendingFilmLaunch.isReadyToOpen(client))
        {
            return;
        }

        if (PendingFilmLaunch.waitTicks < PendingFilmLaunch.OPEN_DELAY_TICKS)
        {
            return;
        }

        boolean worldMatches = WorldLaunchHelper.isCurrentWorld(client, PendingFilmLaunch.worldFolder);
        boolean allowWithoutWorldMatch = PendingFilmLaunch.joinedSinceSchedule
            && PendingFilmLaunch.waitTicks >= PendingFilmLaunch.WORLD_MATCH_GRACE_TICKS;

        if (!worldMatches && !allowWithoutWorldMatch)
        {
            return;
        }

        PendingFilmLaunch.tryOpen(client);
    }

    private static boolean isReadyToOpen(Minecraft client)
    {
        if (client.player == null || client.level == null)
        {
            return false;
        }

        Screen screen = client.gui.screen();

        if (screen instanceof LevelLoadingScreen)
        {
            return false;
        }

        return true;
    }

    private static void tryOpen(Minecraft client)
    {
        if (!PendingFilmLaunch.hasPending())
        {
            return;
        }

        String film = PendingFilmLaunch.filmId;

        PendingFilmLaunch.clear();
        client.execute(() -> FilmLaunchHelper.openFilmNow(film));
    }
}
