package mchorse.bbs_mod.client;

import mchorse.bbs_mod.BBSMod;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;

import java.util.ArrayList;
import java.util.List;

public class BBSShaders
{
    public static final List<Runnable> LOADERS = new ArrayList<>();

    private static final BlendFunction BLEND = BlendFunction.TRANSLUCENT;

    public static final String PICKER_UNIFORM = "BBSPicker";

    private static final RenderPipeline MODEL = registerModel();
    private static final RenderPipeline MULTILINK = registerMultilink();
    private static final RenderPipeline SUBTITLES = registerSubtitles();

    private static final RenderPipeline PICKER_PREVIEW = registerPicker(
        "picker_preview", VertexFormats.POSITION_TEXTURE_COLOR
    );
    private static final RenderPipeline PICKER_BILLBOARD = registerPicker(
        "picker_billboard", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL
    );
    private static final RenderPipeline PICKER_BILLBOARD_NO_SHADING = registerPicker(
        "picker_billboard_no_shading", VertexFormats.POSITION_TEXTURE_LIGHT_COLOR
    );
    private static final RenderPipeline PICKER_PARTICLES = registerPicker(
        "picker_particles", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT
    );
    private static final RenderPipeline PICKER_MODELS = registerPicker(
        "picker_models", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL
    );

    private static final RenderPipeline PARTICLES = registerParticles();

    private static RenderLayer modelLayer;
    private static RenderLayer multiLinkLayer;
    private static RenderLayer subtitlesLayer;
    private static RenderLayer pickerPreviewLayer;
    private static RenderLayer pickerBillboardLayer;
    private static RenderLayer pickerBillboardNoShadingLayer;
    private static RenderLayer pickerParticlesLayer;
    private static RenderLayer pickerModelsLayer;
    private static RenderLayer particlesLayer;

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

        ShaderLoader loader = MinecraftClient.getInstance().getShaderLoader();
        Defines defines = Defines.EMPTY;

