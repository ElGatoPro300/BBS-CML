package mchorse.bbs_mod.events.register;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class RegisterFilmSyncEvent
{
    private static final List<Consumer<Object>> openFilmHandlers = new ArrayList<>();
    private static final List<Consumer<Object>> saveFilmHandlers = new ArrayList<>();
    private static final List<BiConsumer<Object, Object>> renderDopeSheetHandlers = new ArrayList<>();

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

    public void registerRenderDopeSheet(BiConsumer<Object, Object> handler)
    {
        if (handler != null)
        {
            renderDopeSheetHandlers.add(handler);
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

    public List<BiConsumer<Object, Object>> getRenderDopeSheetHandlers()
    {
        return renderDopeSheetHandlers;
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

    public static void postRenderDopeSheet(Object context, Object area)
    {
        for (BiConsumer<Object, Object> handler : renderDopeSheetHandlers)
        {
            try
            {
                handler.accept(context, area);
            }
            catch (Throwable ignored)
            {}
        }
    }
}
