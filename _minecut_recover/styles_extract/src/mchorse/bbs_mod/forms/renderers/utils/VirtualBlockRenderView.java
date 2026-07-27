package mchorse.bbs_mod.forms.renderers.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.class_1920;
import net.minecraft.class_1944;
import net.minecraft.class_1959;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_2378;
import net.minecraft.class_2586;
import net.minecraft.class_2680;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3568;
import net.minecraft.class_3610;
import net.minecraft.class_3612;
import net.minecraft.class_6539;
import net.minecraft.class_7924;

/**
 * Minimal world view to allow block rendering with culling.
 *
 * Provides block states and basic methods required by BlockRenderView.
 * Lighting and color are delegated to the ClientWorld if it exists; in the absence of a world,
 * safe values (max brightness and zero base light) are returned to avoid NPEs.
 */
public class VirtualBlockRenderView implements class_1920
{
    private final Map<class_2338, class_2680> states = new HashMap<>();
    /* Precomputed local block light (max per position) */
    private final Map<class_2338, Integer> localBlockLight = new HashMap<>();
    private int bottomY = 0;
    private int topY = 256;

    /* Biome override, if provided by the UI */
    private class_2960 biomeOverrideId = null;
    private class_1959 biomeOverride = null;

    /* World anchor and base offsets to translate local structure positions
     * to real world coordinates when querying lighting and color. */
    private class_2338 worldAnchor = class_2338.field_10980;
    private int baseDx = 0;
    private int baseDy = 0;
    private int baseDz = 0;
    private boolean lightsEnabled = true;
    private int lightIntensity = 15;
    private boolean forceMaxSkyLight = false;
    private final Map<class_2338, Integer> precomputedSkyLight = new HashMap<>();

    public VirtualBlockRenderView(List<Entry> entries)
    {
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        List<class_2338> emitters = new ArrayList<>();
        List<Integer> emitterLevels = new ArrayList<>();

        for (Entry e : entries)
        {
            this.states.put(e.pos, e.state == null ? class_2246.field_10124.method_9564() : e.state);

            /* Register light emitters for precomputation */
            class_2680 st = this.states.get(e.pos);
            int lum = st == null ? 0 : st.method_26213();
            if (lum > 0)
            {
                emitters.add(e.pos);
                emitterLevels.add(lum);
            }

            if (e.pos.method_10264() < minY) minY = e.pos.method_10264();
            if (e.pos.method_10264() > maxY) maxY = e.pos.method_10264();
        }

        if (minY != Integer.MAX_VALUE && maxY != Integer.MIN_VALUE)
        {
            this.bottomY = minY;
            this.topY = maxY;
        }

        /* Precompute local light contribution at present positions */
        if (!emitters.isEmpty() && !this.states.isEmpty())
        {
            for (Map.Entry<class_2338, class_2680> target : this.states.entrySet())
            {
                class_2338 tp = target.getKey();
                int max = 0;
                for (int i = 0; i < emitters.size(); i++)
                {
                    class_2338 sp = emitters.get(i);
                    int L = emitterLevels.get(i);
                    int dist = Math.abs(sp.method_10263() - tp.method_10263()) + Math.abs(sp.method_10264() - tp.method_10264()) + Math.abs(sp.method_10260() - tp.method_10260());
                    int contrib = L - dist;
                    if (contrib > max)
                    {
                        max = contrib;
                        if (max >= 15)
                        {
                            max = 15;
                            break;
                        }
                    }
                }
                if (max > 0)
                {
                    this.localBlockLight.put(tp, max);
                }
            }
        }
    }

    /**
     * Sets the world anchor and base offset (derived from centering/parity) to
     * map local positions to absolute world positions.
     */
    public VirtualBlockRenderView setWorldAnchor(class_2338 anchor, int baseDx, int baseDy, int baseDz)
    {
        class_2338 newAnchor = anchor == null ? class_2338.field_10980 : anchor;
        boolean changed = !newAnchor.equals(this.worldAnchor)
            || this.baseDx != baseDx
            || this.baseDy != baseDy
            || this.baseDz != baseDz;

        this.worldAnchor = newAnchor;
        this.baseDx = baseDx;
        this.baseDy = baseDy;
        this.baseDz = baseDz;

        if (changed)
        {
            this.rebuildSkyLight();
        }

        return this;
    }

