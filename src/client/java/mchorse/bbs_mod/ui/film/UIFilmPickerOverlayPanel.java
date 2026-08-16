package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.ui.ContentType;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.list.UIDataPathList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.utils.UIDataUtils;
import mchorse.bbs_mod.utils.DataPath;

import java.util.Collection;
import java.util.function.Consumer;

public class UIFilmPickerOverlayPanel extends UIOverlayPanel
{
    private final Consumer<String> callback;
    public final UISearchList<DataPath> filmSearch;
    public final UIDataPathList filmList;

    public UIFilmPickerOverlayPanel(Consumer<String> callback)
    {
        super(UIKeys.RENDER_QUEUE_PICK_TITLE);

        this.callback = callback;

        this.filmList = new UIDataPathList((list) ->
        {
            DataPath selected = list.get(0);

            if (!selected.folder && this.callback != null)
            {
                this.callback.accept(selected.toString());
                this.close();
            }
        });

        this.filmSearch = new UISearchList<>(this.filmList);
        this.filmSearch.label(UIKeys.GENERAL_SEARCH);
        this.filmSearch.full(this.content).x(6).w(1F, -12);

        this.content.add(this.filmSearch);

        UIDataUtils.requestNames(ContentType.FILMS, this::fillFilmNames);
    }

    private void fillFilmNames(Collection<String> names)
    {
        this.filmList.fill(names);
    }
}
