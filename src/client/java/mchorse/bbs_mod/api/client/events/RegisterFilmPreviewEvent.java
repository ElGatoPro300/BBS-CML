package mchorse.bbs_mod.api.client.events;

import mchorse.bbs_mod.ui.film.UIFilmPreview;

import java.util.function.Consumer;

public class RegisterFilmPreviewEvent
{
    public void register(Consumer<UIFilmPreview> consumer)
    {
        UIFilmPreview.extensions.add(consumer);
    }
}
