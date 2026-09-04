package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.utils.iris.ShaderOpacityPatch;

import com.mojang.blaze3d.systems.RenderSystem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * While soft forms are pending, keep translucent terrain from writing depth so soft flushed
 * later is not discarded. Does not change soft flush order (clouds / soft-vs-soft).
 */
@Mixin(RenderSystem.class)
public class RenderSystemDepthMaskMixin
{
    @ModifyVariable(method = "depthMask", at = @At("HEAD"), argsOnly = true)
    private static boolean bbs$filterTranslucentTerrainDepthMask(boolean mask)
    {
        return ShaderOpacityPatch.filterDepthMask(mask);
    }
}
