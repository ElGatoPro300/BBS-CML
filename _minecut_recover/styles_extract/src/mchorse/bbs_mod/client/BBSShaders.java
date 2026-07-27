package mchorse.bbs_mod.client;

import mchorse.bbs_mod.BBSMod;
import net.minecraft.class_290;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3298;
import net.minecraft.class_3300;
import net.minecraft.class_5912;
import net.minecraft.class_5944;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BBSShaders
{
    public static final List<Runnable> LOADERS = new ArrayList<>();

    private static class_5944 model;
    private static class_5944 multiLink;
    private static class_5944 subtitles;

    private static class_5944 pickerPreview;
    private static class_5944 pickerBillboard;
    private static class_5944 pickerBillboardNoShading;
    private static class_5944 pickerParticles;
    private static class_5944 pickerModels;
    private static class_5944 blockPaintOverlay;
    private static class_5944 flatPaintOverlay;
    private static class_5944 blockColorTintOverlay;
    private static class_5944 flatColorTintOverlay;

    static
    {
        setup();
    }

    public static void setup()
    {
        if (model != null) model.close();
        if (subtitles != null) subtitles.close();
        if (subtitles != null) subtitles.close();

        if (pickerPreview != null) pickerPreview.close();
        if (pickerBillboard != null) pickerBillboard.close();
        if (pickerBillboardNoShading != null) pickerBillboardNoShading.close();
        if (pickerParticles != null) pickerParticles.close();
        if (pickerModels != null) pickerModels.close();
        if (blockPaintOverlay != null) blockPaintOverlay.close();
        if (flatPaintOverlay != null) flatPaintOverlay.close();
        if (blockColorTintOverlay != null) blockColorTintOverlay.close();
        if (flatColorTintOverlay != null) flatColorTintOverlay.close();

        try
        {
            class_5912 factory = new ProxyResourceFactory(class_310.method_1551().method_1478());

            model = new class_5944(factory, "model", class_290.field_1580);
            multiLink = new class_5944(factory, "multilink", class_290.field_1575);
            subtitles = new class_5944(factory, "subtitles", class_290.field_1575);

            pickerPreview = new class_5944(factory, "picker_preview", class_290.field_1575);
            pickerBillboard = new class_5944(factory, "picker_billboard", class_290.field_1580);
            pickerBillboardNoShading = new class_5944(factory, "picker_billboard_no_shading", class_290.field_1586);
            pickerParticles = new class_5944(factory, "picker_particles", class_290.field_20888);
            pickerModels = new class_5944(factory, "picker_models", class_290.field_1580);
            blockPaintOverlay = new class_5944(factory, "block_paint_overlay", class_290.field_1580);
            flatPaintOverlay = new class_5944(factory, "flat_paint_overlay", class_290.field_1580);
            blockColorTintOverlay = new class_5944(factory, "block_color_tint_overlay", class_290.field_1580);
            flatColorTintOverlay = new class_5944(factory, "flat_color_tint_overlay", class_290.field_1580);
        
            for (Runnable runnable : LOADERS)
            {
                runnable.run();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static class_5944 getModel()
    {
        return model;
    }

    public static class_5944 getMultilinkProgram()
    {
        return multiLink;
    }

    public static class_5944 getSubtitlesProgram()
    {
        return subtitles;
    }

    public static class_5944 getPickerPreviewProgram()
    {
        return pickerPreview;
    }

    public static class_5944 getPickerBillboardProgram()
    {
        return pickerBillboard;
    }

    public static class_5944 getPickerBillboardNoShadingProgram()
    {
        return pickerBillboardNoShading;
    }

    public static class_5944 getPickerParticlesProgram()
    {
        return pickerParticles;
    }

    public static class_5944 getPickerModelsProgram()
    {
        return pickerModels;
    }

    public static class_5944 getBlockPaintOverlayProgram()
    {
        return blockPaintOverlay;
    }

    public static class_5944 getFlatPaintOverlayProgram()
    {
        return flatPaintOverlay;
    }

    public static class_5944 getBlockColorTintOverlayProgram()
    {
        return blockColorTintOverlay;
    }

    public static class_5944 getFlatColorTintOverlayProgram()
    {
        return flatColorTintOverlay;
    }

    private static class ProxyResourceFactory implements class_5912
    {
        private class_3300 manager;

        public ProxyResourceFactory(class_3300 manager)
        {
            this.manager = manager;
        }

        @Override
        public Optional<class_3298> method_14486(class_2960 id)
        {
            if (id.method_12832().contains("/core/"))
            {
                return this.manager.method_14486(class_2960.method_60655(BBSMod.MOD_ID, id.method_12832()));
            }

            return this.manager.method_14486(id);
        }
    }
}
