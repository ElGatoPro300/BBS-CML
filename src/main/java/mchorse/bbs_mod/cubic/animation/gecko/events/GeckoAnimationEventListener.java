package mchorse.bbs_mod.cubic.animation.gecko.events;

import mchorse.bbs_mod.forms.entities.IEntity;

public interface GeckoAnimationEventListener
{
    void onEvent(IEntity entity, GeckoAnimationEvent event);
}
