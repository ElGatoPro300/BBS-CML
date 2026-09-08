package mchorse.bbs_mod.client;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.forms.renderers.utils.ModelEffectPass;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import java.util.ArrayList;
import java.util.List;

public class BBSShaders
{
    public static final List<Runnable> LOADERS = new ArrayList<>();

    public static RenderPipeline modelPipeline;
    public static RenderPipeline multiLinkPipeline;
    public static RenderPipeline subtitlesPipeline;
    public static RenderPipeline imageOverlayPipeline;

    public static RenderPipeline pickerBillboardPipeline;
    public static RenderPipeline pickerBillboardNoShadingPipeline;
    public static RenderPipeline pickerParticlesPipeline;
    public static RenderPipeline pickerModelsPipeline;
    public static RenderPipeline blockPaintOverlayPipeline;
    public static RenderPipeline flatPaintOverlayPipeline;
    public static RenderPipeline blockGlowOverlayPipeline;
    public static RenderPipeline blockColorTintOverlayPipeline;
    public static RenderPipeline flatColorTintOverlayPipeline;

    static
    {
        setup();
    }

    public static void setup()
    {
        modelPipeline = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.MATRICES_FOG_LIGHT_DIR_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "pipeline/model"))
            .withVertexShader(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "core/model"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "core/model"))
            .withSampler("Sampler0")
            .withSampler("Sampler1")
            .withSampler("Sampler2")
            .withSampler("Sampler3")
            .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.DrawMode.QUADS)
            .build());

        multiLinkPipeline = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "pipeline/multilink"))
            .withVertexShader(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "core/multilink"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "core/multilink"))
            .withSampler("Sampler0")
            .withSampler("Sampler3")
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.DrawMode.QUADS)
            .build());

        subtitlesPipeline = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "pipeline/subtitles"))
            .withVertexShader(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "core/subtitles"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "core/subtitles"))
            .withSampler("Sampler0")
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.DrawMode.QUADS)
            .build());

        imageOverlayPipeline = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "pipeline/image_overlay"))
            .withVertexShader(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "core/image_overlay"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "core/image_overlay"))
            .withSampler("Sampler0")
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.DrawMode.QUADS)
            .build());

        pickerBillboardPipeline = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.MATRICES_FOG_LIGHT_DIR_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "pipeline/picker_billboard"))
            .withVertexShader(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "core/picker_billboard"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "core/picker_billboard"))
            .withSampler("Sampler0")
            .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.DrawMode.QUADS)
            .build());

        pickerBillboardNoShadingPipeline = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "pipeline/picker_billboard_no_shading"))
            .withVertexShader(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "core/picker_billboard_no_shading"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "core/picker_billboard_no_shading"))
            .withSampler("Sampler0")
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR, VertexFormat.DrawMode.QUADS)
            .build());

        pickerParticlesPipeline = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.MATRICES_FOG_LIGHT_DIR_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "pipeline/picker_particles"))
            .withVertexShader(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "core/picker_particles"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "core/picker_particles"))
            .withSampler("Sampler0")
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.DrawMode.QUADS)
            .build());

        pickerModelsPipeline = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.MATRICES_FOG_LIGHT_DIR_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "pipeline/picker_models"))
            .withVertexShader(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "core/picker_models"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "core/picker_models"))
            .withSampler("Sampler0")
            .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.DrawMode.QUADS)
            .build());

        blockPaintOverlayPipeline = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "pipeline/block_paint_overlay"))
            .withVertexShader(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "core/block_paint_overlay"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "core/block_paint_overlay"))
            .withSampler("Sampler0")
            .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.DrawMode.QUADS)
            .build());

        flatPaintOverlayPipeline = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.MATRICES_FOG_LIGHT_DIR_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "pipeline/flat_paint_overlay"))
            .withVertexShader(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "core/flat_paint_overlay"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "core/flat_paint_overlay"))
            .withSampler("Sampler0")
            .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.DrawMode.QUADS)
            .build());

        blockGlowOverlayPipeline = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "pipeline/block_glow_overlay"))
            .withVertexShader(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "core/block_glow_overlay"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "core/block_glow_overlay"))
            .withSampler("Sampler0")
            .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.DrawMode.QUADS)
            .build());

        blockColorTintOverlayPipeline = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "pipeline/block_color_tint_overlay"))
            .withVertexShader(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "core/block_color_tint_overlay"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "core/block_color_tint_overlay"))
            .withSampler("Sampler0")
            .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.DrawMode.QUADS)
            .build());

        flatColorTintOverlayPipeline = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.MATRICES_FOG_LIGHT_DIR_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "pipeline/flat_color_tint_overlay"))
            .withVertexShader(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "core/flat_color_tint_overlay"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "core/flat_color_tint_overlay"))
            .withSampler("Sampler0")
            .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.DrawMode.QUADS)
            .build());

        for (Runnable runnable : LOADERS)
        {
            runnable.run();
        }
    }

    public static GlProgram getModel()
    {
        GlProgram program = ModelEffectPass.program(false);

        if (program == null || program == GlProgram.INVALID_PROGRAM)
        {
            return BBSRendering.getEntityTranslucentProgram();
        }

        return program;
    }

    public static GlProgram getMultilinkProgram()
    {
        return BBSRendering.getProgram(multiLinkPipeline);
    }

    public static GlProgram getSubtitlesProgram()
    {
        return BBSRendering.getProgram(subtitlesPipeline);
    }

    public static GlProgram getImageOverlayProgram()
    {
        return BBSRendering.getProgram(imageOverlayPipeline);
    }

    public static GlProgram getPickerBillboardProgram()
    {
        return ModelEffectPass.program("picker_billboard");
    }

    public static GlProgram getPickerBillboardNoShadingProgram()
    {
        return ModelEffectPass.program("picker_billboard_no_shading");
    }

    public static GlProgram getPickerParticlesProgram()
    {
        return ModelEffectPass.program("picker_particles");
    }

    public static GlProgram getPickerModelsProgram()
    {
        return ModelEffectPass.program(true);
    }

    public static GlProgram getBlockPaintOverlayProgram()
    {
        return ModelEffectPass.program("block_paint_overlay");
    }

    public static GlProgram getFlatPaintOverlayProgram()
    {
        return ModelEffectPass.program("flat_paint_overlay");
    }

    public static GlProgram getBlockGlowOverlayProgram()
    {
        return ModelEffectPass.program("block_glow_overlay");
    }

    public static GlProgram getBlockColorTintOverlayProgram()
    {
        return ModelEffectPass.program("block_color_tint_overlay");
    }

    public static GlProgram getFlatColorTintOverlayProgram()
    {
        return ModelEffectPass.program("flat_color_tint_overlay");
    }
}
