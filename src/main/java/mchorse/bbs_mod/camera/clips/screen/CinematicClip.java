package mchorse.bbs_mod.camera.clips.screen;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.joml.Matrices;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import org.joml.Vector3f;

public class CinematicClip extends CameraClip
{
    /* Defaults for optional fisheye shaping tracks (empty channel = these values). */
    public static final double DEFAULT_LENS_RADIUS = 1D;
    public static final double DEFAULT_LENS_HARDNESS = 1D;
    public static final double DEFAULT_LENS_SHARPEN = 1D;
    public static final double DEFAULT_LENS_DISTANCE_FACTOR = 0D;

    /* Cinematic effects */
    public final KeyframeChannel<Double> aberration = new KeyframeChannel<>("aberration", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> vhs = new KeyframeChannel<>("vhs", KeyframeFactories.DOUBLE);
    /** Intensity — channel id kept as {@code lensDistortion} for save compatibility. */
    public final KeyframeChannel<Double> lensDistortion = new KeyframeChannel<>("lensDistortion", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> lensDistanceFactor = new KeyframeChannel<>("lens_distance_factor", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> lensRadius = new KeyframeChannel<>("lens_radius", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> lensHardness = new KeyframeChannel<>("lens_hardness", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> lensSharpen = new KeyframeChannel<>("lens_sharpen", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> vintage = new KeyframeChannel<>("vintage", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> radialBlur = new KeyframeChannel<>("radialBlur", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> rain = new KeyframeChannel<>("rain", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> dust = new KeyframeChannel<>("dust", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> lightLeak = new KeyframeChannel<>("lightLeak", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> heatStrength = new KeyframeChannel<>("heat_strength", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> heatSpeed = new KeyframeChannel<>("heat_speed", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> heatScale = new KeyframeChannel<>("heat_scale", KeyframeFactories.DOUBLE);

    public final KeyframeChannel<Double>[] channels;

    private ColorEffect effect = new ColorEffect();

    public CinematicClip()
    {
        this.channels = new KeyframeChannel[] {
            this.aberration,
            this.vhs,
            this.lensDistortion,
            this.lensDistanceFactor,
            this.lensRadius,
            this.lensHardness,
            this.lensSharpen,
            this.vintage,
            this.radialBlur,
            this.rain,
            this.dust,
            this.lightLeak,
            this.heatStrength,
            this.heatSpeed,
            this.heatScale,
        };

        this.add(this.aberration);
        this.add(this.vhs);
        this.add(this.lensDistortion);
        this.add(this.lensDistanceFactor);
        this.add(this.lensRadius);
        this.add(this.lensHardness);
        this.add(this.lensSharpen);
        this.add(this.vintage);
        this.add(this.radialBlur);
        this.add(this.rain);
        this.add(this.dust);
        this.add(this.lightLeak);
        this.add(this.heatStrength);
        this.add(this.heatSpeed);
        this.add(this.heatScale);
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        float t = context.relativeTick + context.transition;
        float factor = this.envelope.factorEnabled(this.duration.get(), t);

        this.effect.reset();

        /* Cinematic effects */
        float ab = (this.aberration.isEmpty() ? 0F : (float) (double) this.aberration.interpolate(t)) * 0.25F;
        float vh = (this.vhs.isEmpty() ? 0F : (float) (double) this.vhs.interpolate(t)) * 0.25F;
        float ld = (this.lensDistortion.isEmpty() ? 0F : (float) (double) this.lensDistortion.interpolate(t)) * 0.25F;
        float ldf = this.lensDistanceFactor.isEmpty() ? (float) DEFAULT_LENS_DISTANCE_FACTOR : (float) (double) this.lensDistanceFactor.interpolate(t);
        float lr = this.lensRadius.isEmpty() ? (float) DEFAULT_LENS_RADIUS : (float) (double) this.lensRadius.interpolate(t);
        float lh = this.lensHardness.isEmpty() ? (float) DEFAULT_LENS_HARDNESS : (float) (double) this.lensHardness.interpolate(t);
        float ls = this.lensSharpen.isEmpty() ? (float) DEFAULT_LENS_SHARPEN : (float) (double) this.lensSharpen.interpolate(t);
        float vt = (this.vintage.isEmpty() ? 0F : (float) (double) this.vintage.interpolate(t)) * 0.25F;
        float rb = (this.radialBlur.isEmpty() ? 0F : (float) (double) this.radialBlur.interpolate(t)) * 0.25F;
        float rn = (this.rain.isEmpty() ? 0F : (float) (double) this.rain.interpolate(t)) * 0.25F;
        float ds = (this.dust.isEmpty() ? 0F : (float) (double) this.dust.interpolate(t)) * 0.25F;
        float ll = (this.lightLeak.isEmpty() ? 0F : (float) (double) this.lightLeak.interpolate(t)) * 0.25F;
        float hs = (this.heatStrength.isEmpty() ? 0F : (float) (double) this.heatStrength.interpolate(t)) * 0.25F;
        float hsp = (this.heatSpeed.isEmpty() ? 1F : (float) (double) this.heatSpeed.interpolate(t)) * 0.25F;
        float hsc = (this.heatScale.isEmpty() ? 1F : (float) (double) this.heatScale.interpolate(t)) * 0.25F;

        float lens = ld * factor;
        float radius = Math.max(0F, lr);
        float hardness = Math.max(0F, Math.min(1F, lh));

        /* Dolly along look to counter positive UV fit-zoom (“same place” framing). */
        if (lens > 1.0e-6F && Math.abs(ldf) > 1.0e-6F)
        {
            float distance = LensDistortionOverscan.framingDistanceOffset(lens, radius, hardness) * ldf;

            if (Math.abs(distance) > 1.0e-6F)
            {
                Vector3f rotation = Matrices.rotation(
                    MathUtils.toRad(position.angle.pitch),
                    MathUtils.toRad(-position.angle.yaw - 180)
                );

                position.point.x += rotation.x * distance;
                position.point.y += rotation.y * distance;
                position.point.z += rotation.z * distance;
            }
        }

        if (ab != 0F || vh != 0F || ld != 0F || vt != 0F || rb != 0F || rn != 0F || ds != 0F || ll != 0F || hs != 0F)
        {
            this.effect.hasCinematic = true;
            this.effect.aberration = ab * factor;
            this.effect.vhs = vh * factor;
            this.effect.lensDistortion = lens;
            this.effect.lensRadius = radius;
            this.effect.lensHardness = hardness;
            this.effect.lensSharpen = Math.max(0F, ls) * factor;
            this.effect.vintage = vt * factor;
            this.effect.radialBlur = rb * factor;
            this.effect.rain = rn * factor;
            this.effect.dust = ds * factor;
            this.effect.lightLeak = ll * factor;
            this.effect.heatStrength = hs * factor;
            this.effect.heatSpeed = hsp * factor;
            this.effect.heatScale = hsc * factor;
            this.effect.time = t / 20.0F; /* Convert timeline ticks to seconds */

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
        return new CinematicClip();
    }
}
