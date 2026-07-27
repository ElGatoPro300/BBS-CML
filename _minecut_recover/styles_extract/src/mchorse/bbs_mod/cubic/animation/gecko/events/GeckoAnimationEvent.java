package mchorse.bbs_mod.cubic.animation.gecko.events;

import mchorse.bbs_mod.cubic.animation.gecko.model.GeckoAnimationState;

public class GeckoAnimationEvent
{
    public final GeckoAnimationState state;
    public final String animation;
    public final boolean active;

    public GeckoAnimationEvent(GeckoAnimationState state, String animation, boolean active)
    {
        this.state = state;
        this.animation = animation;
        this.active = active;
    }
}
