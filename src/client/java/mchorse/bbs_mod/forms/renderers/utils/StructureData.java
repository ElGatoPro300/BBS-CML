package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.resources.Link;

import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.WaterlilyBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Encapsulates loaded structure geometry, NBT parsing, block categorization,
 * and virtual block render view cache.
 */
public class StructureData
{
    public static class BlockEntry
    {
        public final BlockState state;
        public final BlockPos pos;
        public final CompoundTag nbt;

        public BlockEntry(BlockState state, BlockPos pos, CompoundTag nbt)
        {
            this.state = state;
            this.pos = pos;
            this.nbt = nbt;
        }
    }

    private final List<BlockEntry> blocks = new ArrayList<>();
    private final List<BlockEntry> animatedBlocks = new ArrayList<>();
    private final List<BlockEntry> biomeTintedBlocks = new ArrayList<>();
    private final List<BlockEntry> translucentBlocks = new ArrayList<>();
    private final List<BlockEntry> blockEntitiesList = new ArrayList<>();

    private String lastFile = null;

    private BlockPos size = BlockPos.ZERO;
    private BlockPos boundsMin = null;
    private BlockPos boundsMax = null;

    private boolean hasTranslucentLayer = false;
    private boolean hasCutoutLayer = false;
    private boolean hasAnimatedLayer = false;
    private boolean hasBiomeTintedLayer = false;
    private boolean hasLeavesLayer = false;
    private boolean hasBlockEntityLayer = false;

    private VirtualBlockRenderView.Entry[] entriesCache = null;
    private StructureVirtualBlockRenderView cachedView = null;

    public StructureData()
    {
    }

    public List<BlockEntry> getBlocks()
    {
        return this.blocks;
    }

    public List<BlockEntry> getAnimatedBlocks()
    {
        return this.animatedBlocks;
    }

    public List<BlockEntry> getBiomeTintedBlocks()
    {
        return this.biomeTintedBlocks;
    }

    public List<BlockEntry> getTranslucentBlocks()
    {
        return this.translucentBlocks;
    }

    public List<BlockEntry> getBlockEntitiesList()
    {
        return this.blockEntitiesList;
    }

    public String getLastFile()
    {
        return this.lastFile;
    }

    public BlockPos getSize()
    {
        return this.size;
    }

    public BlockPos getBoundsMin()
    {
        return this.boundsMin;
    }

    public BlockPos getBoundsMax()
    {
        return this.boundsMax;
    }

    public boolean hasTranslucentLayer()
    {
        return this.hasTranslucentLayer;
    }

    public boolean hasCutoutLayer()
    {
        return this.hasCutoutLayer;
    }

    public boolean hasAnimatedLayer()
    {
        return this.hasAnimatedLayer;
    }

    public boolean hasBiomeTintedLayer()
    {
        return this.hasBiomeTintedLayer;
    }

    public boolean hasLeavesLayer()
    {
        return this.hasLeavesLayer;
    }

    public boolean hasBlockEntityLayer()
    {
        return this.hasBlockEntityLayer;
    }

    public VirtualBlockRenderView.Entry[] getEntriesCache()
    {
        return this.entriesCache;
    }

    public void setEntriesCache(VirtualBlockRenderView.Entry[] cache)
    {
        this.entriesCache = cache;
    }

    public StructureVirtualBlockRenderView getCachedView()
    {
        return this.cachedView;
    }

    public void setCachedView(StructureVirtualBlockRenderView view)
    {
        this.cachedView = view;
    }

    public boolean isEntirelyBlockEntities()
    {
        return this.hasBlockEntityLayer
            && !this.blockEntitiesList.isEmpty()
            && this.blockEntitiesList.size() >= this.blocks.size();
    }

    public void clear()
    {
        this.blocks.clear();
        this.animatedBlocks.clear();
        this.biomeTintedBlocks.clear();
        this.translucentBlocks.clear();
        this.blockEntitiesList.clear();
        this.size = BlockPos.ZERO;
        this.boundsMin = null;
        this.boundsMax = null;
        this.hasTranslucentLayer = false;
        this.hasCutoutLayer = false;
        this.hasAnimatedLayer = false;
        this.hasBiomeTintedLayer = false;
        this.hasLeavesLayer = false;
        this.hasBlockEntityLayer = false;
        this.entriesCache = null;
        this.cachedView = null;
        this.lastFile = null;
    }

