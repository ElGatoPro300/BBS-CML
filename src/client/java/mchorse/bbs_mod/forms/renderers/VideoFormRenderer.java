package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.client.BBSUniform;
import mchorse.bbs_mod.client.render.BufferRenderer;
import mchorse.bbs_mod.client.video.VideoFormEngine;
import mchorse.bbs_mod.client.video.VideoFormPlayback;
import mchorse.bbs_mod.client.video.VideoRenderer;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.ITickable;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.VideoForm;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.forms.renderers.utils.BillboardRenderLayers;
import mchorse.bbs_mod.forms.renderers.utils.SoftFlatFaceSort;
import mchorse.bbs_mod.graphics.texture.AdoptedTexture;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.FFMpegUtils;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.Quad;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.iris.ShaderOpacityPatch;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import mchorse.bbs_mod.client.renderer.LightTexture;
import mchorse.bbs_mod.client.renderer.Tesselator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;

import org.lwjgl.opengl.GL11;

import java.io.File;

/**
 * VideoForm quad powered by {@link VideoFormEngine} (async scaled ffmpeg).
 * Single-sided front only. WaterMedia is only a last-resort fallback.
 */
public class VideoFormRenderer extends FormRenderer<VideoForm> implements ITickable
{
    private static final Quad QUAD = new Quad();
    private static final float FACE_Z_BIAS = 0.0005F;
    /** Opaque sampler for stencil pick — picker_billboard_no_shading discards a < 0.1. */
    private static final Link PICK_PROXY_TEXTURE = Link.bbs("textures/block/white.png");
    /** Dark waiting tint — never cyan/blue “error screen”. */
    private static final int PLACEHOLDER_COLOR = 0xFF141414;
    private static final Link PLACEHOLDER_TEXTURE = Link.assets("textures/video.png");
    /** Wall-clock freeze while Minecraft pause menu is open. */
    private static long pauseFreezeMs = -1L;
    /** Wall/time anchor so play continues from the scrubbed Time value. */
    private long playAnchorTime = -1L;
    private long playAnchorWallMs = -1L;
    private int lastScrubTime = Integer.MIN_VALUE;
    private float lastSpeed = Float.NaN;
    /** Last decoded frame size — used for stencil pick quads (never decode during pick). */
    private float lastFrameW = 16F;
    private float lastFrameH = 9F;
    /** Last frame GL texture — pick samples its alpha (never decode during pick). */
    private int lastFrameTextureId = 0;

    public VideoFormRenderer(VideoForm form)
    {
        super(form);
    }

    @Override
    public void tick(IEntity entity)
    {}

    @Override
    public void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        context.batcher.flush();

        PoseStack stack = new PoseStack();

        stack.pushPose();

