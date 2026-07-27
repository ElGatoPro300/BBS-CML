package mchorse.bbs_mod.cubic.physics;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Force fields acting on spring chains: per-chain gravity (with body-relative
 * pull rotation) and global FBM wind.
 */
final class EnvironmentalForces
{
    private static final float EPS = 1.0e-6f;

    /** Converts film ticks × gust speed into noise-space drift (~20 ticks per noise cell at speed 1). */
    private static final float GUST_TIME_SCALE = 0.04F;

    private EnvironmentalForces()
    {
    }

    static void computeGravityDirection(SpringChainCompiler.CompiledChain chain, Quaternionf parentRotation, float gravity, Vector3f out)
    {
        out.set(0F, -1F, 0F);

        if (chain.bodyRelativePull() && parentRotation != null)
        {
            /* Model bone forward axis is -Y in this rig convention. */
            parentRotation.transform(out);
        }

        if (chain.hasPullRotation())
        {
            if (parentRotation != null)
            {
                /* Apply user rotation in chain local space, then convert back to world space. */
                Quaternionf inverseParent = new Quaternionf(parentRotation).invert();
                inverseParent.transform(out);
                chain.applyPullRotation(out);
                parentRotation.transform(out);
            }
            else
            {
                chain.applyPullRotation(out);
            }
        }

        if (out.lengthSquared() < EPS * EPS)
        {
            out.set(0F, -1F, 0F);
        }

        out.normalize().mul(gravity);
    }

    /**
     * Resolves the wind's unit direction into {@code dirOut} and returns the base
     * acceleration magnitude ({@code base} × power), or 0 when there is no wind.
     */
    static float prepareWind(WindDef wind, float base, Vector3f dirOut)
    {
        if (wind == null || !wind.active())
        {
            return 0F;
        }

        dirOut.set(wind.dirX(), wind.dirY(), wind.dirZ());

        if (dirOut.lengthSquared() < EPS * EPS)
        {
            return 0F;
        }

        dirOut.normalize();

        return base * wind.power();
    }

    /**
     * Wind acceleration at one point. Steady force is {@code dir × baseMagnitude};
     * gustiness modulates magnitude via FBM sampled at position + time drift.
     */
    static void windForceAt(Vector3f dir, float baseMagnitude, WindDef wind, float time, Vector3f pos, Vector3f out)
    {
        float gustiness = wind.gustiness();
        float factor = 1F;

        if (gustiness > 0F)
        {
            float scale = wind.gustScale();
            float drift = time * wind.gustSpeed() * GUST_TIME_SCALE;

            float n = fbm(pos.x * scale - dir.x * drift, pos.y * scale - dir.y * drift, pos.z * scale - dir.z * drift);

            factor = 1F + gustiness * n;

            if (factor < 0F)
            {
                factor = 0F;
            }
        }

        out.set(dir).mul(baseMagnitude * factor);
    }

    /** Two-octave fractal value noise: slow swell plus faster weaker ripple. */
    private static float fbm(float x, float y, float z)
    {
        return valueNoise(x, y, z) * 0.65F
            + valueNoise(x * 2.3F + 11.1F, y * 2.3F + 7.7F, z * 2.3F + 3.3F) * 0.35F;
    }

    private static float valueNoise(float x, float y, float z)
    {
        int xi = floor(x);
        int yi = floor(y);
        int zi = floor(z);

        float xf = fade(x - xi);
        float yf = fade(y - yi);
        float zf = fade(z - zi);

        float x00 = lerp(hash(xi, yi, zi), hash(xi + 1, yi, zi), xf);
        float x10 = lerp(hash(xi, yi + 1, zi), hash(xi + 1, yi + 1, zi), xf);
        float x01 = lerp(hash(xi, yi, zi + 1), hash(xi + 1, yi, zi + 1), xf);
        float x11 = lerp(hash(xi, yi + 1, zi + 1), hash(xi + 1, yi + 1, zi + 1), xf);

        float y0 = lerp(x00, x10, yf);
        float y1 = lerp(x01, x11, yf);

        return lerp(y0, y1, zf);
    }

    private static float hash(int x, int y, int z)
    {
        int h = x * 374761393 + y * 668265263 + z * 1274126177;
        h = (h ^ (h >>> 13)) * 1274126177;
        h = h ^ (h >>> 16);

        return (h & 0xFFFF) / 32767.5F - 1F;
    }

    private static float fade(float t)
    {
        return t * t * t * (t * (t * 6F - 15F) + 10F);
    }

    private static float lerp(float a, float b, float t)
    {
        return a + (b - a) * t;
    }

    private static int floor(float v)
    {
        int i = (int) v;

        return v < i ? i - 1 : i;
    }
}
