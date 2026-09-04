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

<<<<<<< HEAD
    public static final String PICKER_UNIFORM = "BBSPicker";

    private static final RenderPipeline MODEL = registerModel();
    private static final RenderPipeline MULTILINK = registerMultilink();
    private static final RenderPipeline SUBTITLES = registerSubtitles();
    private static final RenderPipeline IMAGE_OVERLAY = registerOverlay("image_overlay", false);

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

    private static final RenderPipeline BLOCK_PAINT_OVERLAY = registerOverlay("block_paint_overlay", false);
    private static final RenderPipeline FLAT_PAINT_OVERLAY = registerOverlay("flat_paint_overlay", false);
    private static final RenderPipeline BLOCK_GLOW_OVERLAY = registerOverlay("block_glow_overlay", false);
    private static final RenderPipeline BLOCK_COLOR_TINT_OVERLAY = registerOverlay("block_color_tint_overlay", true);
    private static final RenderPipeline FLAT_COLOR_TINT_OVERLAY = registerOverlay("flat_color_tint_overlay", true);

    private static RenderLayer modelLayer;
    private static RenderLayer multiLinkLayer;
    private static RenderLayer subtitlesLayer;
    private static RenderLayer imageOverlayLayer;
    private static RenderLayer pickerPreviewLayer;
    private static RenderLayer pickerBillboardLayer;
    private static RenderLayer pickerBillboardNoShadingLayer;
    private static RenderLayer pickerParticlesLayer;
    private static RenderLayer pickerModelsLayer;
    private static RenderLayer particlesLayer;
    private static RenderLayer blockPaintOverlayLayer;
    private static RenderLayer flatPaintOverlayLayer;
    private static RenderLayer blockGlowOverlayLayer;
    private static RenderLayer blockColorTintOverlayLayer;
    private static RenderLayer flatColorTintOverlayLayer;

    public static void setup()
    {
        modelLoadRetried = false;

        if (model != null)
        {
            model.close();
            model = null;
        }

        if (multiLink != null)
        {
            multiLink.close();
            multiLink = null;
        }

        if (subtitles != null)
        {
            subtitles.close();
            subtitles = null;
        }

        if (imageOverlay != null)
        {
            imageOverlay.close();
            imageOverlay = null;
        }

        if (pickerPreview != null)
        {
            pickerPreview.close();
            pickerPreview = null;
        }

        if (pickerBillboard != null)
        {
            pickerBillboard.close();
            pickerBillboard = null;
        }

        if (pickerBillboardNoShading != null)
        {
            pickerBillboardNoShading.close();
            pickerBillboardNoShading = null;
        }

        if (pickerParticles != null)
        {
            pickerParticles.close();
            pickerParticles = null;
        }

        if (pickerModels != null)
        {
            pickerModels.close();
            pickerModels = null;
        }

        if (blockPaintOverlay != null)
        {
            blockPaintOverlay.close();
            blockPaintOverlay = null;
        }

        if (flatPaintOverlay != null)
        {
            flatPaintOverlay.close();
            flatPaintOverlay = null;
        }

        if (blockGlowOverlay != null)
        {
            blockGlowOverlay.close();
            blockGlowOverlay = null;
        }

        if (blockColorTintOverlay != null)
        {
            blockColorTintOverlay.close();
            blockColorTintOverlay = null;
        }

        if (flatColorTintOverlay != null)
        {
            flatColorTintOverlay.close();
            flatColorTintOverlay = null;
        }

        ShaderLoader loader = MinecraftClient.getInstance().getShaderLoader();
        Defines defines = Defines.EMPTY;

        ShaderProgramKey modelKey = new ShaderProgramKey(Identifier.of(BBSMod.MOD_ID, "core/model"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, defines);
        ShaderProgramKey multiLinkKey = new ShaderProgramKey(Identifier.of(BBSMod.MOD_ID, "core/multilink"), VertexFormats.POSITION_TEXTURE_COLOR, defines);
        ShaderProgramKey subtitlesKey = new ShaderProgramKey(Identifier.of(BBSMod.MOD_ID, "core/subtitles"), VertexFormats.POSITION_TEXTURE_COLOR, defines);
        ShaderProgramKey imageOverlayKey = new ShaderProgramKey(Identifier.of(BBSMod.MOD_ID, "core/image_overlay"), VertexFormats.POSITION_TEXTURE_COLOR, defines);

        ShaderProgramKey pickerPreviewKey = new ShaderProgramKey(Identifier.of(BBSMod.MOD_ID, "core/picker_preview"), VertexFormats.POSITION_TEXTURE_COLOR, defines);
        ShaderProgramKey pickerBillboardKey = new ShaderProgramKey(Identifier.of(BBSMod.MOD_ID, "core/picker_billboard"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, defines);
        ShaderProgramKey pickerBillboardNoShadingKey = new ShaderProgramKey(Identifier.of(BBSMod.MOD_ID, "core/picker_billboard_no_shading"), VertexFormats.POSITION_TEXTURE_LIGHT_COLOR, defines);
        ShaderProgramKey pickerParticlesKey = new ShaderProgramKey(Identifier.of(BBSMod.MOD_ID, "core/picker_particles"), VertexFormats.POSITION_COLOR_TEXTURE_LIGHT, defines);
        ShaderProgramKey pickerModelsKey = new ShaderProgramKey(Identifier.of(BBSMod.MOD_ID, "core/picker_models"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, defines);

        ShaderProgramKey blockPaintOverlayKey = new ShaderProgramKey(Identifier.of(BBSMod.MOD_ID, "core/block_paint_overlay"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, defines);
        ShaderProgramKey flatPaintOverlayKey = new ShaderProgramKey(Identifier.of(BBSMod.MOD_ID, "core/flat_paint_overlay"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, defines);
        ShaderProgramKey blockGlowOverlayKey = new ShaderProgramKey(Identifier.of(BBSMod.MOD_ID, "core/block_glow_overlay"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, defines);
        ShaderProgramKey blockColorTintOverlayKey = new ShaderProgramKey(Identifier.of(BBSMod.MOD_ID, "core/block_color_tint_overlay"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, defines);
        ShaderProgramKey flatColorTintOverlayKey = new ShaderProgramKey(Identifier.of(BBSMod.MOD_ID, "core/flat_color_tint_overlay"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, defines);

        model = loader.getOrCreateProgram(modelKey);
        multiLink = loader.getOrCreateProgram(multiLinkKey);
        subtitles = loader.getOrCreateProgram(subtitlesKey);
        imageOverlay = loader.getOrCreateProgram(imageOverlayKey);

        pickerPreview = loader.getOrCreateProgram(pickerPreviewKey);
        pickerBillboard = loader.getOrCreateProgram(pickerBillboardKey);
        pickerBillboardNoShading = loader.getOrCreateProgram(pickerBillboardNoShadingKey);
        pickerParticles = loader.getOrCreateProgram(pickerParticlesKey);
        pickerModels = loader.getOrCreateProgram(pickerModelsKey);
        blockPaintOverlay = loader.getOrCreateProgram(blockPaintOverlayKey);
        flatPaintOverlay = loader.getOrCreateProgram(flatPaintOverlayKey);
        blockGlowOverlay = loader.getOrCreateProgram(blockGlowOverlayKey);
        blockColorTintOverlay = loader.getOrCreateProgram(blockColorTintOverlayKey);
        flatColorTintOverlay = loader.getOrCreateProgram(flatColorTintOverlayKey);

>>>>>>> 1.21.4
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

    public static RenderPipeline getImageOverlayProgram()
    {
        return IMAGE_OVERLAY;
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

    public static RenderPipeline getBlockPaintOverlayProgram()
    {
        return BLOCK_PAINT_OVERLAY;
    }

    public static RenderPipeline getFlatPaintOverlayProgram()
    {
        return FLAT_PAINT_OVERLAY;
    }

    public static RenderPipeline getBlockGlowOverlayProgram()
    {
        return BLOCK_GLOW_OVERLAY;
    }

    public static RenderPipeline getBlockColorTintOverlayProgram()
    {
        return BLOCK_COLOR_TINT_OVERLAY;
    }

    public static RenderPipeline getFlatColorTintOverlayProgram()
    {
        return FLAT_COLOR_TINT_OVERLAY;
    }

    public static RenderLayer getModelLayer()
    {
        if (modelLayer == null)
        {
            modelLayer = layer("model", MODEL, true);
        }

        return modelLayer;
    }

    public static RenderLayer getMultilinkLayer()
    {
        if (multiLinkLayer == null)
        {
            multiLinkLayer = layer("multilink", MULTILINK, false);
        }

        return multiLinkLayer;
    }

    public static RenderLayer getSubtitlesLayer()
    {
        if (subtitlesLayer == null)
        {
            subtitlesLayer = layer("subtitles", SUBTITLES, false);
        }

        return subtitlesLayer;
    }

    public static RenderLayer getImageOverlayLayer()
    {
        if (imageOverlayLayer == null)
        {
            imageOverlayLayer = layer("image_overlay", IMAGE_OVERLAY, true);
        }

        return imageOverlayLayer;
    }

    public static RenderLayer getPickerPreviewLayer()
    {
        if (pickerPreviewLayer == null)
        {
            pickerPreviewLayer = layer("picker_preview", PICKER_PREVIEW, false);
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

    public static RenderLayer getBlockPaintOverlayLayer()
    {
        if (blockPaintOverlayLayer == null)
        {
            blockPaintOverlayLayer = layer("block_paint_overlay", BLOCK_PAINT_OVERLAY, true);
        }

        return blockPaintOverlayLayer;
    }

    public static RenderLayer getFlatPaintOverlayLayer()
    {
        if (flatPaintOverlayLayer == null)
        {
            flatPaintOverlayLayer = layer("flat_paint_overlay", FLAT_PAINT_OVERLAY, true);
        }

        return flatPaintOverlayLayer;
    }

        public static RenderLayer getBlockColorTintOverlayLayer()
    {
        if (blockColorTintOverlayLayer == null)
        {
            blockColorTintOverlayLayer = layer("block_color_tint_overlay", BLOCK_COLOR_TINT_OVERLAY, true);
        }

        return blockColorTintOverlayLayer;
    }

    public static RenderLayer getFlatColorTintOverlayLayer()
    {
        if (flatColorTintOverlayLayer == null)
        {
            flatColorTintOverlayLayer = layer("flat_color_tint_overlay", FLAT_COLOR_TINT_OVERLAY, true);
        }

        return flatColorTintOverlayLayer;
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

    private static RenderPipeline registerOverlay(String name, boolean hasSampler3)
    {
        Identifier shader = Identifier.of(BBSMod.MOD_ID, "core/" + name);

        RenderPipeline.Builder builder = RenderPipeline.builder()
            .withLocation(Identifier.of(BBSMod.MOD_ID, "pipeline/" + name))
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
            .withSampler("Sampler2");

        if (hasSampler3)
        {
            builder.withSampler("Sampler3");
        }

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
