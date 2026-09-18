package mchorse.bbs_mod.graphics;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.utils.iris.IrisFormPipelines;

import net.minecraft.resources.Identifier;

import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.vertex.VertexFormat;

import java.util.IdentityHashMap;
import java.util.Map;

public final class RenderPipelineUtils
{
    private static final Map<RenderPipeline, RenderPipeline> DEPTH_VARIANTS = new IdentityHashMap<>();

    public static boolean isReverseDepth()
    {
        /* Vanilla 26.2 uses reverse Z; BBS preview projections still use forward Z.
         * Both perspective and orthographic projections encode the direction in m22. */
        return BBSRendering.getProjectionMatrix().m22() >= 0F;
    }

    public static double depthClearValue()
    {
        return isReverseDepth() ? 0D : 1D;
    }

    public static RenderPipeline withCurrentDepth(RenderPipeline source)
    {
        DepthStencilState depth = source.getDepthStencilState();

        if (depth == null)
        {
            return source;
        }

        CompareOp test = depth.depthTest();
        boolean reverse = isReverseDepth();
        CompareOp replacement = test;

        if (reverse && test == CompareOp.LESS_THAN_OR_EQUAL)
        {
            replacement = CompareOp.GREATER_THAN_OR_EQUAL;
        }
        else if (!reverse && test == CompareOp.GREATER_THAN_OR_EQUAL)
        {
            replacement = CompareOp.LESS_THAN_OR_EQUAL;
        }

        if (test == replacement)
        {
            return source;
        }

        RenderPipeline variant = DEPTH_VARIANTS.get(source);

        if (variant == null)
        {
            variant = new DepthVariant(source, new DepthStencilState(replacement, depth.writeDepth(),
                -depth.depthBiasScaleFactor(), -depth.depthBiasConstant()));
            DEPTH_VARIANTS.put(source, variant);

            if (BBSRendering.isIrisLoaded())
            {
                IrisFormPipelines.register(variant, source);
            }
        }

        return variant;
    }

    private static final class DepthVariant extends RenderPipeline
    {
        private DepthVariant(RenderPipeline source, DepthStencilState depth)
        {
            super(Identifier.fromNamespaceAndPath("bbs", "depth_variant/" + source.getLocation().getNamespace() + "/" + source.getLocation().getPath()),
                source.getShaders(), source.getShaderDefines(),
                source.getBindGroupLayouts(), source.getColorTargetStates().toArray(new ColorTargetState[0]), depth, source.getPolygonMode(),
                source.isCull(), source.getVertexFormatBindings().toArray(new VertexFormat[0]), source.getPrimitiveTopology(),
                source.pushConstantSize(), source.getSortKey());
        }
    }

    /**
     * Declares the same resource bind group layouts as the source pipeline without inheriting
     * its geometry, shader defines, blending or depth policy.
     */
    public static RenderPipeline.Builder withUniforms(RenderPipeline source)
    {
        RenderPipeline.Builder builder = RenderPipeline.builder();

        for (BindGroupLayout layout : source.getBindGroupLayouts())
        {
            builder.withBindGroupLayout(layout);
        }

        return builder;
    }

    private RenderPipelineUtils()
    {}
}
