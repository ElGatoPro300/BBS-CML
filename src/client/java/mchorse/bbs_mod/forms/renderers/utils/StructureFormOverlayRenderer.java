package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.cubic.render.vao.IModelVAO;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.StructureForm;
import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.EffectTransformMath;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.math.BlockPos;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;

import java.util.function.Consumer;

/**
 * Handles multi-pass shader overlays (Glow, Paint, Color Tint / Grade) and Iris deferred submissions.
 */
public class StructureFormOverlayRenderer
{
    public enum StructurePaintLayer
    {
        BIOME,
        ANIMATED,
        TRANSLUCENT
    }

    public StructureFormOverlayRenderer()
    {
    }

    public void prepareVaoPaintForMainPass(Color resolvedPaint)
    {
        if (resolvedPaint != null && resolvedPaint.a < 0F)
        {
            ModelVAORenderer.setPaint(resolvedPaint.r, resolvedPaint.g, resolvedPaint.b, resolvedPaint.a);
        }
        else
        {
            this.clearVaoPaint();
        }
    }

    public void clearVaoPaint()
    {
        ModelVAORenderer.setPaint(0F, 0F, 0F, 0F);
    }

    public void prepareVaoGlowForMainPass(GlowSettings glowSettings, Color legacyGlow, float glowIntensity)
    {
        if (glowIntensity < 0F)
        {
            Color glowColor = new Color();
            glowSettings.resolveColor(legacyGlow, glowColor);
            ModelVAORenderer.setGlow(glowSettings, glowColor.r, glowColor.g, glowColor.b, legacyGlow);
        }
        else
        {
            this.clearVaoGlow();
        }
    }

    public void clearVaoGlow()
    {
        GlowSettings glowOff = new GlowSettings();
        glowOff.intensity = 0F;
        ModelVAORenderer.setGlow(glowOff, 0F, 0F, 0F, null);
    }

    public void clearVaoColorTint()
    {
        ModelVAORenderer.clearColorEffectTransform();
        ModelVAORenderer.clearFormColorTint();
        ModelVAORenderer.clearFormColorGrade();
        ModelVAORenderer.clearGradeEffectTransforms();
    }

    public void resolveStructureMaskSize(StructureData data, Vector3f dest)
    {
        BlockPos min = data.getBoundsMin();
        BlockPos max = data.getBoundsMax();

        if (min != null && max != null)
        {
            dest.set(
                Math.max(1, max.getX() - min.getX() + 1),
                Math.max(1, max.getY() - min.getY() + 1),
                Math.max(1, max.getZ() - min.getZ() + 1)
            );
            return;
        }

        BlockPos sz = data.getSize();

        dest.set(
            Math.max(1, sz.getX()),
            Math.max(1, sz.getY()),
            Math.max(1, sz.getZ())
        );
    }

    public void resolveStructureMaskHalf(StructureData data, EffectTransform transform, Vector3f dest)
    {
        Vector3f size = new Vector3f();
        this.resolveStructureMaskSize(data, size);
        EffectTransformMath.resolveStructureMaskHalfExtents(transform, dest, size.x, size.y, size.z);
    }

    public void renderStructureGlowOverlay(StructureData data, FormRenderingContext context, MatrixStack stack, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha, int overlay, boolean optimize, boolean useEntityLayers, Consumer<StructurePaintLayer> layerDraw, Runnable culledWorldDraw)
    {
        int layers = FormColorEffects.resolveGlowOverlayLayers(glowIntensity);

        if (optimize)
        {
            this.runStructureBlocksGlowOverlay(glowSettings, legacyGlow, alpha, glowIntensity, layers, culledWorldDraw);

            if (data.hasBiomeTintedLayer())
            {
                this.runStructureBlocksGlowOverlay(glowSettings, legacyGlow, alpha, glowIntensity, layers, () -> layerDraw.accept(StructurePaintLayer.BIOME));
            }

            if (data.hasAnimatedLayer())
            {
                this.runStructureBlocksGlowOverlay(glowSettings, legacyGlow, alpha, glowIntensity, layers, () -> layerDraw.accept(StructurePaintLayer.ANIMATED));
            }

            if (data.hasTranslucentLayer())
            {
                this.runStructureBlocksGlowOverlay(glowSettings, legacyGlow, alpha, glowIntensity, layers, () -> layerDraw.accept(StructurePaintLayer.TRANSLUCENT));
            }
        }
        else
        {
            this.runStructureBlocksGlowOverlay(glowSettings, legacyGlow, alpha, glowIntensity, layers, culledWorldDraw);
        }
    }

