package mchorse.bbs_mod.cubic.physics;

import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.interps.AutoBezier;
import mchorse.bbs_mod.utils.interps.IInterp;

/**
 * Animatable global wind properties layered over the form's wind config at
 * playback. Lives in the main source set; WindDef defaults are mirrored here.
 */
public class WindDynamicParams implements IMapSerializable
{
    /* Mirror WindDef.NONE; duplicated because that config record is client-only. */
    public static final float DEFAULT_POWER = 0F;
    public static final float DEFAULT_DIR_X = 1F;
    public static final float DEFAULT_DIR_Y = 0F;
    public static final float DEFAULT_DIR_Z = 0F;
    public static final float DEFAULT_GUSTINESS = 0.5F;
    public static final float DEFAULT_GUST_SPEED = 1F;
    public static final float DEFAULT_GUST_SCALE = 1F;
    public static final boolean DEFAULT_MODEL_RELATIVE = false;

    public static final WindDynamicParams DEFAULT = new WindDynamicParams();

    public float power = DEFAULT_POWER;
    public float dirX = DEFAULT_DIR_X;
    public float dirY = DEFAULT_DIR_Y;
    public float dirZ = DEFAULT_DIR_Z;
    public float gustiness = DEFAULT_GUSTINESS;
    public float gustSpeed = DEFAULT_GUST_SPEED;
    public float gustScale = DEFAULT_GUST_SCALE;
    public boolean modelRelative = DEFAULT_MODEL_RELATIVE;

    public void identity()
    {
        this.power = DEFAULT_POWER;
        this.dirX = DEFAULT_DIR_X;
        this.dirY = DEFAULT_DIR_Y;
        this.dirZ = DEFAULT_DIR_Z;
        this.gustiness = DEFAULT_GUSTINESS;
        this.gustSpeed = DEFAULT_GUST_SPEED;
        this.gustScale = DEFAULT_GUST_SCALE;
        this.modelRelative = DEFAULT_MODEL_RELATIVE;
    }

    public void lerp(WindDynamicParams preA, WindDynamicParams a, WindDynamicParams b, WindDynamicParams postB, IInterp interp, float frac)
    {
        this.power = (float) interp.interpolate(IInterp.context.set(preA.power, a.power, b.power, postB.power, frac));
        this.dirX = (float) interp.interpolate(IInterp.context.set(preA.dirX, a.dirX, b.dirX, postB.dirX, frac));
        this.dirY = (float) interp.interpolate(IInterp.context.set(preA.dirY, a.dirY, b.dirY, postB.dirY, frac));
        this.dirZ = (float) interp.interpolate(IInterp.context.set(preA.dirZ, a.dirZ, b.dirZ, postB.dirZ, frac));
        this.gustiness = (float) interp.interpolate(IInterp.context.set(preA.gustiness, a.gustiness, b.gustiness, postB.gustiness, frac));
        this.gustSpeed = (float) interp.interpolate(IInterp.context.set(preA.gustSpeed, a.gustSpeed, b.gustSpeed, postB.gustSpeed, frac));
        this.gustScale = (float) interp.interpolate(IInterp.context.set(preA.gustScale, a.gustScale, b.gustScale, postB.gustScale, frac));
        this.modelRelative = a.modelRelative; /* discrete flag, held from the active keyframe */
    }

    public void autoLerp(WindDynamicParams preA, WindDynamicParams a, WindDynamicParams b, WindDynamicParams postB, float pt, float at, float bt, float qt, boolean clamped, float frac)
    {
        this.power = (float) AutoBezier.get(preA.power, a.power, b.power, postB.power, pt, at, bt, qt, clamped, frac);
        this.dirX = (float) AutoBezier.get(preA.dirX, a.dirX, b.dirX, postB.dirX, pt, at, bt, qt, clamped, frac);
        this.dirY = (float) AutoBezier.get(preA.dirY, a.dirY, b.dirY, postB.dirY, pt, at, bt, qt, clamped, frac);
        this.dirZ = (float) AutoBezier.get(preA.dirZ, a.dirZ, b.dirZ, postB.dirZ, pt, at, bt, qt, clamped, frac);
        this.gustiness = (float) AutoBezier.get(preA.gustiness, a.gustiness, b.gustiness, postB.gustiness, pt, at, bt, qt, clamped, frac);
        this.gustSpeed = (float) AutoBezier.get(preA.gustSpeed, a.gustSpeed, b.gustSpeed, postB.gustSpeed, pt, at, bt, qt, clamped, frac);
        this.gustScale = (float) AutoBezier.get(preA.gustScale, a.gustScale, b.gustScale, postB.gustScale, pt, at, bt, qt, clamped, frac);
        this.modelRelative = a.modelRelative; /* discrete flag, held from the active keyframe */
    }

    public WindDynamicParams copy()
    {
        WindDynamicParams params = new WindDynamicParams();

        params.copy(this);

        return params;
    }

    public void copy(WindDynamicParams other)
    {
        this.power = other.power;
        this.dirX = other.dirX;
        this.dirY = other.dirY;
        this.dirZ = other.dirZ;
        this.gustiness = other.gustiness;
        this.gustSpeed = other.gustSpeed;
        this.gustScale = other.gustScale;
        this.modelRelative = other.modelRelative;
    }

    public boolean isDefault()
    {
        return this.equals(DEFAULT);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (obj instanceof WindDynamicParams params)
        {
            return this.power == params.power
                && this.dirX == params.dirX
                && this.dirY == params.dirY
                && this.dirZ == params.dirZ
                && this.gustiness == params.gustiness
                && this.gustSpeed == params.gustSpeed
                && this.gustScale == params.gustScale
                && this.modelRelative == params.modelRelative;
        }

        return false;
    }

    @Override
    public void toData(MapType data)
    {
        data.putDouble("power", this.power);
        data.putDouble("dir_x", this.dirX);
        data.putDouble("dir_y", this.dirY);
        data.putDouble("dir_z", this.dirZ);
        data.putDouble("gustiness", this.gustiness);
        data.putDouble("gust_speed", this.gustSpeed);
        data.putDouble("gust_scale", this.gustScale);
        data.putBool("model_relative", this.modelRelative);
    }

    @Override
    public void fromData(MapType data)
    {
        this.power = (float) data.getDouble("power", DEFAULT.power);
        this.dirX = (float) data.getDouble("dir_x", DEFAULT.dirX);
        this.dirY = (float) data.getDouble("dir_y", DEFAULT.dirY);
        this.dirZ = (float) data.getDouble("dir_z", DEFAULT.dirZ);
        this.gustiness = (float) data.getDouble("gustiness", DEFAULT.gustiness);
        this.gustSpeed = (float) data.getDouble("gust_speed", DEFAULT.gustSpeed);
        this.gustScale = (float) data.getDouble("gust_scale", DEFAULT.gustScale);
        this.modelRelative = data.getBool("model_relative", DEFAULT.modelRelative);
    }
}
