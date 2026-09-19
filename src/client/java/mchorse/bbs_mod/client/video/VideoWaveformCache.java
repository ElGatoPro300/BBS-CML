package mchorse.bbs_mod.client.video;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.audio.SoundBuffer;
import mchorse.bbs_mod.audio.Wave;
import mchorse.bbs_mod.audio.Waveform;
import mchorse.bbs_mod.audio.wav.WaveReader;
import mchorse.bbs_mod.utils.FFMpegUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class VideoWaveformCache
{
    private static final Map<String, SoundBuffer> BUFFERS = new HashMap<>();
    private static final Set<String> FAILED = new HashSet<>();
    private static Boolean ffmpegOk;

    public static SoundBuffer get(String videoPath)
    {
        if (videoPath == null || videoPath.isEmpty())
        {
            return null;
        }

        if (BUFFERS.containsKey(videoPath))
        {
            return BUFFERS.get(videoPath);
        }

        if (FAILED.contains(videoPath))
        {
            return null;
        }

        File videoFile = VideoRenderer.getResolvedVideoFile(videoPath);

        if (videoFile == null)
        {
            FAILED.add(videoPath);
            return null;
        }

        if (ffmpegOk == null)
        {
            ffmpegOk = FFMpegUtils.checkFFMPEG();
        }

        if (!ffmpegOk)
        {
            FAILED.add(videoPath);
            return null;
        }

        File cacheDir = BBSMod.getSettingsPath("video_waveform_cache");
        cacheDir.mkdirs();

        String hash = Integer.toHexString(videoFile.getAbsolutePath().hashCode());
        File cacheFile = new File(cacheDir, hash + ".wav");

        if (!cacheFile.exists() || cacheFile.lastModified() < videoFile.lastModified())
        {
            boolean ok = FFMpegUtils.execute(BBSMod.getGameFolder(),
                "-y",
                "-i", videoFile.getAbsolutePath(),
                "-vn",
                "-ac", "1",
                "-ar", "44100",
                cacheFile.getAbsolutePath());

            if (!ok || !cacheFile.exists())
            {
                FAILED.add(videoPath);
                return null;
            }
        }

        try (FileInputStream stream = new FileInputStream(cacheFile))
        {
            Wave wave = new WaveReader().read(stream);

            if (wave.getBytesPerSample() > 2)
            {
                wave = wave.convertTo16();
            }

            Waveform waveform = new Waveform();
            waveform.generate(wave, null, BBSSettings.audioWaveformDensity.get(), BBSSettings.audioWaveformHeight.get());

            SoundBuffer buffer = new SoundBuffer(null, wave, waveform);
            BUFFERS.put(videoPath, buffer);

            return buffer;
        }
        catch (Exception e)
        {
            FAILED.add(videoPath);
            e.printStackTrace();
            return null;
        }
    }
}