        try
        {
            Matrix4f uiMatrix = ModelFormRenderer.getUIMatrix(context, x1, y1, x2, y2);

            this.applyTransforms(uiMatrix, context.getTransition());
            MatrixStackUtils.multiply(stack, uiMatrix);

            String path = this.form.video.get();
            boolean hasPath = path != null && !path.isEmpty() && !path.equalsIgnoreCase("none") && !path.startsWith("<");
            float w = hasPath ? Math.max(1F, this.lastFrameW) : 412F;
            float h = hasPath ? Math.max(1F, this.lastFrameH) : 344F;
            float ratioX = w > h ? h / w : 1F;
            float fullH = ratioX;
            float translateY = 1.0F - (fullH * 0.75F);

            stack.translate(0F, translateY, 0F);
            stack.scale(1.5F, 1.5F, 1.5F);
            stack.scale(this.form.uiScale.get(), this.form.uiScale.get(), this.form.uiScale.get());

            BBSRendering.setupEntityInUiLighting();

            this.renderModel(stack, Colors.WHITE, context.getTransition(), null, true, true, null);
        }
        finally
        {
            /* WaterMedia/ffmpeg uploads can leave UNPACK_* / lightmap dirty — NeoForge then
             * blacks out every subsequent form-list thumb and creative-inventory icon. */
            BBSRendering.restoreAfterGuiItemForm();
            stack.popPose();
        }
    }

    @Override
    public void render3D(FormRenderingContext context)
    {
        if (context.isShadowPass || BBSRendering.isIrisShadowPass())
        {
            return;
        }

        /* Stencil / Alt-pick must NEVER touch WaterMedia or ffmpeg. Decoding while the
         * pick FBO is bound seeks the shared player every mouse move → FPS collapse and
         * the film preview looking like it scrubs forward/back. */
        if (context.isPicking())
        {
            this.renderPickProxy(context);

            return;
        }

        this.renderModel(context.stack, context.color, context.getTransition(), context.camera,
            false, context.modelRenderer, context);
    }

    /**
     * Cheap pick proxy — same aspect as the last decoded frame. Samples that frame's alpha
     * (no WaterMedia/ffmpeg seek) so highlight matches visible pixels, not the full quad.
     */
    private void renderPickProxy(FormRenderingContext context)
    {
        float w = Math.max(1F, this.lastFrameW);
        float h = Math.max(1F, this.lastFrameH);
        float ratioX = w > h ? h / w : 1F;
        float ratioY = h > w ? w / h : 1F;
        float halfW = 0.5F * ratioY;
        float fullH = ratioX;

        QUAD.p1.set(-halfW, fullH, 0F);
        QUAD.p2.set(halfW, fullH, 0F);
        QUAD.p3.set(-halfW, 0F, 0F);
        QUAD.p4.set(halfW, 0F, 0F);

        if (this.form.billboard.get())
        {
            Matrix4f modelMatrix = context.stack.last().pose();
            Vector3f scale = new Vector3f();

            modelMatrix.getScale(scale);
            modelMatrix.m00(1).m01(0).m02(0);
            modelMatrix.m10(0).m11(1).m12(0);
            modelMatrix.m20(0).m21(0).m22(1);

            if (context.camera != null)
            {
                modelMatrix.mul(context.camera.view);
            }

            modelMatrix.scale(scale);
            context.stack.last().normal().identity();
        }

        GlProgram pickShader = BBSShaders.getPickerBillboardNoShadingProgram();

        this.setupTarget(context, pickShader);
        Matrix4f positionMatrix = new Matrix4f(context.stack.last().pose());
        Quad localQuad = new Quad();

        localQuad.copy(QUAD);
        this.drawPickFront(positionMatrix, localQuad, pickShader, this.resolvePickTextureId());
    }

    /**
     * Prefer the last rendered frame (cached). Peek WaterMedia / ffmpeg only if needed —
     * never call prepare/bindFrame during pick (seeks the shared player).
     */
    private int resolvePickTextureId()
    {
        if (this.lastFrameTextureId > 0)
        {
            return this.lastFrameTextureId;
        }

        String path = this.form.video.get();
        boolean hasPath = path != null && !path.isEmpty() && !path.equalsIgnoreCase("none") && !path.startsWith("<");

        if (hasPath && VideoRenderer.isAvailable())
        {
            VideoRenderer.FrameInfo peeked = VideoRenderer.peekFormFrame(path);

            if (peeked != null && peeked.textureId > 0)
            {
                this.lastFrameTextureId = peeked.textureId;

                if (peeked.width >= 2 && peeked.height >= 2)
                {
                    this.lastFrameW = peeked.width;
                    this.lastFrameH = peeked.height;
                }

                return peeked.textureId;
            }
        }

        if (hasPath)
        {
            int maxLongSide = this.form.getMaxLongSide();
            VideoFormPlayback playback = VideoFormPlayback.get(path, maxLongSide);
            Texture last = playback == null ? null : playback.peekTexture();

            if (last != null && last.isValid())
            {
                this.lastFrameTextureId = last.id;

                return last.id;
            }
        }

        Texture white = BBSModClient.getTextures().getTexture(PICK_PROXY_TEXTURE);

        return white != null ? white.id : 0;
    }

    private static long playbackClockMs()
    {
        long now = System.currentTimeMillis();
        Minecraft client = Minecraft.getInstance();
        boolean paused = client != null && client.isPaused();

        if (paused)
        {
            if (pauseFreezeMs < 0L)
            {
                pauseFreezeMs = now;
            }

            return pauseFreezeMs;
        }

        if (pauseFreezeMs >= 0L)
        {
            pauseFreezeMs = -1L;
        }

        return now;
    }

    /**
     * Video frame tick.
     * <p>
     * In the film editor, time follows the film cursor (plus Time scrub), skipping spans where
     * {@code paused} keyframes are true so unpause resumes in sync with the timeline.
     * Outside the film editor, wall-clock advances from the scrub anchor.
     */
    private long getTickPosition(boolean playing, boolean filmDriven, int filmCursor, UIFilmPanel filmPanel)
    {
        int scrub = Math.max(0, this.form.time.get()) + this.form.offset.get();
        float speed = Math.max(0.01F, this.form.speed.get());

        if (filmDriven)
        {
            this.playAnchorTime = -1L;
            this.playAnchorWallMs = -1L;
            this.lastScrubTime = scrub;
            this.lastSpeed = speed;

            long activeFilmTicks = countUnpausedFilmTicks(filmPanel, filmCursor);

            return scrub + (long) (activeFilmTicks * speed);
        }

        if (!playing)
        {
            this.playAnchorTime = -1L;
            this.playAnchorWallMs = -1L;
            this.lastScrubTime = scrub;
            this.lastSpeed = speed;

            return scrub;
        }

        long now = playbackClockMs();
        boolean scrubChanged = scrub != this.lastScrubTime;
        boolean speedChanged = Float.isNaN(this.lastSpeed) || speed != this.lastSpeed;

        if (this.playAnchorTime < 0L || this.playAnchorWallMs < 0L || scrubChanged || speedChanged)
        {
            if (this.playAnchorTime >= 0L && this.playAnchorWallMs >= 0L && !scrubChanged && speedChanged)
            {
                this.playAnchorTime = this.playAnchorTime
                    + (long) ((now - this.playAnchorWallMs) / 50.0D * this.lastSpeed);
            }
            else
            {
                this.playAnchorTime = scrub;
            }

            this.playAnchorWallMs = now;
            this.lastScrubTime = scrub;
            this.lastSpeed = speed;
        }

        return this.playAnchorTime + (long) ((now - this.playAnchorWallMs) / 50.0D * speed);
    }

    /**
     * Film ticks from 0..cursor where VideoForm {@code paused} was false.
     * Pause keyframes freeze the video clock; after unpause, only post-unpause film time counts.
     */
    private static long countUnpausedFilmTicks(UIFilmPanel filmPanel, int filmCursor)
    {
        if (filmCursor <= 0)
        {
            return 0L;
        }

        KeyframeChannel<Boolean> pausedChannel = findPausedChannel(filmPanel);

        if (pausedChannel == null || pausedChannel.isEmpty())
        {
            return filmCursor;
        }

        double active = 0D;
        float prevTick = 0F;
        boolean paused = Boolean.TRUE.equals(pausedChannel.interpolate(0F, Boolean.FALSE));

        for (Keyframe<Boolean> keyframe : pausedChannel.getKeyframes())
        {
            float tick = keyframe.getTick();

            if (tick <= 0F)
            {
                paused = Boolean.TRUE.equals(keyframe.getValue());
                prevTick = Math.max(prevTick, tick);
                continue;
            }

            if (tick > filmCursor)
            {
                break;
            }

            if (!paused)
            {
                active += tick - prevTick;
            }

            paused = Boolean.TRUE.equals(keyframe.getValue());
            prevTick = tick;
        }

        if (!paused && filmCursor > prevTick)
        {
            active += filmCursor - prevTick;
        }

        return Math.max(0L, Math.round(active));
    }

    @SuppressWarnings("unchecked")
    private static KeyframeChannel<Boolean> findPausedChannel(UIFilmPanel filmPanel)
    {
        if (filmPanel == null || filmPanel.replayEditor == null)
        {
            return null;
        }

        try
        {
            Replay replay = filmPanel.replayEditor.getReplay();

            if (replay == null || replay.properties == null)
            {
                return null;
            }

            KeyframeChannel<?> channel = replay.properties.properties.get("paused");

            if (channel == null || channel.getFactory() != KeyframeFactories.BOOLEAN)
            {
                return null;
            }

            return (KeyframeChannel<Boolean>) channel;
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private static UIFilmPanel getActiveFilmPanel()
    {
        try
        {
            UIDashboard dashboard = BBSModClient.getDashboard();

            if (dashboard != null && dashboard.getPanels() != null && dashboard.getPanels().panel instanceof UIFilmPanel film)
            {
                return film;
            }
        }
        catch (Exception ignored)
        {}

        return null;
    }

    /**
     * Film editor ENTITY renders must follow the timeline; world model-blocks keep wall-clock.
     */
    private static boolean isFilmDrivenContext(FormRenderingContext context)
    {
        return context != null && context.type == FormRenderType.ENTITY && getActiveFilmPanel() != null;
    }

    private void renderModel(PoseStack matrices, int overlayColor, float transition, Camera camera,
        boolean invertY, boolean modelRenderer, FormRenderingContext deferContext)
    {
        String path = this.form.video.get();
        boolean hasPath = path != null && !path.isEmpty() && !path.equalsIgnoreCase("none") && !path.startsWith("<");
        boolean staticPreview = this.isStaticPreview(modelRenderer, deferContext);
        boolean allowFfmpegFallback = this.allowsFfmpegFallback(deferContext);
        String ffmpegPath = FFMpegUtils.getFFMPEG();
        boolean ffmpegOk = ffmpegPath != null && !ffmpegPath.isEmpty() && new File(ffmpegPath).isFile();
        boolean waterMedia = VideoRenderer.isAvailable();

        int textureId = 0;
        float w = 16F;
        float h = 9F;

        if (!hasPath)
        {
            Texture placeholder = BBSModClient.getTextures().getTexture(PLACEHOLDER_TEXTURE);

            if (placeholder != null && placeholder.id > 0)
            {
                textureId = placeholder.id;
                w = placeholder.width > 0 ? placeholder.width : 412F;
                h = placeholder.height > 0 ? placeholder.height : 344F;
            }
            else if (!staticPreview)
            {
                return;
            }
        }
        else if (staticPreview)
        {
            /* Editor / inventory / item: still frame at scrubbed Time. */
            long stillTick = Math.max(0, this.form.time.get()) + this.form.offset.get();

            if (waterMedia)
            {
                VideoRenderer.FrameInfo waterStill = VideoRenderer.ensureFormStillFrame(path, stillTick, this.form.loop.get());

                if (waterStill != null && waterStill.textureId > 0)
                {
                    textureId = waterStill.textureId;
                    w = Math.max(1, waterStill.width);
                    h = Math.max(1, waterStill.height);
                }
            }

            if (textureId <= 0 && ffmpegOk)
            {
                VideoFormEngine.Frame still = VideoFormEngine.bindStill(path, stillTick, this.form.resolution.get());

                if (still != null && still.textureId > 0)
                {
                    textureId = still.textureId;
                    w = Math.max(1, still.width);
                    h = Math.max(1, still.height);
                }
            }
        }
        else
        {
            boolean gamePaused = Minecraft.getInstance().isPaused();
            boolean formPaused = this.form.paused.get();
            boolean filmDriven = isFilmDrivenContext(deferContext);
            UIFilmPanel filmPanel = filmDriven ? getActiveFilmPanel() : null;
            int filmCursor = filmPanel == null ? 0 : filmPanel.getCursor();
            boolean filmPlaying = filmPanel != null
                && filmPanel.isRunning()
                && !filmPanel.getController().isPaused();
            /* Film stop/pause OR form Pause track → freeze. Never free-run against the timeline. */
            boolean playing = !gamePaused && !formPaused && (!filmDriven || filmPlaying);

            long tickPosition = this.getTickPosition(playing, filmDriven, filmCursor, filmPanel);
            /* Film-driven: do not independently loop — that flashes the intro mid-timeline. */
            boolean loop = filmDriven ? false : this.form.loop.get();
            float distSq = this.getDistanceSqToCamera(deferContext);
            float speed = this.form.speed.get();
            int resolutionPreset = this.form.resolution.get();
            int maxLongSide = resolutionPreset > 0 ? resolutionPreset : 0;

            /* WaterMedia v3 primary decoder (hardware-accelerated, zero-copy, dynamic LODs). */
            if (waterMedia)
            {
                VideoRenderer.FrameInfo waterFrame = VideoRenderer.prepareFormFrame(
                    path, tickPosition, loop, distSq, maxLongSide, playing, filmDriven, speed, 0);

                if (waterFrame != null && waterFrame.textureId > 0)
                {
                    textureId = waterFrame.textureId;
                    w = Math.max(1, waterFrame.width);
                    h = Math.max(1, waterFrame.height);
                }
            }

            /* Fallback to background ffmpeg engine if WaterMedia is unavailable or pending. */
            if (textureId <= 0 && ffmpegOk)
            {
                VideoFormEngine.Frame engineFrame = VideoFormEngine.bindFrame(
                    path, tickPosition, speed, loop, this.form.resolution.get(), playing);

                if (engineFrame != null && engineFrame.textureId > 0)
                {
                    textureId = engineFrame.textureId;
                    w = Math.max(1, engineFrame.width);
                    h = Math.max(1, engineFrame.height);
                }
            }

            if (textureId <= 0 && allowFfmpegFallback && ffmpegOk)
            {
                VideoFormPlayback playback = VideoFormPlayback.get(path, maxLongSide);
                Texture ffmpegTexture = playback == null ? null : playback.ensureFrame(tickPosition, speed, loop);

                if (playback != null && playback.getWidth() > 0 && playback.getHeight() > 0)
                {
                    w = playback.getWidth();
                    h = playback.getHeight();
                }

                if (ffmpegTexture != null && ffmpegTexture.isValid())
                {
                    textureId = ffmpegTexture.id;
                }
                else if (!playing && playback != null)
                {
                    Texture last = playback.peekTexture();

                    if (last != null && last.isValid())
                    {
                        textureId = last.id;
                    }
                }
            }
        }

        if (textureId > 0)
        {
            this.lastFrameTextureId = textureId;

            if (w >= 2F && h >= 2F)
            {
                this.lastFrameW = w;
                this.lastFrameH = h;
            }
        }

        float ratioX = w > h ? h / w : 1F;
        float ratioY = h > w ? w / h : 1F;
        float halfW = 0.5F * ratioY;
        float fullH = ratioX;

        QUAD.p1.set(-halfW, fullH, 0F);
        QUAD.p2.set(halfW, fullH, 0F);
        QUAD.p3.set(-halfW, 0F, 0F);
        QUAD.p4.set(halfW, 0F, 0F);

        if (this.form.billboard.get() && (deferContext == null || !deferContext.modelRenderer))
        {
            Matrix4f modelMatrix = matrices.last().pose();
            Vector3f scale = new Vector3f();

            modelMatrix.getScale(scale);

            if (invertY)
            {
                scale.y = -scale.y;
            }

            modelMatrix.m00(1).m01(0).m02(0);
            modelMatrix.m10(0).m11(1).m12(0);
            modelMatrix.m20(0).m21(0).m22(1);

            if (camera != null && !modelRenderer)
            {
                modelMatrix.mul(camera.view);
            }

            modelMatrix.scale(scale);
            matrices.last().normal().identity();
        }

        Color tint = this.resolveTint(overlayColor);

        if (tint.a <= 0.001F)
        {
            return;
        }

        /* Iris world pass: keep entity-local stack verts; live ModelView is the camera.
         * Do NOT bake ModelView into the matrix (that was only for deferred identity-MV flush). */
        Matrix4f positionMatrix = new Matrix4f(matrices.last().pose());
        Color tintSnapshot = tint.copy();
        Quad localQuad = new Quad();

        localQuad.copy(QUAD);

        int textureIdSnapshot = textureId;
        boolean linear = this.form.linear.get();
        boolean picking = deferContext != null && deferContext.isPicking();
        GlProgram pickShader = null;
        int lightSnapshot = deferContext != null ? deferContext.light : LightTexture.FULL_BRIGHT;
        int overlaySnapshot = deferContext != null ? deferContext.overlay : OverlayTexture.NO_OVERLAY;
        boolean guiPass = isGuiVideoPass(deferContext);

        if (picking)
        {
            pickShader = BBSShaders.getPickerBillboardNoShadingProgram();
            this.setupTarget(deferContext, pickShader);
        }

        GlProgram finalPickShader = pickShader;
        Runnable draw = () ->
        {
            if (textureIdSnapshot > 0 && !picking)
            {
                this.drawVideoFront(positionMatrix, tintSnapshot, localQuad, textureIdSnapshot, linear, true,
                    lightSnapshot, overlaySnapshot, guiPass);
            }
            else if (picking && finalPickShader != null)
            {
                this.drawPickFront(positionMatrix, localQuad, finalPickShader,
                    textureIdSnapshot > 0 ? textureIdSnapshot : this.resolvePickTextureId());
            }
            else if (staticPreview)
            {
                /* UI / inventory only — never paint a bright blue error plane in the world. */
                Color placeholder = Color.rgba(PLACEHOLDER_COLOR);

                placeholder.a = tintSnapshot.a;
                this.drawSolidFront(positionMatrix, placeholder, localQuad, null);
            }
        };

        /* Soft form opacity only (same gate as soft Billboard). Opaque video stays live with
         * depth write so soft billboards / soft flats in front occlude correctly at any
         * distance or camera angle. Soft video stays post-deferred, depthWrite false. */
        boolean softFlatWorld = deferContext != null
            && !modelRenderer
            && !deferContext.ui
            && !picking
            && textureIdSnapshot > 0
            && !BBSRendering.isIrisShadowPass()
            && !ShaderOpacityPatch.isFlushingPostDeferred()
            && !ShaderOpacityPatch.isPostDeferredPhase()
            && ShaderOpacityPatch.shouldDelayUntilPostDeferred(tintSnapshot.a);

        if (softFlatWorld)
        {
            boolean irisWorld = BBSRendering.isIrisWorldModelPass();
            boolean afterFluids = ShaderOpacityPatch.shouldFlushAfterFluids(tintSnapshot.a);
            boolean depthWrite = false;
            Matrix4f deferredMatrix = irisWorld
                ? new Matrix4f(positionMatrix)
                : ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(positionMatrix));
            /* Centroid / look-axis (BlockForm-style) — look-ray is unstable from far on
             * bottom-anchored / angled video vs other soft flats. */
            double faceSortKey = this.computeVideoFormSortKey(positionMatrix, deferContext, localQuad);
            Color capturedTint = tintSnapshot.copy();
            Quad capturedQuad = new Quad();
            int capturedTexture = textureIdSnapshot;
            boolean capturedLinear = linear;

            capturedQuad.copy(localQuad);

            boolean capturedDepthWrite = depthWrite;
            int capturedLight = lightSnapshot;
            int capturedOverlay = overlaySnapshot;

            Runnable deferredDraw = () ->
                this.drawVideoFront(deferredMatrix, capturedTint, capturedQuad, capturedTexture, capturedLinear, capturedDepthWrite,
                    capturedLight, capturedOverlay, false);

            if (irisWorld)
            {
                ShaderOpacityPatch.submitPostDeferredForm(0D, faceSortKey, depthWrite, afterFluids, deferredDraw);
            }
            else
            {
                ShaderOpacityPatch.submitPostDeferredBbsForm(0D, faceSortKey, depthWrite, afterFluids, deferredDraw);
            }

            return;
        }

        try
        {
            draw.run();
        }
        finally
        {
            /* GUI thumbs must not call restoreWorldRenderState — it re-enables lightmap/overlay
             * and poisons later form-list / creative previews (NeoForge especially). */
            if (guiPass)
            {
                this.restoreGuiVideoPassState();
            }
            else
            {
                BBSRendering.restoreWorldRenderState();
            }
        }
    }

    private static boolean isGuiVideoPass(FormRenderingContext context)
    {
        return context == null
            || context.ui
            || context.type == FormRenderType.ITEM_INVENTORY;
    }

    /**
     * Match Billboard/Framebuffer after a lit UI draw: leave lightmap/overlay off for Batcher2D
     * and subsequent morph thumbs. Also reset UNPACK from WaterMedia uploads.
     */
    private void restoreGuiVideoPassState()
    {
        BBSRendering.setShaderColor(1F, 1F, 1F, 1F);
        BBSRendering.defaultBlendFunc();
        BBSRendering.resetPixelUnpackState();
        BBSRendering.clearTextureUnit0();
    }

    private float getDistanceSqToCamera(FormRenderingContext context)
    {
        if (context == null || context.ui || context.camera == null || context.stack == null)
        {
            return 0F;
        }

        Matrix4f m = context.stack.last().pose();
        float x = m.m30();
        float y = m.m31();
        float z = m.m32();

        return x * x + y * y + z * z;
    }

    /**
     * Soft-queue key for VideoForm (farther first). Uses the face centroid in view space —
     * same numeric space as BlockForm origin / SoftFlatFaceSort {@code -hit.z}, but without the
     * camera-center look-ray that mis-orders tall or angled video planes from far away.
     * <p>
     * Film ENTITY with an absolute {@code context.world}: look-axis depth of the centroid
     * (ModelForm soft-bone contract). Billboard look-at is already baked into {@code drawMatrix}.
     */
    private double computeVideoFormSortKey(Matrix4f drawMatrix, FormRenderingContext context, Quad quad)
    {
        float cx = (quad.p1.x + quad.p2.x + quad.p3.x + quad.p4.x) * 0.25F;
        float cy = (quad.p1.y + quad.p2.y + quad.p3.y + quad.p4.y) * 0.25F;
        boolean filmEntity = context != null
            && context.type == FormRenderType.ENTITY
            && context.camera != null
            && !context.modelRenderer;

        if (filmEntity && context.world != null && !this.form.billboard.get())
        {
            Vector4f worldPoint = new Vector4f(cx, cy, FACE_Z_BIAS, 1F);

            context.world.last().pose().transform(worldPoint);

            Vector3f look = new Vector3f(0F, 0F, -1F);

            context.camera.view.transformDirection(look);

            double dx = worldPoint.x - context.camera.position.x;
            double dy = worldPoint.y - context.camera.position.y;
            double dz = worldPoint.z - context.camera.position.z;

            return dx * look.x + dy * look.y + dz * look.z - SoftFlatFaceSort.SOFT_FACE_NEAR_BIAS;
        }

        Vector4f centroid = new Vector4f(cx, cy, FACE_Z_BIAS, 1F);
        Matrix4f viewSpace = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(drawMatrix));

        viewSpace.transform(centroid);

        if (filmEntity)
        {
            return -centroid.z - SoftFlatFaceSort.SOFT_FACE_NEAR_BIAS;
        }

        return centroid.x * centroid.x + centroid.y * centroid.y + centroid.z * centroid.z;
    }

    private boolean isStaticPreview(boolean modelRenderer, FormRenderingContext context)
    {
        if (context == null)
        {
            return true;
        }

        FormRenderType type = context.type;

        /* Form editor viewport (PREVIEW): play the video so authors see frames, not a blank still. */
        if (type == FormRenderType.PREVIEW)
        {
            return false;
        }

        if (modelRenderer)
        {
            return true;
        }

        return type == FormRenderType.ITEM_INVENTORY || type == FormRenderType.ITEM;
    }

    private boolean allowsFfmpegFallback(FormRenderingContext context)
    {
        if (context == null)
        {
            return true;
        }

        FormRenderType type = context.type;

        return type == FormRenderType.ENTITY
            || type == FormRenderType.MODEL_BLOCK
            || type == FormRenderType.PREVIEW;
    }

    /**
     * Front face only — nothing on the back. Restores GL state so terrain stays valid.
     *
     * @param depthWrite {@code false} for soft-flat post-deferred draws (same contract as soft
     *                   billboards: color only, no depth punch). Live / preview paths pass
     *                   {@code true} so opaque video still occludes world geometry.
     */
    private void drawVideoFront(Matrix4f matrix, Color tint, Quad quad, int textureId, boolean linear, boolean depthWrite,
        int light, int overlay, boolean guiPass)
    {
        boolean previousCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean previousDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        /* Iris never composites bbs:video. Soft billboards use vanilla entity programs — match
         * that: cutout (opaque, tex discard) or translucent (soft form alpha). bbs:video is
         * no-shader only (cutout + FormColorGrade). */
        boolean irisComposite = BBSRendering.isIrisShadersEnabled()
            && !BBSRendering.isIrisShadowPass();

        try
        {
            if (irisComposite)
            {
                this.drawVideoFrontIris(matrix, tint, quad, textureId, depthWrite, light, overlay);
            }
            else
            {
                this.drawVideoFrontBbs(matrix, tint, quad, textureId, depthWrite);
            }
        }
        finally
        {
            BBSRendering.setShaderColor(1F, 1F, 1F, 1F);
            BBSRendering.enableCull();
            BBSRendering.defaultBlendFunc();

            if (guiPass)
            {
                this.restoreGuiVideoPassState();
            }
            else
            {
                BBSRendering.restoreWorldRenderState();
            }

            BBSRendering.depthMask(previousDepthMask);

            if (!previousCull)
            {
                BBSRendering.disableCull();
            }
        }
    }

    /**
     * Iris path: vanilla entity cutout/translucent so packs composite and texels discard (cutout)
     * or soft-blend (translucent). Vertex tint carries form alpha.
     */
    private void drawVideoFrontIris(Matrix4f matrix, Color tint, Quad quad, int textureId, boolean depthWrite,
        int light, int overlay)
    {
        boolean softForm = !depthWrite || tint.a < ShaderOpacityPatch.LIVE_DEPTH_WRITE_ALPHA;
        GlProgram shader = softForm
            ? BBSRendering.getEntityTranslucentProgram()
            : BBSRendering.getEntityCutoutProgram();

        BBSRendering.bindProgram(shader);
        BBSRendering.setShaderColor(1F, 1F, 1F, 1F);
        BBSRendering.setShaderTexture(0, textureId);
        BBSRendering.enableBlend();
        BBSRendering.defaultBlendFunc();
        BBSRendering.disableCull();
        BBSRendering.enableDepthTest();
        BBSRendering.depthMask(depthWrite);

        PoseStack stack = new PoseStack();

        stack.last().pose().set(matrix);
        stack.last().normal().identity();

        PoseStack.Pose entry = stack.last();
        BufferBuilder buffer = Tesselator.getInstance().begin(
            PrimitiveTopology.TRIANGLES, DefaultVertexFormat.ENTITY);

        this.irisVert(buffer, entry, quad.p3.x, quad.p3.y, FACE_Z_BIAS, 0F, 1F, tint, overlay, light);
        this.irisVert(buffer, entry, quad.p4.x, quad.p4.y, FACE_Z_BIAS, 1F, 1F, tint, overlay, light);
        this.irisVert(buffer, entry, quad.p2.x, quad.p2.y, FACE_Z_BIAS, 1F, 0F, tint, overlay, light);

        this.irisVert(buffer, entry, quad.p3.x, quad.p3.y, FACE_Z_BIAS, 0F, 1F, tint, overlay, light);
        this.irisVert(buffer, entry, quad.p2.x, quad.p2.y, FACE_Z_BIAS, 1F, 0F, tint, overlay, light);
        this.irisVert(buffer, entry, quad.p1.x, quad.p1.y, FACE_Z_BIAS, 0F, 0F, tint, overlay, light);

        Identifier adoptedId = AdoptedTexture.identifier(textureId, (int) Math.max(1, this.lastFrameW), (int) Math.max(1, this.lastFrameH), this.form.linear.get());
        BillboardRenderLayers.draw(buffer.buildOrThrow(), adoptedId, this.form.linear.get(), false, depthWrite, false);
    }

    /** No-shader path: bbs:video (tex discard a < 0.1 + optional FormColorGrade). */
    private void drawVideoFrontBbs(Matrix4f matrix, Color tint, Quad quad, int textureId, boolean depthWrite)
    {
        GlProgram videoProgram = BBSShaders.getVideoProgram();
        GlProgram shader = videoProgram != null
            ? videoProgram
            : BBSRendering.getPositionTexProgram();

        BBSRendering.bindProgram(shader);
        BBSRendering.setShaderColor(tint.r, tint.g, tint.b, tint.a);
        BBSRendering.setShaderTexture(0, textureId);

        if (videoProgram != null)
        {
            Color formColor = this.form.color.get();

            if (formColor != null && formColor.hasColorAdjustments())
            {
                BBSUniform.set(videoProgram, "FormColorGrade", formColor.brightness, formColor.contrast, formColor.hue, formColor.saturation);
            }
            else
            {
                BBSUniform.set(videoProgram, "FormColorGrade", 0F, 0F, 0F, 0F);
            }
        }

        try
        {
            BBSRendering.enableBlend();
            BBSRendering.defaultBlendFunc();
            BBSRendering.disableCull();
            BBSRendering.enableDepthTest();
            BBSRendering.depthMask(depthWrite);

            PoseStack stack = new PoseStack();
            stack.last().pose().set(matrix);
            stack.last().normal().identity();
            PoseStack.Pose entry = stack.last();

            BufferBuilder buffer = Tesselator.getInstance().begin(PrimitiveTopology.TRIANGLES, DefaultVertexFormat.ENTITY);

            int light = LightTexture.FULL_BRIGHT;
            int overlay = OverlayTexture.NO_OVERLAY;

            this.irisVert(buffer, entry, quad.p3.x, quad.p3.y, FACE_Z_BIAS, 0F, 1F, tint, overlay, light);
            this.irisVert(buffer, entry, quad.p4.x, quad.p4.y, FACE_Z_BIAS, 1F, 1F, tint, overlay, light);
            this.irisVert(buffer, entry, quad.p2.x, quad.p2.y, FACE_Z_BIAS, 1F, 0F, tint, overlay, light);

            this.irisVert(buffer, entry, quad.p3.x, quad.p3.y, FACE_Z_BIAS, 0F, 1F, tint, overlay, light);
            this.irisVert(buffer, entry, quad.p2.x, quad.p2.y, FACE_Z_BIAS, 1F, 0F, tint, overlay, light);
            this.irisVert(buffer, entry, quad.p1.x, quad.p1.y, FACE_Z_BIAS, 0F, 0F, tint, overlay, light);

            Identifier adoptedId = AdoptedTexture.identifier(textureId, (int) Math.max(1, this.lastFrameW), (int) Math.max(1, this.lastFrameH), this.form.linear.get());
            BillboardRenderLayers.draw(buffer.buildOrThrow(), adoptedId, this.form.linear.get(), false, depthWrite, false);
        }
        finally
        {
            if (videoProgram != null)
            {
                BBSUniform.set(videoProgram, "FormColorGrade", 0F, 0F, 0F, 0F);
            }
        }
    }

    private void drawSolidFront(Matrix4f matrix, Color color, Quad quad, GlProgram shader)
    {
        BufferBuilder buffer = Tesselator.getInstance().begin(PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        this.col(buffer, matrix, quad.p1.x, quad.p1.y, FACE_Z_BIAS, color);
        this.col(buffer, matrix, quad.p3.x, quad.p3.y, FACE_Z_BIAS, color);
        this.col(buffer, matrix, quad.p4.x, quad.p4.y, FACE_Z_BIAS, color);

        this.col(buffer, matrix, quad.p1.x, quad.p1.y, FACE_Z_BIAS, color);
        this.col(buffer, matrix, quad.p4.x, quad.p4.y, FACE_Z_BIAS, color);
        this.col(buffer, matrix, quad.p2.x, quad.p2.y, FACE_Z_BIAS, color);

        if (shader != null)
        {
            BBSRendering.bindProgram(shader);
            BufferRenderer.drawWithGlobalProgram(buffer.buildOrThrow());
        }
        else
        {
            BBSRendering.bindProgram(BBSRendering.getPositionColorProgram());
            BufferRenderer.drawWithGlobalProgram(buffer.buildOrThrow());
        }
    }

    private void irisVert(BufferBuilder buffer, PoseStack.Pose entry, float x, float y, float z,
        float u, float v, Color tint, int overlay, int light)
    {
        buffer.addVertex(entry.pose(), x, y, z)
            .setColor(tint.r, tint.g, tint.b, tint.a)
            .setUv(u, v)
            .setOverlay(overlay)
            .setLight(light)
            .setNormal(entry, 0F, 0F, 1F);
    }

    /**
     * Stencil pick draw — vertex format must match {@link BBSShaders#getPickerBillboardNoShadingProgram()}.
     * <p>
     * Sampler0 must be set via {@link RenderSystem#setShaderTexture(int, int)} (not raw GL bind)
     * so Alt multi-replay pick does not inherit the previous form's albedo. Prefer the video's
     * last frame so {@code picker_billboard_no_shading} discards fully transparent pixels.
     */
    private void drawPickFront(Matrix4f matrix, Quad quad, GlProgram shader, int textureId)
    {
        if (textureId <= 0)
        {
            return;
        }

        boolean previousCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean previousDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int light = LightTexture.FULL_BRIGHT;
        Color pickColor = Color.white();

        pickColor.a = 1F;

        try
        {
            BBSRendering.bindProgram(shader);
            BBSRendering.setShaderColor(1F, 1F, 1F, 1F);
            BBSRendering.setShaderTexture(0, textureId);
            BBSRendering.enableBlend();
            BBSRendering.defaultBlendFunc();
            BBSRendering.disableCull();
            BBSRendering.enableDepthTest();
            BBSRendering.depthMask(true);

            BufferBuilder buffer = Tesselator.getInstance().begin(PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR);

            /* Same UV winding as {@link #drawVideoFront}. */
            this.pickVert(buffer, matrix, quad.p3.x, quad.p3.y, FACE_Z_BIAS, 0F, 1F, light, pickColor);
            this.pickVert(buffer, matrix, quad.p4.x, quad.p4.y, FACE_Z_BIAS, 1F, 1F, light, pickColor);
            this.pickVert(buffer, matrix, quad.p2.x, quad.p2.y, FACE_Z_BIAS, 1F, 0F, light, pickColor);

            this.pickVert(buffer, matrix, quad.p3.x, quad.p3.y, FACE_Z_BIAS, 0F, 1F, light, pickColor);
            this.pickVert(buffer, matrix, quad.p2.x, quad.p2.y, FACE_Z_BIAS, 1F, 0F, light, pickColor);
            this.pickVert(buffer, matrix, quad.p1.x, quad.p1.y, FACE_Z_BIAS, 0F, 0F, light, pickColor);

            BufferRenderer.drawWithGlobalProgram(buffer.buildOrThrow());
        }
        finally
        {
            BBSRendering.depthMask(previousDepthMask);

            if (previousCull)
            {
                BBSRendering.enableCull();
            }
            else
            {
                BBSRendering.disableCull();
            }
        }
    }

    private void pickVert(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, float u, float v, int light, Color color)
    {
        buffer.addVertex(matrix, x, y, z).setUv(u, v).setLight(light).setColor(color.r, color.g, color.b, color.a);
    }

    private void tex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, float u, float v)
    {
        buffer.addVertex(matrix, x, y, z).setUv(u, v);
    }

    private void col(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, Color color)
    {
        buffer.addVertex(matrix, x, y, z).setColor(color.r, color.g, color.b, color.a);
    }

    private Color resolveTint(int overlayColor)
    {
        Color tint = new Color().set(overlayColor, true);
        Color storedFormColor = this.form.color.get();
        Color formColor = storedFormColor == null
            ? Color.white()
            : (BBSShaders.getVideoProgram() != null
                ? storedFormColor.copyDeferringColorGrade()
                : storedFormColor.copyBakingColorGrade());

        tint.mul(formColor);
        this.form.applyFormOpacity(tint);

        return tint;
    }
}
