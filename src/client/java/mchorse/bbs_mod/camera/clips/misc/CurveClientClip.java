package mchorse.bbs_mod.camera.clips.misc;

import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.iris.ShaderCurves;
import mchorse.bbs_mod.utils.iris.ShaderOpacityPatch;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.KeyframeSegment;

public class CurveClientClip extends CurveClip
{
    public CurveClientClip()
    {}

    @Override
    protected void breakDownClip(Clip original, int offset)
    {
        super.breakDownClip(original, offset);

        /* Clean up keyframes prior to broken apart */
        for (KeyframeChannel<Double> channel : this.channels.getChannels()) this.trimCurrentChannel(channel, offset);
        this.trimCurrentChannel(this.chromaSky, offset);

        CurveClip curveClip = (CurveClip) original;

        /* Clean up keyframes prior to broken apart */
        for (KeyframeChannel<Double> channel : curveClip.channels.getChannels()) this.trimOriginalChannel(channel, offset);
        this.trimOriginalChannel(curveClip.chromaSky, offset);
    }

    private <T> void trimCurrentChannel(KeyframeChannel<T> channel, int offset)
    {
        channel.moveX(-offset);

        KeyframeSegment<T> segment = channel.find(0);

        if (segment != null)
        {
            while (segment.a != channel.get(0)) channel.remove(0);
        }
    }

    private <T> void trimOriginalChannel(KeyframeChannel<T> channel, int offset)
    {
        KeyframeSegment<T> segment = channel.find(offset);

        if (segment != null)
        {
            while (segment.b != channel.get(channel.getKeyframes().size() - 1))
            {
                channel.remove(channel.getKeyframes().size() - 1);
            }
        }
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        super.applyClip(context, position);

        for (KeyframeChannel<Double> channel : this.channels.getChannels())
        {
            if (channel.isEmpty())
            {
                continue;
            }

            String id = channel.getId();

            if (ShaderCurves.SHADER_SHADOW_OPACITY.equals(id)
                || id.equals(SHADER_CURVES_PREFIX + ShaderCurves.SHADER_SHADOW_OPACITY))
            {
                ShaderOpacityPatch.ensureShadowOpacityVariable();

                ShaderCurves.ShaderVariable shadowOpacity = ShaderCurves.variableMap.get(ShaderCurves.SHADER_SHADOW_OPACITY);

                if (shadowOpacity != null)
                {
                    float value = channel.interpolate(context.relativeTick + context.transition).floatValue();

                    shadowOpacity.value = Math.max(0F, Math.min(1F, value));
                }

                continue;
            }

            if (ShaderCurves.SUN_PATH_ROTATION.equals(id)
                || id.equals(SHADER_CURVES_PREFIX + ShaderCurves.SUN_PATH_ROTATION))
            {
                /* Uniform is read live from clip data; keep variable registered for Iris. */
                ShaderCurves.ensureSunPathRotationVariable();

                continue;
            }

            if (id.startsWith(SHADER_CURVES_PREFIX))
            {
                ShaderCurves.ShaderVariable variable = ShaderCurves.variableMap.get(id.substring(SHADER_CURVES_PREFIX.length()));

                if (variable != null)
                {
                    variable.value = channel.interpolate(context.relativeTick + context.transition).floatValue();
                }
            }
        }
    }

    @Override
    protected Clip create()
    {
        return new CurveClientClip();
    }
}
