package mchorse.bbs_mod.audio;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public abstract class BinaryReader
{
    public byte[] buf = new byte[4];

    public static int b2i(byte b0, byte b1, byte b2, byte b3)
    {
        return (b0 & 0xff) | ((b1 & 0xff) << 8) | ((b2 & 0xff) << 16) | ((b3 & 0xff) << 24);
    }

    public int fourChars(char c0, char c1, char c2, char c3)
    {
        return ((c3 << 24) & 0xff000000) | ((c2 << 16) & 0x00ff0000) | ((c1 << 8) & 0x0000ff00) | (c0 & 0x000000ff);
    }

    public int fourChars(String string) throws Exception
    {
        char[] chars = string.toCharArray();

        if (chars.length != 4)
        {
            throw new Exception("Given string '" + string + "'");
        }

        return this.fourChars(chars[0], chars[1], chars[2], chars[3]);
    }

    public String readFourString(InputStream stream) throws Exception
    {
        this.readFully(stream, this.buf, 4);

        return new String(this.buf);
    }

    public int readInt(InputStream stream) throws Exception
    {
        this.readFully(stream, this.buf, 4);

        return b2i(this.buf[0], this.buf[1], this.buf[2], this.buf[3]);
    }

    public int readShort(InputStream stream) throws Exception
    {
        this.readFully(stream, this.buf, 2);

        return b2i(this.buf[0], this.buf[1], (byte) 0, (byte) 0);
    }

    public void readFully(InputStream stream, byte[] buffer, int length) throws IOException
    {
        int total = 0;

        while (total < length)
        {
            int result = stream.read(buffer, total, length - total);

            if (result == -1)
            {
                throw new EOFException();
            }

            total += result;
        }
    }

    public void skip(InputStream stream, long bytes) throws Exception
    {
        while (bytes > 0)
        {
            long skipped = stream.skip(bytes);

            if (skipped > 0)
            {
                bytes -= skipped;
            }
            else
            {
                int size = (int) Math.min(bytes, 2048);
                byte[] buffer = new byte[size];
                int read = stream.read(buffer);

                if (read == -1)
                {
                    throw new EOFException();
                }

                bytes -= read;
            }
        }
    }
}