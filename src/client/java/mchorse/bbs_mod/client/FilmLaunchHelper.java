package mchorse.bbs_mod.client;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.CrossWorldFilmEntry;
import mchorse.bbs_mod.ui.ContentType;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIScreen;

import net.minecraft.client.MinecraftClient;

public class FilmLaunchHelper
{
    public static void launch(CrossWorldFilmEntry entry)
    {
        MinecraftClient client = MinecraftClient.getInstance();

        if (WorldLaunchHelper.isCurrentWorld(client, entry.worldFolder))
        {
            FilmLaunchHelper.openFilm(entry.filmId);

            return;
        }

        PendingFilmLaunch.schedule(entry.worldFolder, entry.filmId);
        WorldLaunchHelper.loadWorld(entry.worldFolder);
    }

    public static void openCrossWorldFilm(CrossWorldFilmEntry entry)
    {
        if (entry == null || entry.filmId.endsWith("/"))
        {
            return;
        }

        UIDashboard dashboard = BBSModClient.getDashboard();

        UIScreen.open(dashboard);
        dashboard.setPanel(dashboard.getPanel(UIFilmPanel.class));

        UIFilmPanel panel = dashboard.getPanel(UIFilmPanel.class);

        if (panel != null)
        {
            panel.openCrossWorldFilm(entry);
        }
    }

    public static void openFilm(String filmId)
    {
        if (filmId == null || filmId.trim().isEmpty())
        {
            return;
        }

        UIDashboard dashboard = BBSModClient.getDashboard();

        UIScreen.open(dashboard);
        dashboard.setPanel(dashboard.getPanel(UIFilmPanel.class));

        UIFilmPanel panel = dashboard.getPanel(UIFilmPanel.class);

        if (panel != null)
        {
            panel.closeCrossWorldFilmTab(filmId);
            panel.clearCrossWorldPendingJoin();
        }

        if (dashboard.documentTabsBar != null)
        {
            dashboard.documentTabsBar.closeCrossWorldFilmTabs(filmId);
            dashboard.documentTabsBar.addOrActivate(ContentType.FILMS, filmId);
        }
    }
}
