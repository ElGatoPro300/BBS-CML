package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.render.BufferRenderer;
import mchorse.bbs_mod.graphics.RenderPipelineUtils;
import mchorse.bbs_mod.graphics.texture.AdoptedTexture;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.utils.iris.IrisFormPipelines;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;

import org.lwjgl.opengl.GL11;

public class ParticleRenderLayers
{
    private static final int TYPE_LIT = 0;
    private static final int TYPE_SHADED = 1;
    private static final int TYPE_UI = 2;
    private static final int TYPE_GLOW = 3;

    private static final RenderPipeline[] PIPELINES = new RenderPipeline[8];

    private static RenderPipeline pipeline(int type, boolean depthWrite)
    {
        int index = (type << 1) | (depthWrite ? 1 : 0);

        if (PIPELINES[index] == null)
        {
            RenderPipeline.Builder builder;

            if (type == TYPE_LIT)
            {
                RenderPipeline source = RenderPipelines.TRANSLUCENT_PARTICLE;

                builder = RenderPipelineUtils.withUniforms(source)
                    .withLocation(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "pipeline/particle_lit" + (depthWrite ? "" : "_no_depth")))
                    .withVertexShader(source.getVertexShader())
                    .withFragmentShader(source.getFragmentShader())
                    .withVertexBinding(0, DefaultVertexFormat.PARTICLE)
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, depthWrite))
                    .withCull(false);
            }
            else if (type == TYPE_SHADED)
            {
                RenderPipeline source = RenderPipelines.ENTITY_TRANSLUCENT;

                builder = RenderPipelineUtils.withUniforms(source)
                    .withLocation(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "pipeline/particle_shaded" + (depthWrite ? "" : "_no_depth")))
                    .withVertexShader(source.getVertexShader())
                    .withFragmentShader(source.getFragmentShader())
                    .withVertexBinding(0, DefaultVertexFormat.ENTITY)
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                    .withShaderDefine("PER_FACE_LIGHTING")
                    .withShaderDefine("ALPHA_CUTOUT", 0.001F)
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, depthWrite))
                    .withCull(false);
            }
            else if (type == TYPE_GLOW)
            {
                RenderPipeline source = RenderPipelines.GUI_TEXTURED;

                builder = RenderPipelineUtils.withUniforms(source)
                    .withLocation(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "pipeline/particle_glow"))
                    .withVertexShader(source.getVertexShader())
                    .withFragmentShader(source.getFragmentShader())
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                    .withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE, BlendFactor.ONE, BlendFactor.ZERO)))
                    .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                    .withCull(false);
            }
            else
            {
                RenderPipeline source = RenderPipelines.GUI_TEXTURED;

                builder = RenderPipelineUtils.withUniforms(source)
                    .withLocation(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "pipeline/particle_ui"))
                    .withVertexShader(source.getVertexShader())
                    .withFragmentShader(source.getFragmentShader())
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                    .withCull(false);
            }

            PIPELINES[index] = builder.build();

            if (BBSRendering.isIrisLoaded())
            {
                IrisFormPipelines.register(PIPELINES[index], type == TYPE_SHADED ? RenderPipelines.ENTITY_TRANSLUCENT
                    : type == TYPE_LIT ? RenderPipelines.TRANSLUCENT_PARTICLE : null);
            }
        }

        return PIPELINES[index];
    }

    public static void draw(MeshData buffer, Texture texture)
    {
        draw(buffer, texture, false, true);
    }

    public static void drawGlow(MeshData buffer, Texture texture)
    {
        draw(buffer, texture, true, false);
    }

    public static void draw(MeshData buffer, Texture texture, boolean glow)
    {
        draw(buffer, texture, glow, !glow);
    }

    public static void draw(MeshData buffer, Texture texture, boolean glow, boolean depthWrite)
    {
        if (buffer == null)
        {
            return;
        }

        if (buffer.drawState() == null || buffer.drawState().vertexCount() == 0)
        {
            buffer.close();

            return;
        }

        Identifier id = AdoptedTexture.identifier(texture);

        if (id == null)
        {
            buffer.close();

            return;
        }

        VertexFormat format = buffer.drawState().format();
        int type;

        if (glow)
        {
            type = TYPE_GLOW;
        }
        else if (format == DefaultVertexFormat.ENTITY
            || (BBSRendering.isIrisLoaded() && IrisFormPipelines.isEntityFormat(format)))
        {
            type = TYPE_SHADED;
        }
        else if (format == DefaultVertexFormat.PARTICLE)
        {
            type = TYPE_LIT;
        }
        else
        {
            type = TYPE_UI;
        }

        boolean linear = texture != null && texture.getFilter() == GL11.GL_LINEAR;
        boolean mipmap = texture != null && texture.isReallyMipmap();
        FilterMode filter = linear ? FilterMode.LINEAR : FilterMode.NEAREST;

        RenderSetup.RenderSetupBuilder setup = RenderSetup.builder(pipeline(type, depthWrite))
            .withTexture("Sampler0", id, () -> RenderSystem.getSamplerCache().getSampler(
                AddressMode.REPEAT, AddressMode.REPEAT, filter, filter, mipmap));

        if (type == TYPE_LIT)
        {
            setup.useLightmap().sortOnUpload();
        }
        else if (type == TYPE_SHADED)
        {
            setup.useLightmap().useOverlay().sortOnUpload();
        }

        /* RenderLayer owns the buffer and binds texture views, samplers and uniform buffers.
         * A raw glBindTexture/glUseProgram does not configure a vanilla render pass. */
        try
        {
            BufferRenderer.draw(RenderType.create("bbs_particle", setup.createRenderSetup()), buffer);
        }
        finally
        {
            if (texture != null)
            {
                texture.bind(0);
            }
        }
    }
}
