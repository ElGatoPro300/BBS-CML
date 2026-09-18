package mchorse.bbs_mod.mixin.client.iris;

import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.targets.RenderTargets;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(IrisRenderingPipeline.class)
public interface IrisRenderingPipelineAccessor
{
    @Accessor(value = "loadedShaders", remap = false)
    public Set bbs$loadedShaders();

    @Accessor(value = "renderTargets", remap = false)
    public RenderTargets bbs$renderTargets();

    @Invoker(value = "bindDefault", remap = false)
    public void bbs$bindDefault();
}
