package mchorse.bbs_mod.graphics;

import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;

public final class RenderPipelineUtils
{
    public static RenderPipeline.Builder withModelResources(boolean picking)
    {
        BindGroupLayout.Builder resources = BindGroupLayout.builder()
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("BbsModelEffects", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0");

        if (!picking)
        {
            resources.withUniform("Fog", UniformType.UNIFORM_BUFFER)
                .withUniform("Lighting", UniformType.UNIFORM_BUFFER)
                .withSampler("Sampler1").withSampler("Sampler2").withSampler("Sampler3");
        }

        return RenderPipeline.builder().withBindGroupLayout(resources.build());
    }

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
