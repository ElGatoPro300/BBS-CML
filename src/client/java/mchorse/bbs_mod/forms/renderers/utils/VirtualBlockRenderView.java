package mchorse.bbs_mod.forms.renderers.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Minimal world view to allow block rendering with culling.
 *
 * Provides block states and basic methods required by BlockRenderView.
 * Lighting and color are delegated to the ClientWorld if it exists; in the absence of a world,
 * safe values (max brightness and zero base light) are returned to avoid NPEs.
 */
public class VirtualBlockRenderView implements BlockAndTintGetter
{
    private final Map<BlockPos, BlockState> states = new HashMap<>();
    /* Precomputed local block light (max per position) */
    private final Map<BlockPos, Integer> localBlockLight = new HashMap<>();
    private int minX = 0;
    private int maxX = 0;
    private int bottomY = 0;
    private int topY = 256;
    private int minZ = 0;
    private int maxZ = 0;

    /* Biome override, if provided by the UI */
    private Identifier biomeOverrideId = null;
    private Biome biomeOverride = null;

    /* World anchor and base offsets to translate local structure positions
     * to real world coordinates when querying lighting and color. */
    private BlockPos worldAnchor = BlockPos.ZERO;
    private int baseDx = 0;
    private int baseDy = 0;
    private int baseDz = 0;
    private boolean lightsEnabled = true;
    private int lightIntensity = 15;
    private boolean forceMaxSkyLight = false;
    private final Map<BlockPos, Integer> precomputedSkyLight = new HashMap<>();

