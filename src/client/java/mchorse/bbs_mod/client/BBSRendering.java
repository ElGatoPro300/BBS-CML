package mchorse.bbs_mod.client;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.camera.clips.misc.ChromaSkyCurveSettings;
import mchorse.bbs_mod.camera.clips.misc.CurveClip;
import mchorse.bbs_mod.camera.clips.misc.HotbarClip;
import mchorse.bbs_mod.camera.clips.misc.HotbarState;
import mchorse.bbs_mod.camera.clips.misc.ImageClip;
import mchorse.bbs_mod.camera.clips.misc.ImageOverlay;
import mchorse.bbs_mod.camera.clips.misc.Subtitle;
import mchorse.bbs_mod.camera.clips.misc.SubtitleClip;
import mchorse.bbs_mod.camera.controller.CameraWorkCameraController;
import mchorse.bbs_mod.camera.controller.PlayCameraController;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.client.renderer.ModelBlockEntityRenderer;
import mchorse.bbs_mod.client.renderer.MorphRenderer;
import mchorse.bbs_mod.client.renderer.TriggerBlockEntityRenderer;
import mchorse.bbs_mod.client.screen.ScreenEffectRenderer;
import mchorse.bbs_mod.client.video.VideoRenderer;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.events.ModelBlockEntityUpdateCallback;
import mchorse.bbs_mod.events.TriggerBlockEntityUpdateCallback;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.utils.RecolorVertexConsumer;
import mchorse.bbs_mod.graphics.InverseView;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.graphics.texture.TextureFormat;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.UIHotbarRenderer;
import mchorse.bbs_mod.ui.film.UIImageRenderer;
import mchorse.bbs_mod.ui.film.UISubtitleRenderer;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIRenderingContext;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.Gizmo;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.VideoRecorder;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.iris.IrisUtils;
import mchorse.bbs_mod.utils.iris.ShaderCurves;
import mchorse.bbs_mod.utils.sodium.SodiumUtils;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.WindowFramebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;