    public boolean ensureLoaded(String file)
    {
        if (file == null || file.isEmpty())
        {
            this.clear();
            return false;
        }

        if (file.equals(this.lastFile) && !this.blocks.isEmpty())
        {
            return false;
        }

        this.clear();
        this.lastFile = file;

        File nbtFile = BBSMod.getProvider().getFile(Link.create(file));

        if (nbtFile != null && nbtFile.exists())
        {
            try
            {
                CompoundTag root = NbtIo.readCompressed(nbtFile.toPath(), NbtAccounter.unlimitedHeap());
                this.parseStructure(root);
                return true;
            }
            catch (IOException e)
            {
                /* Fall through */
            }
        }

        try (InputStream is = BBSMod.getProvider().getAsset(Link.create(file)))
        {
            try
            {
                CompoundTag root = NbtIo.readCompressed(is, NbtAccounter.unlimitedHeap());
                this.parseStructure(root);
                return true;
            }
            catch (IOException e)
            {
                /* Fall through */
            }
        }
        catch (Exception e)
        {
            /* Fall through */
        }

        return true;
    }

    private void parseStructure(CompoundTag root)
    {
        if (root.contains("size"))
        {
            int[] sz = root.getIntArray("size").orElse(new int[0]);

            if (sz.length >= 3)
            {
                this.size = new BlockPos(sz[0], sz[1], sz[2]);
            }
        }

        List<BlockState> paletteStates = new ArrayList<>();

        if (root.contains("palette"))
        {
            ListTag palette = root.getListOrEmpty("palette");

            for (int i = 0; i < palette.size(); i++)
            {
                CompoundTag entry = palette.getCompoundOrEmpty(i);
                BlockState state = this.readBlockState(entry);
                paletteStates.add(state);
            }
        }

        if (root.contains("blocks"))
        {
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;
            ListTag list = root.getListOrEmpty("blocks");

            StructureData.syncFancyGraphicsFromOptions();

            for (int i = 0; i < list.size(); i++)
            {
                CompoundTag be = list.getCompoundOrEmpty(i);
                BlockPos pos = this.readBlockPos(be.getListOrEmpty("pos"));
                int stateIndex = be.getIntOr("state", 0);

                if (stateIndex >= 0 && stateIndex < paletteStates.size())
                {
                    BlockState state = paletteStates.get(stateIndex);

                    if (state == null || state.isAir())
                    {
                        continue;
                    }

                    CompoundTag nbt = be.contains("nbt") ? be.getCompoundOrEmpty("nbt") : null;
                    BlockEntry blockEntry = new BlockEntry(state, pos, nbt);

                    this.blocks.add(blockEntry);

                    if (!state.canOcclude())
                    {
                        this.hasCutoutLayer = true;
                    }

                    if (StructureData.isAnimatedTexture(state))
                    {
                        this.animatedBlocks.add(blockEntry);
                        this.hasAnimatedLayer = true;
                    }

                    if (StructureData.isBiomeTinted(state))
                    {
                        this.biomeTintedBlocks.add(blockEntry);
                        this.hasBiomeTintedLayer = true;
                    }

                    if (state.getBlock() instanceof LeavesBlock)
                    {
                        this.hasLeavesLayer = true;
                    }

                    if (StructureData.isTranslucentBlock(state))
                    {
                        this.translucentBlocks.add(blockEntry);
                        this.hasTranslucentLayer = true;
                    }

                    if (state.getBlock() instanceof EntityBlock)
                    {
                        this.blockEntitiesList.add(blockEntry);
                        this.hasBlockEntityLayer = true;
                    }

                    if (pos.getX() < minX)
                    {
                        minX = pos.getX();
                    }

                    if (pos.getY() < minY)
                    {
                        minY = pos.getY();
                    }

                    if (pos.getZ() < minZ)
                    {
                        minZ = pos.getZ();
                    }

                    if (pos.getX() > maxX)
                    {
                        maxX = pos.getX();
                    }

                    if (pos.getY() > maxY)
                    {
                        maxY = pos.getY();
                    }

                    if (pos.getZ() > maxZ)
                    {
                        maxZ = pos.getZ();
                    }
                }
            }

            if (!this.blocks.isEmpty())
            {
                this.boundsMin = new BlockPos(minX, minY, minZ);
                this.boundsMax = new BlockPos(maxX, maxY, maxZ);
            }
        }
    }

