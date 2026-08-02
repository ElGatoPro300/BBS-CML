package mchorse.bbs_mod.events.register;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RegisterClipInteractionEvent
{
    private final List<Consumer<Object>> doubleClickHandlers = new ArrayList<>();
    private final List<Consumer<Object>> curvePickerHandlers = new ArrayList<>();

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

    public List<Consumer<Object>> getDoubleClickHandlers()
    {
        return this.doubleClickHandlers;
    }

    public List<Consumer<Object>> getCurvePickerHandlers()
    {
        return this.curvePickerHandlers;
    }
}
