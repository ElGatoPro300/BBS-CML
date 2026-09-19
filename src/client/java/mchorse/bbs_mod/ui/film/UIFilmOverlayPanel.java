package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.dashboard.panels.UIDataDashboardPanel;
import mchorse.bbs_mod.ui.dashboard.panels.overlay.UIDataOverlayPanel;

import java.util.function.Consumer;

public class UIFilmOverlayPanel extends UIDataOverlayPanel<Film>
{
    public UIFilmOverlayPanel(IKey title, UIDataDashboardPanel<Film> panel, Consumer<String> callback)
    {
        super(title, panel, callback);
    }

    @Override
    protected void dupeData(String name)
    {
        super.dupeData(name);

        Film film = this.panel.getData();

        if (film != null)
        {
            film.stampCreationTimeNow();
            this.panel.save();
        }
    }
}
