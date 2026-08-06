package mchorse.bbs_mod.events.register;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.Area;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class RegisterClipInteractionEvent
{
    private final List<Consumer<Object>> doubleClickHandlers = new ArrayList<>();
    private final List<Consumer<Object>> curvePickerHandlers = new ArrayList<>();

    private static final List<BiConsumer<UIContext, Area>> dopeSheetRenderHandlers = new ArrayList<>();

    public void registerDoubleClick(Consumer<Object> handler)
    {
        if (handler != null)
        {
            this.doubleClickHandlers.add(handler);
        }
    }

    public void registerCurvePickerHandler(Consumer<Object> handler)
    {
        if (handler != null)
        {
            this.curvePickerHandlers.add(handler);
        }
    }

    public void registerDopeSheetRender(BiConsumer<UIContext, Area> handler)
    {
        if (handler != null)
        {
            dopeSheetRenderHandlers.add(handler);
        }
    }

    public List<Consumer<Object>> getDoubleClickHandlers()
    {
        return this.doubleClickHandlers;
    }

    public List<Consumer<Object>> getCurvePickerHandlers()
    {
        return this.curvePickerHandlers;
    }

    public List<BiConsumer<UIContext, Area>> getDopeSheetRenderHandlers()
    {
        return dopeSheetRenderHandlers;
    }

    public static void postDopeSheetRender(UIContext context, Area area)
    {
        for (BiConsumer<UIContext, Area> handler : dopeSheetRenderHandlers)
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
