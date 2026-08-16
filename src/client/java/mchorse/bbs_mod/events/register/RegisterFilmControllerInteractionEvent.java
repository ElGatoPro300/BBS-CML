package mchorse.bbs_mod.events.register;

import mchorse.bbs_mod.ui.film.controller.UIFilmController;
import mchorse.bbs_mod.ui.framework.UIContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class RegisterFilmControllerInteractionEvent
{
    private final List<BiFunction<UIFilmController, UIContext, Boolean>> clickHandlers = new ArrayList<>();
    private final List<BiConsumer<UIFilmController, UIContext>> updateHandlers = new ArrayList<>();

    public void registerClickHandler(BiFunction<UIFilmController, UIContext, Boolean> handler)
    {
        if (handler != null)
        {
            this.clickHandlers.add(handler);
        }
    }

    public void registerUpdateHandler(BiConsumer<UIFilmController, UIContext> handler)
    {
        if (handler != null)
        {
            this.updateHandlers.add(handler);
        }
    }

    public List<BiFunction<UIFilmController, UIContext, Boolean>> getClickHandlers()
    {
        return this.clickHandlers;
    }

    public List<BiConsumer<UIFilmController, UIContext>> getUpdateHandlers()
    {
        return this.updateHandlers;
    }
}
