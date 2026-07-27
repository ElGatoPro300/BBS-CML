package mchorse.bbs_mod.audio.wav;

import mchorse.bbs_mod.audio.BinaryChunk;
import mchorse.bbs_mod.audio.BinaryReader;
import mchorse.bbs_mod.audio.Wave;
import mchorse.bbs_mod.utils.Pair;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @link http://soundfile.sapp.org/doc/WaveFormat/
 */
public class WaveReader extends BinaryReader
{
    public Wave read(InputStream stream) throws Exception
    {
        try
        {
            BinaryChunk main = this.readChunk(stream);

            if (!main.id.equals("RIFF"))
            {
                throw new Exception("Given file is not 'RIFF'! It's '" + main.id + "' instead...");
            }

            String format = this.readFourString(stream);

            if (!format.equals("WAVE"))
            {
                throw new Exception("Given RIFF file is not a 'WAVE' file! It's '" + format + "' instead...");
            }

            int audioFormat = -1;
            int numChannels = -1;
            int sampleRate = -1;
            int byteRate = -1;
            int blockAlign = -1;
            int bitsPerSample = -1;
            byte[] data = null;
            List<WaveList> lists = new ArrayList<>();
            List<WaveCue> cues = new ArrayList<>();

            while (true)
            {
                try
                {
                    BinaryChunk chunk = this.readChunk(stream);

                    this.log("Reading chunk: " + chunk.id + " " + chunk.size + " " + stream.available());

                    if (chunk.id.equals("fmt "))
                    {
                        audioFormat = this.readShort(stream);
                        numChannels = this.readShort(stream);

                        sampleRate = this.readInt(stream);
                        byteRate = this.readInt(stream);

                        blockAlign = this.readShort(stream);
                        bitsPerSample = this.readShort(stream);

                        /* Discarding extra data */
                        if (chunk.size > 16)
                        {
                            this.skip(stream, chunk.size - 16);
                        }
                    }
                    else if (chunk.id.equals("data"))
                    {
                        data = new byte[chunk.size];
                        this.readFully(stream, data, data.length);
                    }
                    /* https://www.recordingblogs.com/wiki/list-chunk-of-a-wave-file */
                    else if (chunk.id.equals("LIST"))
                    {
                        byte[] listData = new byte[chunk.size];
                        this.readFully(stream, listData, listData.length);

                        try
                        {
                            ByteArrayInputStream bytes = new ByteArrayInputStream(listData);
                            WaveList list = new WaveList(this.readFourString(bytes));

                            while (bytes.available() > 0)
                            {
                                String id = this.readFourString(bytes);
                                int size = this.readInt(bytes);
                                byte[] stringData = new byte[size];

                                bytes.read(stringData);

                                String string = new String(stringData);

                                list.entries.add(new Pair<>(id, string));
                            }

                            lists.add(list);
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                    /* https://www.recordingblogs.com/wiki/cue-chunk-of-a-wave-file */
                    else if (chunk.id.equals("cue "))
                    {
                        byte[] cueData = new byte[chunk.size];
                        this.readFully(stream, cueData, cueData.length);

                        ByteArrayInputStream bytes = new ByteArrayInputStream(cueData);

                        int cuesCount = this.readInt(bytes);

                        while (cuesCount > 0)
                        {
                            WaveCue cue = new WaveCue();

                            cue.id = this.readInt(bytes);
                            cue.position = this.readInt(bytes);
                            cue.dataChunkID = this.readInt(bytes);
                            cue.chunkStart = this.readInt(bytes);
                            cue.blockStart = this.readInt(bytes);
                            cue.sampleStart = this.readInt(bytes);
                            cuesCount -= 1;

                            cues.add(cue);
                        }
                    }
                    else
                    {
                        this.skip(stream, chunk.size);
                    }
                }
                catch (EOFException e)
                {
                    e.printStackTrace();

                    break;
                }
            }

            if (data == null)
            {
                throw new Exception("The data chunk isn't present in this file!");
            }

            Wave wave = new Wave(audioFormat, numChannels, sampleRate, byteRate, blockAlign, bitsPerSample, data);

            wave.lists = lists;
            wave.cues = cues;

            return wave;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    private void log(String s)
    {
        // System.out.println(s);
    }

    public BinaryChunk readChunk(InputStream stream) throws Exception
    {
        this.log("- Starting reading chunk..." + stream.available());

        String id = this.readFourString(stream);

        this.log("- Read chunk ID... " + id + " " + stream.available());

        int size = this.readInt(stream);

        this.log("- Read chunk size... " + size + " " + stream.available());

        return new BinaryChunk(id, size);
    }
}