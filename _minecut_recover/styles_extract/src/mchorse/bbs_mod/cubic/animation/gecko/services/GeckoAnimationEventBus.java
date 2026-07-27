package mchorse.bbs_mod.cubic.animation.gecko.services;

import mchorse.bbs_mod.cubic.animation.gecko.events.GeckoAnimationEvent;
import mchorse.bbs_mod.cubic.animation.gecko.events.GeckoAnimationEventListener;
import mchorse.bbs_mod.cubic.animation.gecko.model.GeckoAnimationState;
import mchorse.bbs_mod.forms.entities.IEntity;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GeckoAnimationEventBus
{
    private final List<GeckoAnimationEventListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(GeckoAnimationEventListener listener)
    {
        if (listener == null)
        {
            return;
        }

        this.listeners.add(listener);
    }

    public void removeListener(GeckoAnimationEventListener listener)
    {
        this.listeners.remove(listener);
    }

    public void emit(IEntity entity, GeckoAnimationState state, String animation, boolean active)
    {
        GeckoAnimationEvent event = new GeckoAnimationEvent(state, animation, active);

        for (GeckoAnimationEventListener listener : this.listeners)
        {
            listener.onEvent(entity, event);
        }
    }
}
