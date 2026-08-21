package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.clips.misc.ImageOverlay;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.forms.renderers.utils.FormTextureBlendRenderer;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.Quad;
import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import com.mojang.blaze3d.opengl.GlStateManager;

import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

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

        net.minecraft.client.gl.Framebuffer fb = MinecraftClient.getInstance().getFramebuffer();
        int width = fb.textureWidth / 2;
        int height = fb.textureHeight / 2;
        float zExtent = Math.max(1000F, Math.max(width, height) * 8F);
        Matrix4f ortho = new Matrix4f().ortho(0, width, height, 0, -zExtent, zExtent);

        GlStateManager._depthFunc(GL11.GL_ALWAYS);
        GlStateManager._disableCull();
        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        for (ImageOverlay overlay : images)
        {
            if (overlay.texture == null || overlay.color.a <= 0F || overlay.opacity <= 0F)
            {
                continue;
            }

            float widthPercent = overlay.width / 100F;
            float heightPercent = overlay.height / 100F;
            int fw = widthPercent == 0F ? 0 : Math.max(1, Math.round(width * Math.abs(widthPercent))) * (widthPercent < 0F ? -1 : 1);
            int fh = heightPercent == 0F ? 0 : Math.max(1, Math.round(height * Math.abs(heightPercent))) * (heightPercent < 0F ? -1 : 1);

            if (fw == 0 || fh == 0)
            {
                continue;
            }

            int x = (int) (width * overlay.windowX + overlay.x);
            int y = (int) (height * overlay.windowY + overlay.y);

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

                /* Rotate around the image anchor in XYZ. Legacy "rotation" is Z
                 * (in-plane); rotationX/Y are additive and default to 0 for old films. */
                if (overlay.rotationX != 0F)
                {
                    stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(overlay.rotationX));
                }

                if (overlay.rotationY != 0F)
                {
                    stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(overlay.rotationY));
                }

                if (overlay.rotation != 0F)
                {
                    stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(overlay.rotation));
                }

                texture.setFilterMipmap(overlay.linear, overlay.mipmap);
                if (overlay.blendMode != 0)
                {
                    batcher.flushDraw();
                    switch (overlay.blendMode)
                    {
                        case 1: /* Multiply */
                            GlStateManager._blendFuncSeparate(GL11.GL_DST_COLOR, GL11.GL_ZERO, 1, 0);
                            break;
                        case 2: /* Screen */
                            GlStateManager._blendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_COLOR, 1, 0);
                            break;
                        case 3: /* Add */
                            GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, 1, 0);
                            break;
                        case 4: /* Saturation */
                            GlStateManager._blendFuncSeparate(GL11.GL_SRC_COLOR, GL11.GL_ONE_MINUS_SRC_COLOR, 1, 0);
                            break;
                        case 5: /* Incrustation */
                            GlStateManager._blendFuncSeparate(GL11.GL_ZERO, GL11.GL_ONE_MINUS_SRC_COLOR, 1, 0);
                            break;
                        case 6: /* Exclusion */
                            GlStateManager._blendFuncSeparate(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ONE_MINUS_SRC_COLOR, 1, 0);
                            break;
                        case 7: /* Overlay */
                            GlStateManager._blendFuncSeparate(GL11.GL_DST_COLOR, GL11.GL_SRC_COLOR, 1, 0);
                            break;
                        case 8: /* Color Dodge */
                            GlStateManager._blendFuncSeparate(GL11.GL_SRC_COLOR, GL11.GL_ONE, 1, 0);
                            break;
                    }
                }
                batcher.texturedBox(texture.id, color, drawX, drawY, fw, fh, uv[0], uv[1], uv[2], uv[3], texture.width, texture.height);
                if (overlay.blendMode != 0)
                {
                    batcher.flushDraw();
                    GlStateManager._blendFuncSeparate(770, 771, 1, 0);
                }
                texture.setFilterMipmap(false, false);

                stack.pop();
            });
        }

        GlStateManager._enableCull();
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

        /* UV shift only — image rotation is applied in screen space above. */
        if (overlay.offsetX != 0F || overlay.offsetY != 0F)
        {
            matrix.identity()
                .translate(overlay.offsetX / ow, overlay.offsetY / oh, 0);

            uvQuad.transform(matrix);
        }

        float u1 = Math.min(Math.min(uvQuad.p1.x, uvQuad.p2.x), Math.min(uvQuad.p3.x, uvQuad.p4.x)) * w;
        float v1 = Math.min(Math.min(uvQuad.p1.y, uvQuad.p2.y), Math.min(uvQuad.p3.y, uvQuad.p4.y)) * h;
        float u2 = Math.max(Math.max(uvQuad.p1.x, uvQuad.p2.x), Math.max(uvQuad.p3.x, uvQuad.p4.x)) * w;
        float v2 = Math.max(Math.max(uvQuad.p1.y, uvQuad.p2.y), Math.max(uvQuad.p3.y, uvQuad.p4.y)) * h;

        return new float[] {u1, v1, u2, v2};
    }
}
