package mchorse.bbs_mod.events.register;

import mchorse.bbs_mod.ui.film.FilmUiCapabilities;
import mchorse.bbs_mod.ui.film.IFilmUiWorkspace;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.styles.UIStyle;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Fired once on client init so a Fabric addon can register Minecut (or other) film UI.
 */
public class RegisterFilmUiAddonEvent
{
    public void registerWorkspaceFactory(Function<UIFilmPanel, IFilmUiWorkspace> factory)
    {
        FilmUiCapabilities.registerWorkspaceFactory(factory);
    }

    public void registerMinecutStyleFactory(Supplier<UIStyle> factory)
    {
        FilmUiCapabilities.registerMinecutStyleFactory(factory);
    }

    public void setSparseTracksPreferred(boolean preferred)
    {
        FilmUiCapabilities.setSparseTracksPreferred(preferred);
    }
}
