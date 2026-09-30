package mchorse.bbs_mod.client.video;

import mchorse.bbs_mod.camera.clips.misc.VideoClip;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.clips.Clip;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.util.math.MatrixStack;

import java.io.File;
import java.util.List;

/**
 * Soft-dependency facade for WaterMedia video playback.
 * <p>
 * {@link WaterMediaVideoRenderer} hard-references WaterMedia APIs. Loading that class
 * without the watermedia mod installed crashes with {@code NoClassDefFoundError}.
 * This facade never touches {@link WaterMediaVideoRenderer} unless watermedia is present,
 * so the rest of BBS keeps working (VideoForm falls back to ffmpeg / placeholders).
 */
public class VideoRenderer
{
    private static final String WATERMEDIA_MOD_ID = "watermedia";

    private static Boolean modPresent;

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

    private VideoRenderer()
    {}

    public static boolean isModPresent()
    {
        if (modPresent == null)
        {
            modPresent = FabricLoader.getInstance().isModLoaded(WATERMEDIA_MOD_ID);
        }

        return modPresent.booleanValue();
    }

    public static boolean isAvailable()
    {
        if (!isModPresent())
        {
            return false;
        }

        try
        {
            return WaterMediaVideoRenderer.isAvailable();
        }
        catch (Throwable t)
        {
            return false;
        }
    }

    public static void update()
    {
        if (!isModPresent())
        {
            return;
        }

        WaterMediaVideoRenderer.update();
    }

    public static void stopAll()
    {
        if (!isModPresent())
        {
            return;
        }

        WaterMediaVideoRenderer.stopAll();
    }

    public static void releaseAllPlayers()
    {
        if (!isModPresent())
        {
            return;
        }

        WaterMediaVideoRenderer.releaseAllPlayers();
    }

    public static void cleanup()
    {
        if (!isModPresent())
        {
            return;
        }

        WaterMediaVideoRenderer.cleanup();
    }

    public static void releaseVideo(String path)
    {
        if (!isModPresent())
        {
            return;
        }

        WaterMediaVideoRenderer.releaseVideo(path);
    }

    public static boolean isRemoteUrl(String path)
    {
        if (path == null)
        {
            return false;
        }

        String lower = path.toLowerCase();

        return lower.startsWith("http://")
            || lower.startsWith("https://")
            || lower.startsWith("rtmp://")
            || lower.startsWith("rtsp://")
            || lower.startsWith("hls://");
    }

    public static File getResolvedVideoFile(String path)
    {
        if (path == null || path.isEmpty())
        {
            return null;
        }

        if (isRemoteUrl(path))
        {
            return null;
        }

        File file = VideoFormPlayback.resolveFile(path);

        return file != null && file.exists() ? file : null;
    }

    public static void renderClip(MatrixStack stack, Batcher2D batcher, VideoClip video, int tick, boolean isRunning, Area area, UIContext context)
    {
        if (!isModPresent())
        {
            return;
        }

        WaterMediaVideoRenderer.renderClip(stack, batcher, video, tick, isRunning, area, context);
    }

    public static void renderClips(MatrixStack stack, Batcher2D batcher, List<Clip> clips, int tick, boolean isRunning, Area viewport, Area globalArea, UIContext context, int screenWidth, int screenHeight, boolean renderGlobal)
    {
        if (!isModPresent())
        {
            return;
        }

        WaterMediaVideoRenderer.renderClips(stack, batcher, clips, tick, isRunning, viewport, globalArea, context, screenWidth, screenHeight, renderGlobal);
    }

    public static void render(MatrixStack stack, String path, long position, boolean playing, int volume, int x, int y, int w, int h, float opacity, int cropX, int cropY, int cropWidth, int cropHeight, boolean loops)
    {
        if (!isModPresent())
        {
            return;
        }

        WaterMediaVideoRenderer.render(stack, path, position, playing, volume, x, y, w, h, opacity, cropX, cropY, cropWidth, cropHeight, loops);
    }

    public static FrameInfo prepareFrame(String path, long tickPosition, boolean playing, boolean loops, int volume)
    {
        if (!isModPresent())
        {
            return null;
        }

        return wrap(WaterMediaVideoRenderer.prepareFrame(path, tickPosition, playing, loops, volume));
    }

    public static FrameInfo prepareFormFrame(String path, long tickPosition, boolean loops)
    {
        if (!isModPresent())
        {
            return null;
        }

        return wrap(WaterMediaVideoRenderer.prepareFormFrame(path, tickPosition, loops));
    }

    public static FrameInfo prepareFormFrame(String path, long tickPosition, boolean loops, float distanceSq)
    {
        if (!isModPresent())
        {
            return null;
        }

        return wrap(WaterMediaVideoRenderer.prepareFormFrame(path, tickPosition, loops, distanceSq));
    }

    public static FrameInfo prepareFormFrame(String path, long tickPosition, boolean loops, float distanceSq, int maxLongSide)
    {
        if (!isModPresent())
        {
            return null;
        }

        return wrap(WaterMediaVideoRenderer.prepareFormFrame(path, tickPosition, loops, distanceSq, maxLongSide));
    }

    public static FrameInfo prepareFormFrame(String path, long tickPosition, boolean loops, float distanceSq, int maxLongSide, boolean playing, boolean filmSync)
    {
        if (!isModPresent())
        {
            return null;
        }

        return wrap(WaterMediaVideoRenderer.prepareFormFrame(path, tickPosition, loops, distanceSq, maxLongSide, playing, filmSync));
    }

    public static FrameInfo prepareFormFrame(String path, long tickPosition, boolean loops, float distanceSq, int maxLongSide, boolean playing, boolean filmSync, float speed, int baseVolume)
    {
        if (!isModPresent())
        {
            return null;
        }

        return wrap(WaterMediaVideoRenderer.prepareFormFrame(path, tickPosition, loops, distanceSq, maxLongSide, playing, filmSync, speed, baseVolume));
    }

    public static boolean isOtherFormVideoLive(String path)
    {
        if (!isModPresent())
        {
            return false;
        }

        return WaterMediaVideoRenderer.isOtherFormVideoLive(path);
    }

    public static FrameInfo peekFormFrame(String path)
    {
        if (!isModPresent())
        {
            return null;
        }

        return wrap(WaterMediaVideoRenderer.peekFormFrame(path));
    }

    public static FrameInfo ensureFormStillFrame(String path, long tickPosition, boolean loops)
    {
        if (!isModPresent())
        {
            return null;
        }

        return wrap(WaterMediaVideoRenderer.ensureFormStillFrame(path, tickPosition, loops));
    }

    public static int getVideoWidth(String path)
    {
        if (!isModPresent())
        {
            return 0;
        }

        return WaterMediaVideoRenderer.getVideoWidth(path);
    }

    public static int getVideoHeight(String path)
    {
        if (!isModPresent())
        {
            return 0;
        }

        return WaterMediaVideoRenderer.getVideoHeight(path);
    }

    public static long getVideoDuration(String path)
    {
        if (!isModPresent())
        {
            return 0L;
        }

        return WaterMediaVideoRenderer.getVideoDuration(path);
    }

    private static FrameInfo wrap(WaterMediaVideoRenderer.FrameInfo frame)
    {
        if (frame == null)
        {
            return null;
        }

        return new FrameInfo(frame.textureId, frame.width, frame.height);
    }
}
