package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.ui.framework.styles.UIStyle;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Registration hub for the optional film UI skin addon (Minecut).
 * Core CML stays classic unless an addon registers here.
 */
public final class FilmUiCapabilities
{
    private static Function<UIFilmPanel, IFilmUiWorkspace> workspaceFactory;
    private static Supplier<UIStyle> minecutStyleFactory;
    private static boolean sparseTracksPreferred;

    private FilmUiCapabilities()
    {}

    public static void registerWorkspaceFactory(Function<UIFilmPanel, IFilmUiWorkspace> factory)
    {
        workspaceFactory = factory;
    }

    public static void registerMinecutStyleFactory(Supplier<UIStyle> factory)
    {
        minecutStyleFactory = factory;
    }

    /**
     * When true, sparse Model-track timeline UX is preferred while the Minecut skin is active.
     */
    public static void setSparseTracksPreferred(boolean preferred)
    {
        sparseTracksPreferred = preferred;
    }

    public static boolean hasAddon()
    {
        return workspaceFactory != null;
    }

    public static IFilmUiWorkspace createWorkspace(UIFilmPanel panel)
    {
        return workspaceFactory == null ? null : workspaceFactory.apply(panel);
    }

    public static UIStyle createMinecutStyle()
    {
        return minecutStyleFactory == null ? null : minecutStyleFactory.get();
    }

    public static boolean prefersSparseModelTracks()
    {
        return sparseTracksPreferred && hasAddon();
    }

    /** Test / unload helper. */
    public static void clear()
    {
        workspaceFactory = null;
        minecutStyleFactory = null;
        sparseTracksPreferred = false;
    }
}
