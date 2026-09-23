package mchorse.bbs_mod.api.client.events;

import mchorse.bbs_mod.film.BaseFilmController;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * The life of a film being played: its scene is built, it ticks, it draws, it ends.
 *
 * <p>Plain Fabric events rather than the addon bus for per-frame performance.</p>
 */
public class FilmEvents
{
    public static final Event<Created> CREATED = EventFactory.createArrayBacked(Created.class, (listeners) -> (controller) ->
    {
        for (Created listener : listeners)
        {
            listener.onFilmCreated(controller);
        }
    });

    public static final Event<Tick> TICK_BEFORE = EventFactory.createArrayBacked(Tick.class, (listeners) -> (controller, ticks) ->
    {
        for (Tick listener : listeners)
        {
            listener.onFilmTick(controller, ticks);
        }
    });

    public static final Event<Tick> TICK_AFTER = EventFactory.createArrayBacked(Tick.class, (listeners) -> (controller, ticks) ->
    {
        for (Tick listener : listeners)
        {
            listener.onFilmTick(controller, ticks);
        }
    });

    public static final Event<Render> RENDER_AFTER = EventFactory.createArrayBacked(Render.class, (listeners) -> (controller, context) ->
    {
        for (Render listener : listeners)
        {
            listener.onFilmRender(controller, context);
        }
    });

    public static final Event<Shutdown> SHUTDOWN = EventFactory.createArrayBacked(Shutdown.class, (listeners) -> (controller) ->
    {
        for (Shutdown listener : listeners)
        {
            listener.onFilmShutdown(controller);
        }
    });

    public static interface Created
    {
        public void onFilmCreated(BaseFilmController controller);
    }

    public static interface Tick
    {
        public void onFilmTick(BaseFilmController controller, int ticks);
    }

    public static interface Render
    {
        public void onFilmRender(BaseFilmController controller, WorldRenderContext context);
    }

    public static interface Shutdown
    {
        public void onFilmShutdown(BaseFilmController controller);
    }
}
