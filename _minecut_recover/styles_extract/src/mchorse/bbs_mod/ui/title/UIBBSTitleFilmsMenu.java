package mchorse.bbs_mod.ui.title;

import mchorse.bbs_mod.client.CrossWorldFilmScanner;
import mchorse.bbs_mod.client.FilmLaunchHelper;
import mchorse.bbs_mod.film.CrossWorldFilmEntry;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.class_310;
import net.minecraft.class_437;
import net.minecraft.class_442;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UIBBSTitleFilmsMenu extends UIBaseMenu
{
    private final class_437 returnScreen;
    private final Map<String, CrossWorldFilmEntry> entryMap = new HashMap<>();
    private final UIStringList filmsList;
    private final UISearchList<String> filmsSearch;
    private final UILabel statusLabel;
    private final UIButton backButton;

    private boolean scanning;

    public UIBBSTitleFilmsMenu()
    {
        this(new class_442());
    }

    public UIBBSTitleFilmsMenu(class_437 returnScreen)
    {
        this.returnScreen = returnScreen == null ? new class_442() : returnScreen;

        UIElement panel = new UIElement();

        this.backButton = new UIButton(UIKeys.RAW_BACK, (b) ->
        {
            class_310.method_1551().method_1507(this.returnScreen);
        });

        this.statusLabel = new UILabel(UIKeys.TITLE_MENU_FILMS_LOADING);
        this.statusLabel.color(0x88ffffff);

        this.filmsList = new UIStringList((list) ->
        {
            if (list.isEmpty())
            {
                return;
            }

            this.openSelected(list.get(0));
        })
        {
            @Override
            protected String elementToString(UIContext context, int i, String element)
            {
                CrossWorldFilmEntry entry = UIBBSTitleFilmsMenu.this.entryMap.get(element);

                return entry == null ? element : entry.getDisplayLabel();
            }

            @Override
            protected void renderElementPart(UIContext context, String element, int i, int x, int y, boolean hover, boolean selected)
            {
                int textColor = hover ? Colors.HIGHLIGHT : Colors.WHITE;
                int textX = x + 4;

                context.batcher.icon(Icons.FILM, Colors.WHITE, x + 4, y + (this.scroll.scrollItemSize - Icons.FILM.h) / 2);
                textX = x + 4 + Icons.FILM.w + 4;

                context.batcher.textShadow(this.elementToString(context, i, element), textX, y + (this.scroll.scrollItemSize - context.batcher.getFont().getHeight()) / 2, textColor);
            }
        };

        this.filmsList.background();

        this.filmsSearch = new UISearchList<>(this.filmsList).label(UIKeys.GENERAL_SEARCH);
        this.filmsSearch.list.background();

        panel.add(this.backButton, this.statusLabel, this.filmsSearch);
        this.main.add(panel);

        panel.relative(this.viewport).x(0.15F).y(0.1F).w(0.7F).h(0.8F);
        this.backButton.relative(panel).x(0).y(0).w(100).h(20);
        this.statusLabel.relative(panel).x(0).y(24).w(1F).h(16);
        this.filmsSearch.relative(panel).x(0).y(44).w(1F).h(1F, -44);
        this.filmsSearch.search.w(1F, -25);
    }

    @Override
    public void onOpen(UIBaseMenu oldMenu)
    {
        super.onOpen(oldMenu);

        this.refreshFilms();
    }

    private void refreshFilms()
    {
        if (this.scanning)
        {
            return;
        }

        this.scanning = true;
        this.statusLabel.label = UIKeys.TITLE_MENU_FILMS_LOADING;
        this.filmsList.clear();
        this.entryMap.clear();

        CrossWorldFilmScanner.scanAsync().whenComplete((entries, error) ->
        {
            class_310.method_1551().execute(() ->
            {
                this.scanning = false;

                if (error != null)
                {
                    error.printStackTrace();
                    this.statusLabel.label = UIKeys.TITLE_MENU_FILMS_ERROR;

                    return;
                }

                List<String> keys = new ArrayList<>();

                for (CrossWorldFilmEntry entry : entries)
                {
                    String key = entry.encodeKey();

                    this.entryMap.put(key, entry);
                    keys.add(key);
                }

                this.filmsList.add(keys);

                if (keys.isEmpty())
                {
                    this.statusLabel.label = UIKeys.TITLE_MENU_FILMS_EMPTY;
                }
                else
                {
                    this.statusLabel.label = UIKeys.TITLE_MENU_FILMS_COUNT.format(String.valueOf(keys.size()));
                }
            });
        });
    }

    private void openSelected(String key)
    {
        CrossWorldFilmEntry entry = this.entryMap.get(key);

        if (entry == null)
        {
            return;
        }

        FilmLaunchHelper.launch(entry);
    }

    @Override
    public boolean canPause()
    {
        return false;
    }
}
