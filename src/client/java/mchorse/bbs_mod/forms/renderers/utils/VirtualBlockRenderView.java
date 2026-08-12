package mchorse.bbs_mod.forms.renderers.utils;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.ColorResolver;
import net.minecraft.world.chunk.light.LightingProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal world view to allow block rendering with culling.
 *
 * Provides block states and basic methods required by BlockRenderView.
 * Lighting and color are delegated to the ClientWorld if it exists; in the absence of a world,
 * safe values (max brightness and zero base light) are returned to avoid NPEs.
 */
public class VirtualBlockRenderView implements BlockRenderView
{
    private final Map<BlockPos, BlockState> states = new HashMap<>();
    /* Precomputed local block light (max per position) */
    private final Map<BlockPos, Integer> localBlockLight = new HashMap<>();
    private int bottomY = 0;
    private int topY = 256;

    /* Biome override, if provided by the UI */
    private Identifier biomeOverrideId = null;
    private Biome biomeOverride = null;

    /* World anchor and base offsets to translate local structure positions
     * to real world coordinates when querying lighting and color. */
    private BlockPos worldAnchor = BlockPos.ORIGIN;
    private int baseDx = 0;
    private int baseDy = 0;
    private int baseDz = 0;
    private boolean lightsEnabled = true;
    private int lightIntensity = 15;
    private boolean forceMaxSkyLight = false;
    private final Map<BlockPos, Integer> precomputedSkyLight = new HashMap<>();

