package mchorse.bbs_mod.events.register;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RegisterFilmSyncEvent
{
    private static final List<Consumer<Object>> openFilmHandlers = new ArrayList<>();
    private static final List<Consumer<Object>> saveFilmHandlers = new ArrayList<>();

    public void registerOpenFilm(Consumer<Object> handler)
    {
        if (handler != null)
        {
            openFilmHandlers.add(handler);
        }
    }

    public void registerSaveFilm(Consumer<Object> handler)
    {
        if (handler != null)
        {
            saveFilmHandlers.add(handler);
        }
    }

    public List<Consumer<Object>> getOpenFilmHandlers()
    {
        return openFilmHandlers;
    }

    public List<Consumer<Object>> getSaveFilmHandlers()
    {
        return saveFilmHandlers;
    }

    public static void postOpenFilm(Object film)
    {
        for (Consumer<Object> handler : openFilmHandlers)
        {
            try
            {
                handler.accept(film);
            }
            catch (Throwable ignored)
            {}
        }
    }

    public static void postSaveFilm(Object film)
    {
        for (Consumer<Object> handler : saveFilmHandlers)
        {
            try
            {
                handler.accept(film);
            }
            catch (Throwable ignored)
            {}
        }
    }
}
