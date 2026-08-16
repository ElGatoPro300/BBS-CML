package mchorse.bbs_mod.camera.clips.screen;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

public class VignetteClip extends CameraClip
{
    public ValueInt color = new ValueInt("color", Colors.A100);
    public final KeyframeChannel<Double> strength = new KeyframeChannel<>("strength", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> smoothness = new KeyframeChannel<>("smoothness", KeyframeFactories.DOUBLE);

    public final KeyframeChannel<Double>[] channels;

    private ColorEffect effect = new ColorEffect();

    public VignetteClip()
    {
        this.channels = new KeyframeChannel[] {this.strength, this.smoothness};

        this.add(this.color);
        this.add(this.strength);
        this.add(this.smoothness);
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        float t = context.relativeTick + context.transition;
        float factor = this.envelope.factorEnabled(this.duration.get(), t);

        this.effect.reset();

        float str = (this.strength.isEmpty() ? 0F : (float) (double) this.strength.interpolate(t)) * 0.25F;

        if (str > 0F)
        {
            /* Default smoothness 2 × 0.25 = 0.5 when channel is empty */
            float smooth = (this.smoothness.isEmpty() ? 2F : (float) (double) this.smoothness.interpolate(t)) * 0.25F;

            this.effect.hasVignette = true;
            this.effect.vignetteColor = this.color.get();
            this.effect.vignetteStrength = str * factor;
            this.effect.vignetteSmoothness = smooth;

            ColorClip.getEffects(context).add(this.effect);
        }
    }

    @Override
    public boolean isPositionClip()
    {
        return false;
    }

    @Override
    protected Clip create()
    {
        return new VignetteClip();
    }
}
