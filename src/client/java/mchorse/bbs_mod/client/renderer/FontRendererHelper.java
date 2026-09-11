package mchorse.bbs_mod.client.renderer;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.FormattedCharSequence;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.VertexConsumer;

public class FontRendererHelper
{
    public static void drawInBatch(Font font, String text, float x, float y, int color, boolean dropShadow, Matrix4f matrix, MultiBufferSource consumers, Font.DisplayMode displayMode, int backgroundColor, int light)
    {
        if (text == null || text.isEmpty())
        {
            return;
        }

        Font.PreparedText prepared = font.prepareText(text, x, y, color, dropShadow, 0);

        prepared.visit(new Font.GlyphVisitor()
        {
            @Override
            public void acceptGlyph(TextRenderable.Styled glyph)
            {
                RenderType layer = glyph.renderType(displayMode);
                VertexConsumer consumer = consumers.getBuffer(layer);

                glyph.render(matrix, consumer, light, dropShadow);
            }

            @Override
            public void acceptEffect(TextRenderable effect)
            {
                RenderType layer = effect.renderType(displayMode);
                VertexConsumer consumer = consumers.getBuffer(layer);

                effect.render(matrix, consumer, light, dropShadow);
            }
        });
    }

    public static void drawInBatch(Font font, FormattedCharSequence text, float x, float y, int color, boolean dropShadow, Matrix4f matrix, MultiBufferSource consumers, Font.DisplayMode displayMode, int backgroundColor, int light)
    {
        if (text == null)
        {
            return;
        }

        Font.PreparedText prepared = font.prepareText(text, x, y, color, dropShadow, false, 0);

        prepared.visit(new Font.GlyphVisitor()
        {
            @Override
            public void acceptGlyph(TextRenderable.Styled glyph)
            {
                RenderType layer = glyph.renderType(displayMode);
                VertexConsumer consumer = consumers.getBuffer(layer);

                glyph.render(matrix, consumer, light, dropShadow);
            }

            @Override
            public void acceptEffect(TextRenderable effect)
            {
                RenderType layer = effect.renderType(displayMode);
                VertexConsumer consumer = consumers.getBuffer(layer);

                effect.render(matrix, consumer, light, dropShadow);
            }
        });
    }
}
