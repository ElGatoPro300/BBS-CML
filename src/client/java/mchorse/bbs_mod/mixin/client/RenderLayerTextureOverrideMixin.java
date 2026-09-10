package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.MobTextureOverride;

import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(RenderTypes.class)
public class RenderLayerTextureOverrideMixin
{
    @ModifyVariable(method = {
        "entityCutoutNoCull(Lnet/minecraft/resources/Identifier;Z)Lnet/minecraft/client/renderer/rendertype/RenderType;",
        "entityCutout(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;",
        "entityTranslucent(Lnet/minecraft/resources/Identifier;Z)Lnet/minecraft/client/renderer/rendertype/RenderType;",
        "itemEntityTranslucentCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;",
        "outline(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"
    }, at = @At("HEAD"), argsOnly = true, require = 5)
    private static Identifier bbs$overrideTexture(Identifier id)
    {
        return MobTextureOverride.getOverridden(id);
    }
}
