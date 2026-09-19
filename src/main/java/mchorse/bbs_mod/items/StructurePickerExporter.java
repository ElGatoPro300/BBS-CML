package mchorse.bbs_mod.items;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.blocks.ModelBlock;
import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.blocks.entities.ModelProperties;
import mchorse.bbs_mod.forms.forms.StructureForm;
import mchorse.bbs_mod.mixin.StructureTemplateAccessor;
import mchorse.bbs_mod.mixin.StructureTemplatePalettedListAccessor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StructurePickerExporter
{
    public static String export(ServerLevel world, List<BlockPos> blocks)
    {
        if (blocks.isEmpty())
        {
            return null;
        }

        BlockPos min = blocks.getFirst();
        BlockPos max = blocks.getFirst();

        for (BlockPos pos : blocks)
        {
            min = StructurePickerSelection.min(min, pos);
            max = StructurePickerSelection.max(max, pos);
        }

        Vec3i size = max.subtract(min).offset(1, 1, 1);
        StructureTemplate template = new StructureTemplate();

        template.fillFromWorld(world, min, size, true, Collections.singletonList(Blocks.STRUCTURE_VOID));
        filterTemplate(template, min, new HashSet<>(blocks));

        File generatedFolder = world.getServer().getWorldPath(LevelResource.GENERATED_DIR).toFile();
        File folder = new File(new File(generatedFolder, "minecraft"), "structures");

        if (!folder.exists())
        {
            folder.mkdirs();
        }

        String fileName = "pick_" + System.currentTimeMillis() + ".nbt";
        File file = new File(folder, fileName);

        try
        {
            CompoundTag nbt = new CompoundTag();

            template.save(nbt);
            NbtIo.writeCompressed(nbt, file.toPath());
        }
        catch (IOException e)
        {
            e.printStackTrace();

            return null;
        }

        return "world:" + fileName;
    }

    public static boolean placeModelBlock(ServerLevel world, BlockPos center, String structurePath)
    {
        if (structurePath == null || structurePath.isEmpty())
        {
            return false;
        }

        if (world.getBlockState(center).is(BBSMod.MODEL_BLOCK))
        {
            BlockEntity blockEntity = world.getBlockEntity(center);

            if (blockEntity instanceof ModelBlockEntity modelBlockEntity)
            {
                StructureForm form = new StructureForm();

                form.structureFile.set(structurePath);

                ModelProperties properties = modelBlockEntity.getProperties();

                properties.setForm(form);
                properties.setName("Structure");
                properties.setHitbox(true);
                modelBlockEntity.setChanged();
                world.sendBlockUpdated(center, world.getBlockState(center), world.getBlockState(center), 3);

                return true;
            }
        }

        StructureForm form = new StructureForm();

        form.structureFile.set(structurePath);

        BlockState modelState = BBSMod.MODEL_BLOCK.defaultBlockState()
            .setValue(BlockStateProperties.WATERLOGGED, world.getFluidState(center).is(Fluids.WATER))
            .setValue(ModelBlock.LIGHT_LEVEL, 0);

        if (!world.setBlock(center, modelState, 3))
        {
            return false;
        }

        BlockEntity blockEntity = world.getBlockEntity(center);

        if (!(blockEntity instanceof ModelBlockEntity modelBlockEntity))
        {
            return false;
        }

        ModelProperties properties = modelBlockEntity.getProperties();

        properties.setForm(form);
        properties.setName("Structure");
        properties.setHitbox(true);
        modelBlockEntity.setChanged();
        world.sendBlockUpdated(center, modelState, modelState, 3);

        return true;
    }

    public static void removeBlocks(ServerLevel world, List<BlockPos> blocks)
    {
        for (BlockPos pos : blocks)
        {
            /* Never break model blocks (e.g. one just placed at the selection center) */
            if (world.getBlockState(pos).is(BBSMod.MODEL_BLOCK))
            {
                continue;
            }

            world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    /**
     * Block position whose center matches the structure form's render pivot,
     * so the rendered structure lines up with the original world blocks.
     */
    public static BlockPos getPlacementPos(BlockPos min, BlockPos max)
    {
        int sizeX = max.getX() - min.getX() + 1;
        int sizeZ = max.getZ() - min.getZ() + 1;

        return new BlockPos(
            min.getX() + (sizeX - 1) / 2,
            min.getY(),
            min.getZ() + (sizeZ - 1) / 2
        );
    }

    private static void filterTemplate(StructureTemplate template, BlockPos origin, Set<BlockPos> selected)
    {
        StructureTemplateAccessor accessor = (StructureTemplateAccessor) template;

        for (StructureTemplate.Palette list : accessor.bbs$getBlockInfoLists())
        {
            StructureTemplatePalettedListAccessor palette = (StructureTemplatePalettedListAccessor) (Object) list;

            palette.bbs$getInfos().removeIf((info) -> !selected.contains(origin.offset(info.pos())));
        }

        accessor.bbs$getBlockInfoLists().removeIf((list) -> ((StructureTemplatePalettedListAccessor) (Object) list).bbs$getInfos().isEmpty());
    }
}
