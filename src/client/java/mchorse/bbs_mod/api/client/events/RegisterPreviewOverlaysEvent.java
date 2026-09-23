package mchorse.bbs_mod.api.client.events;

import mchorse.bbs_mod.ui.film.UIFilmPreview;
import mchorse.bbs_mod.ui.framework.elements.UIElement;

import java.util.function.Function;

/**
 * Posted on the client before any film editor is opened, for an addon that wants a layer of its
 * own over the editor's preview.
 */
public class RegisterPreviewOverlaysEvent
{
    public void register(Function<UIFilmPreview, UIElement> factory)
    {
        UIFilmPreview.registerOverlay(factory);
    }
}
