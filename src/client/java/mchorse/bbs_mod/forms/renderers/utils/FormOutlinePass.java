package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.client.util.math.MatrixStack;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.function.Consumer;

/**
 * Shared silhouette-outline entry: early-outs, form-opacity fade on outline alpha,
 * Iris deferral via {@link ModelVAORenderer#submitOutlineOverlay}.
 */
public final class FormOutlinePass
{
    private FormOutlinePass()
    {
    }

    /**
     * Outline RGB/A from the form, with {@code formAlpha} multiplied into outline alpha
     * so soft/tint opacity fades the silhouette (LabelForm already did this for text outline).
     */
    public static Color resolveOutlineColor(Form form, float formAlpha)
    {
        if (form == null)
        {
            return null;
        }

        Color stored = form.outlineColor.get();

        if (stored == null)
        {
            return null;
        }

        float alpha = MathUtils.clamp(stored.a * MathUtils.clamp(formAlpha, 0F, 1F), 0F, 1F);

        return new Color().set(stored.r, stored.g, stored.b, alpha);
    }

    public static boolean shouldSkip(Form form, FormRenderingContext context, float formAlpha)
    {
        if (form == null || !form.outline.get())
        {
            return true;
        }

        if (context != null && (context.stencilMap != null || context.isPicking() || context.isShadowPass))
        {
            return true;
        }

        if (BBSRendering.isIrisShadowPass() || formAlpha <= 0.001F)
        {
            return true;
        }

        Color color = resolveOutlineColor(form, formAlpha);
        float thickness = form.outlineThickness.get();

        return color == null || color.a <= 0.001F || thickness <= 0F;
    }

    /**
     * Runs dilate/composite outline. {@code maskDraw} receives the matrix stack to use for
     * mask geometry (camera-baked when Iris-deferred). Uses {@code context.stack} as the
     * transform source.
     */
    public static void run(Form form, FormRenderingContext context, float formAlpha, Consumer<MatrixStack> maskDraw)
    {
        run(form, context, formAlpha, context != null ? context.stack : null, maskDraw);
    }

    /**
     * Same as {@link #run(Form, FormRenderingContext, float, Consumer)} but takes an explicit
     * source stack (e.g. Billboard after in-place camera facing).
     */
    public static void run(Form form, FormRenderingContext context, float formAlpha, MatrixStack sourceStack, Consumer<MatrixStack> maskDraw)
    {
        if (shouldSkip(form, context, formAlpha) || maskDraw == null || sourceStack == null)
        {
            return;
        }

        Color outlineColor = resolveOutlineColor(form, formAlpha);
        float thickness = form.outlineThickness.get();
        boolean rainbow = form.outlineRainbow.get();
        float rainbowSpeed = form.outlineRainbowSpeed.get();
        float rainbowScale = form.outlineRainbowScale.get();

        if (BBSRendering.isIrisDeferredModelPass())
        {
            Matrix4f baked = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(sourceStack.peek().getPositionMatrix()));
            MatrixStack deferredStack = new MatrixStack();

            MatrixStackUtils.multiply(deferredStack, baked);
            deferredStack.peek().getNormalMatrix().set(sourceStack.peek().getNormalMatrix());

            Color colorSnapshot = outlineColor.copy();
            float thicknessSnapshot = thickness;
            boolean rainbowSnapshot = rainbow;
            float speedSnapshot = rainbowSpeed;
            float scaleSnapshot = rainbowScale;

            ModelVAORenderer.submitOutlineOverlay(
                new Matrix4f(RenderSystem.getProjectionMatrix()),
                new Matrix4f(RenderSystem.getModelViewMatrix()),
                () -> FormOutlineRenderer.render(deferredStack, colorSnapshot, thicknessSnapshot, rainbowSnapshot, speedSnapshot, scaleSnapshot,
                    () -> maskDraw.accept(deferredStack))
            );
        }
        else
        {
            MatrixStack maskStack = new MatrixStack();

            MatrixStackUtils.multiply(maskStack, sourceStack.peek().getPositionMatrix());
            maskStack.peek().getNormalMatrix().set(sourceStack.peek().getNormalMatrix());

            FormOutlineRenderer.render(maskStack, outlineColor, thickness, rainbow, rainbowSpeed, rainbowScale,
                () -> maskDraw.accept(maskStack));
        }
    }
}
