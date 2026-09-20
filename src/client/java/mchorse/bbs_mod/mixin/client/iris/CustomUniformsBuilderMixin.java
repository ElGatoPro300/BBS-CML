package mchorse.bbs_mod.mixin.client.iris;

import mchorse.bbs_mod.utils.iris.FormColorGradePatch;
import mchorse.bbs_mod.utils.iris.FormFluidShaderPatch;
import mchorse.bbs_mod.utils.iris.FormGlowBloomPatch;
import mchorse.bbs_mod.utils.iris.ShaderCurves;

import net.irisshaders.iris.uniforms.custom.CustomUniforms;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CustomUniforms.Builder.class)
public class CustomUniformsBuilderMixin
{
    @Inject(method = "build", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    public void onBuild(@Coerce Object inputHolder, CallbackInfoReturnable<CustomUniforms> info)
    {
        if (info.getReturnValue() instanceof CustomUniformsAccessor accessor)
        {
            ShaderCurves.addUniforms(accessor.bbs$uniformOrder());
            FormColorGradePatch.addUniforms(accessor.bbs$uniformOrder());
            FormGlowBloomPatch.addUniforms(accessor.bbs$uniformOrder());
            FormFluidShaderPatch.addUniforms(accessor.bbs$uniformOrder());
        }
    }
}
