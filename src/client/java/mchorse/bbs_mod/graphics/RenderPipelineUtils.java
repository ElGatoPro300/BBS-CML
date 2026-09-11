package mchorse.bbs_mod.graphics;

import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.RenderPipeline;

public final class RenderPipelineUtils
{
    /**
     * Declares the same uniform resources as the source shader without inheriting
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
