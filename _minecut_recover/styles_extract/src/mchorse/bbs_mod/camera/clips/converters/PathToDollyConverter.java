package mchorse.bbs_mod.camera.clips.converters;

import mchorse.bbs_mod.camera.clips.overwrite.DollyClip;
import mchorse.bbs_mod.camera.clips.overwrite.PathClip;
import mchorse.bbs_mod.camera.data.Angle;
import mchorse.bbs_mod.camera.data.Position;

public class PathToDollyConverter implements IClipConverter<PathClip, DollyClip>
{
    @Override
    public DollyClip convert(PathClip path)
    {
        if (path.size() != 2)
        {
            return null;
        }

        DollyClip dolly = new DollyClip();

        Position a = path.getPoint(0);
        Position b = path.getPoint(1);
        Angle angle = Angle.angle(a.point, b.point);

        dolly.copy(path);
        dolly.distance.set((float) a.point.length(b.point));
        dolly.position.get().copy(a);

        dolly.yaw.set(angle.yaw);
        dolly.pitch.set(angle.pitch);
        dolly.interp.copy(path.interpolationPoint);

        return dolly;
    }
}