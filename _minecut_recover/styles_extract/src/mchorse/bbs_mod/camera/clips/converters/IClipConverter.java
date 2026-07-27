package mchorse.bbs_mod.camera.clips.converters;

import mchorse.bbs_mod.utils.clips.Clip;

public interface IClipConverter <A extends Clip, B extends Clip>
{
    public B convert(A clip);
}