package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.clips.misc.Subtitle;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.client.screen.ColorGradeRenderer;
import mchorse.bbs_mod.graphics.Framebuffer;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.colors.Colors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

import org.joml.Matrix4f;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class UISubtitleRenderer
{
    private static Framebuffer activeTextTarget;

    public static void rebindTextTarget()
    {
        if (activeTextTarget != null)
        {
            activeTextTarget.bind();
        }
    }

    private static Framebuffer getTextFramebuffer()
    {
        return BBSModClient.getFramebuffers().getFramebuffer(Link.bbs("camera_subtitles"), (f) ->
        {
            Texture texture = BBSModClient.getTextures().createTexture(Link.bbs("test"));

            texture.setFilter(GL11.GL_NEAREST);
            texture.setWrap(GL13.GL_CLAMP_TO_EDGE);

            f.deleteTextures();
            f.attach(texture, GL30.GL_COLOR_ATTACHMENT0);

            f.unbind();
        });
    }

    public static void renderSubtitles(MatrixStack stack, Batcher2D batcher, List<Subtitle> subtitles)
    {
        if (subtitles.isEmpty())
        {
            return;
        }

        ShaderProgram program = BBSShaders.getSubtitlesProgram();
        GlUniform blur = program.getUniform("Blur");
        GlUniform textureSize = program.getUniform("TextureSize");
        net.minecraft.client.gl.Framebuffer fb = MinecraftClient.getInstance().getFramebuffer();
        int width = fb.textureWidth;
        int height = fb.textureHeight;

        /* Text-atlas FBO applyClear() shrinks glViewport; beginWrite(false) alone may not
         * restore it (same class of bug as UIFilmController stencil picking). Save so
         * Hotbar/Bossbar/Image drawn after a Subtitle keep full-frame placement. */
        int[] prevViewport = new int[4];

        GL11.glGetIntegerv(GL11.GL_VIEWPORT, prevViewport);

        batcher.flush();

        Matrix4f cache = new Matrix4f(RenderSystem.getProjectionMatrix());
        ProjectionType cacheType = RenderSystem.getProjectionType();

        /* Both passes use framebuffer coordinates, independent of the caller's GUI/world transforms. */
        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().identity();
        stack.push();
        stack.loadIdentity();

        width /= 2;
        height /= 2;

        Framebuffer framebuffer = getTextFramebuffer();
        Texture texture = framebuffer.getMainTexture();
        float depth = Math.max(1000F, Math.max(width, height) * 8F);
        Matrix4f ortho = new Matrix4f().ortho(0, width, height, 0, -depth, depth);
        FontRenderer font = Batcher2D.getVanillaTextRenderer();
        TextRenderer vanilla = MinecraftClient.getInstance().textRenderer;

        /*
         * After ColorGrade raw-GL, the first textured Minecraft draw repairs Sampler0
         * tracking. If Subtitle is the only HUD clip it would otherwise bake text with
         * texture 0 bound → black atlas. Image/Hotbar/another Subtitle hide the bug by
         * drawing a textured quad first; do that here explicitly.
         */
        fb.beginWrite(false);
        GL11.glViewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]);
        ColorGradeRenderer.resyncMinecraftState(batcher);

        RenderSystem.depthFunc(GL11.GL_ALWAYS);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        try
        {
            for (Subtitle subtitle : subtitles)
            {
                float alpha = Colors.getA(subtitle.color);

                if (alpha <= 0)
                {
                    continue;
                }

                String label = StringUtils.processColoredText(subtitle.label);
                float w = 0;
                float h = 0;
                int x = (int) (width * subtitle.windowX + subtitle.x);
                int y = (int) (height * subtitle.windowY + subtitle.y);
                float scale = subtitle.size;
                int wrapWidth = Math.max(0, Math.round(subtitle.maxWidth));

                List<String> strings = subtitle.maxWidth <= 10F ? Arrays.asList(label) : FontRenderer.wrap(vanilla, label, wrapWidth);

                for (String string : strings)
                {
                    w = Math.max(w, vanilla.getWidth(string.trim()));
                }

                h = (strings.size() - 1) * subtitle.lineHeight + vanilla.fontHeight - 2;

                int fw = (int) ((w + 10) * scale);
                int fh = (int) ((h + 10) * scale);

                if (fw <= 0 || fh <= 0)
                {
                    continue;
                }

                RenderSystem.setProjectionMatrix(new Matrix4f().ortho(0, w + 10, h + 10, 0, -100, 100), ProjectionType.ORTHOGRAPHIC);

                framebuffer.resize(fw, fh);
                bakeText(batcher, framebuffer, subtitle, strings, w, font, vanilla);

                fb.beginWrite(false);
                GL11.glViewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]);

                RenderSystem.setProjectionMatrix(ortho, ProjectionType.ORTHOGRAPHIC);

                drawSubtitle(stack, batcher, program, blur, textureSize, texture, subtitle, x, y, fw, fh, alpha);
            }
        }
        finally
        {
            /* Clear Blur so later HUD draws that reuse this program stay unaffected. */
            if (blur != null)
            {
                blur.set(0F, 0F);
            }

            batcher.flush();
            stack.pop();
            RenderSystem.getModelViewStack().popMatrix();
            fb.beginWrite(false);
            RenderSystem.setProjectionMatrix(cache, cacheType);
            GL11.glViewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]);
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.enableCull();
        }
    }

    private static void drawSubtitle(MatrixStack stack, Batcher2D batcher, ShaderProgram program, GlUniform blur, GlUniform textureSize, Texture texture, Subtitle subtitle, int x, int y, int fw, int fh, float alpha)
    {
        stack.push();

        try
        {
            stack.translate(x, y, 0);

            /* Rotate around the subtitle anchor in XYZ (same contract as Image overlays). */
            if (subtitle.rotationX != 0F)
            {
                stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(subtitle.rotationX));
            }

            if (subtitle.rotationY != 0F)
            {
                stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(subtitle.rotationY));
            }

            if (subtitle.rotation != 0F)
            {
                stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(subtitle.rotation));
            }

            if (blur != null)
            {
                blur.set(subtitle.shadow, subtitle.shadowOpaque ? 1F : 0F);
            }

            if (textureSize != null)
            {
                textureSize.set((float) texture.width, (float) texture.height);
            }

            RenderSystem.disableCull();
            RenderSystem.depthFunc(GL11.GL_ALWAYS);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);

            batcher.texturedBox(program, texture.id, Colors.setA(Colors.WHITE, alpha), -fw * subtitle.anchorX, -fh * subtitle.anchorY, texture.width, texture.height, 0, texture.height, texture.width, 0, texture.width, texture.height);
        }
        finally
        {
            stack.pop();
        }
    }

    private static void bakeText(Batcher2D batcher, Framebuffer framebuffer, Subtitle subtitle, List<String> strings, float width, FontRenderer font, TextRenderer vanilla)
    {
        Framebuffer previousTarget = activeTextTarget;

        /* Minecraft 1.21.4 text layers select MAIN_TARGET during startDrawing.
         * Rebind after layer setup so glyphs go into the subtitle texture. */
        activeTextTarget = framebuffer;

        try
        {
            GL11.glClearColor(0F, 0F, 0F, 0F);
            framebuffer.applyClear();
            RenderSystem.setShaderTexture(0, 0);

            float y = 5F;

            for (String string : strings)
            {
                string = string.trim();

                int textWidth = vanilla.getWidth(string);
                int x = 5 + (int) ((width - textWidth) / 2);

                if (Colors.getA(subtitle.backgroundColor) > 0)
                {
                    float offset = subtitle.backgroundOffset;

                    batcher.box(x - offset, y - offset, x + textWidth + offset - 1, y + vanilla.fontHeight - 2 + offset, subtitle.backgroundColor);
                }

                batcher.text(font, string, x, (int) y, Colors.setA(subtitle.color, 1F), subtitle.textShadow);
                y += subtitle.lineHeight;
            }

            batcher.flush();
        }
        finally
        {
            activeTextTarget = previousTarget;
        }
    }

    public static void renderSubtitle(MatrixStack stack, Batcher2D batcher, Subtitle subtitle)
    {
        if (subtitle == null)
        {
            return;
        }

        renderSubtitles(stack, batcher, Collections.singletonList(subtitle));
    }
}
