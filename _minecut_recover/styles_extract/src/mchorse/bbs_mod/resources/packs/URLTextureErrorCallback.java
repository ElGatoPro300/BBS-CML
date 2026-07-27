package mchorse.bbs_mod.resources.packs;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface URLTextureErrorCallback
{
    public static final Event<URLTextureErrorCallback> EVENT = EventFactory.createArrayBacked(
        URLTextureErrorCallback.class, (listeners) -> (url, error) ->
        {
            for (URLTextureErrorCallback listener : listeners)
            {
                listener.onError(url, error);
            }
        }
    );

    public void onError(String url, URLError error);
}