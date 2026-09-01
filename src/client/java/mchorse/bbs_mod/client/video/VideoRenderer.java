package mchorse.bbs_mod.client.video;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.camera.clips.misc.VideoClip;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.clips.Clip;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.watermedia.api.player.PlayerAPI;
import org.watermedia.api.player.videolan.VideoPlayer;
import org.watermedia.videolan4j.factory.MediaPlayerFactory;

public class VideoRenderer
{
    private static class PlayerWrapper
    {
        public VideoPlayer player;
        public long lastBbsTime = -1;
        public long lastSeekTime = 0;
        public Boolean wasPlaying = null;
        public int lastVolume = -1;
        public Boolean lastLoops = null;
        public long lastVideoTime = -1;
        public long lastRenderTime = 0;
        public int lastWidth;
        public int lastHeight;

        public PlayerWrapper(VideoPlayer player)
        {
            this.player = player;
        }
    }

    private static final Map<String, PlayerWrapper> PLAYERS = new HashMap<>();
    private static MediaPlayerFactory FACTORY;
    private static boolean factoryFailed;
    /* At most one VideoForm path fully decodes; extras show a still frame. */
    private static final int MAX_LIVE_FORM_VIDEOS = 1;
    private static long formPreferFrameMs;
    private static String formPreferredPath;
    private static float formPreferredDistSq = Float.MAX_VALUE;
    private static final Set<String> formLivePaths = new HashSet<>();
    private static final Set<String> formStillKick = new HashSet<>();

    public static boolean isAvailable()
    {
        return !factoryFailed && resolveFactory() != null;
    }

    /**
     * Prefer a BBS software-decode VLC factory.
     * WaterMedia's shared default enables {@code d3d11va}/{@code dxva2}, which can
     * hard-kill the JVM (Invalid memory access) with no Minecraft crash report.
     */
    private static MediaPlayerFactory resolveFactory()
    {
        if (factoryFailed)
        {
            return null;
        }

        try
        {
            if (!PlayerAPI.isReady())
            {
                /* VLC still extracting / not ready — retry on next frame. */
                return null;
            }

            MediaPlayerFactory soft = PlayerAPI.registerFactory("bbs:soft", new String[]{
                "--avcodec-hw=none",
                "--vout=none",
                "--no-video-title-show",
                "--quiet"
            });

            if (soft != null)
            {
                return soft;
            }

            /* Last resort only — may be unstable on some GPUs. */
            return PlayerAPI.getFactory();
        }
        catch (Throwable t)
        {
            /* Missing WaterMedia / VLC: soft-fail forever, no spam. */
            if (!(t instanceof ClassNotFoundException) && !(t instanceof NoClassDefFoundError) && !(t instanceof LinkageError))
            {
                t.printStackTrace();
            }

            factoryFailed = true;

            return null;
        }
    }

    public static void renderClip(MatrixStack stack, Batcher2D batcher, VideoClip video, int tick, boolean isRunning, Area area, UIContext context)
    {
        if (!video.enabled.get() || !video.isInside(tick))
        {
            return;
        }

        Area baseArea = area;
        int actualW = getVideoWidth(video.video.get());
        int actualH = getVideoHeight(video.video.get());

        int baseW = baseArea.w;
        int baseH = baseArea.h;

        if (actualW > 0 && actualH > 0)
        {
            float videoAspect = (float) actualW / actualH;
            float areaAspect = (float) baseArea.w / baseArea.h;

            if (videoAspect > areaAspect)
            {
                baseH = (int) (baseArea.w / videoAspect);
            }
            else
            {
                baseW = (int) (baseArea.h * videoAspect);
            }
        }

        float widthPercent = video.width.get() / 100F;
        float heightPercent = video.height.get() / 100F;

        if (video.width.get() == 0 && video.height.get() == 0)
        {
            widthPercent = 1F;
            heightPercent = 1F;
        }

        int vw = widthPercent == 0F ? 0 : Math.max(1, Math.round(baseW * Math.abs(widthPercent))) * (widthPercent < 0F ? -1 : 1);
        int vh = heightPercent == 0F ? 0 : Math.max(1, Math.round(baseH * Math.abs(heightPercent))) * (heightPercent < 0F ? -1 : 1);

        int vx = baseArea.x + (baseArea.w - vw) / 2 + video.x.get();
        int vy = baseArea.y + (baseArea.h - vh) / 2 + video.y.get();

        batcher.flush();

        render(stack,
            video.video.get(),
            tick - Math.round(video.tick.get()) + video.offset.get(),
            isRunning,
            video.volume.get(),
            vx, vy, vw, vh, video.opacity.get(),
            video.cropX.get(), video.cropY.get(), video.cropWidth.get(), video.cropHeight.get(),
            video.loops.get());
    }

