package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.LabelForm;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.forms.renderers.utils.FormColorBlend;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.utils.FontUtils;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.TextureFont;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.joml.Vectors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;

import java.awt.Font;
import java.util.List;

public class LabelFormRenderer extends FormRenderer<LabelForm>
{
    private float nametagAlpha = 1F;
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
            FormColorBlend.blendFormGlowBrighten(color, glowSettings, legacyGlow);
        }

        int argb = color.getARGBColor();
        String text = StringUtils.processColoredText(this.form.text.get());

        if (text == null || text.isEmpty())
        {
            text = "Text";
        }

        FontRenderer font = context.batcher.getFont();
        int cellW = Math.max(8, x2 - x1 - 6);
        int cellH = Math.max(8, y2 - y1 - 6);
        List<String> wrap = font.wrap(text, Math.max(16, cellW * 2));
        int th = font.getHeight();
        int lineHeight = th + 2;
        int textH = th + Math.max(0, wrap.size() - 1) * lineHeight;
        int textW = 1;

        for (String s : wrap)
        {
            textW = Math.max(textW, font.getWidth(s));
        }

        /* Fit label into the thumbnail cell (cache supersamples then downscales). */
        float scale = Math.min((float) cellW / (float) textW, (float) cellH / (float) textH);

        scale = MathUtils.clamp(scale, 0.75F, 4F);

        float drawW = textW * scale;
        float drawH = textH * scale;
        float startX = (x1 + x2) / 2F - drawW / 2F;
        float y = (y1 + y2) / 2F - drawH / 2F;

        MatrixStack stack = context.batcher.getContext().getMatrices();

        stack.push();
        stack.translate(startX, y, 0F);
        stack.scale(scale, scale, 1F);

        float lineY = 0F;

        for (String s : wrap)
        {
            context.batcher.textShadow(s, 0, (int) lineY, argb);
            lineY += lineHeight;
        }

        if (glowIntensity > 0F)
        {
            Color glowColor = FormColorBlend.resolveGlowOverlayEmissionColor(glowSettings, legacyGlow, 1F, glowIntensity);
            float shaderScale = FormColorBlend.resolveGlowOverlayShaderScale(glowIntensity);

            glowColor.r *= color.r;
            glowColor.g *= color.g;
            glowColor.b *= color.b;

            int glowArgb = glowColor.getARGBColor();

            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            RenderSystem.setShaderColor(shaderScale, shaderScale, shaderScale, 1F);

            lineY = 0F;

            for (String s : wrap)
            {
                context.batcher.text(s, 0, (int) lineY, glowArgb);
                lineY += lineHeight;
            }

            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            RenderSystem.defaultBlendFunc();
        }

        stack.pop();
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
        if (shadowColor.a <= 0)
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
            int originalColor = shadowColor.getARGBColor();
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
            this.drawSimpleText(context, consumers, renderer, customFont, content, x + sx, y + sy, letterSpacing, light, shadowColor.getARGBColor());
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

        Color glowColor = FormColorBlend.resolveGlowOverlayEmissionColor(glowSettings, legacyGlow, alpha, glowIntensity);
        float shaderScale = FormColorBlend.resolveGlowOverlayShaderScale(glowIntensity);
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
        Color shadowColor = this.form.shadowColor.get().copy();
        Color color = new Color().set(context.color, true);

        color.mul(this.form.color.get());

        if (glowIntensity < 0F)
        {
            FormColorBlend.blendFormGlowBrighten(color, glowSettings, legacyGlow);
        }

        shadowColor.a *= this.nametagAlpha;
        color.a *= this.nametagAlpha;
        
        float opacity = this.form.opacity.get();
        color.a *= opacity;
        shadowColor.a *= opacity;

        shadowColor.mul(context.color);

        this.renderTextShadow(context, consumers, renderer, customFont, content, x, y, letterSpacing, light, shadowColor);

        if (this.form.outline.get())
        {
            Color outlineColor = this.form.outlineColor.get().copy();
            outlineColor.a *= opacity;
            int oc = outlineColor.getARGBColor();
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

        if (customFont != null)
        {
            int c1 = color.getARGBColor();
            int c2 = c1;

            if (this.form.gradient.get())
            {
                Color gradientColor = this.form.gradientEndColor.get().copy();
                
                gradientColor.a *= opacity;
                gradientColor.mul(context.color);
                c2 = gradientColor.getARGBColor();
            }

            customFont.draw(content, x, y, c1, c2, letterSpacing, 0F, context.stack.peek().getPositionMatrix(), consumers, light, this.form.gradientOffset.get());
        }
        else
        {
            renderer.draw(
                content,
                x,
                y,
                color.getARGBColor(), false,
                context.stack.peek().getPositionMatrix(),
                consumers,
                TextRenderer.TextLayerType.NORMAL,
                0,
                light
            );
        }

        RenderSystem.enableDepthTest();

        this.flushLabelConsumers(consumers);

        this.renderTextGlowOverlay(context, consumers, renderer, customFont, content, x, y, letterSpacing, glowSettings, legacyGlow, color.a, glowIntensity, color.getARGBColor());

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
        Color color = new Color().set(context.color, true);

        color.mul(this.form.color.get());

        if (glowIntensity < 0F)
        {
            FormColorBlend.blendFormGlowBrighten(color, glowSettings, legacyGlow);
        }
        
        float opacity = this.form.opacity.get();
        color.a *= opacity;
        shadowColor.a *= opacity;

        shadowColor.mul(context.color);
        shadowColor.a *= this.nametagAlpha;
        color.a *= this.nametagAlpha;

        int align = this.form.textAlign.get(); /* 0: Left, 1: Center, 2: Right */
        boolean anchorLines = this.form.anchorLines.get();

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
                outlineColor.a *= opacity;
                int oc = outlineColor.getARGBColor();
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

            if (customFont != null)
            {
                int c1 = color.getARGBColor();
                int c2 = c1;

                if (this.form.gradient.get())
                {
                    Color gradientColor = this.form.gradientEndColor.get().copy();
                    
                    gradientColor.a *= opacity;
                    gradientColor.mul(context.color);
                    c2 = gradientColor.getARGBColor();
                }

                customFont.draw(line, lx, y, c1, c2, letterSpacing, 0F, context.stack.peek().getPositionMatrix(), consumers, light);
            }
            else
            {
                renderer.draw(
                    line,
                    lx,
                    y,
                    color.getARGBColor(), false,
                    context.stack.peek().getPositionMatrix(),
                    consumers,
                    TextRenderer.TextLayerType.NORMAL,
                    0,
                    light
                );
            }

            y += lineHeight;
        }

        RenderSystem.enableDepthTest();

        this.flushLabelConsumers(consumers);

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

            this.renderTextGlowOverlay(context, consumers, renderer, customFont, line, lx, y, letterSpacing, glowSettings, legacyGlow, color.a, glowIntensity, color.getARGBColor());

            y += lineHeight;
        }

        this.renderShadow(context, x, shadowY, w, totalHeight);
    }

    private void renderShadow(FormRenderingContext context, int x, int y, int w, int h)
    {
        float offset = this.form.offset.get();
        Color color = this.form.background.get().copy();

        color.mul(context.color);

        if (color.a <= 0)
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
}
