package mchorse.bbs_mod.camera.clips.converters;

import mchorse.bbs_mod.camera.clips.overwrite.DollyClip;
import mchorse.bbs_mod.camera.clips.overwrite.KeyframeClip;

public class DollyToKeyframeConverter implements IClipConverter<DollyClip, KeyframeClip>
{
    @Override
    public KeyframeClip convert(DollyClip clip)
    {
        return new PathToKeyframeConverter().convert(new DollyToPathConverter().convert(clip));
    }
}