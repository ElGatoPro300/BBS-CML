package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.clips.misc.Subtitle;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.client.BBSUniform;
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
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.util.math.MatrixStack;

import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class UISubtitleRenderer
{
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

    public static void renderSubtitles(Batcher2D batcher, List<Subtitle> subtitles, int width, int height)
    {
        if (subtitles == null || subtitles.isEmpty())
        {
            return;
        }

        ShaderProgram program = BBSShaders.getSubtitlesProgram();
        Supplier<ShaderProgram> supplier = () -> program;

        /* Text-atlas FBO applyClear() shrinks glViewport; beginWrite(false) alone may not
         * restore it (same class of bug as UIFilmController stencil picking). Save so
         * Hotbar/Bossbar/Image drawn after a Subtitle keep full-frame placement. */
        int[] prevViewport = new int[4];

        GL11.glGetIntegerv(GL11.GL_VIEWPORT, prevViewport);

        RenderSystem.backupProjectionMatrix();

        Framebuffer framebuffer = getTextFramebuffer();
        Texture texture = framebuffer.getMainTexture();
        Matrix4f ortho = new Matrix4f().ortho(0, width, height, 0, -100, 100);
        FontRenderer font = Batcher2D.getVanillaTextRenderer();
        TextRenderer vanilla = MinecraftClient.getInstance().textRenderer;

        /*
         * After ColorGrade raw-GL, the first textured Minecraft draw repairs Sampler0
         * tracking. If Subtitle is the only HUD clip it would otherwise bake text with
         * texture 0 bound → black atlas. Image/Hotbar/another Subtitle hide the bug by
         * drawing a textured quad first; do that here explicitly.
         */
        BBSRendering.bindMainFramebuffer(false);
        GL11.glViewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]);
        ColorGradeRenderer.resyncMinecraftState(batcher);

        BBSRendering.depthFunc(GL11.GL_ALWAYS);
        BBSRendering.disableCull();
        BBSRendering.enableBlend();
        BBSRendering.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);

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
            int subColor = subtitle.color;
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

            BBSRendering.setProjectionMatrix(new Matrix4f().ortho(0, w + 10, h + 10, 0, -100, 100), ProjectionType.ORTHOGRAPHIC);

            framebuffer.resize(fw, fh);
            /* Transparent clear — opaque world clear-color would show as a black plate
             * if text baking still failed. */
            GL11.glClearColor(0F, 0F, 0F, 0F);
            framebuffer.applyClear();
            GlStateManager._bindTexture(0);

            float yy = 5F;

            for (String string : strings)
            {
                string = string.trim();

                int xx = 5 + (int) ((w - vanilla.getWidth(string)) / 2);

                if (Colors.getA(subtitle.backgroundColor) > 0)
                {
                    float offset = subtitle.backgroundOffset;
                    int tw = vanilla.getWidth(string);
                    int th = vanilla.fontHeight - 2;

                    batcher.box(xx - offset, yy - offset, xx + tw + offset - 1, yy + th + offset, Colors.mulA(subtitle.backgroundColor, alpha));
                    batcher.text(font, string, xx, (int) yy, Colors.setA(subColor, 1F), subtitle.textShadow);
                }
                else
                {
                    batcher.text(font, string, xx, (int) yy, Colors.setA(subColor, 1F), subtitle.textShadow);
                }

                yy += subtitle.lineHeight;
            }

            batcher.flush();

            /* Do not clear the main target — that would wipe Hotbar/Bossbar/Image already
             * drawn earlier in renderHudOverlays. Also restore viewport explicitly. */
            BBSRendering.bindMainFramebuffer(false);
            GL11.glViewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]);

            BBSRendering.setProjectionMatrix(ortho, ProjectionType.ORTHOGRAPHIC);

            Matrix3x2fStack matrices = batcher.getContext().getMatrices();

            matrices.pushMatrix();
            matrices.translate(x, y);

            if (subtitle.rotation != 0F)
            {
                matrices.rotate((float) Math.toRadians(subtitle.rotation));
            }

            batcher.texturedBox(texture, Colors.setA(Colors.WHITE, alpha), -fw * subtitle.anchorX, -fh * subtitle.anchorY, texture.width, texture.height, 0, 0, texture.width, texture.height);

            matrices.popMatrix();
        }

        RenderSystem.restoreProjectionMatrix();
        BBSRendering.enableCull();
        BBSRendering.depthFunc(GL11.GL_LEQUAL);
        BBSRendering.defaultBlendFunc();
        BBSRendering.bindProgram(BBSRendering.getPositionTexColorProgram());
    }

    public static void renderSubtitle(Batcher2D batcher, Subtitle subtitle, int width, int height)
    {
        if (subtitle == null)
        {
            return;
        }

        renderSubtitles(batcher, Collections.singletonList(subtitle), width, height);
    }

    public static void renderSubtitles(MatrixStack stack, Batcher2D batcher, List<Subtitle> subtitles)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();

        renderSubtitles(batcher, subtitles, width, height);
    }

    public static void renderSubtitle(MatrixStack stack, Batcher2D batcher, Subtitle subtitle)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();

        renderSubtitle(batcher, subtitle, width, height);
    }
}