import net.irisshaders.iris.uniforms.custom.cached.CachedUniform;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class BBSRendering
{
    /**
     * Cached rendered model blocks
     */
    public static final Set<ModelBlockEntity> capturedModelBlocks = new HashSet<>();

    public static final float TRANSLUCENT_ALPHA_DISCARD_REF = 0.1F;

    public static boolean canRender;

    public static boolean renderingWorld;
    private static boolean irisChunkLayerPass;
    public static int lastAction;

    /* Optional IRLights / IRL-editor shadow baker (no hard dependency). */
    private static final String IRL_SHADOW_BAKE_STATE = "org.qualet.irl.light.shadow.ShadowBakeState";
    private static Boolean irlShadowBakePresent;
    private static Method irlShadowBakeIsBaking;

    public static final Matrix4f camera = new Matrix4f();

    /**
     * Iris world rendering multiplies the terrain {@code positionMatrix} into the
     * {@link MatrixStack} before entity transforms.
     * {@link Matrix4f#getTranslation()} on that product no longer equals the
     * camera-relative entity offset, so callers that rebuild world space from
     * translation + camera position must strip the terrain matrix first.
     */
    public static Matrix4f stripTerrainPositionMatrix(Matrix4f composed)
    {
        Matrix4f inverse = new Matrix4f(camera);

        inverse.invert();

        Matrix4f entity = new Matrix4f();

        inverse.mul(composed, entity);

        return entity;
    }

    private static boolean customSize;
    private static boolean iris;
    private static boolean sodium;
    private static boolean optifine;

    private static int width;
    private static int height;

    private static final UIBaseMenu replayHudMenu = new UIBaseMenu() {};

    private static boolean toggleFramebuffer;
    private static Framebuffer framebuffer;
    private static Framebuffer clientFramebuffer;
    private static Texture texture;

    /** Private read FBO used to snapshot our framebuffer's colour attachment into {@link #texture}. */
    private static int captureReadFramebuffer = -1;

    private static CloudRenderMode cachedCloudRenderMode;
    private static boolean cloudsForced;

    public static int getMotionBlur()
    {
        return getMotionBlur(BBSSettings.videoSettings.frameRate.get(), getMotionBlurFactor());
    }

    public static int getMotionBlur(double fps, int target)
    {
        int i = 0;

        while (fps < target)
        {
            fps *= 2;

            i++;
        }

        return i;
    }

    public static int getMotionBlurFactor()
    {
        return getMotionBlurFactor(BBSSettings.videoSettings.motionBlur.get());
    }

    public static int getMotionBlurFactor(int integer)
    {
        return integer == 0 ? 0 : (int) Math.pow(2, 6 + integer);
    }

    public static int getVideoWidth()
    {
        return width == 0 ? BBSSettings.videoSettings.width.get() : width;
    }

    public static int getVideoHeight()
    {
        return height == 0 ? BBSSettings.videoSettings.height.get() : height;
    }

    public static int getVideoFrameRate()
    {
        int frameRate = BBSSettings.videoSettings.frameRate.get();

        return frameRate * (1 << getMotionBlur(frameRate, getMotionBlurFactor()));
    }

    public static File getVideoFolder()
    {
        File movies = new File(BBSMod.getSettingsFolder().getParentFile(), "movies");
        File exportPath = new File(BBSSettings.videoSettings.path.get());

        if (exportPath.isDirectory())
        {
            movies = exportPath;
        }

        movies.mkdirs();

        return movies;
    }

    public static boolean canReplaceFramebuffer()
    {
        return customSize && renderingWorld;
    }

    /**
     * Ensures paint overlays draw into the same framebuffer as the film viewport world pass.
     */
    public static void ensurePaintOverlayTargetFramebuffer()
    {
        if (toggleFramebuffer && framebuffer != null)
        {
            reassignFramebuffer(framebuffer);
        }
    }

    public static boolean isCustomSize()
    {
        return customSize;
    }

    public static boolean isFramebufferToggled()
    {
        return toggleFramebuffer;
    }

    public static void setCustomSize(boolean customSize)
    {
        setCustomSize(customSize, 0, 0);
    }

    public static void setCustomSize(boolean customSize, int w, int h)
    {
        BBSRendering.customSize = customSize;

        width = !customSize ? 0 : w;
        height = !customSize ? 0 : h;

        if (!customSize)
        {
            ensureMainFramebuffer();
            resizeExtraFramebuffers();
        }
    }

    /**
     * Model/trigger block panels render directly to the main framebuffer. If a film
     * session left {@link #toggleFramebuffer} enabled, the world keeps drawing offscreen
     * and only the cleared sky color is visible behind the UI.
     */
    public static void ensureMainFramebuffer()
    {
        if (!toggleFramebuffer)
        {
            return;
        }

        toggleFramebuffer(false);
    }

    public static Texture getTexture()
    {
        if (texture == null)
        {
            texture = new Texture();
            texture.setFormat(TextureFormat.RGB_U8);
            texture.setFilter(GL11.GL_NEAREST);
        }

        return texture;
    }

    public static void startTick()
    {
        capturedModelBlocks.clear();
        TriggerBlockEntityRenderer.capturedTriggerBlocks.clear();
    }

    public static void setup()
    {
        iris = FabricLoader.getInstance().isModLoaded("iris");
        sodium = FabricLoader.getInstance().isModLoaded("sodium");
        optifine = FabricLoader.getInstance().isModLoaded("optifabric");

        ModelBlockEntityUpdateCallback.EVENT.register((entity) ->
        {
            if (entity.hasWorld() && entity.getWorld().isClient())
            {
                capturedModelBlocks.add(entity);
            }
        });

        TriggerBlockEntityUpdateCallback.EVENT.register((entity) ->
        {
            if (entity.hasWorld() && entity.getWorld().isClient())
            {
                TriggerBlockEntityRenderer.capturedTriggerBlocks.add(entity);
            }
        });

        if (!iris)
        {
            return;
        }

        IrisUtils.setup();
    }

    /* Framebuffers */

    public static Framebuffer getFramebuffer()
    {
        return framebuffer;
    }

    public static void setupFramebuffer()
    {
        Window window = MinecraftClient.getInstance().getWindow();

        framebuffer = new WindowFramebuffer(window.getFramebufferWidth(), window.getFramebufferHeight());
    }

    public static void resizeExtraFramebuffers()
    {
        Set<Framebuffer> buffers = new HashSet<>();
        MinecraftClient mc = MinecraftClient.getInstance();

        buffers.add(mc.worldRenderer.getEntityOutlinesFramebuffer());
        buffers.add(mc.worldRenderer.getTranslucentFramebuffer());
        buffers.add(mc.worldRenderer.getEntityFramebuffer());
        buffers.add(mc.worldRenderer.getParticlesFramebuffer());
        buffers.add(mc.worldRenderer.getWeatherFramebuffer());
        buffers.add(mc.worldRenderer.getCloudsFramebuffer());

        for (Framebuffer buffer : buffers)
        {
            resizeFramebuffer(buffer);
        }
    }

    public static void resizeFramebuffer(Framebuffer framebuffer)
    {
        if (framebuffer == null)
        {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        int w = mc.getWindow().getFramebufferWidth();
        int h = mc.getWindow().getFramebufferHeight();

        if (framebuffer.textureWidth == w && framebuffer.textureHeight == h)
        {
            return;
        }

        framebuffer.resize(w, h);
    }

    public static void toggleFramebuffer(boolean toggleFramebuffer)
    {
        if (toggleFramebuffer == BBSRendering.toggleFramebuffer)
        {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        Window window = mc.getWindow();

        BBSRendering.toggleFramebuffer = toggleFramebuffer;

        if (toggleFramebuffer)
        {
            int w = mc.getWindow().getFramebufferWidth();
            int h = mc.getWindow().getFramebufferHeight();

            resizeExtraFramebuffers();

            if (framebuffer.textureWidth != w || framebuffer.textureHeight != h)
            {
                framebuffer.resize(w, h);
            }

            clientFramebuffer = mc.getFramebuffer();

            reassignFramebuffer(framebuffer);

            mc.worldRenderer.onResized(w, h);

            /* 1.21.11: Framebuffer.beginWrite(boolean) was removed */
        }
        else
        {
            Framebuffer target = clientFramebuffer != null ? clientFramebuffer : mc.getFramebuffer();

            /* 1.21.11: Framebuffer.beginWrite(boolean) was removed */

            int fbW = window.getFramebufferWidth();
            int fbH = window.getFramebufferHeight();

            if (width != 0 || customSize)
            {
                /* 1.21.11: Framebuffer.draw(w, h) -> blitToScreen() */
                framebuffer.blitToScreen();
            }
        }
    }

    private static void reassignFramebuffer(Framebuffer framebuffer)
    {
        MinecraftClient.getInstance().framebuffer = framebuffer;
    }

    /* Rendering */

    public static void onWorldRenderBegin()
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        BBSModClient.getFilms().startRenderFrame(mc.getRenderTickCounter().getTickProgress(false));

        UIBaseMenu menu = UIScreen.getCurrentMenu();

        if (menu != null)
        {
            menu.startRenderFrame(mc.getRenderTickCounter().getTickProgress(false));
        }

        GlStateManager._depthFunc(GL11.GL_LEQUAL);
        GlStateManager._enableDepthTest();
        GlStateManager._depthMask(true);
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);

        renderingWorld = true;
        updateCloudRenderMode(mc);
        ModelVAORenderer.clearPaintOverlayQueue();

        if (!customSize)
        {
            ensureMainFramebuffer();

            return;
        }

        toggleFramebuffer(true);
    }

    public static void onWorldRenderEnd()
    {
        ModelVAORenderer.flushPaintOverlayQueue();

        MinecraftClient mc = MinecraftClient.getInstance();
        UIBaseMenu currentMenu = UIScreen.getCurrentMenu();

        /* 1.21.11: RenderSystem.getProjectionMatrix/getProjectionType removed */
        // Matrix4f cache = new Matrix4f(RenderSystem.getProjectionMatrix());
        // ProjectionType cacheType = RenderSystem.getProjectionType();

        if (BBSModClient.getCameraController().getCurrent() instanceof PlayCameraController controller)
        {
            /* 1.21.11: DrawContext constructor changed */
            DrawContext drawContext = new DrawContext(mc, new GuiRenderState(), mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
            Batcher2D batcher = new Batcher2D(drawContext);
            Window window = mc.getWindow();
            Area area = new Area(0, 0, window.getScaledWidth(), window.getScaledHeight());
            Matrix4f ortho = new Matrix4f().ortho(0, area.w, area.h, 0, -1000, 3000);

            /* 1.21.11: RenderSystem.setProjectionMatrix removed */
            renderHudOverlays(batcher, controller.getContext(), area.w, area.h);
            /* 1.21.11: batcher.getContext().getMatrices() returns Matrix3x2fStack; use new MatrixStack() */
            VideoRenderer.renderClips(new MatrixStack(), batcher, controller.getContext().clips.getClips(controller.getContext().relativeTick), controller.getContext().relativeTick, true, area, area, null, area.w, area.h, false);

            ScreenEffectRenderer.render(batcher, controller.getContext(), area.w, area.h);

            /* 1.21.11: DrawContext.draw() removed (two-phase GUI) */
        }

        if (BBSModClient.getVideoRecorder().isRecording() && BBSModClient.getCameraController().getCurrent() instanceof CameraWorkCameraController controller)
        {
            DrawContext drawContext = new DrawContext(mc, new GuiRenderState(), mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
            Batcher2D batcher = new Batcher2D(drawContext);
            Window window = mc.getWindow();

            renderHudOverlays(batcher, controller.getContext(), window.getScaledWidth(), window.getScaledHeight());

            /* 1.21.11: DrawContext.draw() removed (two-phase GUI) */
        }

        if (!customSize)
        {
            renderingWorld = false;

            return;
        }

        if (currentMenu instanceof UIDashboard dashboard)
        {
            if (dashboard.getPanels().panel instanceof UIFilmPanel panel && panel.getData() != null)
            {
                DrawContext drawContext = new DrawContext(mc, new GuiRenderState(), mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
                Batcher2D offscreenBatcher = new Batcher2D(drawContext);

                Window window = mc.getWindow();
                Area fullScreen = new Area(0, 0, window.getScaledWidth(), window.getScaledHeight());
                Matrix4f ortho = new Matrix4f().ortho(0, window.getScaledWidth(), window.getScaledHeight(), 0, -1000, 3000);

                /* 1.21.11: RenderSystem.setProjectionMatrix removed */
                renderHudOverlays(offscreenBatcher, panel.getRunner().getContext(), fullScreen.w, fullScreen.h);
                VideoRenderer.renderClips(new MatrixStack(), offscreenBatcher, panel.getData().camera.getClips(panel.getCursor()), panel.getCursor(), panel.getRunner().isRunning(), fullScreen, fullScreen, null, window.getScaledWidth(), window.getScaledHeight(), false);

                ScreenEffectRenderer.render(offscreenBatcher, panel.getRunner().getContext(), window.getScaledWidth(), window.getScaledHeight());

                /* 1.21.11: DrawContext.draw() removed (two-phase GUI) */
            }
        }

        renderingWorld = false;
    }

    private static void updateCloudRenderMode(MinecraftClient mc)
    {
        boolean shouldHideClouds = isChromaSkyEnabled() && !isChromaSkyClouds();

        if (shouldHideClouds)
        {
            if (!cloudsForced)
            {
                cachedCloudRenderMode = mc.options.getCloudRenderMode().getValue();
                cloudsForced = true;
            }

            if (mc.options.getCloudRenderMode().getValue() != CloudRenderMode.OFF)
            {
                mc.options.getCloudRenderMode().setValue(CloudRenderMode.OFF);
            }
        }
        else if (cloudsForced)
        {
            if (cachedCloudRenderMode != null)
            {
                mc.options.getCloudRenderMode().setValue(cachedCloudRenderMode);
            }

            cloudsForced = false;
        }
    }

    public static void onRenderBeforeScreen()
    {
        /* Snapshot only when we actually redirected the world into our framebuffer this frame (film panel
         * open / recording). Outside that, mc.framebuffer was never swapped, so our framebuffer holds nothing
         * worth copying and the snapshot would just waste a per-frame GPU copy. */
        if (customSize)
        {
            Texture texture = getTexture();
            int w = framebuffer.textureWidth;
            int h = framebuffer.textureHeight;

            /* Snapshot the world that just rendered into our reassigned WindowFramebuffer into the BBS texture
             * that the film preview blits and the VideoRecorder reads back.
             *
             * 1.21.11: Framebuffer.beginWrite() was removed, so glCopyTexSubImage2D no longer has our framebuffer
             * bound as the GL read target (it would copy the desktop/window instead). Bind the colour attachment
             * to our own read FBO first, then glCopyTexSubImage2D into the (RGB8) snapshot — this also drops the
             * framebuffer's non-opaque sky alpha so the preview stays opaque. */
            if (texture.width != w || texture.height != h)
            {
                texture.bind();
                texture.setSize(w, h);
                texture.unbind();
            }

            if (captureReadFramebuffer == -1)
            {
                captureReadFramebuffer = GL30.glGenFramebuffers();
            }

            int previousRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
            int sourceId = ((GlTexture) framebuffer.getColorAttachment()).getGlId();

            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, captureReadFramebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, sourceId, 0);
            GL30.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);

            texture.bind();
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);
            texture.unbind();

            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousRead);
        }


        toggleFramebuffer(false);
    }

    public static void onRenderChunkLayer(MatrixStack stack)
    {
        /* 1.21.11: WorldRenderContextImpl and RenderSystem.getProjectionMatrix/getModelViewMatrix removed.
         * Iris support is permanently disabled; this path is dead code. */
        if (!isIrisShadersEnabled())
        {
            /* renderCoolStuff would need a reconstructed WorldRenderContext */
        }
    }

    public static void onRenderChunkLayer(Matrix4f positionMatrix, Matrix4f projectionMatrix)
    {
        /* 1.21.11: WorldRenderContextImpl removed. Iris-only path, dead code. */
        if (isIrisShadersEnabled())
        {
            /* renderCoolStuff would need a reconstructed WorldRenderContext */
        }
    }

    public static void renderHud(DrawContext drawContext, float tickDelta)
    {
        Batcher2D batcher2D = new Batcher2D(drawContext);
        VideoRecorder videoRecorder = BBSModClient.getVideoRecorder();

        BBSModClient.getFilms().renderHud(batcher2D, tickDelta);

        boolean showRecordingOverlay = videoRecorder.isRecording() && BBSSettings.recordingOverlays.get() && UIScreen.getCurrentMenu() == null;

        if (showRecordingOverlay)
        {
            int count = videoRecorder.getCounter();
            String label = UIKeys.FILM_VIDEO_RECORDING.format(
                count,
                BBSModClient.getKeyRecordVideo().getBoundKeyLocalizedText().getString()
            ).get();

            int x = 5;
            int y = 5;
            int w = batcher2D.getFont().getWidth(label);

            batcher2D.box(x, y, x + 18 + w + 3, y + 16, Colors.A50);
            batcher2D.icon(Icons.SPHERE, Colors.RED | Colors.A100, x, y);
            batcher2D.textShadow(label, x + 18, y + 4);
        }

        if (UIScreen.getCurrentMenu() == null && BBSSettings.editorReplayHud.get())
        {
            renderSelectedReplayHud(drawContext, batcher2D, showRecordingOverlay ? 20 : 0);
        }
    }

    private static void renderSelectedReplayHud(DrawContext drawContext, Batcher2D batcher2D, int yOffset)
    {
        Replay replay = BBSModClient.getSelectedReplay();

        if (replay == null)
        {
            return;
        }

        Form form = replay.form.get();
        String label = getReplayHudLabel(replay);
        boolean hasLabel = BBSSettings.editorReplayHudDisplayName.get() && !label.isEmpty();
        boolean hasForm = form != null;

        if (!hasForm && !hasLabel)
        {
            return;
        }

        int size = hasForm ? 24 : 0;
        int padding = 3;
        int gap = hasForm && hasLabel ? 4 : 0;

        int margin = 5;
        float textScale = 0.67F;
        int textWidth = hasLabel ? batcher2D.getFont().getWidth(label) : 0;
        int textHeight = hasLabel ? batcher2D.getFont().getHeight() : 0;
        int scaledTextWidth = Math.round(textWidth * textScale);
        int scaledTextHeight = Math.round(textHeight * textScale);
        int boxH = Math.max(size, scaledTextHeight) + padding * 2;
        int textBoxW = hasLabel ? scaledTextWidth + padding * 2 : 0;
        int totalW = (hasForm ? size : 0) + (hasLabel ? gap + textBoxW : 0);
        int x = getReplayHudX(margin, totalW);
        int y = getReplayHudY(margin + yOffset, boxH);
        int contentX = x + padding;
        int contentY = y + padding;

        int textBoxX = contentX + (hasForm ? size + gap : 0) - padding;
        int textBoxH = scaledTextHeight + padding * 2;
        int textBoxY = y + (boxH - textBoxH) / 2;

        if (hasLabel)
        {
            batcher2D.box(textBoxX, textBoxY, textBoxX + textBoxW, textBoxY + textBoxH, Colors.A50);
        }

        if (hasForm)
        {
            MinecraftClient mc = MinecraftClient.getInstance();
            Window window = mc.getWindow();

            replayHudMenu.resize(window.getScaledWidth(), window.getScaledHeight());
            replayHudMenu.context.setup(new UIRenderingContext(drawContext));

            int modelX1 = contentX;
            int modelY1 = contentY + (boxH - padding * 2 - size) / 2;
            int modelX2 = modelX1 + size;
            int modelY2 = modelY1 + size;

            try
            {
                FormRenderer.setSuppressFormDisplayName(true);
                FormUtilsClient.renderUI(form, replayHudMenu.context, modelX1, modelY1, modelX2, modelY2);
            }
            finally
            {
                FormRenderer.setSuppressFormDisplayName(false);
            }
        }

        if (hasLabel)
        {
            int textX = textBoxX + padding;
            int textY = textBoxY + padding;

            /* 1.21.11: drawContext.getMatrices() returns Matrix3x2fStack without push/scale/pop */
            batcher2D.textShadow(label, textX / textScale, textY / textScale);
        }
    }

    private static String getReplayHudLabel(Replay replay)
    {
        String label = replay.label.get();

        if (!label.isEmpty())
        {
            return label;
        }

        Form form = replay.form.get();

        return form == null ? "" : form.getDefaultDisplayNameForHud();
    }

    private static int getReplayHudX(int margin, int totalW)
    {
        int position = BBSSettings.editorReplayHudPosition.get();
        int screenW = MinecraftClient.getInstance().getWindow().getScaledWidth();
        boolean right = position == 1 || position == 3;

        return right ? screenW - margin - totalW : margin;
    }

    private static int getReplayHudY(int margin, int boxH)
    {
        int position = BBSSettings.editorReplayHudPosition.get();
        int screenH = MinecraftClient.getInstance().getWindow().getScaledHeight();
        boolean bottom = position == 2 || position == 3;
        int extraTopLeft = position == 0 ? 12 : 0;

        return bottom ? screenH - margin - boxH : margin + extraTopLeft;
    }

    public static void renderCoolStuff(WorldRenderContext worldRenderContext)
    {
        /* Feed the world camera orientation into the holder that replaced RenderSystem's inverse view
         * rotation matrix, so billboards and particles keep facing the camera in world space. */
        InverseView.set(new Matrix3f().rotation(MinecraftClient.getInstance().gameRenderer.getCamera().getRotation()));

        /* Draw morph forms collected during the (build-phase) entity render. AFTER_ENTITIES is the only
         * world context where the BBS immediate form pipeline lands correctly (entity queue flushed +
         * camera model-view still active). See MorphRenderer / LivingEntityRendererMorphMixin. */
        MorphRenderer.renderQueued(worldRenderContext);

        if (MinecraftClient.getInstance().currentScreen instanceof UIScreen screen)
        {
            screen.renderInWorld(worldRenderContext);
        }

        BBSModClient.getFilms().render(worldRenderContext);
    }

    public static boolean isOptifinePresent()
    {
        return optifine;
    }

    public static boolean isRenderingWorld()
    {
        return renderingWorld;
    }

    public static boolean isIrisChunkLayerPass()
    {
        return irisChunkLayerPass;
    }

    /**
     * Any Iris world draw (chunk-layer film/editor pass or entity/gbuffer pass). VAO models
     * must use the vanilla translucent program for the base pass so Iris can composite them;
     * the custom BBS model shader is only used for deferred paint/glow overlays.
     */
    public static boolean isIrisWorldModelPass()
    {
        return isIrisShadersEnabled() && isRenderingWorld();
    }

    /**
     * Iris entity/gbuffer pass (not the chunk-layer film/editor hook). Used to decide whether
     * paint overlays run immediately or are queued for {@code WorldRenderEvents.LAST}.
     */
    public static boolean isIrisDeferredModelPass()
    {
        return isIrisWorldModelPass() && !isIrisChunkLayerPass();
    }

    /**
     * When true, VAO model paint must not be applied in the base pass; use the BBS model
     * shader overlay ({@link ModelVAORenderer#submitPaintOverlay})
     * so paint matches the no-shader path under an active Iris shader pack.
     */
    public static boolean isIrisWorldPaintDeferral()
    {
        return isIrisWorldModelPass();
    }

    public static boolean isIrisLoaded()
    {
        return iris;
    }

    public static boolean isIrisShadersEnabled()
    {
        if (!iris)
        {
            return false;
        }

        return IrisUtils.isShaderPackEnabled();
    }

    public static void toggleShaders()
    {
        if (!iris)
        {
            return;
        }

        IrisUtils.toggleShaders();
    }

    public static void openShaderPackScreen()
    {
        if (!iris)
        {
            return;
        }

        IrisUtils.openShaderPackScreen();
    }

    /**
     * True while any depth/shadow bake is drawing casters with light-space matrices.
     * Includes Iris's shadow pass and optional IRLights ({@code ShadowBakeState}) so
     * color/paint/grade overlays are not queued with light projections and flushed onto
     * the film color buffer (side-of-screen tint masks when a light touches an actor).
     */
    public static boolean isIrisShadowPass()
    {
        if (isIrlShadowBakePass())
        {
            return true;
        }

        if (!iris)
        {
            return false;
        }

        return IrisUtils.isShadowPass();
    }

    /**
     * IRLights / IRL-editor bake forms into per-light depth maps outside Iris's
     * {@code isRenderingShadowPass()}. Detected via reflection so BBS stays optional.
     */
    public static boolean isIrlShadowBakePass()
    {
        if (irlShadowBakePresent == Boolean.FALSE)
        {
            return false;
        }

        try
        {
            if (irlShadowBakePresent == null)
            {
                Class<?> bakeState = Class.forName(IRL_SHADOW_BAKE_STATE);

                irlShadowBakeIsBaking = bakeState.getMethod("isBaking");
                irlShadowBakePresent = Boolean.TRUE;
            }

            Object baking = irlShadowBakeIsBaking.invoke(null);

            return baking instanceof Boolean && (Boolean) baking;
        }
        catch (Throwable t)
        {
            irlShadowBakePresent = Boolean.FALSE;
            irlShadowBakeIsBaking = null;

            return false;
        }
    }

    public static void trackTexture(Texture texture)
    {
        if (!iris)
        {
            return;
        }

        IrisUtils.trackTexture(texture);
    }

    public static void setPBRTextureIntensity(float normalIntensity, float specularIntensity)
    {
        if (!iris)
        {
            return;
        }

        IrisUtils.setPBRTextureIntensity(normalIntensity, specularIntensity);
    }

    public static void clearPBRTextureIntensity()
    {
        if (!iris)
        {
            return;
        }

        IrisUtils.clearPBRTextureIntensity();
    }

    public static float[] calculateTangents(float[] t, float[] v, float[] n, float[] u)
    {
        if (!iris)
        {
            return t;
        }

        return IrisUtils.calculateTangents(t, v, n, u);
    }

    public static float[] calculateTangents(float[] v, float[] n, float[] u)
    {
        if (!iris)
        {
            return v;
        }

        return IrisUtils.calculateTangents(v, n, u);
    }

    public static void addUniforms(List<CachedUniform> list, Map<String, ShaderCurves.ShaderVariable> variableMap)
    {
        if (!iris)
        {
            return;
        }

        IrisUtils.addUniforms(list, variableMap);
    }

    public static List<String> getShadersSliderOptions()
    {
        if (!iris)
        {
            return Collections.emptyList();
        }

        return IrisUtils.getSliderProperties();
    }

    public static Map<String, String> getShadersLanguageMap(String language)
    {
        if (!iris)
        {
            return Collections.emptyMap();
        }

        return IrisUtils.getShadersLanguageMap(language);
    }

    /* Curves */

    private static Double getCurveValue(String key)
    {
        if (!MinecraftClient.getInstance().isOnThread())
        {
            return null;
        }

        if (BBSModClient.getCameraController().getCurrent() instanceof CameraWorkCameraController controller)
        {
            Map<String, Double> values = CurveClip.getValues(controller.getContext());

            return values != null ? values.get(key) : null;
        }

        return null;
    }

    public static boolean isImmersiveWorldPanel()
    {
        UIBaseMenu menu = UIScreen.getCurrentMenu();

        if (!(menu instanceof UIDashboard dashboard))
        {
            return false;
        }

        UIDashboardPanel panel = dashboard.getPanels().panel;

        return panel != null && !panel.needsBackground();
    }

    /**
     * Chroma sky can hide terrain for film export, but model/trigger block editors must
     * always show the live world behind their UI cards.
     */
    public static boolean shouldHideChromaTerrain()
    {
        if (!isChromaSkyEnabled() || isChromaSkyTerrain())
        {
            return false;
        }

        return !isImmersiveWorldPanel();
    }

    public static boolean isChromaSkyEnabled()
    {
        ChromaSkyCurveSettings settings = getChromaSkySettings();

        return settings != null ? settings.enabled : BBSSettings.chromaSkyEnabled.get();
    }

    public static boolean isChromaSkyTerrain()
    {
        ChromaSkyCurveSettings settings = getChromaSkySettings();

        return settings != null ? settings.terrain : BBSSettings.chromaSkyTerrain.get();
    }

    public static boolean isChromaSkyClouds()
    {
        ChromaSkyCurveSettings settings = getChromaSkySettings();

        return settings != null ? settings.clouds : BBSSettings.chromaSkyClouds.get();
    }

    public static float getChromaSkyBillboard()
    {
        ChromaSkyCurveSettings settings = getChromaSkySettings();

        return settings == null ? BBSSettings.chromaSkyBillboard.get() : settings.billboard;
    }

    public static int getChromaSkyColor()
    {
        ChromaSkyCurveSettings settings = getChromaSkySettings();

        return settings == null ? BBSSettings.chromaSkyColor.get() : settings.color.getARGBColor();
    }

    private static ChromaSkyCurveSettings getChromaSkySettings()
    {
        if (getCurveValue(CurveClip.CHROMA_SKY_MARKER) == null)
        {
            return null;
        }

        if (BBSModClient.getCameraController().getCurrent() instanceof CameraWorkCameraController controller)
        {
            return CurveClip.getChromaSkySettings(controller.getContext());
        }

        return null;
    }

    public static Long getTimeOfDay()
    {
        Double v = getCurveValue(ShaderCurves.SUN_ROTATION);

        return v == null ? null : (long) (v * 1000L);
    }

    public static Double getBrightness()
    {
        return getCurveValue(ShaderCurves.BRIGHTNESS);
    }

    public static Double getWeather()
    {
        return getCurveValue(ShaderCurves.WEATHER);
    }

    public static Function<VertexConsumer, VertexConsumer> getColorConsumer(Color color)
    {
        return (b) -> new RecolorVertexConsumer(b, color);
    }

    public static Function<VertexConsumer, VertexConsumer> getColorConsumer(Color color, Color paintColor)
    {
        if (paintColor == null || paintColor.a == 0F)
        {
            return getColorConsumer(color);
        }

        if (sodium)
        {
            /* The Sodium consumer path only multiplies the vertex color; paint blending is applied on the vanilla consumer */
            return (b) -> SodiumUtils.createVertexBuffer(b, color);
        }

        return (b) -> new RecolorVertexConsumer(b, color, paintColor);
    }

    private static void renderHudOverlays(Batcher2D batcher, ClipContext context, int width, int height)
    {
        List<Subtitle> subtitles = SubtitleClip.getSubtitles(context);
        List<HotbarState> hotbars = HotbarClip.getHotbars(context);
        List<ImageOverlay> images = ImageClip.getImages(context);

        if (subtitles.isEmpty() && hotbars.isEmpty() && images.isEmpty())
        {
            return;
        }

        /* 1.21.11: depth-test state is managed via RenderPipeline; GlStateManager._disableDepthTest() removed */
        // GlStateManager._disableDepthTest();

        /* 1.21.11: batcher.getContext().getMatrices() returns Matrix3x2fStack; use new MatrixStack() */
        MatrixStack matrices = new MatrixStack();
        int subtitleIndex = 0;
        int hotbarIndex = 0;
        int imageIndex = 0;

        while (subtitleIndex < subtitles.size() || hotbarIndex < hotbars.size() || imageIndex < images.size())
        {
            int subtitleOrder = subtitleIndex < subtitles.size() ? subtitles.get(subtitleIndex).renderOrder : Integer.MAX_VALUE;
            int hotbarOrder = hotbarIndex < hotbars.size() ? hotbars.get(hotbarIndex).renderOrder : Integer.MAX_VALUE;
            int imageOrder = imageIndex < images.size() ? images.get(imageIndex).renderOrder : Integer.MAX_VALUE;

            if (subtitleOrder <= hotbarOrder && subtitleOrder <= imageOrder)
            {
                UISubtitleRenderer.renderSubtitle(matrices, batcher, subtitles.get(subtitleIndex));
                subtitleIndex += 1;
            }
            else if (hotbarOrder <= imageOrder)
            {
                UIHotbarRenderer.renderHotbar(matrices, batcher, hotbars.get(hotbarIndex), 0, 0, width, height);
                hotbarIndex += 1;
            }
            else
            {
                UIImageRenderer.renderImage(matrices, batcher, images.get(imageIndex));
                imageIndex += 1;
            }
        }

        /* 1.21.11: depth-test state is managed via RenderPipeline; GlStateManager._enableDepthTest() removed */
        // GlStateManager._enableDepthTest();
    }

    public static boolean needsIrisTranslucentModelDeferral(float alpha)
    {
        return false;
    }

    public static boolean needsIrisNoshadingOpacityDeferral(float alpha, boolean noshading)
    {
        return false;
    }

    public static float easeIrisModelAlpha(float alpha)
    {
        return alpha;
    }

    public static float easeDeferredModelAlpha(float alpha)
    {
        return alpha;
    }

    public static void applyDeferredModelHandoffRgb(Color color)
    {}

    public static boolean shapeKeyPositiveOverlay()
    {
        return false;
    }

    public static boolean shouldDeferPaintOverlayToFrameEnd()
    {
        return false;
    }
}
