package mchorse.bbs_mod.camera.clips.converters;

import mchorse.bbs_mod.camera.clips.overwrite.IdleClip;
import mchorse.bbs_mod.camera.clips.overwrite.PathClip;

public class IdleToPathConverter implements IClipConverter<IdleClip, PathClip>
{
    @Override
    public PathClip convert(IdleClip clip)
    {
        PathClip pathClip = new PathClip();

        pathClip.copy(clip);
        pathClip.points.reset();
        pathClip.points.add(clip.position.get().copy());

        return pathClip;
    }
}