package mchorse.bbs_mod.events.register;

import mchorse.bbs_mod.camera.clips.ClipFactoryData;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.factory.MapFactory;

public class RegisterCameraClipsEvent
{
    private final MapFactory<Clip, ClipFactoryData> factory;

    public RegisterCameraClipsEvent(MapFactory<Clip, ClipFactoryData> factory)
    {
        this.factory = factory;
    }

    public MapFactory<Clip, ClipFactoryData> getFactory()
    {
        return this.factory;
    }
}
