package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.LabelForm;
import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.EffectTransformMath;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.forms.renderers.utils.BlockEffectOverlayUniforms;
import mchorse.bbs_mod.forms.renderers.utils.FlatColorTintOverlayPass;
import mchorse.bbs_mod.forms.renderers.utils.FlatPaintOverlayPass;
import mchorse.bbs_mod.forms.renderers.utils.FormColorEffects;
import mchorse.bbs_mod.forms.renderers.utils.LabelTextTintQuadCapture;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.utils.FontUtils;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.TextureFont;
import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;

import java.awt.Font;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LabelFormRenderer extends FormRenderer<LabelForm>
{
    /**
     * Minecraft's {@link TextRenderer} treats {@code (color & 0xFC000000) == 0} as fully
     * opaque, so alpha bytes 0–3 become 255. Keep a minimum of 4 when opacity is intended.
     */
    private static final int MIN_TEXT_ALPHA_BYTE = 4;

    private float nametagAlpha = 1F;
    private int lastBoundTextTexture;
    private final Vector3f maskHalfExtents = new Vector3f();
    private final LabelTextTintQuadCapture tintCapture = new LabelTextTintQuadCapture();
    private final Matrix4f identityMatrix = new Matrix4f();

    public static void fillQuad(BufferBuilder builder, MatrixStack stack, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float b, float a)
    {
        Matrix4f matrix4f = stack.peek().getPositionMatrix();

        /* 1 - BR, 2 - BL, 3 - TL, 4 - TR */
        builder.vertex(matrix4f, x1, y1, z1).color(r, g, b, a);
        builder.vertex(matrix4f, x2, y2, z2).color(r, g, b, a);
        builder.vertex(matrix4f, x3, y3, z3).color(r, g, b, a);
        builder.vertex(matrix4f, x1, y1, z1).color(r, g, b, a);
        builder.vertex(matrix4f, x3, y3, z3).color(r, g, b, a);
        builder.vertex(matrix4f, x4, y4, z4).color(r, g, b, a);
    }

    public LabelFormRenderer(LabelForm form)
    {
        super(form);
    }

    @Override
    public void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        GlowSettings glowSettings = this.form.glowSettings.get();
        Color legacyGlow = this.form.glowingColor.get();
        float glowIntensity = glowSettings.resolveIntensity(legacyGlow);
        Color color = this.form.color.get().copy();

        if (glowIntensity < 0F)
        {
            FormColorEffects.blendFormGlowBrighten(color, glowSettings, legacyGlow);
        }

        /* Minecraft TextRenderer treats ARGB alpha 0 as fully opaque. */
        if (isFullyTransparent(color))
        {
            return;
        }

        int argb = toSafeTextArgb(color);
        String text = StringUtils.processColoredText(this.form.text.get());
        List<String> wrap = context.batcher.getFont().wrap(text, x2 - x1 - 4);

        int th = context.batcher.getFont().getHeight();
        int lineHeight = th + 4;
        int h = th + (wrap.size() - 1) * lineHeight;
        int y = (y2 + y1) / 2 - h / 2;

        for (String s : wrap)
        {
            context.batcher.textShadow(s, x1 + 2, y, argb);

            y += lineHeight;
        }

        if (glowIntensity > 0F)
        {
            Color glowColor = FormColorEffects.resolveGlowOverlayEmissionColor(glowSettings, legacyGlow, 1F, glowIntensity);
            float shaderScale = FormColorEffects.resolveGlowOverlayShaderScale(glowIntensity);

            glowColor.r *= color.r;
            glowColor.g *= color.g;
            glowColor.b *= color.b;

            int glowArgb = toSafeTextArgb(glowColor);
            int glowY = (y2 + y1) / 2 - h / 2;

            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            RenderSystem.setShaderColor(shaderScale, shaderScale, shaderScale, 1F);

            for (String s : wrap)
            {
                context.batcher.text(s, x1 + 2, glowY, glowArgb);

                glowY += lineHeight;
            }

            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            RenderSystem.defaultBlendFunc();
        }
    }

    @Override
    public void render3D(FormRenderingContext context)
    {
        context.stack.push();

        if (this.form.billboard.get())
        {
            Matrix4f modelMatrix = context.stack.peek().getPositionMatrix();
            Vector3f scale = new Vector3f();

            modelMatrix.getScale(scale);

            modelMatrix.m00(1).m01(0).m02(0);
            modelMatrix.m10(0).m11(1).m12(0);
            modelMatrix.m20(0).m21(0).m22(1);

            if (!context.modelRenderer && !context.isPicking())
            {
                modelMatrix.mul(context.camera.view);
            }

            modelMatrix.scale(scale);

            context.stack.peek().getNormalMatrix().identity();
            context.stack.peek().getNormalMatrix().scale(
                MatrixStackUtils.safeNormalScaleReciprocal(scale.x),
                MatrixStackUtils.safeNormalScaleReciprocal(scale.y),
                MatrixStackUtils.safeNormalScaleReciprocal(scale.z)
            );
        }

        TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        float fontSize = this.form.fontSize.get();
        float scale = (1F / 16F) * (fontSize <= 0 ? 1F : fontSize);
        int light = context.light;

        this.nametagAlpha = 1F;

        if (this.form.nametag.get() && context.entity != null && context.entity.isSneaking())
        {
            context.stack.translate(0F, -0.5F, 0F);
            this.nametagAlpha = 0.125F;
        }

        MatrixStackUtils.scaleStack(context.stack, scale, -scale, scale);

        RenderSystem.disableCull();

        if (context.isPicking())
        {
            CustomVertexConsumerProvider.hijackVertexFormat((layer) ->
            {
                /* startDrawing may re-enable culling; keep both sides of the label visible. */
                RenderSystem.disableCull();
                this.setupTarget(context, BBSShaders.getPickerModelsProgram());
                RenderSystem.setShader(BBSShaders::getPickerModelsProgram);
            });

            light = 0;
        }
        else
        {
            CustomVertexConsumerProvider.hijackVertexFormat((layer) ->
            {
                RenderSystem.disableCull();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
            });
        }

        if (this.form.max.get() <= 10)
        {
            this.renderString(context, consumers, renderer, light);
        }
        else
        {
            this.renderLimitedString(context, consumers, renderer, light);
        }

        /* Glow overlay clears the hijack; re-apply disableCull for any leftover shared-buffer
         * flush so the last label keeps both faces when WorldRenderer draws later. */
        CustomVertexConsumerProvider.hijackVertexFormat((layer) -> RenderSystem.disableCull());
        this.flushLabelConsumers(consumers);

        CustomVertexConsumerProvider.clearRunnables();
        RenderSystem.defaultBlendFunc();

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();

        context.stack.pop();
    }

    /**
     * Text {@link RenderLayer}s restore GL culling in
     * {@code startDrawing}. Labels use a negative Y scale (flipped winding), so both faces
     * must stay unculled at flush time or the back of the last drawn label disappears.
     */
    private void flushLabelConsumers(CustomVertexConsumerProvider consumers)
    {
        RenderSystem.disableCull();
        consumers.draw();
    }

    private String applyStyles(String content)
    {
        StringBuilder prefix = new StringBuilder();
        if (this.form.fontWeight.get() >= 700) prefix.append("\u00A7l");
        if (this.form.fontStyle.get() >= 1) prefix.append("\u00A7o");
        if (this.form.underline.get()) prefix.append("\u00A7n");
        if (this.form.strikethrough.get()) prefix.append("\u00A7m");
        
        return prefix.toString() + content;
    }

    private void renderTextShadow(FormRenderingContext context, CustomVertexConsumerProvider consumers, TextRenderer renderer, TextureFont customFont, String content, float x, float y, float letterSpacing, int light, Color shadowColor)
    {
        if (isFullyTransparent(shadowColor))
        {
            return;
        }

        context.stack.push();
        context.stack.translate(0F, 0F, -0.05F);

        float sx = this.form.shadowX.get();
        float sy = this.form.shadowY.get();
        float blur = this.form.shadowBlur.get();

        if (blur > 0)
        {
            int originalColor = toSafeTextArgb(shadowColor);
            int alpha = (originalColor >> 24) & 0xFF;
            int rgb = originalColor & 0x00FFFFFF;
            int blurAlpha = Math.max(1, alpha / 4);
            int blurColor = (blurAlpha << 24) | rgb;

            this.drawSimpleText(context, consumers, renderer, customFont, content, x + sx - blur, y + sy, letterSpacing, light, blurColor);
            this.drawSimpleText(context, consumers, renderer, customFont, content, x + sx + blur, y + sy, letterSpacing, light, blurColor);
            this.drawSimpleText(context, consumers, renderer, customFont, content, x + sx, y + sy - blur, letterSpacing, light, blurColor);
            this.drawSimpleText(context, consumers, renderer, customFont, content, x + sx, y + sy + blur, letterSpacing, light, blurColor);
        }
        else
        {
            this.drawSimpleText(context, consumers, renderer, customFont, content, x + sx, y + sy, letterSpacing, light, toSafeTextArgb(shadowColor));
        }

        context.stack.pop();
    }

    private void drawSimpleText(FormRenderingContext context, CustomVertexConsumerProvider consumers, TextRenderer renderer, TextureFont customFont, String content, float x, float y, float letterSpacing, int light, int color)
    {
        if (customFont != null)
        {
            customFont.draw(content, x, y, color, color, letterSpacing, 0F, context.stack.peek().getPositionMatrix(), consumers, light);
        }
        else
        {
            renderer.draw(
                content,
                x,
                y,
                color, false,
                context.stack.peek().getPositionMatrix(),
                consumers,
                TextRenderer.TextLayerType.NORMAL,
                0,
                light
            );
        }
    }

    private void renderTextGlowOverlay(FormRenderingContext context, CustomVertexConsumerProvider consumers, TextRenderer renderer, TextureFont customFont, String content, float x, float y, float letterSpacing, GlowSettings glowSettings, Color legacyGlow, float alpha, float glowIntensity, int textColor)
    {
        if (context.isPicking() || glowIntensity <= 0F)
        {
            return;
        }

        context.stack.push();
        context.stack.translate(0F, 0F, 0.002F);

        CustomVertexConsumerProvider.clearRunnables();
        CustomVertexConsumerProvider.hijackVertexFormat((layer) ->
        {
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        });

        Color glowColor = FormColorEffects.resolveGlowOverlayEmissionColor(glowSettings, legacyGlow, alpha, glowIntensity);
        float shaderScale = FormColorEffects.resolveGlowOverlayShaderScale(glowIntensity);
        int maxLight = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        boolean savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean savedPolygonOffsetFill = GL11.glGetBoolean(GL11.GL_POLYGON_OFFSET_FILL);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.depthMask(false);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1F, -1F);
        RenderSystem.setShaderColor(shaderScale, shaderScale, shaderScale, 1F);

        try
        {
            consumers.setSubstitute(BBSRendering.getTextGlowOverlayConsumer(glowColor));

            if (customFont != null)
            {
                customFont.draw(content, x, y, textColor, textColor, letterSpacing, 0F, context.stack.peek().getPositionMatrix(), consumers, maxLight);
            }
            else
            {
                renderer.draw(
                    content,
                    x,
                    y,
                    textColor,
                    false,
                    context.stack.peek().getPositionMatrix(),
                    consumers,
                    TextRenderer.TextLayerType.NORMAL,
                    0,
                    maxLight
                );
            }

            this.flushLabelConsumers(consumers);
        }
        finally
        {
            consumers.setSubstitute(null);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            GL11.glPolygonOffset(0F, 0F);

            if (!savedPolygonOffsetFill)
            {
                GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            }

            RenderSystem.depthMask(savedDepthMask);
            RenderSystem.defaultBlendFunc();
            CustomVertexConsumerProvider.clearRunnables();
        }

        context.stack.pop();
    }

    private void renderString(FormRenderingContext context, CustomVertexConsumerProvider consumers, TextRenderer renderer, int light)
    {
        String content = applyStyles(StringUtils.processColoredText(this.form.text.get()));
        String fontName = this.form.font.get();
        TextureFont customFont = null;
        
        if (!fontName.isEmpty())
        {
            int style = Font.PLAIN;
            if (this.form.fontWeight.get() >= 700) style |= Font.BOLD;
            if (this.form.fontStyle.get() >= 1) style |= Font.ITALIC;
            
            customFont = FontUtils.getFont(fontName, style);
        }

        float transition = context.getTransition();
        float letterSpacing = this.form.letterSpacing.get();
        int w = customFont != null ? customFont.getWidth(content, letterSpacing) : renderer.getWidth(content) - 1;
        int h = customFont != null ? customFont.getHeight() : renderer.fontHeight - 2;
        int x = (int) (-w * this.form.anchorX.get());
        int y = (int) (-h * this.form.anchorY.get());

        GlowSettings glowSettings = this.form.glowSettings.get();
        Color legacyGlow = this.form.glowingColor.get();
        float glowIntensity = glowSettings.resolveIntensity(legacyGlow);
        PaintSettings paintSettings = this.form.paintSettings.get();
        Color legacyPaint = this.form.paintColor.get();
        Color shadowColor = this.form.shadowColor.get().copy();
        Color storedFormColor = this.form.color.get();
        boolean colorTransformWanted = FormColorEffects.wantsColorTransformMask(storedFormColor) && !context.isPicking();
        Color contextColor = new Color().set(context.color, true);
        Color color = contextColor.copy();
        Color formTintColor = null;
        EffectTransform colorTransform = null;

        /* Spatial Color transform: bake mask per glyph (AABB overlay would tint the background). */
        if (colorTransformWanted)
        {
            color.r = 1F;
            color.g = 1F;
            color.b = 1F;
            this.form.applyFormOpacity(color);
            /* Keep base + FlatColorTint opacity in sync (context alpha used to hit only the tint). */
            color.a *= contextColor.a;
            formTintColor = storedFormColor.copyDeferringColorGrade().copy();
            this.form.applyFormOpacity(formTintColor);
            formTintColor.mul(contextColor);
            colorTransform = storedFormColor.transform == null ? null : storedFormColor.transform.copy();
        }
        else
        {
            color.mul(storedFormColor);
        }

        FormColorEffects.applyPaintBlend(color, paintSettings, legacyPaint);

        if (glowIntensity < 0F)
        {
            FormColorEffects.blendFormGlowBrighten(color, glowSettings, legacyGlow);
        }

        shadowColor.a *= this.nametagAlpha;
        color.a *= this.nametagAlpha;

        if (formTintColor != null)
        {
            formTintColor.a *= this.nametagAlpha;
        }

        float formOpacity = color.a;
        shadowColor.a *= this.form.color.get().a;
        shadowColor.mul(context.color);

        if (isFullyTransparent(color) && !context.isPicking())
        {
            this.renderShadow(context, x, y, w, h);

            return;
        }

        this.renderTextShadow(context, consumers, renderer, customFont, content, x, y, letterSpacing, light, shadowColor);

        if (this.form.outline.get() && !isFullyTransparent(color))
        {
            Color outlineColor = this.form.outlineColor.get().copy();
            outlineColor.a *= formOpacity;
            int oc = toSafeTextArgb(outlineColor);
            float ow = this.form.outlineWidth.get();

            context.stack.push();
            context.stack.translate(0, 0, -0.025F);

            if (customFont != null)
            {
                customFont.draw(content, x - ow, y, oc, oc, letterSpacing, 0F, context.stack.peek().getPositionMatrix(), consumers, light);
                customFont.draw(content, x + ow, y, oc, oc, letterSpacing, 0F, context.stack.peek().getPositionMatrix(), consumers, light);
                customFont.draw(content, x, y - ow, oc, oc, letterSpacing, 0F, context.stack.peek().getPositionMatrix(), consumers, light);
                customFont.draw(content, x, y + ow, oc, oc, letterSpacing, 0F, context.stack.peek().getPositionMatrix(), consumers, light);
            }
            else
            {
                renderer.draw(content, x - ow, y, oc, false, context.stack.peek().getPositionMatrix(), consumers, TextRenderer.TextLayerType.NORMAL, 0, light);
                renderer.draw(content, x + ow, y, oc, false, context.stack.peek().getPositionMatrix(), consumers, TextRenderer.TextLayerType.NORMAL, 0, light);
                renderer.draw(content, x, y - ow, oc, false, context.stack.peek().getPositionMatrix(), consumers, TextRenderer.TextLayerType.NORMAL, 0, light);
                renderer.draw(content, x, y + ow, oc, false, context.stack.peek().getPositionMatrix(), consumers, TextRenderer.TextLayerType.NORMAL, 0, light);
            }

            context.stack.pop();
        }

        Color gradientEnd = null;

        if (this.form.gradient.get() && !colorTransformWanted)
        {
            gradientEnd = this.form.gradientEndColor.get().copy();
            gradientEnd.a *= formOpacity;
            gradientEnd.mul(context.color);
        }

        int textArgb = this.drawLabelContent(context, consumers, renderer, customFont, content, x, y, letterSpacing, light, color, gradientEnd);

        RenderSystem.enableDepthTest();

        this.flushLabelConsumers(consumers);

        if (formTintColor != null)
        {
            this.tintCapture.clear();
            this.captureLabelGlyphs(this.tintCapture, renderer, customFont, content, x, y, letterSpacing, light);
            this.submitOrRenderLabelColorTint(context, x, y, w, h, formTintColor, colorTransform, this.tintCapture.snapshot());
        }

        this.renderTextGlowOverlay(context, consumers, renderer, customFont, content, x, y, letterSpacing, glowSettings, legacyGlow, color.a, glowIntensity, textArgb);

        this.renderShadow(context, x, y, w, h);
    }

    private void renderLimitedString(FormRenderingContext context, CustomVertexConsumerProvider consumers, TextRenderer renderer, int light)
    {
        float transition = context.getTransition();
        int w = 0;
        int h = renderer.fontHeight - 2;
        String content = applyStyles(StringUtils.processColoredText(this.form.text.get()));
        
        String fontName = this.form.font.get();
        TextureFont customFont = null;
        
        if (!fontName.isEmpty())
        {
            int style = Font.PLAIN;
            if (this.form.fontWeight.get() >= 700) style |= Font.BOLD;
            if (this.form.fontStyle.get() >= 1) style |= Font.ITALIC;
            
            customFont = FontUtils.getFont(fontName, style);
        }

        float letterSpacing = this.form.letterSpacing.get();
        List<String> lines;
        
        if (customFont != null)
        {
            lines = customFont.wrap(content, this.form.max.get(), letterSpacing);
        }
        else
        {
            lines = FontRenderer.wrap(renderer, content, this.form.max.get());
        }

        if (lines.size() <= 1)
        {
            this.renderString(context, consumers, renderer, light);
            return;
        }

        for (int i = 0; i < lines.size(); i++)
        {
            lines.set(i, lines.get(i).trim());
        }

        for (String line : lines)
        {
            int lw = customFont != null ? customFont.getWidth(line, letterSpacing) : renderer.getWidth(line) - 1;
            w = Math.max(lw, w);
        }

        int fh = customFont != null ? customFont.getHeight() : renderer.fontHeight;
        int lineHeight = (int) (fh + this.form.lineHeight.get());
        int totalHeight = (lines.size() - 1) * lineHeight + fh - 2;

        float anchorX = this.form.anchorX.get();
        int x = (int) (-w * anchorX);
        int y = (int) (-totalHeight * this.form.anchorY.get());
        int shadowY = y;

        GlowSettings glowSettings = this.form.glowSettings.get();
        Color legacyGlow = this.form.glowingColor.get();
        float glowIntensity = glowSettings.resolveIntensity(legacyGlow);
        Color shadowColor = this.form.shadowColor.get().copy();
        Color storedFormColor = this.form.color.get();
        boolean colorTransformWanted = FormColorEffects.wantsColorTransformMask(storedFormColor) && !context.isPicking();
        Color contextColor = new Color().set(context.color, true);
        Color color = contextColor.copy();
        Color formTintColor = null;
        EffectTransform colorTransform = null;

        if (colorTransformWanted)
        {
            color.r = 1F;
            color.g = 1F;
            color.b = 1F;
            this.form.applyFormOpacity(color);
            color.a *= contextColor.a;
            formTintColor = storedFormColor.copyDeferringColorGrade().copy();
            this.form.applyFormOpacity(formTintColor);
            formTintColor.mul(contextColor);
            colorTransform = storedFormColor.transform == null ? null : storedFormColor.transform.copy();
        }
        else
        {
            color.mul(storedFormColor);
        }

        if (glowIntensity < 0F)
        {
            FormColorEffects.blendFormGlowBrighten(color, glowSettings, legacyGlow);
        }

        float formOpacity = color.a;
        shadowColor.a *= this.form.color.get().a;

        shadowColor.mul(context.color);
        shadowColor.a *= this.nametagAlpha;
        color.a *= this.nametagAlpha;

        if (formTintColor != null)
        {
            formTintColor.a *= this.nametagAlpha;
        }

        if (isFullyTransparent(color) && !context.isPicking())
        {
            this.renderShadow(context, x, shadowY, w, totalHeight);

            return;
        }

        int align = this.form.textAlign.get(); /* 0: Left, 1: Center, 2: Right */
        boolean anchorLines = this.form.anchorLines.get();
        Color gradientEnd = null;

        if (this.form.gradient.get() && !colorTransformWanted)
        {
            gradientEnd = this.form.gradientEndColor.get().copy();
            gradientEnd.a *= formOpacity;
            gradientEnd.mul(context.color);
        }

        int textArgb = toSafeTextArgb(color);

        this.tintCapture.clear();

        for (String line : lines)
        {
            int lw = customFont != null ? customFont.getWidth(line, letterSpacing) : renderer.getWidth(line) - 1;
            int lx = x;

            if (anchorLines)
            {
                lx = (int) (-lw * anchorX);
            }
            else if (align == 1)
            {
                lx = x + (w - lw) / 2;
            }
            else if (align == 2)
            {
                lx = x + (w - lw);
            }

            this.renderTextShadow(context, consumers, renderer, customFont, line, lx, y, letterSpacing, light, shadowColor);

            if (this.form.outline.get())
            {
                Color outlineColor = this.form.outlineColor.get().copy();
                outlineColor.a *= formOpacity;
                int oc = toSafeTextArgb(outlineColor);
                float ow = this.form.outlineWidth.get();

                context.stack.push();
                context.stack.translate(0, 0, -0.025F);

                if (customFont != null)
                {
                    customFont.draw(line, lx - ow, y, oc, oc, letterSpacing, 0F, context.stack.peek().getPositionMatrix(), consumers, light);
                    customFont.draw(line, lx + ow, y, oc, oc, letterSpacing, 0F, context.stack.peek().getPositionMatrix(), consumers, light);
                    customFont.draw(line, lx, y - ow, oc, oc, letterSpacing, 0F, context.stack.peek().getPositionMatrix(), consumers, light);
                    customFont.draw(line, lx, y + ow, oc, oc, letterSpacing, 0F, context.stack.peek().getPositionMatrix(), consumers, light);
                }
                else
                {
                    renderer.draw(line, lx - ow, y, oc, false, context.stack.peek().getPositionMatrix(), consumers, TextRenderer.TextLayerType.NORMAL, 0, light);
                    renderer.draw(line, lx + ow, y, oc, false, context.stack.peek().getPositionMatrix(), consumers, TextRenderer.TextLayerType.NORMAL, 0, light);
                    renderer.draw(line, lx, y - ow, oc, false, context.stack.peek().getPositionMatrix(), consumers, TextRenderer.TextLayerType.NORMAL, 0, light);
                    renderer.draw(line, lx, y + ow, oc, false, context.stack.peek().getPositionMatrix(), consumers, TextRenderer.TextLayerType.NORMAL, 0, light);
                }
                context.stack.pop();
            }

            textArgb = this.drawLabelContent(context, consumers, renderer, customFont, line, lx, y, letterSpacing, light, color, gradientEnd);

            if (formTintColor != null)
            {
                this.captureLabelGlyphs(this.tintCapture, renderer, customFont, line, lx, y, letterSpacing, light);
            }

            y += lineHeight;
        }

        RenderSystem.enableDepthTest();

        this.flushLabelConsumers(consumers);

        if (formTintColor != null)
        {
            this.submitOrRenderLabelColorTint(context, x, shadowY, w, totalHeight, formTintColor, colorTransform, this.tintCapture.snapshot());
        }

        y = shadowY;

        for (String line : lines)
        {
            int lw = customFont != null ? customFont.getWidth(line, letterSpacing) : renderer.getWidth(line) - 1;
            int lx = x;

            if (anchorLines)
            {
                lx = (int) (-lw * anchorX);
            }
            else if (align == 1)
            {
                lx = x + (w - lw) / 2;
            }
            else if (align == 2)
            {
                lx = x + (w - lw);
            }

            this.renderTextGlowOverlay(context, consumers, renderer, customFont, line, lx, y, letterSpacing, glowSettings, legacyGlow, color.a, glowIntensity, textArgb);

            y += lineHeight;
        }

        this.renderShadow(context, x, shadowY, w, totalHeight);
    }

    /**
     * Draws label glyphs with a flat vertex color (no spatial mask bake). Color transform is
     * applied afterward via FlatColorTint on captured glyph quads.
     */
    private int drawLabelContent(FormRenderingContext context, CustomVertexConsumerProvider consumers, TextRenderer renderer, TextureFont customFont, String content, float drawX, float drawY, float letterSpacing, int light, Color color, Color gradientEnd)
    {
        int c1 = toSafeTextArgb(color);
        int c2 = c1;

        if (gradientEnd != null)
        {
            c2 = toSafeTextArgb(gradientEnd);
        }

        if (customFont != null)
        {
            customFont.draw(content, drawX, drawY, c1, c2, letterSpacing, 0F, context.stack.peek().getPositionMatrix(), consumers, light, this.form.gradientOffset.get());
        }
        else
        {
            renderer.draw(
                content,
                drawX,
                drawY,
                c1, false,
                context.stack.peek().getPositionMatrix(),
                consumers,
                TextRenderer.TextLayerType.NORMAL,
                0,
                light
            );
        }

        return c1;
    }

    private void captureLabelGlyphs(LabelTextTintQuadCapture capture, TextRenderer renderer, TextureFont customFont, String content, float x, float y, float letterSpacing, int light)
    {
        int opaqueWhite = 0xFFFFFFFF;

        this.identityMatrix.identity();

        if (customFont != null)
        {
            customFont.draw(content, x, y, opaqueWhite, opaqueWhite, letterSpacing, 0F, this.identityMatrix, capture, light);
        }
        else
        {
            renderer.draw(content, x, y, opaqueWhite, false, this.identityMatrix, capture, TextRenderer.TextLayerType.NORMAL, 0, light);
        }
    }

    private void submitOrRenderLabelColorTint(FormRenderingContext context, float x, float y, float w, float h, Color formTintColor, EffectTransform colorTransform, List<LabelTextTintQuadCapture.GlyphQuad> quads)
    {
        if (formTintColor == null || quads == null || quads.isEmpty())
        {
            return;
        }

        /* Glyph AABB can extend past layout metrics (descenders, bearings). Keep the mask
         * origin on the label layout center (same as the form gizmo), and grow half extents
         * so every captured glyph stays inside. */
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;

        for (LabelTextTintQuadCapture.GlyphQuad quad : quads)
        {
            minX = Math.min(minX, Math.min(Math.min(quad.x0, quad.x1), Math.min(quad.x2, quad.x3)));
            minY = Math.min(minY, Math.min(Math.min(quad.y0, quad.y1), Math.min(quad.y2, quad.y3)));
            maxX = Math.max(maxX, Math.max(Math.max(quad.x0, quad.x1), Math.max(quad.x2, quad.x3)));
            maxY = Math.max(maxY, Math.max(Math.max(quad.y0, quad.y1), Math.max(quad.y2, quad.y3)));
        }

        float centerX = x + w * 0.5F;
        float centerY = y + h * 0.5F;
        float resolvedHalfX = w * 0.5F;
        float resolvedHalfY = h * 0.5F;

        if (minX < maxX && minY < maxY)
        {
            resolvedHalfX = Math.max(resolvedHalfX, Math.max(Math.abs(maxX - centerX), Math.abs(minX - centerX)));
            resolvedHalfY = Math.max(resolvedHalfY, Math.max(Math.abs(maxY - centerY), Math.abs(minY - centerY)));
        }

        final float halfX = Math.max(resolvedHalfX, 0.001F);
        final float halfY = Math.max(resolvedHalfY, 0.001F);

        Color tintSnapshot = formTintColor.copy();
        EffectTransform transformSnapshot = colorTransform == null ? null : colorTransform.copy();
        List<LabelTextTintQuadCapture.GlyphQuad> quadSnapshot = new ArrayList<>(quads);
        boolean defer = BBSRendering.isIrisWorldModelPass() && !context.modelRenderer && !context.isPicking();

        context.stack.push();
        context.stack.translate(centerX, centerY, 0.001F);

        Matrix4f rootMatrix = new Matrix4f(context.stack.peek().getPositionMatrix());

        context.stack.pop();

        if (defer)
        {
            Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(rootMatrix);

            ModelVAORenderer.submitColorTintOverlay(() ->
            {
                MatrixStack overlayStack = new MatrixStack();

                overlayStack.peek().getPositionMatrix().set(positionMatrix);
                this.renderLabelColorTintOverlay(overlayStack, centerX, centerY, halfX, halfY, tintSnapshot, transformSnapshot, quadSnapshot, FlatPaintOverlayPass.DEFERRED_BILLBOARD_FACTOR, FlatPaintOverlayPass.DEFERRED_BILLBOARD_UNITS);
            });
        }
        else
        {
            MatrixStack overlayStack = new MatrixStack();

            overlayStack.peek().getPositionMatrix().set(rootMatrix);
            this.renderLabelColorTintOverlay(overlayStack, centerX, centerY, halfX, halfY, tintSnapshot, transformSnapshot, quadSnapshot, FlatPaintOverlayPass.DEFAULT_FACTOR, FlatPaintOverlayPass.DEFAULT_UNITS);
        }
    }

    /**
     * Billboard-style FlatColorTint on glyph quads. Glyph positions are converted into
     * AABB-centered local space so mask scale/offset match other forms (origin at text center).
     */
    private void renderLabelColorTintOverlay(MatrixStack stack, float centerX, float centerY, float halfX, float halfY, Color formTintColor, EffectTransform colorTransform, List<LabelTextTintQuadCapture.GlyphQuad> quads, float polygonOffsetFactor, float polygonOffsetUnits)
    {
        Matrix4f tintMatrix = stack.peek().getPositionMatrix();
        MatrixStack.Entry entry = stack.peek();
        Matrix4f formRootInverse = new Matrix4f(tintMatrix).invert();

        EffectTransformMath.resolveBillboardMaskHalfExtents(colorTransform, this.maskHalfExtents, halfX, halfY);

        Map<RenderLayer, List<LabelTextTintQuadCapture.GlyphQuad>> byLayer = new LinkedHashMap<>();

        for (LabelTextTintQuadCapture.GlyphQuad quad : quads)
        {
            byLayer.computeIfAbsent(quad.layer, (layer) -> new ArrayList<>()).add(quad);
        }

        FlatColorTintOverlayPass.render(polygonOffsetFactor, polygonOffsetUnits, formRootInverse, colorTransform, false, this.maskHalfExtents, formTintColor, () ->
        {
            int tintLight = LightmapTextureManager.MAX_LIGHT_COORDINATE;
            int overlay = OverlayTexture.DEFAULT_UV;

            RenderSystem.disableCull();

            for (Map.Entry<RenderLayer, List<LabelTextTintQuadCapture.GlyphQuad>> layerEntry : byLayer.entrySet())
            {
                this.bindTextLayerTexture(layerEntry.getKey());
                /* Text RenderLayer.startDrawing replaces the FlatColorTint program — restore it. */
                BlockEffectOverlayUniforms.configureFlatColorTintOverlay(formRootInverse, colorTransform, false, this.maskHalfExtents, formTintColor);
                GlStateManager._bindTexture(this.lastBoundTextTexture);

                BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);

                for (LabelTextTintQuadCapture.GlyphQuad quad : layerEntry.getValue())
                {
                    this.fillLabelTint(builder, tintMatrix, entry, quad.x0 - centerX, quad.y0 - centerY, quad.u0, quad.v0, overlay, tintLight);
                    this.fillLabelTint(builder, tintMatrix, entry, quad.x1 - centerX, quad.y1 - centerY, quad.u1, quad.v1, overlay, tintLight);
                    this.fillLabelTint(builder, tintMatrix, entry, quad.x2 - centerX, quad.y2 - centerY, quad.u2, quad.v2, overlay, tintLight);

                    this.fillLabelTint(builder, tintMatrix, entry, quad.x0 - centerX, quad.y0 - centerY, quad.u0, quad.v0, overlay, tintLight);
                    this.fillLabelTint(builder, tintMatrix, entry, quad.x2 - centerX, quad.y2 - centerY, quad.u2, quad.v2, overlay, tintLight);
                    this.fillLabelTint(builder, tintMatrix, entry, quad.x3 - centerX, quad.y3 - centerY, quad.u3, quad.v3, overlay, tintLight);
                }

                BufferRenderer.drawWithGlobalProgram(builder.end());
            }

            RenderSystem.enableCull();
        });
    }

    private void bindTextLayerTexture(RenderLayer layer)
    {
        this.lastBoundTextTexture = 0;

        if (layer == null)
        {
            return;
        }

        layer.startDrawing();
        this.lastBoundTextTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        layer.endDrawing();
    }

    private void fillLabelTint(BufferBuilder builder, Matrix4f matrix, MatrixStack.Entry entry, float x, float y, float u, float v, int overlay, int light)
    {
        builder.vertex(matrix, x, y, 0F).color(1F, 1F, 1F, 1F).texture(u, v).overlay(overlay).light(light).normal(entry, 0F, 0F, 1F);
    }

    private void renderShadow(FormRenderingContext context, int x, int y, int w, int h)
    {
        float offset = this.form.offset.get();
        Color color = this.form.background.get().copy();

        color.mul(context.color);

        if (isFullyTransparent(color))
        {
            return;
        }

        context.stack.push();
        context.stack.translate(0, 0, -0.2F);

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        fillQuad(
            builder, context.stack,
            x + w + offset, y - offset, 0,
            x - offset, y - offset, 0,
            x - offset, y + h + offset, 0,
            x + w + offset, y + h + offset, 0,
            color.r, color.g, color.b, color.a
        );

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferRenderer.drawWithGlobalProgram(builder.end());
        context.stack.pop();
    }

    /**
     * Skip only when opacity is truly zero. Minecraft forces alpha bytes 0–3 to opaque.
     */
    private static boolean isFullyTransparent(Color color)
    {
        return color == null || color.a <= 0F;
    }

    private static int toSafeTextArgb(Color color)
    {
        int argb = color.getARGBColor();
        int alpha = (argb >>> 24) & 0xFF;

        if (color.a > 0F && alpha < MIN_TEXT_ALPHA_BYTE)
        {
            argb = (argb & 0x00FFFFFF) | (MIN_TEXT_ALPHA_BYTE << 24);
        }

        return argb;
    }
}
