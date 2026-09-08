package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.graphics.texture.AdoptedTexture;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.utils.iris.IrisFormPipelines;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;

public class BillboardRenderLayers
{
    private static final RenderPipeline[] PIPELINES = new RenderPipeline[32];

    private static RenderPipeline pipeline(boolean shaded, boolean depthWrite, boolean cull, boolean quads, boolean glow)
    {
        int index = (shaded ? 1 : 0) | (depthWrite ? 2 : 0) | (cull ? 4 : 0) | (quads ? 8 : 0) | (glow ? 16 : 0);

        if (PIPELINES[index] == null)
        {
            RenderPipeline source = shaded
                ? (depthWrite ? RenderPipelines.ENTITY_CUTOUT : RenderPipelines.ENTITY_TRANSLUCENT)
                : RenderPipelines.GUI_TEXTURED;
            BlendFunction blend = glow
                ? new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ONE, DestFactor.ZERO)
                : BlendFunction.TRANSLUCENT;

            RenderPipeline.Builder builder = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "pipeline/billboard_" + index))
                .withVertexShader(source.getVertexShader())
                .withFragmentShader(source.getFragmentShader())
                .withVertexFormat(shaded ? DefaultVertexFormat.ENTITY : DefaultVertexFormat.POSITION_TEX_COLOR,
                    quads ? VertexFormat.Mode.QUADS : VertexFormat.Mode.TRIANGLES)
                .withSampler("Sampler0")
                .withColorTargetState(new ColorTargetState(blend))
                .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, depthWrite))
                .withCull(cull);

            if (shaded)
            {
                builder.withSampler("Sampler1").withSampler("Sampler2")
                    .withShaderDefine("PER_FACE_LIGHTING")
                    .withShaderDefine("ALPHA_CUTOUT", 0.001F);
            }

            PIPELINES[index] = RenderPipelines.register(builder.build());

            if (BBSRendering.isIrisLoaded())
            {
                IrisFormPipelines.register(PIPELINES[index], shaded ? source : null, depthWrite);
            }
        }

        return PIPELINES[index];
    }

    public static void draw(MeshData buffer, Texture texture, boolean linear, boolean mipmap, boolean depthWrite, boolean cull)
    {
        draw(buffer, texture, linear, mipmap, depthWrite, cull, false);
    }

    public static void draw(MeshData buffer, Texture texture, boolean linear, boolean mipmap, boolean depthWrite, boolean cull, boolean glow)
    {
        try
        {
            draw(buffer, AdoptedTexture.identifier(texture), linear, mipmap, depthWrite, cull, glow);
        }
        finally
        {
            texture.bind(0);
        }
    }

    public static void draw(MeshData buffer, Identifier id, boolean linear, boolean mipmap, boolean depthWrite, boolean cull, boolean glow)
    {
        if (id == null)
        {
            buffer.close();

            return;
        }

        VertexFormat format = buffer.drawState().format();
        boolean shaded = format == DefaultVertexFormat.ENTITY
            || (BBSRendering.isIrisLoaded() && IrisFormPipelines.isEntityFormat(format));
        boolean quads = buffer.drawState().mode() == VertexFormat.Mode.QUADS;
        FilterMode filter = linear ? FilterMode.LINEAR : FilterMode.NEAREST;
        RenderSetup.RenderSetupBuilder setup = RenderSetup.builder(pipeline(shaded, depthWrite, cull, quads, glow))
            .withTexture("Sampler0", id, () -> RenderSystem.getSamplerCache().getSampler(
                AddressMode.REPEAT, AddressMode.REPEAT, filter, filter, mipmap));

        if (shaded)
        {
            setup.useLightmap().useOverlay();
        }

        RenderType.create("bbs_billboard", setup.createRenderSetup()).draw(buffer);
    }
}