    public static void renderClips(MatrixStack stack, Batcher2D batcher, List<Clip> clips, int tick, boolean isRunning, Area viewport, Area globalArea, UIContext context, int screenWidth, int screenHeight, boolean renderGlobal)
    {
        for (Clip clip : clips)
        {
            if (clip instanceof VideoClip && clip.isInside(tick) && clip.enabled.get())
            {
                VideoClip video = (VideoClip) clip;

                if (video.global.get() != renderGlobal)
                {
                    continue;
                }

                Area baseArea = (video.global.get() && globalArea != null) ? globalArea : viewport;
                int actualW = getVideoWidth(video.video.get());
                int actualH = getVideoHeight(video.video.get());

                int baseW = baseArea.w;
                int baseH = baseArea.h;

                if (actualW > 0 && actualH > 0)
                {
                    float videoAspect = (float) actualW / actualH;
                    float areaAspect = (float) baseArea.w / baseArea.h;

                    if (videoAspect > areaAspect)
                    {
                        baseH = (int) (baseArea.w / videoAspect);
                    }
                    else
                    {
                        baseW = (int) (baseArea.h * videoAspect);
                    }
                }

                float widthPercent = video.width.get() / 100F;
                float heightPercent = video.height.get() / 100F;

                if (video.width.get() == 0 && video.height.get() == 0)
                {
                    widthPercent = 1F;
                    heightPercent = 1F;
                }

                int vw = widthPercent == 0F ? 0 : Math.max(1, Math.round(baseW * Math.abs(widthPercent))) * (widthPercent < 0F ? -1 : 1);
                int vh = heightPercent == 0F ? 0 : Math.max(1, Math.round(baseH * Math.abs(heightPercent))) * (heightPercent < 0F ? -1 : 1);

                int vx = baseArea.x + (baseArea.w - vw) / 2 + video.x.get();
                int vy = baseArea.y + (baseArea.h - vh) / 2 + video.y.get();

                if (!video.global.get())
                {
                    if (context != null)
                    {
                        batcher.clip(viewport, context);
                    }
                    else
                    {
                        batcher.clip(viewport.x, viewport.y, viewport.w, viewport.h, screenWidth, screenHeight);
                    }
                }
                else
                {
                    batcher.flush();
                }

                render(stack,
                    video.video.get(),
                    tick - Math.round(video.tick.get()) + video.offset.get(),
                    isRunning,
                    video.volume.get(),
                    vx, vy, vw, vh, video.opacity.get(),
                    video.cropX.get(), video.cropY.get(), video.cropWidth.get(), video.cropHeight.get(),
                    video.loops.get());

                if (!video.global.get())
                {
                    if (context != null)
                    {
                        batcher.unclip(context);
                    }
                    else
                    {
                        batcher.unclip(screenWidth, screenHeight);
                    }
                }
            }
        }
    }

    private static String resolveVideoPath(String path)
    {
        File file = VideoFormPlayback.resolveFile(path);

        return file == null ? null : file.getAbsolutePath();
    }

    public static File getResolvedVideoFile(String path)
    {
        String resolved = resolveVideoPath(path);

        if (resolved == null || resolved.isEmpty())
        {
            return null;
        }

        File file = new File(resolved);

        return file.exists() ? file : null;
    }