    private class_2338 toWorldPos(class_2338 localPos)
    {
        return this.worldAnchor.method_10069(this.baseDx + localPos.method_10263(), this.baseDy + localPos.method_10264(), this.baseDz + localPos.method_10260());
    }

    private void rebuildSkyLight()
    {
        this.precomputedSkyLight.clear();

        if (this.states.isEmpty())
        {
            return;
        }

        Map<Long, List<class_2338>> columns = new HashMap<>();

        for (class_2338 pos : this.states.keySet())
        {
            long key = class_2338.method_10064(pos.method_10263(), 0, pos.method_10260());

            columns.computeIfAbsent(key, (k) -> new ArrayList<>()).add(pos);
        }

        var world = class_310.method_1551().field_1687;

        for (List<class_2338> column : columns.values())
        {
            column.sort((a, b) -> Integer.compare(b.method_10264(), a.method_10264()));

            int sky = -1;

            for (class_2338 pos : column)
            {
                if (sky < 0)
                {
                    sky = this.computeColumnTopSky(pos, world);
                }

                this.precomputedSkyLight.put(pos, sky);
                sky = Math.max(0, sky - 1);
            }
        }

        /* Expand sky light into the air shell around the structure so side faces are not pitch black */
        for (int pass = 0; pass < 3; pass++)
        {
            Map<class_2338, Integer> updates = new HashMap<>();

            for (class_2338 pos : this.getShellPositions())
            {
                int max = 0;

                for (class_2350 dir : class_2350.values())
                {
                    Integer neighbor = this.precomputedSkyLight.get(pos.method_10093(dir));

                    if (neighbor != null)
                    {
                        max = Math.max(max, Math.max(0, neighbor - 1));
                    }
                }

                if (max > 0)
                {
                    updates.put(pos, max);
                }
            }

            for (Map.Entry<class_2338, Integer> entry : updates.entrySet())
            {
                this.precomputedSkyLight.merge(entry.getKey(), entry.getValue(), Math::max);
            }
        }
    }

    private int computeColumnTopSky(class_2338 topInColumn, net.minecraft.class_1937 world)
    {
        class_2338 above = topInColumn.method_10084();

        while (this.states.containsKey(above))
        {
            above = above.method_10084();
        }

        if (world != null && !this.forceMaxSkyLight)
        {
            class_2338 worldPos = this.toWorldPos(above);
            int worldSky = world.method_8314(class_1944.field_9284, worldPos);

            if (!world.method_8311(worldPos))
            {
                return worldSky;
            }

            return Math.max(worldSky, 14);
        }

        return 15;
    }

    private List<class_2338> getShellPositions()
    {
        List<class_2338> shell = new ArrayList<>();

        for (class_2338 pos : this.states.keySet())
        {
            for (class_2350 dir : class_2350.values())
            {
                class_2338 neighbor = pos.method_10093(dir);

                if (!this.states.containsKey(neighbor))
                {
                    shell.add(neighbor);
                }
            }
        }

        return shell;
    }

    private int getVirtualSkyLight(class_2338 pos)
    {
        Integer sky = this.precomputedSkyLight.get(pos);

        if (sky != null)
        {
            return sky;
        }

        int max = 0;

        for (class_2350 dir : class_2350.values())
        {
            Integer neighbor = this.precomputedSkyLight.get(pos.method_10093(dir));

            if (neighbor != null)
            {
                max = Math.max(max, Math.max(0, neighbor - 1));
            }
        }

        return max;
    }

    protected class_2338 getWorldAnchor()
    {
        return this.worldAnchor;
    }

    protected int getBaseDx()
    {
        return this.baseDx;
    }

    protected int getBaseDy()
    {
        return this.baseDy;
    }

    protected int getBaseDz()
    {
        return this.baseDz;
    }

