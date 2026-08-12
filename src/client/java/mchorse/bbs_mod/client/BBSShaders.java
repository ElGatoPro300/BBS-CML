package mchorse.bbs_mod.client;

import mchorse.bbs_mod.BBSMod;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
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
        "picker_preview", DefaultVertexFormat.POSITION_TEX_COLOR
    );
    private static final RenderPipeline PICKER_BILLBOARD = registerPicker(
        "picker_billboard", DefaultVertexFormat.NEW_ENTITY
    );
    private static final RenderPipeline PICKER_BILLBOARD_NO_SHADING = registerPicker(
        "picker_billboard_no_shading", DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR
    );
    private static final RenderPipeline PICKER_PARTICLES = registerPicker(
        "picker_particles", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP
    );
    private static final RenderPipeline PICKER_MODELS = registerPicker(
        "picker_models", DefaultVertexFormat.NEW_ENTITY
    );

    private static final RenderPipeline PARTICLES = registerParticles();

    private static RenderType modelLayer;
    private static RenderType multiLinkLayer;
    private static RenderType subtitlesLayer;
    private static RenderType pickerPreviewLayer;
    private static RenderType pickerBillboardLayer;
    private static RenderType pickerBillboardNoShadingLayer;
    private static RenderType pickerParticlesLayer;
    private static RenderType pickerModelsLayer;
    private static RenderType particlesLayer;

    public static void setup()
    {
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

    public static RenderType getMultilinkLayer()
    {
        if (multiLinkLayer == null)
        {
            multiLinkLayer = layer("multilink", MULTILINK, false);
        }

        return multiLinkLayer;
    }

    public static RenderType getSubtitlesLayer()
    {
        if (subtitlesLayer == null)
        {
            subtitlesLayer = layer("subtitles", SUBTITLES, false);
        }

        return subtitlesLayer;
    }

    public static RenderType getPickerPreviewLayer()
    {
        if (pickerPreviewLayer == null)
        {
            pickerPreviewLayer = layer("picker_preview", PICKER_PREVIEW, false);
        }

        return pickerPreviewLayer;
    }

    public static RenderType getPickerBillboardLayer()
    {
        if (pickerBillboardLayer == null)
        {
            pickerBillboardLayer = layer("picker_billboard", PICKER_BILLBOARD, true);
        }

        return pickerBillboardLayer;
    }

    public static RenderType getPickerBillboardNoShadingLayer()
    {
        if (pickerBillboardNoShadingLayer == null)
        {
            pickerBillboardNoShadingLayer = layer("picker_billboard_no_shading", PICKER_BILLBOARD_NO_SHADING, true);
        }

        return pickerBillboardNoShadingLayer;
    }

    public static RenderType getPickerParticlesLayer()
    {
        if (pickerParticlesLayer == null)
        {
            pickerParticlesLayer = layer("picker_particles", PICKER_PARTICLES, false);
        }

        return pickerParticlesLayer;
    }

    public static RenderType getPickerModelsLayer()
    {
        if (pickerModelsLayer == null)
        {
            pickerModelsLayer = layer("picker_models", PICKER_MODELS, true);
        }

        return pickerModelsLayer;
    }

    public static RenderType getParticlesLayer()
    {
        if (particlesLayer == null)
        {
            RenderSetup.RenderSetupBuilder setup = RenderSetup.builder(PARTICLES)
                .bufferSize(RenderType.BIG_BUFFER_SIZE)
                .sortOnUpload()
                .useLightmap();

            particlesLayer = RenderType.create(BBSMod.MOD_ID + "_particles", setup.createRenderSetup());
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
        Identifier shader = Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "core/model");

        RenderPipeline.Builder builder = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "pipeline/model"))
            .withVertexShader(shader)
            .withFragmentShader(shader)
            .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.DrawMode.QUADS)
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
        Identifier shader = Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "core/particles");

        RenderPipeline.Builder builder = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "pipeline/particles"))
            .withVertexShader(shader)
            .withFragmentShader(shader)
            .withVertexFormat(DefaultVertexFormat.PARTICLE, VertexFormat.DrawMode.QUADS)
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
        Identifier shader = Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "core/multilink");

        RenderPipeline.Builder builder = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "pipeline/multilink"))
            .withVertexShader(shader)
            .withFragmentShader(shader)
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.DrawMode.QUADS)
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
        Identifier shader = Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "core/subtitles");

        RenderPipeline.Builder builder = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "pipeline/subtitles"))
            .withVertexShader(shader)
            .withFragmentShader(shader)
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.DrawMode.QUADS)
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
        Identifier shader = Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "core/" + name);

        RenderPipeline.Builder builder = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "pipeline/" + name))
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

    private static RenderType layer(String name, RenderPipeline pipeline, boolean useLightmapOverlay)
    {
        RenderSetup.RenderSetupBuilder setup = RenderSetup.builder(pipeline)
            .bufferSize(RenderType.BIG_BUFFER_SIZE)
            .sortOnUpload();

        if (useLightmapOverlay)
        {
            setup.useLightmap().useOverlay();
        }

        return RenderType.create(BBSMod.MOD_ID + "_" + name, setup.createRenderSetup());
    }
}
