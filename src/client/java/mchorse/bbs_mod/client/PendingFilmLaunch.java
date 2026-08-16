package mchorse.bbs_mod.client;

import net.minecraft.client.MinecraftClient;

public class PendingFilmLaunch
{
    private static String worldFolder;
    private static String filmId;
    private static int waitTicks;

    public static void schedule(String world, String film)
    {
        PendingFilmLaunch.worldFolder = world;
        PendingFilmLaunch.filmId = film;
        PendingFilmLaunch.waitTicks = 0;
    }

    public static void clear()
    {
        PendingFilmLaunch.worldFolder = null;
        PendingFilmLaunch.filmId = null;
        PendingFilmLaunch.waitTicks = 0;
    }

    public static boolean hasPending()
    {
        return PendingFilmLaunch.worldFolder != null && PendingFilmLaunch.filmId != null;
    }

    public static void onJoin()
    {
        if (PendingFilmLaunch.hasPending())
        {
            PendingFilmLaunch.waitTicks = 0;
        }
    }

    public static void tick(MinecraftClient client)
    {
        if (!PendingFilmLaunch.hasPending())
        {
            return;
        }

        if (client.player == null || client.world == null || !client.isIntegratedServerRunning())
        {
            return;
        }

        PendingFilmLaunch.waitTicks += 1;

        if (PendingFilmLaunch.waitTicks < 10)
        {
            return;
        }

        if (!WorldLaunchHelper.isCurrentWorld(client, PendingFilmLaunch.worldFolder))
        {
            return;
        }

        String film = PendingFilmLaunch.filmId;

        PendingFilmLaunch.clear();
        FilmLaunchHelper.openFilm(film);
    }
}
