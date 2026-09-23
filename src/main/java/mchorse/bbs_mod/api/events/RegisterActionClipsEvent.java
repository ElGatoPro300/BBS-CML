package mchorse.bbs_mod.api.events;

import mchorse.bbs_mod.camera.clips.ClipFactoryData;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.factory.MapFactory;

/**
 * Posted on both sides once BBS has registered its built-in action clips.
 */
public class RegisterActionClipsEvent extends BaseRegisterClipsEvent
{
    public RegisterActionClipsEvent(MapFactory<Clip, ClipFactoryData> factory)
    {
        super(factory);
    }
}