    public VirtualBlockRenderView(List<Entry> entries)
    {
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        List<BlockPos> emitters = new ArrayList<>();
        List<Integer> emitterLevels = new ArrayList<>();

        for (Entry e : entries)
        {
            this.states.put(e.pos, e.state == null ? Blocks.AIR.getDefaultState() : e.state);

            /* Register light emitters for precomputation */
            BlockState st = this.states.get(e.pos);
            int lum = st == null ? 0 : st.getLuminance();
            if (lum > 0)
            {
                emitters.add(e.pos);
                emitterLevels.add(lum);
            }

            if (e.pos.getY() < minY) minY = e.pos.getY();
            if (e.pos.getY() > maxY) maxY = e.pos.getY();
        }

        if (minY != Integer.MAX_VALUE && maxY != Integer.MIN_VALUE)
        {
            this.bottomY = minY;
            this.topY = maxY;
        }

        /* Precompute local light contribution at present positions */
        if (!emitters.isEmpty() && !this.states.isEmpty())
        {
            for (Map.Entry<BlockPos, BlockState> target : this.states.entrySet())
            {
                BlockPos tp = target.getKey();
                int max = 0;
                for (int i = 0; i < emitters.size(); i++)
                {
                    BlockPos sp = emitters.get(i);
                    int L = emitterLevels.get(i);
                    int dist = Math.abs(sp.getX() - tp.getX()) + Math.abs(sp.getY() - tp.getY()) + Math.abs(sp.getZ() - tp.getZ());
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
    public VirtualBlockRenderView setWorldAnchor(BlockPos anchor, int baseDx, int baseDy, int baseDz)
    {
        BlockPos newAnchor = anchor == null ? BlockPos.ORIGIN : anchor;
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

    private BlockPos toWorldPos(BlockPos localPos)
    {
        return this.worldAnchor.add(this.baseDx + localPos.getX(), this.baseDy + localPos.getY(), this.baseDz + localPos.getZ());
    }

    private void rebuildSkyLight()
    {
        this.precomputedSkyLight.clear();

        if (this.states.isEmpty())
        {
            return;
        }

        Map<Long, List<BlockPos>> columns = new HashMap<>();

        for (BlockPos pos : this.states.keySet())
        {
            long key = BlockPos.asLong(pos.getX(), 0, pos.getZ());

            columns.computeIfAbsent(key, (k) -> new ArrayList<>()).add(pos);
        }

        var world = MinecraftClient.getInstance().world;

        for (List<BlockPos> column : columns.values())
        {
            column.sort((a, b) -> Integer.compare(b.getY(), a.getY()));

            int sky = -1;

            for (BlockPos pos : column)
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
            Map<BlockPos, Integer> updates = new HashMap<>();

            for (BlockPos pos : this.getShellPositions())
            {
                int max = 0;

                for (Direction dir : Direction.values())
                {
                    Integer neighbor = this.precomputedSkyLight.get(pos.offset(dir));

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

            for (Map.Entry<BlockPos, Integer> entry : updates.entrySet())
            {
                this.precomputedSkyLight.merge(entry.getKey(), entry.getValue(), Math::max);
            }
        }
    }

    private int computeColumnTopSky(BlockPos topInColumn, World world)
    {
        BlockPos above = topInColumn.up();

        while (this.states.containsKey(above))
        {
            above = above.up();
        }

        if (world != null && !this.forceMaxSkyLight)
        {
            BlockPos worldPos = this.toWorldPos(above);
            int worldSky = world.getLightLevel(LightType.SKY, worldPos);

            if (!world.isSkyVisible(worldPos))
            {
                return worldSky;
            }

            return Math.max(worldSky, 14);
        }

        return 15;
    }

    private List<BlockPos> getShellPositions()
    {
        List<BlockPos> shell = new ArrayList<>();

        for (BlockPos pos : this.states.keySet())
        {
            for (Direction dir : Direction.values())
            {
                BlockPos neighbor = pos.offset(dir);

                if (!this.states.containsKey(neighbor))
                {
                    shell.add(neighbor);
                }
            }
        }

        return shell;
    }

    private int getVirtualSkyLight(BlockPos pos)
    {
        Integer sky = this.precomputedSkyLight.get(pos);

        if (sky != null)
        {
            return sky;
        }

        int max = 0;

        for (Direction dir : Direction.values())
        {
            Integer neighbor = this.precomputedSkyLight.get(pos.offset(dir));

            if (neighbor != null)
            {
                max = Math.max(max, Math.max(0, neighbor - 1));
            }
        }

        return max;
    }

    protected BlockPos getWorldAnchor()
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
            this.biomeOverrideId = null;
            this.biomeOverride = null;
            return this;
        }

        try
        {
            this.biomeOverrideId = Identifier.of(biomeId);
            /* Resolve preferably from the client world */
            if (MinecraftClient.getInstance().world != null)
            {
                Registry<Biome> reg = MinecraftClient.getInstance().world.getRegistryManager().get(RegistryKeys.BIOME);
                this.biomeOverride = reg.get(this.biomeOverrideId);
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
        if (level < 1) level = 1;
        if (level > 15) level = 15;
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
    public BlockEntity getBlockEntity(BlockPos pos)
    {
        return null;
    }

    @Override
    public BlockState getBlockState(BlockPos pos)
    {
        BlockState state = this.states.get(pos);
        return state != null ? state : Blocks.AIR.getDefaultState();
    }

    @Override
    public FluidState getFluidState(BlockPos pos)
    {
        return Fluids.EMPTY.getDefaultState();
    }

    @Override
    public int getLuminance(BlockPos pos)
    {
        if (!this.lightsEnabled)
        {
            return 0;
        }
        BlockState s = getBlockState(pos);
        int lum = s == null ? 0 : s.getLuminance();
        return Math.min(lum, this.lightIntensity);
    }

    @Override
    public int getMaxLightLevel()
    {
        return 15;
    }

    // BlockRenderView
    @Override
    public float getBrightness(Direction direction, boolean shaded)
    {
        if (MinecraftClient.getInstance().world != null)
        {
            return MinecraftClient.getInstance().world.getBrightness(direction, shaded);
        }

        return 1.0F;
    }

    @Override
    public LightingProvider getLightingProvider()
    {
        if (MinecraftClient.getInstance().world != null)
        {
            return MinecraftClient.getInstance().world.getLightingProvider();
        }

        /* Without a world: returning null is not ideal, but the UI route maintains render as entity.
         * This class is used solely in 3D render where there is a world. */
        return null;
    }

    @Override
    public int getColor(BlockPos pos, ColorResolver colorResolver)
    {
        /* If there is a forced biome, use it to resolve the color */
        if (this.biomeOverride != null)
        {
            int wx = this.worldAnchor.getX() + this.baseDx + pos.getX();
            int wz = this.worldAnchor.getZ() + this.baseDz + pos.getZ();
            return colorResolver.getColor(this.biomeOverride, wx, wz);
        }

        if (MinecraftClient.getInstance().world != null)
        {
            BlockPos worldPos = this.worldAnchor.add(this.baseDx + pos.getX(), this.baseDy + pos.getY(), this.baseDz + pos.getZ());
            return MinecraftClient.getInstance().world.getColor(worldPos, colorResolver);
        }

        return 0xFFFFFF;
    }

    @Override
    public int getLightLevel(LightType type, BlockPos pos)
    {
        if (type == LightType.SKY && !this.precomputedSkyLight.isEmpty())
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

    private int queryWorldLightLevel(LightType type, BlockPos pos)
    {
        /* UI or forced mode: return safe and bright levels
         * to avoid dark models. Sky at max; block according to local emitters. */
        if (this.forceMaxSkyLight || MinecraftClient.getInstance().world == null)
        {
            if (type == LightType.SKY)
            {
                return 15;
            }
            else /* LightType.BLOCK */
            {
                return this.lightsEnabled ? Math.min(this.localBlockLight.getOrDefault(pos, 0), this.lightIntensity) : 0;
            }
        }

        int worldLevel = 0;
        BlockPos worldPos = this.toWorldPos(pos);
        worldLevel = MinecraftClient.getInstance().world.getLightLevel(type, worldPos);

        /* For block light, combine with that emitted by luminous blocks
         * contained in this virtual view (not present in the real world). */
        if (type == LightType.BLOCK)
        {
            int local = this.lightsEnabled ? Math.min(this.localBlockLight.getOrDefault(pos, 0), this.lightIntensity) : 0;
            return Math.max(worldLevel, local);
        }

        return worldLevel;
    }

    @Override
    public int getBaseLightLevel(BlockPos pos, int ambientDarkness)
    {
        if (!this.precomputedSkyLight.isEmpty())
        {
            int sky = this.getLightLevel(LightType.SKY, pos);
            int block = this.getLightLevel(LightType.BLOCK, pos);

            return Math.max(this.queryBaseLightLevel(pos, ambientDarkness), Math.max(sky, block));
        }

        return this.queryBaseLightLevel(pos, ambientDarkness);
    }

    private int queryBaseLightLevel(BlockPos pos, int ambientDarkness)
    {
        /* UI or forced mode: use max base brightness to avoid darkening. */
        if (this.forceMaxSkyLight || MinecraftClient.getInstance().world == null)
        {
            return 15;
        }

        BlockPos worldPos = this.toWorldPos(pos);
        int worldBase = MinecraftClient.getInstance().world.getBaseLightLevel(worldPos, ambientDarkness);

        /* The base level is the maximum between sky/block. Incorporate the local
         * block contribution so that virtual sources illuminate correctly. */
        int localBlock = this.lightsEnabled ? Math.min(this.localBlockLight.getOrDefault(pos, 0), this.lightIntensity) : 0;
        return Math.max(worldBase, localBlock);
    }

    @Override
    public boolean isSkyVisible(BlockPos pos)
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

        if (this.forceMaxSkyLight || MinecraftClient.getInstance().world == null)
        {
            /* In UI, assume sky visibility to avoid excessive shading. */
            return true;
        }

        return MinecraftClient.getInstance().world.isSkyVisible(this.toWorldPos(pos));
    }

    /**
     * Calculates local block light emitted by states within this view.
     * Approximation: Manhattan distance attenuation as in classic propagation.
     * Ignores occlusion to keep cost low and avoid complex paths.
     */
    /* Method removed: now using the O(1) precomputed map */

    // HeightLimitView
    @Override
    public int getBottomY()
    {
        return this.bottomY;
    }

    @Override
    public int getTopY()
    {
        return this.topY;
    }

    @Override
    public int getHeight()
    {
        return this.topY - this.bottomY + 1;
    }

    public static class Entry
    {
        public final BlockState state;
        public final BlockPos pos;

        public Entry(BlockState state, BlockPos pos)
        {
            this.state = state;
            this.pos = pos;
        }
    }
}
