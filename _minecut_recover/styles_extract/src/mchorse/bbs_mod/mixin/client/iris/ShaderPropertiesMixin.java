package mchorse.bbs_mod.mixin.client.iris;

import mchorse.bbs_mod.utils.iris.ShaderOpacityPatch;

import net.irisshaders.iris.helpers.OptionalBoolean;
import net.irisshaders.iris.shaderpack.option.ShaderPackOptions;
import net.irisshaders.iris.shaderpack.properties.ShaderProperties;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShaderProperties.class)
public class ShaderPropertiesMixin
{
    @Shadow(remap = false)
    private OptionalBoolean separateEntityDraws;

    @ModifyVariable(
        method = "<init>(Ljava/lang/String;Lnet/irisshaders/iris/shaderpack/option/ShaderPackOptions;Ljava/lang/Iterable;)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0,
        remap = false,
        require = 0
    )
    private static String bbsPatchPropertiesContents(String contents)
    {
        return ShaderOpacityPatch.patchPropertiesContents(contents);
    }

    @Inject(
        method = "<init>(Ljava/lang/String;Lnet/irisshaders/iris/shaderpack/option/ShaderPackOptions;Ljava/lang/Iterable;)V",
        at = @At("RETURN"),
        remap = false,
        require = 0
    )
    private void bbsApplyComplementaryOpacityPatch(String contents, ShaderPackOptions options, Iterable environmentDefines, CallbackInfo ci)
    {
        ShaderOpacityPatch.applyAlphaTestOverrides((ShaderProperties) (Object) this);
        ShaderOpacityPatch.applySeparateEntityDraws((value) -> this.separateEntityDraws = value);
    }
}
