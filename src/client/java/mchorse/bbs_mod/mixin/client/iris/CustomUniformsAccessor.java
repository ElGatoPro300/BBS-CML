package mchorse.bbs_mod.mixin.client.iris;

import java.util.List;

import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CustomUniforms.class)
public interface CustomUniformsAccessor
{
    @Accessor(value = "uniforms", remap = false)
    public List bbs$uniforms();

    @Accessor(value = "uniformOrder", remap = false)
    public List bbs$uniformOrder();
}