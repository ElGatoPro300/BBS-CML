package mchorse.bbs_mod.camera.clips.converters;

import mchorse.bbs_mod.camera.clips.overwrite.KeyframeClip;
import mchorse.bbs_mod.camera.clips.overwrite.PathClip;
import mchorse.bbs_mod.camera.data.Position;

public class PathToKeyframeConverter implements IClipConverter<PathClip, KeyframeClip>
{
    @Override
    public KeyframeClip convert(PathClip path)
    {
        int c = path.size();

        long duration = path.duration.get();
        KeyframeClip keyframe = new KeyframeClip();

        keyframe.copy(path);

        for (int i = 0; i < path.size(); i++)
        {
            Position point = path.points.get(i);
            long x = (int) (i / (c - 1F) * duration);

            int index = keyframe.x.insert(x, point.point.x);
            keyframe.y.insert(x, point.point.y);
            keyframe.z.insert(x, point.point.z);
            keyframe.yaw.insert(x, (double) point.angle.yaw);
            keyframe.pitch.insert(x, (double) point.angle.pitch);
            keyframe.roll.insert(x, (double) point.angle.roll);
            keyframe.fov.insert(x, (double) point.angle.fov);

            keyframe.x.get(index).getInterpolation().copy(path.interpolationPoint);
            keyframe.y.get(index).getInterpolation().copy(path.interpolationPoint);
            keyframe.z.get(index).getInterpolation().copy(path.interpolationPoint);
            keyframe.yaw.get(index).getInterpolation().copy(path.interpolationAngle);
            keyframe.pitch.get(index).getInterpolation().copy(path.interpolationAngle);
            keyframe.roll.get(index).getInterpolation().copy(path.interpolationAngle);
            keyframe.fov.get(index).getInterpolation().copy(path.interpolationAngle);
        }

        return keyframe;
    }
}