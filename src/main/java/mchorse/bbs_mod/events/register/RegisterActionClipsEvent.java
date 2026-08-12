package mchorse.bbs_mod.events.register;

import mchorse.bbs_mod.camera.clips.ClipFactoryData;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.factory.MapFactory;

public class RegisterActionClipsEvent
{
    private final MapFactory<Clip, ClipFactoryData> factory;

    public RegisterActionClipsEvent(MapFactory<Clip, ClipFactoryData> factory)
    {
        this.factory = factory;
    }

    public MapFactory<Clip, ClipFactoryData> getFactory()
    {
        return this.factory;
    }
}
