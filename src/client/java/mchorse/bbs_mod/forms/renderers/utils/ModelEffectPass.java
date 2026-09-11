package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.render.ImmediateMesh;
import mchorse.bbs_mod.graphics.RenderPipelineUtils;
import mchorse.bbs_mod.client.BBSUniform;
import mchorse.bbs_mod.client.ModelEffectUniforms;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.graphics.texture.AdoptedTexture;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.utils.iris.IrisCustomPass;
import mchorse.bbs_mod.utils.iris.IrisFormPipelines;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.resources.Identifier;

import org.joml.Matrix4f;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/** Explicit model effect/picking pass. Each draw owns its uniform snapshot. */
public final class ModelEffectPass
{
    private static final class Key
    {
        private final VertexFormat format;
        private final PrimitiveTopology mode;
        private final boolean picking;
        private final boolean depthWrite;
        private final boolean cull;
        private final boolean overlay;
        private final String shader;
        private final boolean multiply;
        private final boolean additive;

        public Key(VertexFormat format, PrimitiveTopology mode, boolean picking, boolean depthWrite, boolean cull, boolean overlay, String shader, boolean multiply, boolean additive)
        {
            this.format = format;
            this.mode = mode;
            this.picking = picking;
            this.depthWrite = depthWrite;
            this.cull = cull;
            this.overlay = overlay;
            this.shader = shader;
            this.multiply = multiply;
            this.additive = additive;
        }

        public VertexFormat format()
        {
            return this.format;
        }

        public PrimitiveTopology mode()
        {
            return this.mode;
        }

        public boolean picking()
        {
            return this.picking;
        }

        public boolean depthWrite()
        {
            return this.depthWrite;
        }

        public boolean cull()
        {
            return this.cull;
        }

        public boolean overlay()
        {
            return this.overlay;
        }

        public String shader()
        {
            return this.shader;
        }

        public boolean multiply()
        {
            return this.multiply;
        }

        public boolean additive()
        {
            return this.additive;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }

            if (!(o instanceof Key other))
            {
                return false;
            }

