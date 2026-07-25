package mchorse.bbs_mod.client.video;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.FFMpegUtils;
import mchorse.bbs_mod.utils.resources.Pixels;

import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decodes MP4 frames with ffmpeg into a GL {@link Texture} for {@code VideoForm}.
 * WaterMedia is used by film VideoClip; forms use this path so world/editor preview
 * reliably gets pixels even when VLC textures stay empty.
 */
public final class VideoFormPlayback
{
    private static final int MAX_LONG_SIDE = 1920;
    private static final Pattern SIZE_PATTERN = Pattern.compile("(\\d{2,5})x(\\d{2,5})");
    private static final Pattern FPS_PATTERN = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*fps");
    private static final Pattern DURATION_PATTERN = Pattern.compile("Duration:\\s*(\\d+):(\\d+):(\\d+(?:\\.\\d+)?)");
    private static final Map<String, VideoFormPlayback> CACHE = new ConcurrentHashMap<>();

    private final File file;
    private final Texture texture = new Texture();

    private int width = 16;
    private int height = 9;
    private float fps = 30F;
    private double durationSec = 0D;
    private boolean probed;

    private Process process;
    private InputStream stdout;
    private ByteBuffer frameBuffer;
    private int frameBytes;
    private long currentFrameIndex = -1L;
    private long streamStartFrame;
    private boolean streamAlive;

    private VideoFormPlayback(File file)
    {
        this.file = file;
        this.texture.setFilter(GL11.GL_LINEAR);
        this.texture.setWrap(org.lwjgl.opengl.GL13.GL_CLAMP_TO_EDGE);
        this.texture.setSize(16, 9);
    }

    public static VideoFormPlayback get(String path)
    {
        File file = resolveFile(path);

        if (file == null)
        {
            return null;
        }

        String key = file.getAbsolutePath();

        return CACHE.computeIfAbsent(key, (k) -> new VideoFormPlayback(file));
    }

    public static void release(String path)
    {
        File file = resolveFile(path);

        if (file == null)
        {
            return;
        }

        VideoFormPlayback playback = CACHE.remove(file.getAbsolutePath());

        if (playback != null)
        {
            playback.close();
        }
    }

    public Texture ensureFrame(long tickPosition, float speed, boolean loop)
    {
        this.ensureProbed();

        if (!this.probed || this.width <= 0 || this.height <= 0)
        {
            return null;
        }

        double timeSec = Math.max(0D, tickPosition / 20.0D) * Math.max(0.01F, speed);
        long totalFrames = this.durationSec > 0D
            ? Math.max(1L, Math.round(this.durationSec * this.fps))
            : Long.MAX_VALUE;
        long targetFrame = (long) Math.floor(timeSec * this.fps);

        if (loop && totalFrames > 0 && totalFrames != Long.MAX_VALUE)
        {
            targetFrame = Math.floorMod(targetFrame, totalFrames);
        }
        else if (totalFrames != Long.MAX_VALUE)
        {
            targetFrame = Math.min(targetFrame, totalFrames - 1);
        }

        if (targetFrame == this.currentFrameIndex && this.texture.isValid())
        {
            return this.texture;
        }

        if (!this.streamAlive || targetFrame < this.currentFrameIndex || targetFrame > this.currentFrameIndex + 45L)
        {
            this.restartStream(targetFrame);
        }

        if (!this.streamAlive)
        {
            return this.texture.isValid() ? this.texture : null;
        }

        while (this.currentFrameIndex < targetFrame)
        {
            if (!this.readOneFrame())
            {
                this.streamAlive = false;
                break;
            }
        }

        return this.texture.isValid() ? this.texture : null;
    }

    public int getWidth()
    {
        return this.width;
    }

    public int getHeight()
    {
        return this.height;
    }

