package mchorse.bbs_mod.mixin.client;

import net.minecraft.client.render.WorldRenderer;

import org.spongepowered.asm.mixin.Mixin;

/* BackgroundRenderer was removed in 1.21.11 Yarn */
@Mixin(WorldRenderer.class)
public class BackgroundRendererMixin
{
}
