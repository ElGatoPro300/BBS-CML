package mchorse.bbs_mod.utils;

import org.lwjgl.openal.SOFTLoopback;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.file.Path;

public class AmbientAudioCapture
{
    private static final int SAMPLE_RATE = 48000;
    private static final int CHANNELS = 2;
    private static final int BITS_PER_SAMPLE = 16;

    private final File file;
    private final int fps;
    private final FileOutputStream stream;
    private double pendingSamples;
    private long writtenBytes;
    private boolean closed;

    private AmbientAudioCapture(File file, int fps, FileOutputStream stream) throws IOException
    {
        this.file = file;
        this.fps = Math.max(1, fps);
        this.stream = stream;

        this.stream.write(new byte[44]);
    }

    public static AmbientAudioCapture open(Path folder, String baseName, int fps) throws IOException
    {
        File target = folder.resolve(baseName + "_ambient.wav").toFile();

        return new AmbientAudioCapture(target, fps, new FileOutputStream(target));
    }

    public File getFile()
    {
        return this.file;
    }

    public void captureFrame()
    {
        if (this.closed)
        {
            return;
        }

        long device = LoopbackAudioController.getLoopbackDevice();

        if (device == 0L)
        {
            return;
        }

        this.pendingSamples += (double) SAMPLE_RATE / this.fps;

        int samples = (int) this.pendingSamples;

        if (samples <= 0)
        {
            return;
        }

        this.pendingSamples -= samples;

        int floats = samples * CHANNELS;
        FloatBuffer floatBuffer = MemoryUtil.memAllocFloat(floats);

        try
        {
            SOFTLoopback.alcRenderSamplesSOFT(device, floatBuffer, samples);
            byte[] pcm = new byte[floats * 2];
            int index = 0;

            for (int i = 0; i < floats; i++)
            {
                float value = Math.max(-1F, Math.min(1F, floatBuffer.get(i)));
                short pcm16 = (short) (value * Short.MAX_VALUE);

                pcm[index++] = (byte) (pcm16 & 0xFF);
                pcm[index++] = (byte) ((pcm16 >> 8) & 0xFF);
            }

            this.stream.write(pcm);
            this.writtenBytes += pcm.length;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            MemoryUtil.memFree(floatBuffer);
        }
    }

    public void close() throws IOException
    {
        if (this.closed)
        {
            return;
        }

        this.closed = true;
        this.stream.flush();
        this.stream.close();

        int byteRate = SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8;
        short blockAlign = (short) (CHANNELS * BITS_PER_SAMPLE / 8);

        try (RandomAccessFile raf = new RandomAccessFile(this.file, "rw"))
        {
            raf.writeBytes("RIFF");
            raf.writeInt(Integer.reverseBytes((int) (36 + this.writtenBytes)));
            raf.writeBytes("WAVE");
            raf.writeBytes("fmt ");
            raf.writeInt(Integer.reverseBytes(16));
            raf.writeShort(Short.reverseBytes((short) 1));
            raf.writeShort(Short.reverseBytes((short) CHANNELS));
            raf.writeInt(Integer.reverseBytes(SAMPLE_RATE));
            raf.writeInt(Integer.reverseBytes(byteRate));
            raf.writeShort(Short.reverseBytes(blockAlign));
            raf.writeShort(Short.reverseBytes((short) BITS_PER_SAMPLE));
            raf.writeBytes("data");
            raf.writeInt(Integer.reverseBytes((int) this.writtenBytes));
        }
    }
}
