package mchorse.bbs_mod.client;

import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.items.StructurePickerMode;
import mchorse.bbs_mod.items.StructurePickerSelection;

import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StructurePickerShapeOutline
{
    private static final float FACE_FILL_ALPHA = 0.42F;

    public static void render(PoseStack stack, BlockPos first, BlockPos second, StructurePickerMode mode, Direction triangleFacing, float r, float g, float b, float a)
    {
        if (!mode.hasShapeOutline())
        {
            return;
        }

        List<BlockPos> blocks = StructurePickerSelection.preview(null, first, second, mode, triangleFacing);

        if (blocks.isEmpty())
        {
            return;
        }

        StructurePickerShapeOutline.renderFaces(stack, new HashSet<>(blocks), r, g, b);
    }

    private static void renderFaces(PoseStack stack, Set<BlockPos> blocks, float r, float g, float b)
    {
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        for (BlockPos pos : blocks)
        {
            if (!blocks.contains(pos.above()))
            {
                StructurePickerShapeOutline.drawTopFaceFill(builder, stack, pos, r, g, b);
            }

            if (!blocks.contains(pos.below()))
            {
                StructurePickerShapeOutline.drawBottomFaceFill(builder, stack, pos, r, g, b);
            }

            if (!blocks.contains(pos.north()))
            {
                StructurePickerShapeOutline.drawNorthFaceFill(builder, stack, pos, r, g, b);
            }

            if (!blocks.contains(pos.south()))
            {
                StructurePickerShapeOutline.drawSouthFaceFill(builder, stack, pos, r, g, b);
            }

            if (!blocks.contains(pos.west()))
            {
                StructurePickerShapeOutline.drawWestFaceFill(builder, stack, pos, r, g, b);
            }

            if (!blocks.contains(pos.east()))
            {
                StructurePickerShapeOutline.drawEastFaceFill(builder, stack, pos, r, g, b);
            }
        }

        MeshData built = builder.build();

        if (built != null)
        {
            RenderTypes.debugFilledBox().draw(built);
        }
    }

    private static void drawTopFaceFill(BufferBuilder builder, PoseStack stack, BlockPos pos, float r, float g, float b)
    {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        Draw.fillBox(builder, stack, x, y + 1F, z, x + 1F, y + 1.001F, z + 1F, r, g, b, FACE_FILL_ALPHA);
    }

    private static void drawBottomFaceFill(BufferBuilder builder, PoseStack stack, BlockPos pos, float r, float g, float b)
    {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        Draw.fillBox(builder, stack, x, y, z, x + 1F, y + 0.001F, z + 1F, r, g, b, FACE_FILL_ALPHA);
    }

    private static void drawNorthFaceFill(BufferBuilder builder, PoseStack stack, BlockPos pos, float r, float g, float b)
    {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        Draw.fillBox(builder, stack, x, y, z, x + 1F, y + 1F, z + 0.001F, r, g, b, FACE_FILL_ALPHA);
    }

    private static void drawSouthFaceFill(BufferBuilder builder, PoseStack stack, BlockPos pos, float r, float g, float b)
    {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        Draw.fillBox(builder, stack, x, y, z + 1F - 0.001F, x + 1F, y + 1F, z + 1F, r, g, b, FACE_FILL_ALPHA);
    }

    private static void drawWestFaceFill(BufferBuilder builder, PoseStack stack, BlockPos pos, float r, float g, float b)
    {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        Draw.fillBox(builder, stack, x, y, z, x + 0.001F, y + 1F, z + 1F, r, g, b, FACE_FILL_ALPHA);
    }

    private static void drawEastFaceFill(BufferBuilder builder, PoseStack stack, BlockPos pos, float r, float g, float b)
    {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        Draw.fillBox(builder, stack, x + 1F - 0.001F, y, z, x + 1F, y + 1F, z + 1F, r, g, b, FACE_FILL_ALPHA);
    }
}
