package mchorse.bbs_mod.client;

import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.items.StructurePickerMode;
import mchorse.bbs_mod.items.StructurePickerSelection;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_286;
import net.minecraft.class_287;
import net.minecraft.class_289;
import net.minecraft.class_290;
import net.minecraft.class_293;
import net.minecraft.class_4587;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StructurePickerShapeOutline
{
    private static final float FACE_FILL_ALPHA = 0.42F;

    public static void render(class_4587 stack, class_2338 first, class_2338 second, StructurePickerMode mode, class_2350 triangleFacing, float r, float g, float b, float a)
    {
        if (!mode.hasShapeOutline())
        {
            return;
        }

        List<class_2338> blocks = StructurePickerSelection.preview(null, first, second, mode, triangleFacing);

        if (blocks.isEmpty())
        {
            return;
        }

        StructurePickerShapeOutline.renderFaces(stack, new HashSet<>(blocks), r, g, b);
    }

    private static void renderFaces(class_4587 stack, Set<class_2338> blocks, float r, float g, float b)
    {
        class_287 builder = class_289.method_1348().method_60827(class_293.class_5596.field_27379, class_290.field_1576);

        for (class_2338 pos : blocks)
        {
            if (!blocks.contains(pos.method_10084()))
            {
                StructurePickerShapeOutline.drawTopFaceFill(builder, stack, pos, r, g, b);
            }

            if (!blocks.contains(pos.method_10074()))
            {
                StructurePickerShapeOutline.drawBottomFaceFill(builder, stack, pos, r, g, b);
            }

            if (!blocks.contains(pos.method_10095()))
            {
                StructurePickerShapeOutline.drawNorthFaceFill(builder, stack, pos, r, g, b);
            }

            if (!blocks.contains(pos.method_10072()))
            {
                StructurePickerShapeOutline.drawSouthFaceFill(builder, stack, pos, r, g, b);
            }

            if (!blocks.contains(pos.method_10067()))
            {
                StructurePickerShapeOutline.drawWestFaceFill(builder, stack, pos, r, g, b);
            }

            if (!blocks.contains(pos.method_10078()))
            {
                StructurePickerShapeOutline.drawEastFaceFill(builder, stack, pos, r, g, b);
            }
        }

        class_286.method_43433(builder.method_60800());
    }

    private static void drawTopFaceFill(class_287 builder, class_4587 stack, class_2338 pos, float r, float g, float b)
    {
        int x = pos.method_10263();
        int y = pos.method_10264();
        int z = pos.method_10260();

        Draw.fillBox(builder, stack, x, y + 1F, z, x + 1F, y + 1.001F, z + 1F, r, g, b, FACE_FILL_ALPHA);
    }

    private static void drawBottomFaceFill(class_287 builder, class_4587 stack, class_2338 pos, float r, float g, float b)
    {
        int x = pos.method_10263();
        int y = pos.method_10264();
        int z = pos.method_10260();

        Draw.fillBox(builder, stack, x, y, z, x + 1F, y + 0.001F, z + 1F, r, g, b, FACE_FILL_ALPHA);
    }

    private static void drawNorthFaceFill(class_287 builder, class_4587 stack, class_2338 pos, float r, float g, float b)
    {
        int x = pos.method_10263();
        int y = pos.method_10264();
        int z = pos.method_10260();

        Draw.fillBox(builder, stack, x, y, z, x + 1F, y + 1F, z + 0.001F, r, g, b, FACE_FILL_ALPHA);
    }

    private static void drawSouthFaceFill(class_287 builder, class_4587 stack, class_2338 pos, float r, float g, float b)
    {
        int x = pos.method_10263();
        int y = pos.method_10264();
        int z = pos.method_10260();

        Draw.fillBox(builder, stack, x, y, z + 1F - 0.001F, x + 1F, y + 1F, z + 1F, r, g, b, FACE_FILL_ALPHA);
    }

    private static void drawWestFaceFill(class_287 builder, class_4587 stack, class_2338 pos, float r, float g, float b)
    {
        int x = pos.method_10263();
        int y = pos.method_10264();
        int z = pos.method_10260();

        Draw.fillBox(builder, stack, x, y, z, x + 0.001F, y + 1F, z + 1F, r, g, b, FACE_FILL_ALPHA);
    }

    private static void drawEastFaceFill(class_287 builder, class_4587 stack, class_2338 pos, float r, float g, float b)
    {
        int x = pos.method_10263();
        int y = pos.method_10264();
        int z = pos.method_10260();

        Draw.fillBox(builder, stack, x + 1F - 0.001F, y, z, x + 1F, y + 1F, z + 1F, r, g, b, FACE_FILL_ALPHA);
    }
}
