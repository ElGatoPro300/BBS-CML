package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.MobTextureOverride;
import net.minecraft.class_1921;
import net.minecraft.class_2960;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(class_1921.class)
public class RenderLayerTextureOverrideMixin
{
    @ModifyVariable(method = "getEntityCutoutNoCull", at = @At("HEAD"), argsOnly = true, require = 0)
    private static class_2960 bbs$overrideEntityCutoutNoCull(class_2960 id)
    {
        return MobTextureOverride.getOverridden(id);
    }

    @ModifyVariable(method = "getEntityCutout", at = @At("HEAD"), argsOnly = true, require = 0)
    private static class_2960 bbs$overrideEntityCutout(class_2960 id)
    {
        return MobTextureOverride.getOverridden(id);
    }

    @ModifyVariable(method = "getEntityTranslucent", at = @At("HEAD"), argsOnly = true, require = 0)
    private static class_2960 bbs$overrideEntityTranslucent(class_2960 id)
    {
        return MobTextureOverride.getOverridden(id);
    }

    @ModifyVariable(method = "getEntityTranslucentCull", at = @At("HEAD"), argsOnly = true, require = 0)
    private static class_2960 bbs$overrideEntityTranslucentCull(class_2960 id)
    {
        return MobTextureOverride.getOverridden(id);
    }

    @ModifyVariable(method = "getItemEntityTranslucentCull", at = @At("HEAD"), argsOnly = true, require = 0)
    private static class_2960 bbs$overrideItemEntityTranslucentCull(class_2960 id)
    {
        return MobTextureOverride.getOverridden(id);
    }

    @ModifyVariable(method = "getOutline", at = @At("HEAD"), argsOnly = true, require = 0)
    private static class_2960 bbs$overrideOutline(class_2960 id)
    {
        return MobTextureOverride.getOverridden(id);
    }
}
