package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.clips.misc.ImageOverlay;
import mchorse.bbs_mod.forms.renderers.utils.FormTextureBlendRenderer;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.Quad;
import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;

import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class UIImageRenderer
{
    private static final Quad uvQuad = new Quad();
    private static final Matrix4f matrix = new Matrix4f();

    public static void renderImages(MatrixStack stack, Batcher2D batcher, List<ImageOverlay> images)
    {
        if (images.isEmpty())
        {
            return;
        }

        /* Use the vanilla textured program so subtitle Blur/TextureSize uniforms
         * never leak into image overlays when both share the HUD pass. */
        Supplier<ShaderProgram> supplier = GameRenderer::getPositionTexColorProgram;

        net.minecraft.client.gl.Framebuffer fb = MinecraftClient.getInstance().getFramebuffer();
        int width = fb.textureWidth / 2;
        int height = fb.textureHeight / 2;
        Matrix4f cache = new Matrix4f(RenderSystem.getProjectionMatrix());
        Matrix4f ortho = new Matrix4f().ortho(0, width, height, 0, -100, 100);

        RenderSystem.setProjectionMatrix(ortho, VertexSorter.BY_Z);
        RenderSystem.depthFunc(GL11.GL_ALWAYS);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);

        for (ImageOverlay overlay : images)
        {
            if (overlay.texture == null || overlay.color.a <= 0F || overlay.opacity <= 0F)
            {
                continue;
            }

            float widthPercent = overlay.width / 100F;
            float heightPercent = overlay.height / 100F;
            /* Keep sub-pixel size so width/height keyframes interpolate smoothly
             * instead of stair-stepping on whole pixels (worse over long spans). */
            float fw = widthPercent == 0F ? 0F : width * widthPercent;
            float fh = heightPercent == 0F ? 0F : height * heightPercent;

            if (fw == 0F || fh == 0F)
            {
                continue;
            }

            float x = width * overlay.windowX + overlay.x;
            float y = height * overlay.windowY + overlay.y;

            FormTextureBlendRenderer.draw(overlay.textureBlend, overlay.texture, (link, alphaFactor) ->
            {
                Texture texture = BBSModClient.getTextures().getTexture(link);

                if (texture == null)
                {
                    return;
                }

                float[] uv = computeUV(overlay, texture);
                Color drawColor = overlay.color.copy();

                drawColor.a *= alphaFactor;

                int color = drawColor.getARGBColor();
                float drawX = -fw * overlay.anchorX;
                float drawY = -fh * overlay.anchorY;

                stack.push();
                stack.translate(x, y, 0);

                texture.setFilterMipmap(overlay.linear, overlay.mipmap);
                batcher.texturedBox(supplier, texture.id, color, drawX, drawY, fw, fh, uv[0], uv[1], uv[2], uv[3], texture.width, texture.height);
                texture.setFilterMipmap(false, false);

                stack.pop();
            });
        }

        RenderSystem.setProjectionMatrix(cache, VertexSorter.BY_Z);
        RenderSystem.enableCull();
    }

    public static void renderImage(MatrixStack stack, Batcher2D batcher, ImageOverlay overlay)
    {
        if (overlay == null)
        {
            return;
        }

        renderImages(stack, batcher, Collections.singletonList(overlay));
    }

    private static float[] computeUV(ImageOverlay overlay, Texture texture)
    {
        float w = texture.width;
        float h = texture.height;
        float ow = w;
        float oh = h;
        Vector4f crop = overlay.crop;
        float uvTLx = crop.x / w;
        float uvTLy = crop.y / h;
        float uvBRx = 1F - crop.z / w;
        float uvBRy = 1F - crop.w / h;

        uvQuad.p1.set(uvTLx, uvTLy, 0);
        uvQuad.p2.set(uvBRx, uvTLy, 0);
        uvQuad.p3.set(uvTLx, uvBRy, 0);
        uvQuad.p4.set(uvBRx, uvBRy, 0);

        if (overlay.resizeCrop)
        {
            uvTLx = 0F;
            uvTLy = 0F;
            uvBRx = 1F;
            uvBRy = 1F;

            uvQuad.p1.set(uvTLx, uvTLy, 0);
            uvQuad.p2.set(uvBRx, uvTLy, 0);
            uvQuad.p3.set(uvTLx, uvBRy, 0);
            uvQuad.p4.set(uvBRx, uvBRy, 0);
        }

        if (overlay.offsetX != 0F || overlay.offsetY != 0F || overlay.rotation != 0F)
        {
            float centerX = (crop.x + (ow - crop.z)) / 2F / ow;
            float centerY = (crop.y + (oh - crop.w)) / 2F / oh;

            matrix.identity()
                .translate(centerX, centerY, 0)
                .rotateZ(MathUtils.toRad(overlay.rotation))
                .translate(overlay.offsetX / ow, overlay.offsetY / oh, 0)
                .translate(-centerX, -centerY, 0);

            uvQuad.transform(matrix);
        }

        float u1 = Math.min(Math.min(uvQuad.p1.x, uvQuad.p2.x), Math.min(uvQuad.p3.x, uvQuad.p4.x)) * w;
        float v1 = Math.min(Math.min(uvQuad.p1.y, uvQuad.p2.y), Math.min(uvQuad.p3.y, uvQuad.p4.y)) * h;
        float u2 = Math.max(Math.max(uvQuad.p1.x, uvQuad.p2.x), Math.max(uvQuad.p3.x, uvQuad.p4.x)) * w;
        float v2 = Math.max(Math.max(uvQuad.p1.y, uvQuad.p2.y), Math.max(uvQuad.p3.y, uvQuad.p4.y)) * h;

        return new float[] {u1, v1, u2, v2};
    }
}