    /**
     * Sets a biome to use for color queries. Pass null or "" to clear.
     */
    public VirtualBlockRenderView setBiomeOverride(String biomeId)
    {
        if (biomeId == null || biomeId.isEmpty())
        {
            if (this.biomeOverrideId != null || this.biomeOverride != null)
            {
                this.biomeOverrideId = null;
                this.biomeOverride = null;
            }

            return this;
        }

        try
        {
            class_2960 id = class_2960.method_60654(biomeId);

            if (id.equals(this.biomeOverrideId) && this.biomeOverride != null)
            {
                return this;
            }

            this.biomeOverrideId = id;

            /* Resolve preferably from the client world */
            if (class_310.method_1551().field_1687 != null)
            {
                class_2378<class_1959> reg = class_310.method_1551().field_1687.method_30349().method_30530(class_7924.field_41236);
                this.biomeOverride = reg.method_10223(this.biomeOverrideId);
            }
            else
            {
                this.biomeOverride = null;
            }
        }
        catch (Throwable t)
        {
            this.biomeOverrideId = null;
            this.biomeOverride = null;
        }

        return this;
    }

    /**
     * Enables or disables local block light contribution.
     */
    public VirtualBlockRenderView setLightsEnabled(boolean enabled)
    {
        this.lightsEnabled = enabled;

        return this;
    }

    /**
     * Sets the light intensity cap (1-15) for local light.
     */
    public VirtualBlockRenderView setLightIntensity(int level)
    {
        if (level < 1)
        {
            level = 1;
        }

        if (level > 15)
        {
            level = 15;
        }

        this.lightIntensity = level;

        return this;
    }

    /**
     * Forces max sky light regardless of the present world.
     */
    public VirtualBlockRenderView setForceMaxSkyLight(boolean force)
    {
        this.forceMaxSkyLight = force;
        return this;
    }

    // BlockView
    @Override
    public class_2586 method_8321(class_2338 pos)
    {
        return null;
    }

    @Override
    public class_2680 method_8320(class_2338 pos)
    {
        class_2680 state = this.states.get(pos);
        return state != null ? state : class_2246.field_10124.method_9564();
    }

    @Override
    public class_3610 method_8316(class_2338 pos)
    {
        return class_3612.field_15906.method_15785();
    }

    @Override
    public int method_8317(class_2338 pos)
    {
        if (!this.lightsEnabled)
        {
            return 0;
        }
        class_2680 s = method_8320(pos);
        int lum = s == null ? 0 : s.method_26213();
        return Math.min(lum, this.lightIntensity);
    }

    @Override
    public int method_8315()
    {
        return 15;
    }

    // BlockRenderView
    @Override
    public float method_24852(class_2350 direction, boolean shaded)
    {
        if (class_310.method_1551().field_1687 != null)
        {
            return class_310.method_1551().field_1687.method_24852(direction, shaded);
        }

        return 1.0F;
    }

    @Override
    public class_3568 method_22336()
    {
        if (class_310.method_1551().field_1687 != null)
        {
            return class_310.method_1551().field_1687.method_22336();
        }

        /* Without a world: returning null is not ideal, but the UI route maintains render as entity.
         * This class is used solely in 3D render where there is a world. */
        return null;
    }

    @Override
    public int method_23752(class_2338 pos, class_6539 colorResolver)
    {
        /* If there is a forced biome, use it to resolve the color */
        if (this.biomeOverride != null)
        {
            int wx = this.worldAnchor.method_10263() + this.baseDx + pos.method_10263();
            int wz = this.worldAnchor.method_10260() + this.baseDz + pos.method_10260();
            return colorResolver.getColor(this.biomeOverride, wx, wz);
        }

        if (class_310.method_1551().field_1687 != null)
        {
            class_2338 worldPos = this.worldAnchor.method_10069(this.baseDx + pos.method_10263(), this.baseDy + pos.method_10264(), this.baseDz + pos.method_10260());
            return class_310.method_1551().field_1687.method_23752(worldPos, colorResolver);
        }

        return 0xFFFFFF;
    }

