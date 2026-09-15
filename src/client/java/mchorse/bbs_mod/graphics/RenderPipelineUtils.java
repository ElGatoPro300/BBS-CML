package mchorse.bbs_mod.graphics;

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

        for (RenderPipeline.UniformDescription uniform : source.getUniforms())
        {
            if (uniform.textureFormat() == null)
            {
                builder.withUniform(uniform.name(), uniform.type());
            }
            else
            {
                builder.withUniform(uniform.name(), uniform.type(), uniform.textureFormat());
            }
        }

        return builder;
    }

    private RenderPipelineUtils()
    {}
}