    private void ensureProbed()
    {
        if (this.probed)
        {
            return;
        }

        this.probed = true;

        try
        {
            ProcessBuilder builder = new ProcessBuilder(
                FFMpegUtils.getFFMPEG(),
                "-hide_banner",
                "-i", this.file.getAbsolutePath()
            );
            builder.redirectErrorStream(true);
            Process probe = builder.start();
            String output = new String(probe.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            probe.waitFor();

            Matcher size = SIZE_PATTERN.matcher(output);

            if (size.find())
            {
                this.width = Integer.parseInt(size.group(1));
                this.height = Integer.parseInt(size.group(2));
            }

            Matcher fps = FPS_PATTERN.matcher(output);

            if (fps.find())
            {
                this.fps = Math.max(1F, Float.parseFloat(fps.group(1)));
            }

            Matcher duration = DURATION_PATTERN.matcher(output);

            if (duration.find())
            {
                double hours = Double.parseDouble(duration.group(1));
                double minutes = Double.parseDouble(duration.group(2));
                double seconds = Double.parseDouble(duration.group(3));

                this.durationSec = hours * 3600D + minutes * 60D + seconds;
            }

            int longSide = Math.max(this.width, this.height);

            if (longSide > MAX_LONG_SIDE)
            {
                float scale = MAX_LONG_SIDE / (float) longSide;

                this.width = Math.max(2, Math.round(this.width * scale) & ~1);
                this.height = Math.max(2, Math.round(this.height * scale) & ~1);
            }

            this.frameBytes = this.width * this.height * 4;
            this.frameBuffer = MemoryUtil.memAlloc(this.frameBytes);
            this.texture.setSize(this.width, this.height);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            this.width = 0;
            this.height = 0;
        }
    }

    private void restartStream(long startFrame)
    {
        this.closeProcess();

        double startSec = startFrame / (double) this.fps;
        String scale = this.width + ":" + this.height;

        try
        {
            ProcessBuilder builder = new ProcessBuilder(
                FFMpegUtils.getFFMPEG(),
                "-hide_banner",
                "-loglevel", "error",
                "-ss", String.format(java.util.Locale.ROOT, "%.3f", startSec),
                "-i", this.file.getAbsolutePath(),
                "-an",
                "-vf", "scale=" + scale,
                "-f", "rawvideo",
                "-pix_fmt", "rgba",
                "-"
            );
            builder.redirectError(ProcessBuilder.Redirect.DISCARD);
            this.process = builder.start();
            this.stdout = new BufferedInputStream(this.process.getInputStream(), this.frameBytes * 2);
            this.streamStartFrame = startFrame;
            this.currentFrameIndex = startFrame - 1L;
            this.streamAlive = true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            this.streamAlive = false;
        }
    }

    private boolean readOneFrame()
    {
        if (this.stdout == null || this.frameBuffer == null)
        {
            return false;
        }

        try
        {
            this.frameBuffer.clear();
            byte[] chunk = new byte[8192];
            int remaining = this.frameBytes;

            while (remaining > 0)
            {
                int read = this.stdout.read(chunk, 0, Math.min(chunk.length, remaining));

                if (read < 0)
                {
                    return false;
                }

                this.frameBuffer.put(chunk, 0, read);
                remaining -= read;
            }

            this.frameBuffer.flip();
            this.currentFrameIndex++;

            ByteBuffer copy = MemoryUtil.memAlloc(this.frameBytes);

            MemoryUtil.memCopy(this.frameBuffer, copy);
            copy.rewind();

            Pixels upload = new Pixels(copy, this.width, this.height, 4);

            this.texture.bind();
            this.texture.setFilter(GL11.GL_LINEAR);
            this.texture.uploadTexture(upload);
            this.texture.unbind();

            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    private void closeProcess()
    {
        this.streamAlive = false;

        if (this.stdout != null)
        {
            try
            {
                this.stdout.close();
            }
            catch (Exception ignored)
            {}

            this.stdout = null;
        }

        if (this.process != null)
        {
            this.process.destroyForcibly();

            try
            {
                this.process.waitFor();
            }
            catch (Exception ignored)
            {}

            this.process = null;
        }
    }

    private void close()
    {
        this.closeProcess();

        if (this.frameBuffer != null)
        {
            MemoryUtil.memFree(this.frameBuffer);
            this.frameBuffer = null;
        }

        if (this.texture.isValid())
        {
            this.texture.delete();
        }
    }

    public static File resolveFile(String path)
    {
        if (path == null || path.isEmpty())
        {
            return null;
        }

        if (path.equalsIgnoreCase("none") || path.startsWith("<"))
        {
            return null;
        }

        if (path.startsWith("external:"))
        {
            String raw = path.substring("external:".length()).trim();
            File file = new File(raw);

            if (!file.isAbsolute())
            {
                file = new File(BBSMod.getGameFolder(), raw);
            }

            return file.isFile() ? file : null;
        }

        try
        {
            Link link = Link.create(path);
            File file = BBSMod.getProvider().getFile(link);

            if (file != null && file.isFile())
            {
                return file;
            }
        }
        catch (Throwable ignored)
        {}

        String relative = path.startsWith("assets:") ? path.substring("assets:".length()) : path;
        File assetsFile = BBSMod.getAssetsPath(relative);

        if (assetsFile.isFile())
        {
            return assetsFile;
        }

        /* Legacy / alternate folders. */
        String name = relative;
        int slash = Math.max(relative.lastIndexOf('/'), relative.lastIndexOf('\\'));

        if (slash >= 0)
        {
            name = relative.substring(slash + 1);
        }

        File[] candidates = new File[] {
            BBSMod.getAssetsPath("video/" + name),
            BBSMod.getAssetsPath("videos/" + name),
            new File(BBSMod.getGameFolder(), relative),
            new File(BBSMod.getGameFolder(), path)
        };

        for (File candidate : candidates)
        {
            if (candidate != null && candidate.isFile())
            {
                return candidate;
            }
        }

        return null;
    }
}
