package mchorse.bbs_mod.events.register;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RegisterFormRenderPhaseEvent
{
    private final List<Consumer<Object>> preRenderListeners = new ArrayList<>();
    private final List<Consumer<Object>> postRenderListeners = new ArrayList<>();

    public void registerPreRender(Consumer<Object> listener)
    {
        if (listener != null)
        {
            this.preRenderListeners.add(listener);
        }
    }

    public void registerPostRender(Consumer<Object> listener)
    {
        if (listener != null)
        {
            this.postRenderListeners.add(listener);
        }
    }

    public List<Consumer<Object>> getPreRenderListeners()
    {
        return this.preRenderListeners;
    }

    public List<Consumer<Object>> getPostRenderListeners()
    {
        return this.postRenderListeners;
    }
}