    private void runStructureBlocksGlowOverlay(GlowSettings glowSettings, Color legacyGlow, float alpha, float glowIntensity, int layers, Runnable draw)
    {
        if (draw == null)
        {
            return;
        }

        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        int savedDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean savedPolygonOffsetFill = GL11.glGetBoolean(GL11.GL_POLYGON_OFFSET_FILL);
        Color glowColor = FormColorEffects.resolveGlowOverlayEmissionColor(glowSettings, legacyGlow, alpha, glowIntensity);
        float shaderScale = FormColorEffects.resolveGlowOverlayShaderScale(glowIntensity);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1F, -1F);
        RenderSystem.setShaderColor(shaderScale, shaderScale, shaderScale, 1F);

        try
        {
            consumers.setSubstitute(BBSRendering.getGlowOverlayConsumer(glowColor));
            draw.run();
            consumers.draw();
        }
        finally
        {
            consumers.setSubstitute(null);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            RenderSystem.depthMask(savedDepthMask);
            RenderSystem.depthFunc(savedDepthFunc);

            if (savedPolygonOffsetFill)
            {
                GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            }
            else
            {
                GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            }

            RenderSystem.defaultBlendFunc();
        }
    }

    public void renderStructurePaintOverlay(StructureData data, IModelVAO vao, FormRenderingContext context, MatrixStack stack, Color resolvedPaint, float alpha, int overlay, boolean optimize, boolean useEntityLayers, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, Consumer<StructurePaintLayer> layerDraw, Runnable culledWorldDraw)
    {
        Color paintOverlay = new Color(resolvedPaint.r, resolvedPaint.g, resolvedPaint.b, resolvedPaint.a);
        paintOverlay.a *= alpha;

        this.renderStructurePaintOverlayPass(data, vao, context, stack, paintOverlay, overlay, optimize, useEntityLayers, transform, glowSettings, legacyGlow, glowIntensity, alpha, layerDraw, culledWorldDraw);
    }

    public void submitDeferredStructurePaintOverlay(StructureData data, IModelVAO vao, FormRenderingContext context, Color resolvedPaint, float alpha, int overlay, boolean optimize, boolean useEntityLayers, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, Consumer<StructurePaintLayer> layerDraw, Runnable culledWorldDraw)
    {
        Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(context.stack.peek().getPositionMatrix()));
        Matrix3f normalMatrix = new Matrix3f(context.stack.peek().getNormalMatrix());
        Color paintOverlay = new Color(resolvedPaint.r, resolvedPaint.g, resolvedPaint.b, resolvedPaint.a);
        paintOverlay.a *= alpha;

        ModelVAORenderer.submitPaintOverlay(false, () ->
        {
            MatrixStack overlayStack = new MatrixStack();
            overlayStack.peek().getPositionMatrix().set(positionMatrix);
            overlayStack.peek().getNormalMatrix().set(normalMatrix);

            this.renderStructurePaintOverlayPass(data, vao, context, overlayStack, paintOverlay, overlay, optimize, useEntityLayers, transform, glowSettings, legacyGlow, glowIntensity, alpha, layerDraw, culledWorldDraw);
        });
    }

    private void renderStructurePaintOverlayPass(StructureData data, IModelVAO vao, FormRenderingContext context, MatrixStack stack, Color paintOverlay, int overlay, boolean optimize, boolean useEntityLayers, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha, Consumer<StructurePaintLayer> layerDraw, Runnable culledWorldDraw)
    {
        if (optimize)
        {
            if (vao != null)
            {
                this.renderStructureVaoPaintOverlay(data, vao, stack, Color.white(), paintOverlay, LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay, transform);
            }

            if (data.hasBiomeTintedLayer())
            {
                this.runStructureBlocksPaintOverlay(data, paintOverlay, stack, transform, glowSettings, legacyGlow, glowIntensity, alpha, () -> layerDraw.accept(StructurePaintLayer.BIOME));
            }

            if (data.hasAnimatedLayer())
            {
                this.runStructureBlocksPaintOverlay(data, paintOverlay, stack, transform, glowSettings, legacyGlow, glowIntensity, alpha, () -> layerDraw.accept(StructurePaintLayer.ANIMATED));
            }

            if (data.hasTranslucentLayer())
            {
                this.runStructureBlocksPaintOverlay(data, paintOverlay, stack, transform, glowSettings, legacyGlow, glowIntensity, alpha, () -> layerDraw.accept(StructurePaintLayer.TRANSLUCENT));
            }
        }
        else
        {
            this.runStructureBlocksPaintOverlay(data, paintOverlay, stack, transform, glowSettings, legacyGlow, glowIntensity, alpha, culledWorldDraw);
        }
    }

    private void renderStructureVaoPaintOverlay(StructureData data, IModelVAO vao, MatrixStack stack, Color tint, Color paintOverlay, int light, int overlay, EffectTransform transform)
    {
        GameRenderer gameRenderer = MinecraftClient.getInstance().gameRenderer;
        Matrix4f formRootInverse = MatrixStackUtils.invertFormRootMatrixForOverlay(stack.peek().getPositionMatrix());
        Vector3f paintMaskHalf = new Vector3f();

        this.resolveStructureMaskHalf(data, transform, paintMaskHalf);

        gameRenderer.getLightmapTextureManager().enable();
        gameRenderer.getOverlayTexture().setupOverlayColor();

        try
        {
            this.clearVaoColorTint();
            ModelVAORenderer.beginPaintOverlayPass(false);
            GL11.glPolygonOffset(FlatPaintOverlayPass.POLYGON_OFFSET_FACTOR, FlatPaintOverlayPass.POLYGON_OFFSET_UNITS);
            ModelVAORenderer.setPaint(paintOverlay.r, paintOverlay.g, paintOverlay.b, paintOverlay.a);
            ModelVAORenderer.setPaintEffectTransform(formRootInverse, transform, paintMaskHalf, true);
            RenderSystem.setShader(BBSShaders::getModel);
            RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            RenderSystem.depthMask(false);
            ModelVAORenderer.render(BBSShaders.getModel(), vao, stack, tint.r, tint.g, tint.b, tint.a, light, overlay);
        }
        finally
        {
            RenderSystem.depthMask(true);
            ModelVAORenderer.clearPaintEffectTransform();
            ModelVAORenderer.endPaintOverlayPass();
            this.clearVaoPaint();
            gameRenderer.getLightmapTextureManager().disable();
            gameRenderer.getOverlayTexture().teardownOverlayColor();
        }
    }

    private void runStructureBlocksPaintOverlay(StructureData data, Color paintOverlay, MatrixStack stack, EffectTransform transform, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha, Runnable draw)
    {
        if (draw == null)
        {
            return;
        }

        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        Matrix4f formRootInverse = MatrixStackUtils.invertFormRootMatrixForOverlay(stack.peek().getPositionMatrix());
        int savedDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean savedPolygonOffsetFill = GL11.glGetBoolean(GL11.GL_POLYGON_OFFSET_FILL);

        CustomVertexConsumerProvider.clearRunnables();

        Vector3f structureSize = new Vector3f();
        this.resolveStructureMaskSize(data, structureSize);
        CustomVertexConsumerProvider.hijackVertexFormat((l) -> BlockEffectOverlayUniforms.configurePaintOverlayRenderStateStructure(formRootInverse, transform, true, glowSettings, legacyGlow, glowIntensity, alpha, structureSize.x, structureSize.y, structureSize.z));

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(FlatPaintOverlayPass.POLYGON_OFFSET_FACTOR, FlatPaintOverlayPass.POLYGON_OFFSET_UNITS);

        consumers.setSubstitute(BBSRendering.getBlockPaintOverlayConsumer(paintOverlay));

        try
        {
            draw.run();
            consumers.draw();
        }
        finally
        {
            consumers.setSubstitute(null);
            RenderSystem.depthMask(savedDepthMask);
            RenderSystem.depthFunc(savedDepthFunc);
            GL11.glPolygonOffset(0F, 0F);

            if (savedPolygonOffsetFill)
            {
                GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            }
            else
            {
                GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            }

            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            CustomVertexConsumerProvider.clearRunnables();
        }
    }

    public Color resolveStructureColorTintUniform(StructureForm form, Color formColor)
    {
        Color stored = form.color.get();

        if (stored != null && (stored.hasColorAdjustments() || stored.hasActiveTransform()))
        {
            Color tint = stored.hasColorAdjustments() ? stored.copyDeferringColorGrade() : stored.copy();

            if (formColor != null && formColor.transform != null)
            {
                tint.transform = formColor.transform.copy();
            }
            else if (stored.transform != null)
            {
                tint.transform = stored.transform.copy();
            }

            form.applyFormOpacity(tint);

            return tint;
        }

        return formColor;
    }

    public void renderStructureColorTintOverlay(StructureData data, StructureForm form, IModelVAO vao, FormRenderingContext context, MatrixStack stack, Color formColor, float alpha, int overlay, boolean optimize, boolean useEntityLayers, boolean includeVao, Consumer<StructurePaintLayer> layerDraw, Runnable culledWorldDraw)
    {
        this.renderStructureColorTintOverlayPass(data, form, vao, context, stack, formColor, alpha, overlay, optimize, useEntityLayers, includeVao, layerDraw, culledWorldDraw);
    }

    public void submitDeferredStructureColorTintOverlay(StructureData data, StructureForm form, IModelVAO vao, FormRenderingContext context, Color formColor, float alpha, int overlay, boolean optimize, boolean useEntityLayers, Consumer<StructurePaintLayer> layerDraw, Runnable culledWorldDraw)
    {
        Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(context.stack.peek().getPositionMatrix()));
        Matrix3f normalMatrix = new Matrix3f(context.stack.peek().getNormalMatrix());
        Color formColorSnapshot = formColor.copy();

        ModelVAORenderer.submitColorTintOverlay(() ->
        {
            MatrixStack overlayStack = new MatrixStack();
            overlayStack.peek().getPositionMatrix().set(positionMatrix);
            overlayStack.peek().getNormalMatrix().set(normalMatrix);

            this.renderStructureColorTintOverlayPass(data, form, vao, context, overlayStack, formColorSnapshot, alpha, overlay, optimize, useEntityLayers, false, layerDraw, culledWorldDraw);
        });
    }

    private void renderStructureColorTintOverlayPass(StructureData data, StructureForm form, IModelVAO vao, FormRenderingContext context, MatrixStack stack, Color formColor, float alpha, int overlay, boolean optimize, boolean useEntityLayers, boolean includeVao, Consumer<StructurePaintLayer> layerDraw, Runnable culledWorldDraw)
    {
        Color tintUniform = this.resolveStructureColorTintUniform(form, formColor);
        int light = context == null ? LightmapTextureManager.MAX_LIGHT_COORDINATE : context.light;

        if (optimize)
        {
            if (vao != null && includeVao)
            {
                this.renderStructureVaoColorTintOverlay(data, vao, stack, tintUniform, light, overlay);
            }
            else if (culledWorldDraw != null)
            {
                this.runStructureBlocksColorTintOverlay(data, form, tintUniform, stack, form.color.get(), culledWorldDraw);
            }

            if (data.hasBiomeTintedLayer())
            {
                this.runStructureBlocksColorTintOverlay(data, form, tintUniform, stack, form.color.get(), () -> layerDraw.accept(StructurePaintLayer.BIOME));
            }

            if (data.hasAnimatedLayer())
            {
                this.runStructureBlocksColorTintOverlay(data, form, tintUniform, stack, form.color.get(), () -> layerDraw.accept(StructurePaintLayer.ANIMATED));
            }

            if (data.hasTranslucentLayer())
            {
                this.runStructureBlocksColorTintOverlay(data, form, tintUniform, stack, form.color.get(), () -> layerDraw.accept(StructurePaintLayer.TRANSLUCENT));
            }
        }
        else if (culledWorldDraw != null)
        {
            this.runStructureBlocksColorTintOverlay(data, form, tintUniform, stack, form.color.get(), culledWorldDraw);
        }
    }

    private void renderStructureVaoColorTintOverlay(StructureData data, IModelVAO vao, MatrixStack stack, Color tintUniform, int light, int overlay)
    {
        GameRenderer gameRenderer = MinecraftClient.getInstance().gameRenderer;
        Matrix4f formRootInverse = MatrixStackUtils.invertFormRootMatrixForOverlay(stack.peek().getPositionMatrix());
        Vector3f colorMaskHalf = new Vector3f();
        EffectTransform transform = tintUniform.transform;

        this.resolveStructureMaskHalf(data, transform, colorMaskHalf);

        gameRenderer.getLightmapTextureManager().enable();
        gameRenderer.getOverlayTexture().setupOverlayColor();

        try
        {
            this.clearVaoColorTint();
            ModelVAORenderer.beginColorTintOverlayPass();
            GL11.glPolygonOffset(-1F, -2F);
            ModelVAORenderer.setFormColorTint(tintUniform.r, tintUniform.g, tintUniform.b, tintUniform.a);
            ModelVAORenderer.setColorEffectTransform(formRootInverse, transform, colorMaskHalf);
            RenderSystem.setShader(BBSShaders::getModel);
            RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
            ModelVAORenderer.render(BBSShaders.getModel(), vao, stack, 1F, 1F, 1F, 1F, light, overlay);
        }
        finally
        {
            RenderSystem.depthMask(true);
            ModelVAORenderer.clearColorEffectTransform();
            ModelVAORenderer.endColorTintOverlayPass();
            this.clearVaoColorTint();
            gameRenderer.getLightmapTextureManager().disable();
            gameRenderer.getOverlayTexture().teardownOverlayColor();
        }
    }

    private void runStructureBlocksColorTintOverlay(StructureData data, StructureForm form, Color formColor, MatrixStack stack, Color gradeSource, Runnable draw)
    {
        if (draw == null)
        {
            return;
        }

        if (!BlockEffectOverlayUniforms.hasColorTintOverlayShader())
        {
            return;
        }

        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        Matrix4f formRootInverse = MatrixStackUtils.invertFormRootMatrixForOverlay(stack.peek().getPositionMatrix());
        int savedDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean savedPolygonOffsetFill = GL11.glGetBoolean(GL11.GL_POLYGON_OFFSET_FILL);

        CustomVertexConsumerProvider.clearRunnables();

        Vector3f structureSize = new Vector3f();
        this.resolveStructureMaskSize(data, structureSize);
        CustomVertexConsumerProvider.hijackVertexFormat((l) -> BlockEffectOverlayUniforms.configureColorTintOverlayRenderStateStructure(formRootInverse, formColor.transform, true, formColor, gradeSource, structureSize.x, structureSize.y, structureSize.z));

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1F, -2F);

        consumers.setSubstitute(BBSRendering.getBlockColorTintOverlayConsumer());

        try
        {
            draw.run();
            consumers.draw();
        }
        finally
        {
            consumers.setSubstitute(null);
            RenderSystem.depthMask(savedDepthMask);
            RenderSystem.depthFunc(savedDepthFunc);
            GL11.glPolygonOffset(0F, 0F);

            if (savedPolygonOffsetFill)
            {
                GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            }
            else
            {
                GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            }

            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            RenderSystem.defaultBlendFunc();
            CustomVertexConsumerProvider.clearRunnables();
        }
    }
}
