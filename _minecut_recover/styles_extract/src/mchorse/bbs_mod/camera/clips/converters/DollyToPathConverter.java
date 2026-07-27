package mchorse.bbs_mod.camera.clips.converters;

import mchorse.bbs_mod.camera.clips.CameraClipContext;
import mchorse.bbs_mod.camera.clips.overwrite.DollyClip;
import mchorse.bbs_mod.camera.clips.overwrite.PathClip;
import mchorse.bbs_mod.camera.data.Position;

public class DollyToPathConverter implements IClipConverter<DollyClip, PathClip>
{
    @Override
    public PathClip convert(DollyClip dolly)
    {
        PathClip path = new PathClip();
        Position position = new Position();

        dolly.applyLast(new CameraClipContext(), position);

        path.copy(dolly);
        path.points.reset();
        path.points.add(dolly.position.get().copy());
        path.points.add(position);
        path.interpolationPoint.copy(dolly.interp);
        path.interpolationAngle.copy(dolly.interp);

        return path;
    }
}