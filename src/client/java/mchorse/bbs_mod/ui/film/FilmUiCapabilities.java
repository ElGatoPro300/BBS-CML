package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.ui.framework.styles.UIStyle;
import mchorse.bbs_mod.ui.utils.icons.Icon;

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
    private static Function<String, Icon> trackIconResolver;
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

    public static void registerTrackIconResolver(Function<String, Icon> resolver)
    {
        trackIconResolver = resolver;
    }

    /**
     * When true, sparse Model-track timeline UX is preferred while the Minecut skin is active
     * (default Pose/Transform, keep tracks with keyframes, Remove track in the context menu).
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

    public static Icon resolveTrackIcon(String trackId)
    {
        if (trackIconResolver == null || trackId == null || !UIStyle.isMinecut())
        {
            return null;
        }

        return trackIconResolver.apply(trackId);
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
        trackIconResolver = null;
        sparseTracksPreferred = false;
    }
}
