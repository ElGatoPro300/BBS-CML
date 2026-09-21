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
 * Frontend for video rendering and playback in BBS.
 * Safe to load even when optional WaterMedia mod is absent.
 */
public class VideoRenderer
{
    private static Boolean available = null;

    public static boolean isAvailable()
    {
        if (available != null)
        {
            return available;
        }

        try
        {
            if (!FabricLoader.getInstance().isModLoaded("watermedia"))
            {
                available = false;
                return false;
            }

            available = WaterMediaBackend.isAvailable();
        }
        catch (Throwable t)
        {
            available = false;
        }

        return available != null && available;
    }

    public static void renderClip(MatrixStack stack, Batcher2D batcher, VideoClip video, int tick, boolean isRunning, Area area, UIContext context)
    {
        if (!isAvailable())
        {
            return;
        }

        WaterMediaBackend.renderClip(stack, batcher, video, tick, isRunning, area, context);
    }

    public static void renderClips(MatrixStack stack, Batcher2D batcher, List<Clip> clips, int tick, boolean isRunning, Area viewport, Area globalArea, UIContext context, int screenWidth, int screenHeight, boolean renderGlobal)
    {
        if (!isAvailable())
        {
            return;
        }

        WaterMediaBackend.renderClips(stack, batcher, clips, tick, isRunning, viewport, globalArea, context, screenWidth, screenHeight, renderGlobal);
    }

    public static String resolveVideoPath(String path)
    {
        if (path == null || path.isEmpty())
        {
            return null;
        }

        if (isRemoteUrl(path))
        {
            return path;
        }

        File file = VideoFormPlayback.resolveFile(path);

        return file == null ? null : file.getAbsolutePath();
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
        if (!isAvailable())
        {
            return;
        }

        WaterMediaBackend.render(stack, path, position, playing, volume, x, y, w, h, opacity, cropX, cropY, cropWidth, cropHeight, loops);
    }

    public static FrameInfo prepareFrame(String path, long tickPosition, boolean playing, boolean loops, int volume)
    {
        if (!isAvailable())
        {
            return null;
        }

        return WaterMediaBackend.prepareFrame(path, tickPosition, playing, loops, volume);
    }

    public static FrameInfo prepareFormFrame(String path, long tickPosition, boolean loops)
    {
        return prepareFormFrame(path, tickPosition, loops, 0F, 0, true, false, 1F, 0);
    }

    public static FrameInfo prepareFormFrame(String path, long tickPosition, boolean loops, float distanceSq)
    {
        return prepareFormFrame(path, tickPosition, loops, distanceSq, 0, true, false, 1F, 0);
    }

    public static FrameInfo prepareFormFrame(String path, long tickPosition, boolean loops, float distanceSq, int maxLongSide)
    {
        return prepareFormFrame(path, tickPosition, loops, distanceSq, maxLongSide, true, false, 1F, 0);
    }

    public static FrameInfo prepareFormFrame(String path, long tickPosition, boolean loops, float distanceSq, int maxLongSide, boolean playing, boolean filmSync)
    {
        return prepareFormFrame(path, tickPosition, loops, distanceSq, maxLongSide, playing, filmSync, 1F, 0);
    }

    public static FrameInfo prepareFormFrame(String path, long tickPosition, boolean loops, float distanceSq, int maxLongSide, boolean playing, boolean filmSync, float speed, int baseVolume)
    {
        if (!isAvailable())
        {
            return null;
        }

        return WaterMediaBackend.prepareFormFrame(path, tickPosition, loops, distanceSq, maxLongSide, playing, filmSync, speed, baseVolume);
    }

    public static boolean isOtherFormVideoLive(String path)
    {
        return false;
    }

    public static FrameInfo peekFormFrame(String path)
    {
        if (!isAvailable())
        {
            return null;
        }

        return WaterMediaBackend.peekFormFrame(path);
    }

    public static FrameInfo ensureFormStillFrame(String path, long tickPosition, boolean loops)
    {
        if (!isAvailable())
        {
            return null;
        }

        return WaterMediaBackend.ensureFormStillFrame(path, tickPosition, loops);
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
        if (!isAvailable())
        {
            return 0;
        }

        return WaterMediaBackend.getVideoWidth(path);
    }

    public static int getVideoHeight(String path)
    {
        if (!isAvailable())
        {
            return 0;
        }

        return WaterMediaBackend.getVideoHeight(path);
    }

    public static long getVideoDuration(String path)
    {
        if (!isAvailable())
        {
            return 0L;
        }

        return WaterMediaBackend.getVideoDuration(path);
    }

    public static void releaseVideo(String path)
    {
        if (!isAvailable())
        {
            return;
        }

        WaterMediaBackend.releaseVideo(path);
    }

    public static void update()
    {
        if (!isAvailable())
        {
            return;
        }

        WaterMediaBackend.update();
    }

    public static void stopAll()
    {
        if (!isAvailable())
        {
            return;
        }

        WaterMediaBackend.stopAll();
    }

    public static void releaseAllPlayers()
    {
        if (!isAvailable())
        {
            return;
        }

        WaterMediaBackend.releaseAllPlayers();
    }

    public static void cleanup()
    {
        releaseAllPlayers();
    }
}
