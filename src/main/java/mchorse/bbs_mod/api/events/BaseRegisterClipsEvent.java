package mchorse.bbs_mod.api.events;

import mchorse.bbs_mod.camera.clips.ClipFactoryData;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.factory.MapFactory;

/**
 * Shared base class for clip registration events.
 */
public abstract class BaseRegisterClipsEvent
{
    public final MapFactory<Clip, ClipFactoryData> factory;

    public BaseRegisterClipsEvent(MapFactory<Clip, ClipFactoryData> factory)
    {
        this.factory = factory;
    }
}
