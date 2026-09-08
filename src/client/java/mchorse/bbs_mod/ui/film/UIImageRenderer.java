package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.clips.misc.ImageOverlay;
import mchorse.bbs_mod.forms.renderers.utils.FormTextureBlendRenderer;
import mchorse.bbs_mod.graphics.texture.AdoptedTexture;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.Quad;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.vertex.VertexFormat;

import java.util.Collections;
import java.util.List;

public class UIImageRenderer
{
    private static final Quad uvQuad = new Quad();
    private static final Matrix4f matrix = new Matrix4f();
    private static final RenderPipeline[] PIPELINES = new RenderPipeline[9];

    private static RenderPipeline getPipeline(int blendMode)
    {
        int mode = Math.max(0, Math.min(8, blendMode));

        if (PIPELINES[mode] == null)
        {
            BlendFunction blend;

            switch (mode)
            {
                case 1: /* Multiply: dst * src */
                    blend = new BlendFunction(SourceFactor.DST_COLOR, DestFactor.ZERO, SourceFactor.ZERO, DestFactor.ONE);
                    break;
                case 2: /* Screen: 1 - (1-src)*(1-dst) */
                    blend = new BlendFunction(SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_COLOR, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
                    break;
                case 3: /* Add / Linear Dodge: src + dst */
                    blend = new BlendFunction(SourceFactor.ONE, DestFactor.ONE, SourceFactor.ONE, DestFactor.ONE);
                    break;
                case 4: /* Saturation */
                    blend = new BlendFunction(SourceFactor.SRC_COLOR, DestFactor.ONE_MINUS_SRC_COLOR, SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
                    break;
                case 5: /* Incrustation (Silhouette Luma) */
                    blend = new BlendFunction(SourceFactor.ZERO, DestFactor.ONE_MINUS_SRC_COLOR, SourceFactor.ZERO, DestFactor.ONE_MINUS_SRC_ALPHA);
                    break;
                case 6: /* Exclusion */
                    blend = new BlendFunction(SourceFactor.ONE_MINUS_DST_COLOR, DestFactor.ONE_MINUS_SRC_COLOR, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
                    break;
                case 7: /* Overlay */
                    blend = new BlendFunction(SourceFactor.DST_COLOR, DestFactor.SRC_COLOR, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
                    break;
                case 8: /* Color Dodge */
                    blend = new BlendFunction(SourceFactor.SRC_COLOR, DestFactor.ONE, SourceFactor.SRC_ALPHA, DestFactor.ONE);
                    break;
                default:
                    blend = BlendFunction.TRANSLUCENT;
                    break;
            }

            RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelines.POSITION_TEX_COLOR_SNIPPET)
                .withLocation(Identifier.of(BBSMod.MOD_ID, "pipeline/image_overlay_" + mode))
                .withBlend(blend)
                .withCull(false);

            PIPELINES[mode] = RenderPipelines.register(builder.build());
        }

        return PIPELINES[mode];
    }

    public static void renderImages(Batcher2D batcher, List<ImageOverlay> images, int width, int height)
    {
        if (images == null || images.isEmpty())
        {
            return;
        }

        Matrix3x2fStack matrices = batcher.getContext().getMatrices();

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

                if (Colors.getA(color) <= 0F)
                {
                    color = Colors.opaque(color);
                }

                float drawX = -fw * overlay.anchorX;
                float drawY = -fh * overlay.anchorY;

                matrices.pushMatrix();
                matrices.translate(x, y);

                if (overlay.rotation != 0F)
                {
                    matrices.rotate((float) Math.toRadians(overlay.rotation));
                }

                if (overlay.rotationX != 0F)
                {
                    matrices.scale(1F, (float) Math.cos(Math.toRadians(overlay.rotationX)));
                }

                if (overlay.rotationY != 0F)
                {
                    matrices.scale((float) Math.cos(Math.toRadians(overlay.rotationY)), 1F);
                }

                texture.setFilterMipmap(overlay.linear, overlay.mipmap);

                RenderPipeline pipeline = getPipeline(overlay.blendMode);
                Identifier id = AdoptedTexture.identifier(texture);

                if (id != null)
                {
                    batcher.getContext().drawTexture(
                        pipeline,
                        id,
                        (int) drawX,
                        (int) drawY,
                        uv[0],
                        uv[1],
                        (int) fw,
                        (int) fh,
                        (int) (uv[2] - uv[0]),
                        (int) (uv[3] - uv[1]),
                        texture.width,
                        texture.height,
                        color
                    );
                }

                texture.setFilterMipmap(false, false);

                matrices.popMatrix();
            });
        }
    }

    public static void renderImage(Batcher2D batcher, ImageOverlay overlay, int width, int height)
    {
        if (overlay == null)
        {
            return;
        }

        renderImages(batcher, Collections.singletonList(overlay), width, height);
    }

    public static void renderImages(MatrixStack stack, Batcher2D batcher, List<ImageOverlay> images)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();

        renderImages(batcher, images, width, height);
    }

    public static void renderImage(MatrixStack stack, Batcher2D batcher, ImageOverlay overlay)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();

        renderImage(batcher, overlay, width, height);
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