            return this.picking == other.picking
                && this.depthWrite == other.depthWrite
                && this.cull == other.cull
                && this.overlay == other.overlay
                && this.multiply == other.multiply
                && this.additive == other.additive
                && Objects.equals(this.format, other.format)
                && this.mode == other.mode
                && Objects.equals(this.shader, other.shader);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(this.format, this.mode, this.picking, this.depthWrite, this.cull, this.overlay, this.shader, this.multiply, this.additive);
        }
    }

    private static final Supplier<String> PASS_LABEL = new Supplier<>()
    {
        @Override
        public String get()
        {
            return "BBS model effects";
        }
    };

    private static final Map<Key, RenderPipeline> PIPELINES = new HashMap<>();
    private static final Map<GlProgram, String> PROGRAMS = new WeakHashMap<>();
    private static GlProgram boundEffects;

    public static void bound(GlProgram program)
    {
        boundEffects = PROGRAMS.containsKey(program) ? program : null;
    }

    public static boolean hasBinding()
    {
        return boundEffects != null;
    }

    public static boolean isEffectProgram(GlProgram program)
    {
        return PROGRAMS.containsKey(program);
    }

    public static boolean isPickingProgram(GlProgram program)
    {
        String name = PROGRAMS.get(program);

        return name != null && name.startsWith("picker_");
    }

    private static RenderPipeline pipeline(Key key)
    {
        RenderPipeline existing = PIPELINES.get(key);

        if (existing != null)
        {
            return existing;
        }

        Identifier vertex = Identifier.fromNamespaceAndPath("bbs", "core/" + (key.shader().equals("block_glow_overlay") ? "block_paint_overlay" : key.shader()));
        Identifier fragment = Identifier.fromNamespaceAndPath("bbs", "core/" + key.shader());
        RenderPipeline.Builder builder = RenderPipelineUtils.withModelResources(key.picking())
            .withLocation(Identifier.fromNamespaceAndPath("bbs", "pipeline/model_effect_" + PIPELINES.size()))
            .withVertexShader(vertex).withFragmentShader(fragment)
            .withVertexBinding(0, key.format())
            .withPrimitiveTopology(key.mode());

        BlendFunction blend = null;

        if (!key.picking())
        {
            blend = key.multiply()
                ? new BlendFunction(BlendFactor.DST_COLOR, BlendFactor.ZERO, BlendFactor.ZERO, BlendFactor.ONE)
                : key.additive() || key.shader().equals("block_glow_overlay")
                ? new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE, BlendFactor.ONE, BlendFactor.ZERO)
                : BlendFunction.TRANSLUCENT;
        }

        builder.withColorTargetState(blend != null ? new ColorTargetState(blend) : ColorTargetState.DEFAULT)
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, key.depthWrite(), 0F, key.overlay() ? -4F : 0F))
            .withCull(key.cull());

        RenderPipeline pipeline = RenderPipelines.register(builder.build());

        if (BBSRendering.isIrisLoaded())
        {
            IrisFormPipelines.register(pipeline, key.picking() ? null : (key.depthWrite() ? RenderPipelines.ENTITY_CUTOUT : RenderPipelines.ENTITY_TRANSLUCENT), key.depthWrite());
        }

        PIPELINES.put(key, pipeline);

        return pipeline;
    }

    public static GlProgram program(boolean picking)
    {
        return program(picking ? "picker_models" : "model");
    }

    public static GlProgram program(String name)
    {
        if (BBSRendering.isIrisLoaded())
        {
            return IrisCustomPass.run(new Supplier<GlProgram>()
            {
                @Override
                public GlProgram get()
                {
                    return createProgram(name);
                }
            });
        }

        return createProgram(name);
    }

    private static GlProgram createProgram(String name)
    {
        GlProgram shader = ModelEffectUniforms.register(BBSRendering.getProgram(pipeline(new Key(
            DefaultVertexFormat.ENTITY, PrimitiveTopology.TRIANGLES,
            name.startsWith("picker_"), true, false, false, name, false, false))));

        if (shader != null && shader != GlProgram.INVALID_PROGRAM)
        {
            PROGRAMS.put(shader, name);
        }

        return shader;
    }

    public static boolean drawBound(MeshData buffer)
    {
        return drawBound(buffer, null);
    }

    public static boolean drawBound(MeshData buffer, Identifier layerTexture)
    {
        return drawBound(buffer, layerTexture, true);
    }

    public static boolean drawBound(MeshData buffer, Identifier layerTexture, boolean pretransformed)
    {
        int current = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        GlProgram shader = boundEffects;

        for (GlProgram candidate : PROGRAMS.keySet())
        {
            if (shader == null && candidate.getProgramId() == current)
            {
                shader = candidate;
                break;
            }
        }

        if (shader == null)
        {
            return false;
        }

        int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        GL13.glActiveTexture(active);
        Identifier id = layerTexture != null ? layerTexture : AdoptedTexture.identifier(texture, width, height, false);
        boolean picking = PROGRAMS.get(shader).startsWith("picker_");
        boolean overlay = PROGRAMS.get(shader).endsWith("_overlay") || ModelVAORenderer.isPaintOverlayPass()
            || ModelVAORenderer.isColorTintOverlayPass() || ModelVAORenderer.isColorGradeOverlayPass();
        boolean depthWrite = picking || (!overlay && GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK));
        GlProgram parameters = shader;
        if (pretransformed)
        {
            BBSUniform.setMatrix4f(parameters, "ModelViewMat", new Matrix4f(RenderSystem.getModelViewMatrixCopy()));
        }

        if (BBSRendering.isIrisLoaded())
        {
            IrisCustomPass.run(new Supplier<Void>()
            {
                @Override
                public Void get()
                {
                    drawCustom(buffer, id, parameters, picking, depthWrite, false, overlay);
                    return null;
                }
            });
        }
        else
        {
            drawCustom(buffer, id, parameters, picking, depthWrite, false, overlay);
        }

        return true;
    }

    public static void draw(MeshData buffer, Texture texture, GlProgram parameters, boolean picking, boolean depthWrite, boolean cull, boolean overlay)
    {
        if (BBSRendering.isIrisLoaded())
        {
            IrisCustomPass.run(new Supplier<Void>()
            {
                @Override
                public Void get()
                {
                    drawCustom(buffer, AdoptedTexture.identifier(texture), parameters, picking, depthWrite, cull, overlay);
                    return null;
                }
            });
        }
        else
        {
            drawCustom(buffer, AdoptedTexture.identifier(texture), parameters, picking, depthWrite, cull, overlay);
        }
    }

    private static void drawCustom(MeshData buffer, Identifier id, GlProgram parameters, boolean picking, boolean depthWrite, boolean cull, boolean overlay)
    {
        try (buffer)
        {
            if (id == null)
            {
                return;
            }

            MeshData.DrawState draws = buffer.drawState();
            RenderPipeline pipeline = pipeline(new Key(draws.format(), draws.primitiveTopology(), picking, depthWrite, cull, overlay, PROGRAMS.get(parameters),
                (ModelVAORenderer.isColorTintOverlayPass() || PROGRAMS.get(parameters).endsWith("color_tint_overlay"))
                    && ModelEffectUniforms.value(parameters, "ColorGradeActive") < 0.5F, ModelVAORenderer.isGlowEmissionPass()));
            RenderSetup.RenderSetupBuilder setup = RenderSetup.builder(pipeline).withTexture("Sampler0", id);

            if (!picking)
            {
                Texture scene = ModelVAORenderer.isColorGradeOverlayPass() || ModelEffectUniforms.value(parameters, "ColorGradeActive") > 0.5F
                    ? ModelVAORenderer.getGradeSceneColor() : null;
                Identifier sceneId = scene != null ? AdoptedTexture.identifier(scene) : null;

                if (sceneId == null && ModelEffectUniforms.value(parameters, "TextureBlendActive") > 0.5F)
                {
                    int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
                    GL13.glActiveTexture(GL13.GL_TEXTURE3);
                    int blendTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
                    int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
                    int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
                    GL13.glActiveTexture(active);
                    sceneId = AdoptedTexture.identifier(blendTexture, width, height, false);
                }

                setup.useLightmap().useOverlay().withTexture("Sampler3", sceneId != null ? sceneId : id);
            }

            RenderSetup renderSetup = setup.createRenderSetup();
            List<PreparedRenderType.Texture> textures =
                renderSetup.prepareTextures(
                    Minecraft.getInstance().getTextureManager(),
                    RenderSystem.getSamplerCache(),
                    Minecraft.getInstance().gameRenderer.overlayTexture().getTextureView(),
                    Minecraft.getInstance().gameRenderer.lightmap());
            RenderTarget target = Minecraft.getInstance().gameRenderer.mainRenderTarget();

            try (ImmediateMesh mesh = new ImmediateMesh(buffer);
                 GpuBuffer uniforms = RenderSystem.getDevice().createBuffer(PASS_LABEL, GpuBuffer.USAGE_UNIFORM, ModelEffectUniforms.data(parameters));
                 RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(PASS_LABEL,
                     RenderSystem.outputColorTextureOverride != null ? RenderSystem.outputColorTextureOverride : target.getColorTextureView(), Optional.empty(),
                     RenderSystem.outputDepthTextureOverride != null ? RenderSystem.outputDepthTextureOverride : target.getDepthTextureView(), OptionalDouble.empty()))
            {
                pass.setPipeline(pipeline);
                ScissorState scissor = RenderSystem.getScissorStateForRenderTypeDraws();

                if (scissor.enabled())
                {
                    pass.enableScissor(scissor.x(), scissor.y(), scissor.width(), scissor.height());
                }

                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("BbsModelEffects", uniforms);

                if (textures != null)
                {
                    for (PreparedRenderType.Texture entry : textures)
                    {
                        pass.bindTexture(entry.name(), entry.textureView(), entry.sampler());
                    }
                }

                mesh.draw(pass);
            }
        }
    }
}
