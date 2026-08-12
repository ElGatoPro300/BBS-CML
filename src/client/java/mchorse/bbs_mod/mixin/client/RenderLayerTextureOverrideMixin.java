package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.MobTextureOverride;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

import org.spongepowered.asm.mixin.Mixin;

/* TODO(1.21.11 render): RenderLayer entity factory methods (getEntityCutoutNoCull,
 * getEntityCutout, getEntityTranslucent, etc.) were removed in the render pipeline
 * rewrite. The texture override mechanism needs to be re-implemented using the
 * new RenderSetup/TextureSpec system. This mixin is currently a no-op stub. */
@Mixin(RenderLayer.class)
public class RenderLayerTextureOverrideMixin
{
}
