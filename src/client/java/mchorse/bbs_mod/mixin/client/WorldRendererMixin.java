package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.SunPathRotation;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.iris.ShaderOpacityPatch;

import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.Vec3d;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin
{
    @Shadow
    public Framebuffer entityOutlinesFramebuffer;

    @Unique
    private boolean bbs$suppressingTranslucentDepth;

    @Inject(method = "renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V", at = @At("HEAD"), cancellable = true, require = 0)
    public void onRenderSky(Matrix4f modelView, Matrix4f projectionMatrix, float tickDelta, Camera camera, boolean thickFog, Runnable fogCallback, CallbackInfo info)
    {
        if (BBSRendering.isChromaSkyEnabled())
        {
            Color color = Color.rgb(BBSRendering.getChromaSkyColor());

            GL11.glClearColor(color.r, color.g, color.b, 1F);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            RenderSystem.setShaderFogColor(color.r, color.g, color.b, 1F);

            info.cancel();

            return;
        }

        SunPathRotation.begin(modelView);
    }

    @Inject(method = "renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V", at = @At("RETURN"), require = 0)
    public void onRenderSkyReturn(Matrix4f modelView, Matrix4f projectionMatrix, float tickDelta, Camera camera, boolean thickFog, Runnable fogCallback, CallbackInfo info)
    {
        SunPathRotation.end(modelView);
    }

    @Inject(method = "renderLayer", at = @At("HEAD"), cancellable = true)
    public void onRenderLayer(RenderLayer renderLayer, double cameraX, double cameraY, double cameraZ, Matrix4f positionMatrix, Matrix4f projectionMatrix, CallbackInfo info)
    {
        /* Only translucent terrain: no soft flush / peel — keep soft after clouds for
         * soft-vs-soft and cloud order. Glass color stays; depth writes are suppressed so
         * soft drawn later is not discarded. */
        if (this.bbs$isWorldTranslucentTerrainLayer(renderLayer)
            && ShaderOpacityPatch.hasPendingSoftAfterFluids())
        {
            this.bbs$suppressingTranslucentDepth = true;
            ShaderOpacityPatch.beginTranslucentTerrainDepthSuppress();
        }

        if (BBSRendering.shouldHideChromaTerrain())
        {
            BBSRendering.onRenderChunkLayer(positionMatrix, projectionMatrix);

            info.cancel();
        }
    }

    @Inject(method = "renderLayer", at = @At("TAIL"))
    public void onRenderChunkLayer(RenderLayer layer, double cameraX, double cameraY, double cameraZ, Matrix4f positionMatrix, Matrix4f projectionMatrix, CallbackInfo info)
    {
        if (this.bbs$suppressingTranslucentDepth)
        {
            this.bbs$suppressingTranslucentDepth = false;

            /* Iris: keep suppress until AFTER_TRANSLUCENT (may span multiple layers).
             * Vanilla: end per layer; onAfterTranslucentTerrain also clears as safety. */
            if (!BBSRendering.isIrisShadersEnabled())
            {
                ShaderOpacityPatch.endTranslucentTerrainDepthSuppress();
            }
        }

        if (layer == RenderLayer.getSolid())
        {
            BBSRendering.onRenderChunkLayer(positionMatrix, projectionMatrix);
        }
    }

    @Unique
    private boolean bbs$isWorldTranslucentTerrainLayer(RenderLayer layer)
    {
        return layer == RenderLayer.getTranslucent()
            || layer == RenderLayer.getTranslucentMovingBlock();
    }

    @Inject(method = "setupFrustum", at = @At("HEAD"))
    public void onSetupFrustum(Vec3d vec3d, Matrix4f matrix4f, Matrix4f positionMatrix, CallbackInfo info)
    {
        BBSRendering.camera.set(matrix4f);
    }

    @Inject(at = @At("RETURN"), method = "loadEntityOutlinePostProcessor")
    private void onLoadEntityOutlineShader(CallbackInfo info)
    {
        BBSRendering.resizeExtraFramebuffers();
    }

    @Inject(at = @At("RETURN"), method = "onResized")
    private void onResized(CallbackInfo info)
    {
        if (this.entityOutlinesFramebuffer == null)
        {
            return;
        }

        BBSRendering.resizeExtraFramebuffers();
    }
}