    public static void render(MatrixStack stack, String path, long position, boolean playing, int volume, int x, int y, int w, int h, float opacity, int cropX, int cropY, int cropWidth, int cropHeight, boolean loops)
    {
        FrameInfo frame = prepareFrame(path, position, playing, loops, volume);

        if (frame == null || frame.textureId <= 0)
        {
            return;
        }

        int vw = frame.width;
        int vh = frame.height;

        if (w == 0 || h == 0)
        {
            if (vw > 0 && vh > 0)
            {
                /* Caller supplies container size; keep prior stretch behavior. */
            }
        }

        /* Recorte por lados (izq/arr/der/abajo) y ajuste de tamaño para evitar estirar. */
        float left = Math.max(0F, Math.min(1F, cropX / 100F));
        float top = Math.max(0F, Math.min(1F, cropY / 100F));
        float right = Math.max(0F, Math.min(1F, cropWidth / 100F));
        float bottom = Math.max(0F, Math.min(1F, cropHeight / 100F));

        float u0 = left;
        float v0 = top;
        float u1 = 1F - right;
        float v1 = 1F - bottom;

        float cropWidthPercent = u1 - u0;
        float cropHeightPercent = v1 - v0;

        if (cropWidthPercent <= 0F || cropHeightPercent <= 0F)
        {
            return;
        }

        int wSign = w < 0 ? -1 : 1;
        int hSign = h < 0 ? -1 : 1;
        int absW = Math.abs(w);
        int absH = Math.abs(h);
        int drawW = Math.round(absW * cropWidthPercent) * wSign;
        int drawH = Math.round(absH * cropHeightPercent) * hSign;

        if (drawW == 0 || drawH == 0)
        {
            return;
        }

        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, frame.textureId);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, opacity);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        Matrix4f matrix = stack.peek().getPositionMatrix();

        /* Desplazar por recorte de izquierda/arriba para mantener el contenido en su lugar. */
        int drawX = x + Math.round(absW * left) * wSign;
        int drawY = y + Math.round(absH * top) * hSign;

        buffer.vertex(matrix, drawX, drawY + drawH, 0).texture(u0, v1);
        buffer.vertex(matrix, drawX + drawW, drawY + drawH, 0).texture(u1, v1);
        buffer.vertex(matrix, drawX + drawW, drawY, 0).texture(u1, v0);
        buffer.vertex(matrix, drawX, drawY, 0).texture(u0, v0);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Ensure a WaterMedia player is synced to the given film/entity tick and return its GL texture.
     * Volume 0 mutes audio (used by VideoForm).
     */
    public static FrameInfo prepareFrame(String path, long tickPosition, boolean playing, boolean loops, int volume)
    {
        return prepareFrame(path, tickPosition, playing, loops, volume, false, false);
    }

    private static FrameInfo prepareFrame(String path, long tickPosition, boolean playing, boolean loops, int volume, boolean formMode)
    {
        return prepareFrame(path, tickPosition, playing, loops, volume, formMode, false);
    }

    /**
     * VideoForm path: muted VLC. Only the closest form stays live; others peek a still.
     */
    public static FrameInfo prepareFormFrame(String path, long tickPosition, boolean loops)
    {
        return prepareFormFrame(path, tickPosition, loops, 0F, 0, true, false);
    }

    public static FrameInfo prepareFormFrame(String path, long tickPosition, boolean loops, float distanceSq)
    {
        return prepareFormFrame(path, tickPosition, loops, distanceSq, 0, true, false);
    }

    public static FrameInfo prepareFormFrame(String path, long tickPosition, boolean loops, float distanceSq, int maxLongSide)
    {
        return prepareFormFrame(path, tickPosition, loops, distanceSq, maxLongSide, true, false);
    }

    /**
     * @param playing when false, VLC is paused and seeks to {@code tickPosition}
     * @param filmSync when true, keep VLC locked to film ticks (no free-run)
     */
    public static FrameInfo prepareFormFrame(String path, long tickPosition, boolean loops, float distanceSq, int maxLongSide, boolean playing, boolean filmSync)
    {
        String resolved = resolveVideoPath(path);

        if (resolved == null || resolved.isEmpty())
        {
            return null;
        }

        long frameMs = System.currentTimeMillis() / 50L;

        if (frameMs != formPreferFrameMs)
        {
            formPreferFrameMs = frameMs;
            formPreferredPath = null;
            formPreferredDistSq = Float.MAX_VALUE;
            formLivePaths.clear();
        }

        if (distanceSq < formPreferredDistSq)
        {
            /* Closer form wins — pause any previously preferred different path this frame. */
            if (formPreferredPath != null && !formPreferredPath.equals(resolved))
            {
                pauseFormPlayer(formPreferredPath);
                formLivePaths.remove(formPreferredPath);
            }

            formPreferredDistSq = distanceSq;
            formPreferredPath = resolved;
        }

        boolean mayLive = resolved.equals(formPreferredPath)
            && (formLivePaths.isEmpty()
                || formLivePaths.contains(resolved)
                || formLivePaths.size() < MAX_LIVE_FORM_VIDEOS);

        if (!mayLive)
        {
            pauseFormPlayer(resolved);

            FrameInfo peeked = peekFormFrame(path);

            if (peeked != null && !filmSync)
            {
                return peeked;
            }

            /* Cold still / film scrub: seek paused to the requested tick. */
            return prepareFrame(path, tickPosition, false, loops, 0, true, filmSync);
        }

        formLivePaths.add(resolved);

        /* Skip GPU blit on the live path — per-frame 4K→720 blit often costs more than it saves.
         * Resolution presets still scale ffmpeg decode; WaterMedia keeps native upload. */
        return prepareFrame(path, tickPosition, playing, loops, 0, true, filmSync);
    }

    /**
     * True when another VideoForm path is already decoding live (blocks ffmpeg doubling load).
     */
    public static boolean isOtherFormVideoLive(String path)
    {
        String resolved = resolveVideoPath(path);

        if (formLivePaths.isEmpty())
        {
            return false;
        }

        if (resolved == null || resolved.isEmpty())
        {
            return true;
        }

        return !formLivePaths.contains(resolved);
    }

    private static void pauseFormPlayer(String resolved)
    {
        PlayerWrapper wrapper = PLAYERS.get(resolved);

        if (wrapper != null && wrapper.player != null && Boolean.TRUE.equals(wrapper.wasPlaying))
        {
            wrapper.player.pause();
            wrapper.wasPlaying = false;
        }
    }

    /**
     * Return the current GL texture if a player already exists — no start/seek/play.
     * Used when a still was already decoded.
     */
    public static FrameInfo peekFormFrame(String path)
    {
        String resolved = resolveVideoPath(path);

        if (resolved == null || resolved.isEmpty())
        {
            return null;
        }

        PlayerWrapper wrapper = PLAYERS.get(resolved);

        if (wrapper == null || wrapper.player == null)
        {
            return null;
        }

        int texture = wrapper.player.texture();

        if (texture <= 0)
        {
            return null;
        }

        int width = wrapper.lastWidth > 0 ? wrapper.lastWidth : wrapper.player.width();
        int height = wrapper.lastHeight > 0 ? wrapper.lastHeight : wrapper.player.height();

        if (width <= 0 || height <= 0)
        {
            width = 16;
            height = 9;
        }

        return new FrameInfo(texture, width, height);
    }

    /**
     * Editor / inventory / item icon: load first frame once, keep paused (no live decode).
     */
    public static FrameInfo ensureFormStillFrame(String path, long tickPosition, boolean loops)
    {
        FrameInfo peeked = peekFormFrame(path);

        if (peeked != null)
        {
            return peeked;
        }

        String resolved = resolveVideoPath(path);

        if (resolved == null || resolved.isEmpty())
        {
            return null;
        }

        /* One kick-play so VLC uploads a frame, then stay paused. */
        if (!formStillKick.contains(resolved))
        {
            formStillKick.add(resolved);
            prepareFrame(path, tickPosition, true, loops, 0, true);
        }

        return prepareFrame(path, tickPosition, false, loops, 0, true);
    }

    private static FrameInfo prepareFrame(String path, long tickPosition, boolean playing, boolean loops, int volume, boolean formMode, boolean filmSync)
    {
        String resolved = resolveVideoPath(path);

        if (resolved == null || resolved.isEmpty())
        {
            return null;
        }

        PlayerWrapper wrapper = PLAYERS.get(resolved);
        VideoPlayer player;

        if (wrapper == null)
        {
            if (factoryFailed)
            {
                return null;
            }

            if (FACTORY == null)
            {
                FACTORY = resolveFactory();

                if (FACTORY == null)
                {
                    return null;
                }
            }

            try
            {
                /* Constructor calls native LibVLC — can throw Error (Invalid memory access), not just Exception. */
                player = new VideoPlayer(FACTORY, MinecraftClient.getInstance());
                player.start(new File(resolved).toURI());
                player.setVolume(volume);
                /* Force decode so the first form frame is not blank while dimensions are still 0. */
                player.play();
                player.setRepeatMode(loops);
                wrapper = new PlayerWrapper(player);
                wrapper.lastVolume = volume;
                wrapper.lastLoops = loops;
                wrapper.wasPlaying = true;
                PLAYERS.put(resolved, wrapper);
            }
            catch (Throwable e)
            {
                if (!(e instanceof ClassNotFoundException) && !(e instanceof NoClassDefFoundError) && !(e instanceof LinkageError))
                {
                    e.printStackTrace();
                }

                factoryFailed = true;

                return null;
            }
        }
        else
        {
            player = wrapper.player;

            if (wrapper.lastVolume != volume)
            {
                player.setVolume(volume);
                wrapper.lastVolume = volume;
            }
        }

        if (wrapper.lastLoops == null || wrapper.lastLoops != loops)
        {
            player.setRepeatMode(loops);
            wrapper.lastLoops = loops;
        }

        if (wrapper.wasPlaying == null || wrapper.wasPlaying != playing)
        {
            if (playing)
            {
                player.play();
            }
            else
            {
                player.pause();
            }

            wrapper.wasPlaying = playing;
        }

        long videoTime = player.getTime();
        long bbsTime = tickPosition * 50L;
        long systemTime = System.currentTimeMillis();
        wrapper.lastRenderTime = systemTime;
        long duration = player.getDuration();

        if (!playing)
        {
            if (wrapper.lastVideoTime != -1 && videoTime != wrapper.lastVideoTime)
            {
                if ((systemTime - wrapper.lastSeekTime) > 1000)
                {
                    player.pause();
                }
            }

            wrapper.lastVideoTime = videoTime;
        }

        if (loops && duration > 0 && !filmSync)
        {
            bbsTime = bbsTime % duration;

            if (bbsTime < 0)
            {
                bbsTime += duration;
            }
        }
        else if (filmSync && duration > 0 && bbsTime > duration)
        {
            /* Film past video end — clamp, do not wrap (avoids intro flash). */
            bbsTime = duration;
        }

        boolean shouldSeek = false;
        long seekThreshold = filmSync ? 80L : (formMode ? 2500L : 1000L);
        long seekCooldown = filmSync ? 40L : (formMode ? 5000L : 3000L);

        if (!playing)
        {
            /* Still frame: seek only when the target tick changes.
             * Chasing VLC clock drift while paused caused scrub-jitter + FPS death
             * (especially together with Alt stencil re-draws). */
            if (wrapper.lastBbsTime != bbsTime)
            {
                shouldSeek = true;
            }
        }
        else if (formMode && !filmSync)
        {
            /* Free-run in world forms — seeking from multiple forms fights and tanks FPS. */
            shouldSeek = false;
        }
        else
        {
            long diff = Math.abs(videoTime - bbsTime);

            if (loops && duration > 0 && !filmSync)
            {
                long loopDiff = Math.abs(diff - duration);
                diff = Math.min(diff, loopDiff);
            }

            if (diff > seekThreshold && (systemTime - wrapper.lastSeekTime) > seekCooldown)
            {
                shouldSeek = true;
            }
        }

        if (shouldSeek)
        {
            player.seekTo(bbsTime);
            wrapper.lastSeekTime = systemTime;
            wrapper.lastBbsTime = bbsTime;

            if (!playing)
            {
                player.pause();
                wrapper.wasPlaying = false;
            }
        }
        else if (!playing)
        {
            wrapper.lastBbsTime = bbsTime;
        }

        int texture = player.texture();

        if (texture <= 0)
        {
            return null;
        }

        int width = player.width();
        int height = player.height();

        if (width > 0 && height > 0)
        {
            wrapper.lastWidth = width;
            wrapper.lastHeight = height;
        }
        else if (wrapper.lastWidth > 0 && wrapper.lastHeight > 0)
        {
            width = wrapper.lastWidth;
            height = wrapper.lastHeight;
        }
        else
        {
            /* WaterMedia often reports 0×0 until the first decoded frame — still draw. */
            width = 16;
            height = 9;
        }

        return new FrameInfo(texture, width, height);
    }

    public static final class FrameInfo
    {
        public final int textureId;
        public final int width;
        public final int height;

        public FrameInfo(int textureId, int width, int height)
        {
            this.textureId = textureId;
            this.width = width;
            this.height = height;
        }
    }

    public static int getVideoWidth(String path)
    {
        String resolved = resolveVideoPath(path);
        PlayerWrapper wrapper = resolved == null ? null : PLAYERS.get(resolved);
        return wrapper != null && wrapper.player != null ? wrapper.player.width() : 0;
    }

    public static int getVideoHeight(String path)
    {
        String resolved = resolveVideoPath(path);
        PlayerWrapper wrapper = resolved == null ? null : PLAYERS.get(resolved);
        return wrapper != null && wrapper.player != null ? wrapper.player.height() : 0;
    }

    public static long getVideoDuration(String path)
    {
        String resolved = resolveVideoPath(path);

        if (resolved == null || resolved.isEmpty())
        {
            return 0L;
        }

        PlayerWrapper wrapper = PLAYERS.get(resolved);

        if (wrapper != null && wrapper.player != null)
        {
            return wrapper.player.getDuration();
        }

        if (factoryFailed)
        {
            return 0L;
        }

        if (FACTORY == null)
        {
            FACTORY = resolveFactory();

            if (FACTORY == null)
            {
                return 0L;
            }
        }

        try
        {
            VideoPlayer player = new VideoPlayer(FACTORY, MinecraftClient.getInstance());
            player.start(new File(resolved).toURI());
            player.pause();

            wrapper = new PlayerWrapper(player);
            wrapper.lastRenderTime = System.currentTimeMillis();
            wrapper.wasPlaying = false;
            PLAYERS.put(resolved, wrapper);

            return player.getDuration();
        }
        catch (Throwable e)
        {
            if (!(e instanceof ClassNotFoundException) && !(e instanceof NoClassDefFoundError) && !(e instanceof LinkageError))
            {
                e.printStackTrace();
            }

            factoryFailed = true;
            return 0L;
        }
    }

    public static void releaseVideo(String path)
    {
        String resolved = resolveVideoPath(path);

        if (resolved != null && PLAYERS.containsKey(resolved))
        {
            PlayerWrapper wrapper = PLAYERS.remove(resolved);
            formStillKick.remove(resolved);

            if (wrapper != null && wrapper.player != null)
            {
                wrapper.player.release();
            }
        }
    }

    public static void update()
    {
        long now = System.currentTimeMillis();
        List<String> toRemove = new ArrayList<>();
        
        for (Map.Entry<String, PlayerWrapper> entry : PLAYERS.entrySet())
        {
            PlayerWrapper wrapper = entry.getValue();
            long diff = now - wrapper.lastRenderTime;

            if (diff > 1000)
            {
                if (wrapper.wasPlaying != null && wrapper.wasPlaying)
                {
                    wrapper.player.pause();
                    wrapper.wasPlaying = false;
                }
            }
            
            if (diff > 5000)
            {
                wrapper.player.release();
                toRemove.add(entry.getKey());
            }
        }
        
        for (String key : toRemove)
        {
            PLAYERS.remove(key);
            formStillKick.remove(key);
        }
    }

    public static void stopAll()
    {
        for (PlayerWrapper wrapper : PLAYERS.values())
        {
            if (wrapper.wasPlaying != null && wrapper.wasPlaying)
            {
                wrapper.player.pause();
                wrapper.wasPlaying = false;
            }
        }
    }

    /**
     * Release every cached VLC player but keep the factory (menu open/close cycles).
     * {@link #stopAll()} only pauses — that leaked native memory when the dashboard closed.
     */
    public static void releaseAllPlayers()
    {
        for (PlayerWrapper wrapper : PLAYERS.values())
        {
            try
            {
                if (wrapper != null && wrapper.player != null)
                {
                    wrapper.player.release();
                }
            }
            catch (Throwable t)
            {}
        }

        PLAYERS.clear();
        formStillKick.clear();
        formLivePaths.clear();
    }

    public static void cleanup()
    {
        releaseAllPlayers();

        if (FACTORY != null)
        {
            try
            {
                FACTORY.release();
            }
            catch (Throwable t)
            {}

            FACTORY = null;
        }
    }
}