    public VirtualBlockRenderView(List<Entry> entries)
    {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;

        List<BlockPos> emitters = new ArrayList<>();
        List<Integer> emitterLevels = new ArrayList<>();

        for (Entry e : entries)
        {
            this.states.put(e.pos, e.state == null ? Blocks.AIR.defaultBlockState() : e.state);

            /* Register light emitters for precomputation */
            BlockState st = this.states.get(e.pos);
            int lum = st == null ? 0 : st.getLightEmission();
            if (lum > 0)
            {
                emitters.add(e.pos);
                emitterLevels.add(lum);
            }

            if (e.pos.getX() < minX) minX = e.pos.getX();
            if (e.pos.getX() > maxX) maxX = e.pos.getX();
            if (e.pos.getY() < minY) minY = e.pos.getY();
            if (e.pos.getY() > maxY) maxY = e.pos.getY();
            if (e.pos.getZ() < minZ) minZ = e.pos.getZ();
            if (e.pos.getZ() > maxZ) maxZ = e.pos.getZ();
        }

        if (minY != Integer.MAX_VALUE && maxY != Integer.MIN_VALUE)
        {
            this.minX = minX;
            this.maxX = maxX;
            this.bottomY = minY;
            this.topY = maxY;
            this.minZ = minZ;
            this.maxZ = maxZ;
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

        this.rebuildSkyLight();
    }

    /**
     * Sets the world anchor and base offset (derived from centering/parity) to
     * map local positions to absolute world positions.
     */
    public VirtualBlockRenderView setWorldAnchor(BlockPos anchor, int baseDx, int baseDy, int baseDz)
    {
        BlockPos newAnchor = anchor == null ? BlockPos.ZERO : anchor;
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
        return this.worldAnchor.offset(this.baseDx + localPos.getX(), this.baseDy + localPos.getY(), this.baseDz + localPos.getZ());
    }

    public void rebuildSkyLight()
    {
        this.precomputedSkyLight.clear();

        if (this.states.isEmpty())
        {
            return;
        }

        int envSky = this.getEnvironmentSkyLight();
        int pad = 3;
        int startX = this.minX - pad;
        int endX = this.maxX + pad;
        int startY = this.bottomY - pad;
        int endY = this.topY + pad;
        int startZ = this.minZ - pad;
        int endZ = this.maxZ + pad;

        Queue<BlockPos> queue = new ArrayDeque<>();

        /* Step 1: Vertical raycast */
        for (int x = startX; x <= endX; x++)
        {
            for (int z = startZ; z <= endZ; z++)
            {
                int sky = envSky;

                for (int y = endY; y >= startY; y--)
                {
                    BlockPos pos = new BlockPos(x, y, z);
                    int opacity = this.getBlockOpacity(pos);

                    if (opacity > 0)
                    {
                        sky = Math.max(0, sky - opacity);
                    }

                    this.precomputedSkyLight.put(pos, sky);

                    if (sky > 0)
                    {
                        queue.add(pos);
                    }
                }
            }
        }

        /* Step 2: 3D BFS diffusion */
        while (!queue.isEmpty())
        {
            BlockPos pos = queue.poll();
            int currentLevel = this.precomputedSkyLight.getOrDefault(pos, 0);

            if (currentLevel <= 1)
            {
                continue;
            }

            for (Direction dir : Direction.values())
            {
                BlockPos neighborPos = pos.relative(dir);

                if (neighborPos.getX() < startX || neighborPos.getX() > endX
                    || neighborPos.getY() < startY || neighborPos.getY() > endY
                    || neighborPos.getZ() < startZ || neighborPos.getZ() > endZ)
                {
                    continue;
                }

                int opacity = this.getBlockOpacity(neighborPos);

                if (opacity >= 15)
                {
                    continue;
                }

                int targetLevel = Math.max(0, currentLevel - 1 - opacity);

                if (targetLevel > this.precomputedSkyLight.getOrDefault(neighborPos, 0))
                {
                    this.precomputedSkyLight.put(neighborPos, targetLevel);
                    queue.add(neighborPos);
                }
            }
        }
    }

    private int getEnvironmentSkyLight()
    {
        if (this.forceMaxSkyLight || Minecraft.getInstance().level == null)
        {
            return 15;
        }

        Level world = Minecraft.getInstance().level;
        int topWorldY = this.worldAnchor.getY() + this.baseDy + this.topY + 1;
        int centerWorldX = this.worldAnchor.getX() + this.baseDx + (this.minX + this.maxX) / 2;
        int centerWorldZ = this.worldAnchor.getZ() + this.baseDz + (this.minZ + this.maxZ) / 2;
        BlockPos abovePos = new BlockPos(centerWorldX, topWorldY, centerWorldZ);

        int worldSky = world.getBrightness(LightLayer.SKY, abovePos);

        if (world.canSeeSky(abovePos))
        {
            return Math.max(worldSky, 15);
        }

        if (worldSky == 0 && world.canSeeSky(this.worldAnchor))
        {
            return 15;
        }

        return worldSky;
    }

    private int getBlockOpacity(BlockPos pos)
    {
        BlockState state = this.states.get(pos);

        if (state == null || state.isAir())
        {
            return 0;
        }

        if (state.getBlock() instanceof LeavesBlock)
        {
            return 1;
        }

        if (state.isSolidRender())
        {
            return 15;
        }

        int opacity = state.getLightBlock();

        return Math.max(0, Math.min(15, opacity));
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

    public boolean isForceMaxSkyLight()
    {
        return this.forceMaxSkyLight;
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
            this.biomeOverrideId = Identifier.parse(biomeId);
            /* Resolve preferably from the client world */
            if (Minecraft.getInstance().level != null)
            {
                Registry<Biome> reg = Minecraft.getInstance().level.registryAccess().lookupOrThrow(Registries.BIOME);
                this.biomeOverride = reg.getValue(this.biomeOverrideId);
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
        if (this.forceMaxSkyLight != force)
        {
            this.forceMaxSkyLight = force;
            this.rebuildSkyLight();
        }

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
        return state != null ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    public FluidState getFluidState(BlockPos pos)
    {
        return Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public int getLightEmission(BlockPos pos)
    {
        if (!this.lightsEnabled)
        {
            return 0;
        }
        BlockState s = getBlockState(pos);
        int lum = s == null ? 0 : s.getLightEmission();
        return Math.min(lum, this.lightIntensity);
    }

    public float getShade(Direction direction, boolean shaded)
    {
        return 1.0F;
    }

    @Override
    public LevelLightEngine getLightEngine()
    {
        if (Minecraft.getInstance().level != null)
        {
            return Minecraft.getInstance().level.getLightEngine();
        }

        /* Without a world: returning null is not ideal, but the UI route maintains render as entity.
         * This class is used solely in 3D render where there is a world. */
        return null;
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver colorResolver)
    {
        /* If there is a forced biome, use it to resolve the color */
        if (this.biomeOverride != null)
        {
            int wx = this.worldAnchor.getX() + this.baseDx + pos.getX();
            int wz = this.worldAnchor.getZ() + this.baseDz + pos.getZ();
            return colorResolver.getColor(this.biomeOverride, wx, wz);
        }

        if (Minecraft.getInstance().level != null)
        {
            BlockPos worldPos = this.worldAnchor.offset(this.baseDx + pos.getX(), this.baseDy + pos.getY(), this.baseDz + pos.getZ());
            return Minecraft.getInstance().level.getBlockTint(worldPos, colorResolver);
        }

        return 0xFFFFFF;
    }

    @Override
    public int getBrightness(LightLayer type, BlockPos pos)
    {
        if (type == LightLayer.SKY)
        {
            if (this.forceMaxSkyLight)
            {
                return 15;
            }

            Integer sky = this.precomputedSkyLight.get(pos);

            if (sky != null)
            {
                return sky;
            }

            return this.queryWorldLightLevel(LightLayer.SKY, pos);
        }

        return this.queryWorldLightLevel(type, pos);
    }

    private int queryWorldLightLevel(LightLayer type, BlockPos pos)
    {
        /* UI or forced mode: return safe and bright levels
         * to avoid dark models. Sky at max; block according to local emitters. */
        if (this.forceMaxSkyLight || Minecraft.getInstance().level == null)
        {
            if (type == LightLayer.SKY)
            {
                return 15;
            }
            else /* LightType.BLOCK */
            {
                return this.lightsEnabled ? Math.min(this.localBlockLight.getOrDefault(pos, 0), this.lightIntensity) : 0;
            }
        }

        BlockPos worldPos = this.toWorldPos(pos);
        int worldLevel = Minecraft.getInstance().level.getBrightness(type, worldPos);

        if (type == LightLayer.SKY)
        {
            if (Minecraft.getInstance().level.canSeeSky(worldPos))
            {
                return Math.max(worldLevel, 15);
            }

            return worldLevel;
        }

        /* For block light, combine with that emitted by luminous blocks
         * contained in this virtual view (not present in the real world). */
        int local = this.lightsEnabled ? Math.min(this.localBlockLight.getOrDefault(pos, 0), this.lightIntensity) : 0;

        return Math.max(worldLevel, local);
    }

    @Override
    public int getRawBrightness(BlockPos pos, int ambientDarkness)
    {
        int sky = this.getBrightness(LightLayer.SKY, pos);

        if (!this.forceMaxSkyLight && Minecraft.getInstance().level != null)
        {
            sky = Math.max(0, sky - ambientDarkness);
        }

        int block = this.getBrightness(LightLayer.BLOCK, pos);

        return Math.max(sky, block);
    }

    @Override
    public boolean canSeeSky(BlockPos pos)
    {
        if (this.forceMaxSkyLight || Minecraft.getInstance().level == null)
        {
            /* In UI, assume sky visibility to avoid excessive shading. */
            return true;
        }

        Integer sky = this.precomputedSkyLight.get(pos);

        if (sky != null && sky >= 8)
        {
            return true;
        }

        return Minecraft.getInstance().level.canSeeSky(this.toWorldPos(pos));
    }

    // HeightLimitView
    public int getMinY()
    {
        return this.bottomY;
    }

    public int getTopY()
    {
        return this.topY;
    }

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