        ShaderProgramKey modelKey = new ShaderProgramKey(Identifier.of(BBSMod.MOD_ID, "core/model"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, defines);
        ShaderProgramKey multiLinkKey = new ShaderProgramKey(Identifier.of(BBSMod.MOD_ID, "core/multilink"), VertexFormats.POSITION_TEXTURE_COLOR, defines);
        ShaderProgramKey subtitlesKey = new ShaderProgramKey(Identifier.of(BBSMod.MOD_ID, "core/subtitles"), VertexFormats.POSITION_TEXTURE_COLOR, defines);

        ShaderProgramKey pickerPreviewKey = new ShaderProgramKey(Identifier.of(BBSMod.MOD_ID, "core/picker_preview"), VertexFormats.POSITION_TEXTURE_COLOR, defines);
        ShaderProgramKey pickerBillboardKey = new ShaderProgramKey(Identifier.of(BBSMod.MOD_ID, "core/picker_billboard"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, defines);
        ShaderProgramKey pickerBillboardNoShadingKey = new ShaderProgramKey(Identifier.of(BBSMod.MOD_ID, "core/picker_billboard_no_shading"), VertexFormats.POSITION_TEXTURE_LIGHT_COLOR, defines);
        ShaderProgramKey pickerParticlesKey = new ShaderProgramKey(Identifier.of(BBSMod.MOD_ID, "core/picker_particles"), VertexFormats.POSITION_COLOR_TEXTURE_LIGHT, defines);
        ShaderProgramKey pickerModelsKey = new ShaderProgramKey(Identifier.of(BBSMod.MOD_ID, "core/picker_models"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, defines);

        ShaderProgramKey blockPaintOverlayKey = new ShaderProgramKey(Identifier.of(BBSMod.MOD_ID, "core/block_paint_overlay"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, defines);
        ShaderProgramKey flatPaintOverlayKey = new ShaderProgramKey(Identifier.of(BBSMod.MOD_ID, "core/flat_paint_overlay"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, defines);
        ShaderProgramKey blockColorTintOverlayKey = new ShaderProgramKey(Identifier.of(BBSMod.MOD_ID, "core/block_color_tint_overlay"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, defines);
        ShaderProgramKey flatColorTintOverlayKey = new ShaderProgramKey(Identifier.of(BBSMod.MOD_ID, "core/flat_color_tint_overlay"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, defines);

        model = loader.getOrCreateProgram(modelKey);
        multiLink = loader.getOrCreateProgram(multiLinkKey);
        subtitles = loader.getOrCreateProgram(subtitlesKey);

        pickerPreview = loader.getOrCreateProgram(pickerPreviewKey);
        pickerBillboard = loader.getOrCreateProgram(pickerBillboardKey);
        pickerBillboardNoShading = loader.getOrCreateProgram(pickerBillboardNoShadingKey);
        pickerParticles = loader.getOrCreateProgram(pickerParticlesKey);
        pickerModels = loader.getOrCreateProgram(pickerModelsKey);
        blockPaintOverlay = loader.getOrCreateProgram(blockPaintOverlayKey);
        flatPaintOverlay = loader.getOrCreateProgram(flatPaintOverlayKey);
        blockColorTintOverlay = loader.getOrCreateProgram(blockColorTintOverlayKey);
        flatColorTintOverlay = loader.getOrCreateProgram(flatColorTintOverlayKey);

        for (Runnable runnable : LOADERS)
        {
            runnable.run();
        }
    }

    public static RenderPipeline getModel()
    {
        return MODEL;
    }

    public static RenderPipeline getMultilinkProgram()
    {
        return MULTILINK;
    }

    public static RenderPipeline getSubtitlesProgram()
    {
        return SUBTITLES;
    }

    public static RenderPipeline getPickerPreviewProgram()
    {
        return PICKER_PREVIEW;
    }

    public static RenderPipeline getPickerBillboardProgram()
    {
        return PICKER_BILLBOARD;
    }

    public static RenderPipeline getPickerBillboardNoShadingProgram()
    {
        return PICKER_BILLBOARD_NO_SHADING;
    }

    public static RenderPipeline getPickerParticlesProgram()
    {
        return PICKER_PARTICLES;
    }

    public static RenderPipeline getPickerModelsProgram()
    {
        return PICKER_MODELS;
    }

    public static RenderPipeline getParticles()
    {
        return PARTICLES;
    }

    public static ShaderProgram getFlatPaintOverlayProgram()
    {
        if (multiLinkLayer == null)
        {
            multiLinkLayer = layer("multilink", MULTILINK, false);
        }

        return multiLinkLayer;
    }

    public static ShaderProgram getBlockColorTintOverlayProgram()
    {
        return blockColorTintOverlay;
    }

    public static ShaderProgram getFlatColorTintOverlayProgram()
    {
        if (subtitlesLayer == null)
        {
            subtitlesLayer = layer("subtitles", SUBTITLES, false);

        return subtitlesLayer;
    }

    public static RenderLayer getPickerPreviewLayer()
    {
        }

        return pickerPreviewLayer;
    }

    public static RenderLayer getPickerBillboardLayer()
    {
        if (pickerBillboardLayer == null)
        {
            pickerBillboardLayer = layer("picker_billboard", PICKER_BILLBOARD, true);
        }

        return pickerBillboardLayer;
    }

    public static RenderLayer getPickerBillboardNoShadingLayer()
    {
        if (pickerBillboardNoShadingLayer == null)
        {
            pickerBillboardNoShadingLayer = layer("picker_billboard_no_shading", PICKER_BILLBOARD_NO_SHADING, true);
        }

        return pickerBillboardNoShadingLayer;
    }

    public static RenderLayer getPickerParticlesLayer()
    {
        if (pickerParticlesLayer == null)
        {
            pickerParticlesLayer = layer("picker_particles", PICKER_PARTICLES, false);
        }

        return pickerParticlesLayer;
    }

    public static RenderLayer getPickerModelsLayer()
    {
        if (pickerModelsLayer == null)
        {
            pickerModelsLayer = layer("picker_models", PICKER_MODELS, true);
        }

        return pickerModelsLayer;
    }

    public static RenderLayer getParticlesLayer()
    {
        if (particlesLayer == null)
        {
            RenderSetup.Builder setup = RenderSetup.builder(PARTICLES)
                .expectedBufferSize(RenderLayer.field_64008)
                .translucent()
                .useLightmap();

            particlesLayer = RenderLayer.of(BBSMod.MOD_ID + "_particles", setup.build());
        }

        return particlesLayer;
    }

    public static RenderPipeline getBlockPaintOverlayProgram()
    {
        return MODEL;
    }

    public static RenderPipeline getBlockColorTintOverlayProgram()
    {
        return MODEL;
    }

    public static RenderPipeline getFlatColorTintOverlayProgram()
    {
        return MODEL;
    }

    public static RenderPipeline getFlatPaintOverlayProgram()
    {
        return MODEL;
    }

    public static RenderPipeline getModelProgram()
    {
        return MODEL;
    }

    private static RenderPipeline registerModel()
    {
        Identifier shader = Identifier.of(BBSMod.MOD_ID, "core/model");

        RenderPipeline.Builder builder = RenderPipeline.builder()
            .withLocation(Identifier.of(BBSMod.MOD_ID, "pipeline/model"))
            .withVertexShader(shader)
            .withFragmentShader(shader)
            .withVertexFormat(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS)
            .withBlend(BLEND)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withCull(false)
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("Fog", UniformType.UNIFORM_BUFFER)
            .withUniform("Lighting", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withSampler("Sampler1")
            .withSampler("Sampler2");

        return RenderPipelines.register(builder.build());
    }

    private static RenderPipeline registerParticles()
    {
        Identifier shader = Identifier.of(BBSMod.MOD_ID, "core/particles");

        RenderPipeline.Builder builder = RenderPipeline.builder()
            .withLocation(Identifier.of(BBSMod.MOD_ID, "pipeline/particles"))
            .withVertexShader(shader)
            .withFragmentShader(shader)
            .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR_LIGHT, VertexFormat.DrawMode.QUADS)
            .withBlend(BLEND)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withCull(false)
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("Fog", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withSampler("Sampler2");

        return RenderPipelines.register(builder.build());
    }

    private static RenderPipeline registerMultilink()
    {
        Identifier shader = Identifier.of(BBSMod.MOD_ID, "core/multilink");

        RenderPipeline.Builder builder = RenderPipeline.builder()
            .withLocation(Identifier.of(BBSMod.MOD_ID, "pipeline/multilink"))
            .withVertexShader(shader)
            .withFragmentShader(shader)
            .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
            .withBlend(BLEND)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withCull(false)
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("MultilinkInfo", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withSampler("Sampler3");

        return RenderPipelines.register(builder.build());
    }

    private static RenderPipeline registerSubtitles()
    {
        Identifier shader = Identifier.of(BBSMod.MOD_ID, "core/subtitles");

        RenderPipeline.Builder builder = RenderPipeline.builder()
            .withLocation(Identifier.of(BBSMod.MOD_ID, "pipeline/subtitles"))
            .withVertexShader(shader)
            .withFragmentShader(shader)
            .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
            .withBlend(BLEND)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withCull(false)
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("SubtitlesInfo", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0");

        return RenderPipelines.register(builder.build());
    }

    private static RenderPipeline registerPicker(String name, VertexFormat format)
    {
        Identifier shader = Identifier.of(BBSMod.MOD_ID, "core/" + name);

        RenderPipeline.Builder builder = RenderPipeline.builder()
            .withLocation(Identifier.of(BBSMod.MOD_ID, "pipeline/" + name))
            .withVertexShader(shader)
            .withFragmentShader(shader)
            .withVertexFormat(format, VertexFormat.DrawMode.QUADS)
            .withBlend(BLEND)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withCull(false)
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform(PICKER_UNIFORM, UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0");

        return RenderPipelines.register(builder.build());
    }

    private static RenderLayer layer(String name, RenderPipeline pipeline, boolean useLightmapOverlay)
    {
        RenderSetup.Builder setup = RenderSetup.builder(pipeline)
            .expectedBufferSize(RenderLayer.field_64008)
            .translucent();

        if (useLightmapOverlay)
        {
            setup.useLightmap().useOverlay();
        }

        return RenderLayer.of(BBSMod.MOD_ID + "_" + name, setup.build());
    }
}
