package mchorse.bbs_mod.camera.clips.screen;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import java.util.ArrayList;
import java.util.List;

public class GrainClip extends CameraClip
{
    public final KeyframeChannel<Double> strength = new KeyframeChannel<>("strength", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> size = new KeyframeChannel<>("size", KeyframeFactories.DOUBLE);

    public final KeyframeChannel<Double>[] channels;

    private GrainEffect effect = new GrainEffect();

    public static List<GrainEffect> getEffects(ClipContext context)
    {
        return context.clipData.get("grainEffects", ArrayList::new);
    }

    public GrainClip()
    {
        this.channels = new KeyframeChannel[] {this.strength, this.size};

        this.add(this.strength);
        this.add(this.size);
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        float t = context.relativeTick + context.transition;
        float factor = this.envelope.factorEnabled(this.duration.get(), t);

        float str = (this.strength.isEmpty() ? 0F : (float) (double) this.strength.interpolate(t)) * 0.25F;

        if (str > 0F)
        {
            /* Default size 1px when channel is empty */
            float px = this.size.isEmpty() ? 1F : (float) (double) this.size.interpolate(t) * 0.25F;

            this.effect.strength = str * factor;
            this.effect.size = Math.max(0.25F, px);

            getEffects(context).add(this.effect);
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
        return new GrainClip();
    }
}
