package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.MobTextureOverride;

import net.minecraft.client.render.RenderLayers;
import net.minecraft.util.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(RenderLayers.class)
public class RenderLayerTextureOverrideMixin
{
    @ModifyVariable(method = "entityCutoutNoCull(Lnet/minecraft/util/Identifier;Z)Lnet/minecraft/client/render/RenderLayer;", at = @At("HEAD"), argsOnly = true, require = 0)
    private static Identifier bbs$overrideEntityCutoutNoCull(Identifier id)
    {
        return MobTextureOverride.getOverridden(id);
    }

    @ModifyVariable(method = "entityCutout(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;", at = @At("HEAD"), argsOnly = true, require = 0)
    private static Identifier bbs$overrideEntityCutout(Identifier id)
    {
        return MobTextureOverride.getOverridden(id);
    }

    @ModifyVariable(method = "entityTranslucent(Lnet/minecraft/util/Identifier;Z)Lnet/minecraft/client/render/RenderLayer;", at = @At("HEAD"), argsOnly = true, require = 0)
    private static Identifier bbs$overrideEntityTranslucent(Identifier id)
    {
        return MobTextureOverride.getOverridden(id);
    }

    @ModifyVariable(method = "itemEntityTranslucentCull", at = @At("HEAD"), argsOnly = true, require = 0)
    private static Identifier bbs$overrideItemEntityTranslucentCull(Identifier id)
    {
        return MobTextureOverride.getOverridden(id);
    }
}
