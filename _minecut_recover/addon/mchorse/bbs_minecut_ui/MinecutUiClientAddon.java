package mchorse.bbs_minecut_ui;

import mchorse.bbs_minecut_ui.film.UIMinecutFilmWorkspace;
import mchorse.bbs_minecut_ui.styles.MinecutUIStyle;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.addons.BBSClientAddon;
import mchorse.bbs_mod.events.Subscribe;
import mchorse.bbs_mod.events.register.RegisterFilmUiAddonEvent;
import mchorse.bbs_mod.events.register.RegisterL10nEvent;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.film.FilmUiCapabilities;
import mchorse.bbs_mod.ui.framework.styles.UIStyle;

import java.util.Collections;

/**
 * Fabric client addon that enables Minecut film UI on top of BBS CML Edition.
 */
public class MinecutUiClientAddon extends BBSClientAddon
{
    public MinecutUiClientAddon()
    {
        /* Only register factories here. Do NOT touch UIKeys / L10n — entrypoints
           load before BBSModClient creates L10n (see crash NPE on UIKeys.<clinit>). */
        this.installFilmUiFactories();
    }

    private void installFilmUiFactories()
    {
        FilmUiCapabilities.registerWorkspaceFactory(UIMinecutFilmWorkspace::new);
        FilmUiCapabilities.registerMinecutStyleFactory(MinecutUIStyle::new);
        FilmUiCapabilities.setSparseTracksPreferred(true);
    }

    private void refreshUiStyleModes()
    {
        if (BBSSettings.uiStyle == null)
        {
            return;
        }

        /* Use L10n.lang keys directly — avoid UIKeys static init during early boot. */
        BBSSettings.uiStyle.modes(
            L10n.lang("bbs.ui_style.classic"),
            L10n.lang("bbs.ui_style.minecut")
        );
        UIStyle.invalidateMinecutCache();
    }

    @Subscribe
    @Override
    public void onRegisterFilmUiAddon(RegisterFilmUiAddonEvent event)
    {
        this.installFilmUiFactories();
        this.refreshUiStyleModes();
    }

    @Subscribe
    @Override
    public void onRegisterL10n(RegisterL10nEvent event)
    {
        event.l10n.register((lang) ->
            Collections.singletonList(new Link("bbs_minecut_ui", "strings/" + lang + ".json")));
        event.l10n.reload();
        this.refreshUiStyleModes();
    }
}
