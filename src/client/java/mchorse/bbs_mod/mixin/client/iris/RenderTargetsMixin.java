package mchorse.bbs_mod.mixin.client.iris;

import net.irisshaders.iris.gl.texture.DepthBufferFormat;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.targets.RenderTargets;

import com.mojang.blaze3d.textures.GpuTexture;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RenderTargets.class, remap = false)
public class RenderTargetsMixin
{
    @Shadow
    private int cachedDepthBufferVersion;
    @Shadow
    private GpuTexture currentDepthTexture;

    @Inject(method = "resizeIfNeeded", at = @At("HEAD"))
    private void bbs$detectDepthTargetChange(int depthBufferVersion, GpuTexture depthTexture, int width, int height,
        DepthBufferFormat depthFormat, PackDirectives directives, CallbackInfoReturnable<Boolean> info)
    {
        /* Iris versions are local to each RenderTarget. Switching between the window
         * and film can preserve the version while changing the depth attachment.
         * Let Iris refresh its source texture and framebuffer attachments normally. */
        if (this.currentDepthTexture != depthTexture && this.cachedDepthBufferVersion == depthBufferVersion)
        {
            this.cachedDepthBufferVersion = depthBufferVersion - 1;
        }
    }
}
