package mchorse.bbs_mod.events.register;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RegisterFilmSyncEvent
{
    private final List<Consumer<Object>> openFilmHandlers = new ArrayList<>();
    private final List<Consumer<Object>> saveFilmHandlers = new ArrayList<>();

    public void registerOpenFilm(Consumer<Object> handler)
    {
        if (handler != null)
        {
            this.openFilmHandlers.add(handler);
        }
    }

    public void registerSaveFilm(Consumer<Object> handler)
    {
        if (handler != null)
        {
            this.saveFilmHandlers.add(handler);
        }
    }

    public List<Consumer<Object>> getOpenFilmHandlers()
    {
        return this.openFilmHandlers;
    }

    public List<Consumer<Object>> getSaveFilmHandlers()
    {
        return this.saveFilmHandlers;
    }
}
