package mchorse.bbs_mod.camera.clips.misc;

/**
 * Screen-space video draw request registered from {@link VideoClip#applyClip}.
 * Drawn by {@code ScreenEffectRenderer} in {@code renderOrder} with images/subtitles.
 */
public class VideoOverlay
{
    public String path = "";
    public long videoTick;
    public int volume;
    public int x;
    public int y;
    public int width;
    public int height;
    public int cropX;
    public int cropY;
    public int cropWidth;
    public int cropHeight;
    public float opacity = 1F;
    public boolean loops;
    public int renderOrder;

    public void update(String path, long videoTick, int volume, int x, int y, int width, int height,
        int cropX, int cropY, int cropWidth, int cropHeight, float opacity, boolean loops, int renderOrder)
    {
        this.path = path == null ? "" : path;
        this.videoTick = videoTick;
        this.volume = volume;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.cropX = cropX;
        this.cropY = cropY;
        this.cropWidth = cropWidth;
        this.cropHeight = cropHeight;
        this.opacity = opacity;
        this.loops = loops;
        this.renderOrder = renderOrder;
    }
}