    private BlockPos readBlockPos(ListTag list)
    {
        if (list == null || list.size() < 3)
        {
            return BlockPos.ZERO;
        }

        return new BlockPos(list.getIntOr(0, 0), list.getIntOr(1, 0), list.getIntOr(2, 0));
    }

    private BlockState readBlockState(CompoundTag entry)
    {
        String name = entry.getStringOr("Name", "");
        Block block;
        BlockState state;

        try
        {
            Identifier id = Identifier.parse(name);
            block = BuiltInRegistries.BLOCK.getValue(id);

            if (block == null)
            {
                block = Blocks.AIR;
            }
        }
        catch (Exception e)
        {
            block = Blocks.AIR;
        }

        if ("minecraft:jigsaw".equals(name) || block == Blocks.JIGSAW)
        {
            return Blocks.AIR.defaultBlockState();
        }

        state = block.defaultBlockState();

        if (entry.contains("Properties"))
        {
            CompoundTag props = entry.getCompoundOrEmpty("Properties");

            for (String key : props.keySet())
            {
                String value = props.getStringOr(key, "");
                Property<?> property = block.getStateDefinition().getProperty(key);

                if (property != null)
                {
                    Optional<?> parsed = property.getValue(value);

                    if (parsed.isPresent())
                    {
                        try
                        {
                            @SuppressWarnings({"rawtypes", "unchecked"})
                            Property raw = property;
                            @SuppressWarnings("unchecked")
                            Comparable c = (Comparable) parsed.get();
                            state = state.setValue(raw, c);
                        }
                        catch (Exception ignored)
                        {
                            /* Ignore malformed property */
                        }
                    }
                }
            }
        }

        return state;
    }

    public static boolean isTranslucentBlock(BlockState state)
    {
        if (state == null || StructureData.isAnimatedTexture(state))
        {
            return false;
        }

        return state.propagatesSkylightDown();
    }

    public static boolean isAnimatedTexture(BlockState state)
    {
        if (state == null)
        {
            return false;
        }

        if (state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE))
        {
            return true;
        }

        FluidState fs = state.getFluidState();

        if (fs != null)
        {
            if (fs.getType() == Fluids.WATER || fs.getType() == Fluids.FLOWING_WATER ||
                fs.getType() == Fluids.LAVA || fs.getType() == Fluids.FLOWING_LAVA)
            {
                return true;
            }
        }

        return false;
    }

    public static boolean isBiomeTinted(BlockState state)
    {
        if (state == null)
        {
            return false;
        }

        Block b = state.getBlock();

        return (b instanceof LeavesBlock)
            || (b instanceof GrassBlock)
            || (b instanceof VineBlock)
            || (b instanceof WaterlilyBlock)
            || (b instanceof RedStoneWireBlock)
            || (b instanceof StemBlock)
            || (b instanceof AttachedStemBlock)
            || state.is(Blocks.FERN)
            || state.is(Blocks.SUGAR_CANE)
            || state.is(Blocks.SHORT_GRASS)
            || state.is(Blocks.TALL_GRASS)
            || state.is(Blocks.LARGE_FERN);
    }

    public static boolean isFancyGraphicsEnabled()
    {
        try
        {
            return Minecraft.getInstance().options.graphicsPreset().get() != GraphicsPreset.FAST;
        }
        catch (Throwable ignored)
        {
            return true;
        }
    }

    public static void syncFancyGraphicsFromOptions()
    {
        /* 1.21.11: RenderLayers option sync no longer required */
    }
}
