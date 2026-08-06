package mchorse.bbs_mod.ui.film.audio;

import mchorse.bbs_mod.audio.Wave;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALC11;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class OpenALRecorder implements Runnable
{
    private static final int SAMPLE_RATE = 44100;
    private static final int FORMAT = AL10.AL_FORMAT_MONO16;
    private static final int BUFFER_SAMPLES = 1024;

    private long captureDevice;
    private ByteBuffer buffer;
    private boolean running = true;
    private Consumer<Wave> consumer;
    private long startTime;
    private float volume;

    public OpenALRecorder(Consumer<Wave> consumer)
    {
        this.consumer = consumer;
    }

    public void stop()
    {
        this.running = false;
    }

    public long getTime()
    {
        return System.currentTimeMillis() - this.startTime;
    }

    public float getVolume()
    {
        return this.volume;
    }

    public void init()
    {
        String defaultDeviceName = ALC11.alcGetString(0, ALC11.ALC_CAPTURE_DEFAULT_DEVICE_SPECIFIER);

        if (defaultDeviceName == null)
        {
            throw new IllegalStateException("No capture devices available.");
        }

        this.captureDevice = ALC11.alcCaptureOpenDevice(defaultDeviceName, SAMPLE_RATE, FORMAT, BUFFER_SAMPLES);

        if (this.captureDevice == 0)
        {
            throw new RuntimeException("Failed to open capture device.");
        }

        ALC11.alcCaptureStart(this.captureDevice);

        this.buffer = MemoryUtil.memAlloc(SAMPLE_RATE * 2);
        this.startTime = System.currentTimeMillis();
    }

    public void pollAndProcess()
    {
        int available = ALC10.alcGetInteger(this.captureDevice, ALC11.ALC_CAPTURE_SAMPLES);

        if (available > 0)
        {
            if (this.buffer.position() + available * 2 > this.buffer.capacity())
            {
                ByteBuffer newBuffer = MemoryUtil.memAlloc(this.buffer.capacity() * 2);

                this.buffer.flip();
                newBuffer.put(this.buffer);
                MemoryUtil.memFree(this.buffer);

                this.buffer = newBuffer;
            }

            ByteBuffer buffer = BufferUtils.createByteBuffer(available * 2);

            ALC11.alcCaptureSamples(this.captureDevice, buffer, available);
            this.buffer.put(buffer);

            this.volume = 0F;

            for (int i = 0; i < available; i++)
            {
                this.volume = Math.max(Math.abs(buffer.getShort(0) / 65535F), this.volume);
            }
        }
    }

    public void cleanup()
    {
        ALC11.alcCaptureStop(this.captureDevice);
        ALC11.alcCaptureCloseDevice(this.captureDevice);

        this.buffer.flip();

        byte[] pcm = new byte[this.buffer.limit()];

        this.buffer.get(pcm);
        MemoryUtil.memFree(this.buffer);

        this.buffer = null;

        if (this.consumer != null)
        {
            this.consumer.accept(new Wave(1, 1, SAMPLE_RATE, 16, pcm));
        }
    }

    @Override
    public void run()
    {
        this.init();

        while (this.running)
        {
            this.pollAndProcess();

            try
            {
                Thread.sleep(50);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        this.cleanup();
    }
}
