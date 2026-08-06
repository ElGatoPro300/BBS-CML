package mchorse.bbs_mod.cubic.model.fbx;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class FBXParser
{
    public static FBXNode parse(InputStream stream) throws IOException
    {
        byte[] data = readAll(stream);

        if (FBXBinaryReader.isBinary(data))
        {
            return new FBXBinaryReader(data).read();
        }

        String text = new String(data, StandardCharsets.UTF_8);

        return new FBXAsciiReader(text).read();
    }

    private static byte[] readAll(InputStream stream) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;

        while ((n = stream.read(buffer)) != -1)
        {
            out.write(buffer, 0, n);
        }

        return out.toByteArray();
    }
}