    @Override
    public int method_8314(class_1944 type, class_2338 pos)
    {
        if (type == class_1944.field_9284 && !this.precomputedSkyLight.isEmpty())
        {
            if (this.forceMaxSkyLight)
            {
                return 15;
            }

            int worldLevel = this.queryWorldLightLevel(type, pos);
            int virtualLevel = this.getVirtualSkyLight(pos);

            return Math.max(worldLevel, virtualLevel);
        }

        return this.queryWorldLightLevel(type, pos);
    }

    private int queryWorldLightLevel(class_1944 type, class_2338 pos)
    {
        /* UI or forced mode: return safe and bright levels
         * to avoid dark models. Sky at max; block according to local emitters. */
        if (this.forceMaxSkyLight || class_310.method_1551().field_1687 == null)
        {
            if (type == class_1944.field_9284)
            {
                return 15;
            }
            else /* LightType.BLOCK */
            {
                return this.lightsEnabled ? Math.min(this.localBlockLight.getOrDefault(pos, 0), this.lightIntensity) : 0;
            }
        }

        int worldLevel = 0;
        class_2338 worldPos = this.toWorldPos(pos);
        worldLevel = class_310.method_1551().field_1687.method_8314(type, worldPos);

        /* For block light, combine with that emitted by luminous blocks
         * contained in this virtual view (not present in the real world). */
        if (type == class_1944.field_9282)
        {
            int local = this.lightsEnabled ? Math.min(this.localBlockLight.getOrDefault(pos, 0), this.lightIntensity) : 0;
            return Math.max(worldLevel, local);
        }

        return worldLevel;
    }

    @Override
    public int method_22335(class_2338 pos, int ambientDarkness)
    {
        if (!this.precomputedSkyLight.isEmpty())
        {
            int sky = this.method_8314(class_1944.field_9284, pos);
            int block = this.method_8314(class_1944.field_9282, pos);

            return Math.max(this.queryBaseLightLevel(pos, ambientDarkness), Math.max(sky, block));
        }

        return this.queryBaseLightLevel(pos, ambientDarkness);
    }

    private int queryBaseLightLevel(class_2338 pos, int ambientDarkness)
    {
        /* UI or forced mode: use max base brightness to avoid darkening. */
        if (this.forceMaxSkyLight || class_310.method_1551().field_1687 == null)
        {
            return 15;
        }

        class_2338 worldPos = this.toWorldPos(pos);
        int worldBase = class_310.method_1551().field_1687.method_22335(worldPos, ambientDarkness);

        /* The base level is the maximum between sky/block. Incorporate the local
         * block contribution so that virtual sources illuminate correctly. */
        int localBlock = this.lightsEnabled ? Math.min(this.localBlockLight.getOrDefault(pos, 0), this.lightIntensity) : 0;
        return Math.max(worldBase, localBlock);
    }

    @Override
    public boolean method_8311(class_2338 pos)
    {
        if (!this.precomputedSkyLight.isEmpty())
        {
            Integer sky = this.precomputedSkyLight.get(pos);

            if (sky != null && sky >= 8)
            {
                return true;
            }

            if (this.getVirtualSkyLight(pos) >= 8)
            {
                return true;
            }
        }

        if (this.forceMaxSkyLight || class_310.method_1551().field_1687 == null)
        {
            /* In UI, assume sky visibility to avoid excessive shading. */
            return true;
        }

        return class_310.method_1551().field_1687.method_8311(this.toWorldPos(pos));
    }

    /**
     * Calculates local block light emitted by states within this view.
     * Approximation: Manhattan distance attenuation as in classic propagation.
     * Ignores occlusion to keep cost low and avoid complex paths.
     */
    /* Method removed: now using the O(1) precomputed map */

    // HeightLimitView
    @Override
    public int method_31607()
    {
        return this.bottomY;
    }

    @Override
    public int method_31600()
    {
        return this.topY;
    }

    @Override
    public int method_31605()
    {
        return this.topY - this.bottomY + 1;
    }

    public static class Entry
    {
        public final class_2680 state;
        public final class_2338 pos;

        public Entry(class_2680 state, class_2338 pos)
        {
            this.state = state;
            this.pos = pos;
        }
    }
}
