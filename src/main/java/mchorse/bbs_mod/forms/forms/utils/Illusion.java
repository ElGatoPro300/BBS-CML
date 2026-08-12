package mchorse.bbs_mod.forms.forms.utils;

import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.pose.Transform;

import java.util.ArrayList;
import java.util.List;

/**
 * Illusions: purely visual duplicates of a form that spread away from it in the
 * picked directions (no extra entities, so they're cheap to render).
 */
public class Illusion implements IMapSerializable
{
    /* Direction bit flags */
    public static final int FRONT = 1;
    public static final int BACK = 2;
    public static final int LEFT = 4;
    public static final int RIGHT = 8;
    public static final int UP = 16;
    public static final int DOWN = 32;

    public int count;
    public float spread;
    public int directions;
    public float offset;
    public float opacity;
    public boolean opacityUniform;
    public boolean invert;

    /* Equal gaps between illusions (instead of progressively shrinking ones) */
    public boolean uniform;
    public float spacing;

    /* Per illusion texture overrides */
    public final List<Link> textures = new ArrayList<>();
    public boolean randomTextures;

    /* "Real" illusions interact with world blocks (step up onto blocks in their way) */
    public boolean real;

    /* Per-illusion transform (rotation, scale, etc.) applied to every copy in this layer */
    public final Transform transform = new Transform();

    /* The illusion transform ramps from the main model (0) through each copy up to the last one (full) */
    public boolean gradual;
    public boolean gradualInvert;

    /* Staggered animation: illusion N lags behind the main model by N * delay ticks (0 = disabled) */
    public float delay;

    /* Disintegration: illusions fall apart into horizontal streaks (0 = disabled) */
    public float distort;
    public boolean distortUniform;
    public boolean distortInvert;

    /* Glow: illusions get emissive (positive) or darkened (negative), by default fading from the first illusion to the last (0 = disabled) */
    public float glow;
    public boolean glowUniform;
    public boolean glowInvert;

    public Illusion()
    {}

    public boolean hasSameShape(Illusion illusion)
    {
        return illusion != null
            && this.directions == illusion.directions
            && this.invert == illusion.invert
            && this.uniform == illusion.uniform
            && this.randomTextures == illusion.randomTextures
            && this.real == illusion.real
            && this.gradual == illusion.gradual
            && this.gradualInvert == illusion.gradualInvert
            && this.opacityUniform == illusion.opacityUniform
            && this.glowUniform == illusion.glowUniform
            && this.glowInvert == illusion.glowInvert
            && this.distortUniform == illusion.distortUniform
            && this.distortInvert == illusion.distortInvert
            && this.textures.equals(illusion.textures);
    }

    public Illusion copy()
    {
        Illusion illusion = new Illusion();

        illusion.count = this.count;
        illusion.spread = this.spread;
        illusion.directions = this.directions;
        illusion.offset = this.offset;
        illusion.opacity = this.opacity;
        illusion.opacityUniform = this.opacityUniform;
        illusion.invert = this.invert;
        illusion.uniform = this.uniform;
        illusion.spacing = this.spacing;
        illusion.textures.addAll(this.textures);
        illusion.randomTextures = this.randomTextures;
        illusion.real = this.real;
        illusion.transform.copy(this.transform);
        illusion.gradual = this.gradual;
        illusion.gradualInvert = this.gradualInvert;
        illusion.delay = this.delay;
        illusion.distort = this.distort;
        illusion.distortUniform = this.distortUniform;
        illusion.distortInvert = this.distortInvert;
        illusion.glow = this.glow;
        illusion.glowUniform = this.glowUniform;
        illusion.glowInvert = this.glowInvert;

        return illusion;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (super.equals(obj))
        {
            return true;
        }

        if (obj instanceof Illusion illusion)
        {
            return this.hasSameShape(illusion)
                && this.count == illusion.count
                && this.spread == illusion.spread
                && this.offset == illusion.offset
                && this.opacity == illusion.opacity
                && this.spacing == illusion.spacing
                && this.delay == illusion.delay
                && this.distort == illusion.distort
                && this.glow == illusion.glow
                && this.transform.equals(illusion.transform);
        }

        return false;
    }

    @Override
    public void fromData(MapType data)
    {
        this.count = data.getInt("count", 0);
        this.spread = data.getFloat("spread", 0F);
        this.directions = data.getInt("directions", 0);
        this.offset = data.getFloat("offset", 0F);
        this.opacity = data.getFloat("opacity", 0F);
        this.opacityUniform = data.getBool("opacity_uniform", false);
        this.invert = data.getBool("invert", false);
        this.uniform = data.getBool("uniform", false);
        this.spacing = data.getFloat("spacing", 0F);
        this.randomTextures = data.getBool("random_textures", false);
        this.real = data.getBool("real", false);
        this.gradual = data.getBool("gradual", false);
        this.gradualInvert = data.getBool("gradual_invert", false);
        this.delay = data.getFloat("delay", 0F);

        if (data.has("transform"))
        {
            this.transform.fromData(data.getMap("transform"));
        }
        else
        {
            this.transform.identity();
        }
        this.distort = data.getFloat("distort", 0F);
        this.distortUniform = data.getBool("distort_uniform", false);
        this.distortInvert = data.getBool("distort_invert", false);
        this.glow = data.getFloat("glow", 0F);
        this.glowUniform = data.getBool("glow_uniform", false);
        this.glowInvert = data.getBool("glow_invert", false);
        this.textures.clear();

        /* Legacy enable toggles: zero out when explicitly disabled in older saves */
        if (data.has("delay_enabled") && !data.getBool("delay_enabled", false))
        {
            this.delay = 0F;
        }

        if (data.has("glow_enabled") && !data.getBool("glow_enabled", false))
        {
            this.glow = 0F;
        }

        if (data.has("textures"))
        {
            for (BaseType type : data.getList("textures"))
            {
                if (type.isString())
                {
                    this.textures.add(Link.create(type.asString()));
                }
            }
        }
    }

    @Override
    public void toData(MapType data)
    {
        data.putInt("count", this.count);
        data.putFloat("spread", this.spread);
        data.putInt("directions", this.directions);
        data.putFloat("offset", this.offset);
        data.putFloat("opacity", this.opacity);
        data.putBool("opacity_uniform", this.opacityUniform);
        data.putBool("invert", this.invert);
        data.putBool("uniform", this.uniform);
        data.putFloat("spacing", this.spacing);
        data.putBool("random_textures", this.randomTextures);
        data.putBool("real", this.real);
        data.putBool("gradual", this.gradual);
        data.putBool("gradual_invert", this.gradualInvert);
        data.putFloat("delay", this.delay);

        if (!this.transform.isDefault())
        {
            data.put("transform", this.transform.toData());
        }
        data.putFloat("distort", this.distort);
        data.putBool("distort_uniform", this.distortUniform);
        data.putBool("distort_invert", this.distortInvert);
        data.putFloat("glow", this.glow);
        data.putBool("glow_uniform", this.glowUniform);
        data.putBool("glow_invert", this.glowInvert);

        if (!this.textures.isEmpty())
        {
            ListType list = new ListType();

            for (Link link : this.textures)
            {
                list.addString(link.toString());
            }

            data.put("textures", list);
        }
    }
}
