package mchorse.bbs_mod.cubic.model.gltf;

import mchorse.bbs_mod.cubic.model.gltf.data.GLTF;
import mchorse.bbs_mod.utils.IOUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class GLTFParser
{
    private static final int GLB_MAGIC = 0x46546C67;
    private static final int CHUNK_JSON = 0x4E4F534A;
    private static final int CHUNK_BIN = 0x004E4942;

    private static final Gson gson = new GsonBuilder().create();

    public static GLTF parse(InputStream stream) throws IOException
    {
        // Read first 4 bytes to detect GLB
        byte[] magicBytes = new byte[4];
        if (stream.read(magicBytes) != 4)
        {
            throw new IOException("Failed to read file header");
        }

        int magic = (magicBytes[0] & 0xFF) | ((magicBytes[1] & 0xFF) << 8) | ((magicBytes[2] & 0xFF) << 16) | ((magicBytes[3] & 0xFF) << 24);

        if (magic == GLB_MAGIC)
        {
            return parseGLB(stream);
        }
        else
        {
            // It's likely GLTF JSON. Reconstruct stream or use PushbackInputStream?
            // Since we already read 4 bytes, we need to prepend them.
            byte[] fullData = stream.readAllBytes();
            byte[] combined = new byte[magicBytes.length + fullData.length];
            System.arraycopy(magicBytes, 0, combined, 0, magicBytes.length);
            System.arraycopy(fullData, 0, combined, magicBytes.length, fullData.length);
            
            String json = new String(combined, StandardCharsets.UTF_8);
            return gson.fromJson(json, GLTF.class);
        }
    }

    private static GLTF parseGLB(InputStream stream) throws IOException
    {
        DataInputStream data = new DataInputStream(stream);
        // We already read magic.
        int version = Integer.reverseBytes(data.readInt()); // GLB is Little Endian usually?
        // Wait, Java DataInputStream is Big Endian. GLTF is Little Endian.
        // We need to handle endianness.
        
        // Actually, let's wrap logic to read LE.
        
        if (version != 2)
        {
            throw new IOException("Unsupported GLB version: " + version);
        }
        
        int length = Integer.reverseBytes(data.readInt());
        
        GLTF gltf = null;
        byte[] binData = null;
        
        long bytesRead = 12; // Header
        
        while (bytesRead < length)
        {
            int chunkLength = Integer.reverseBytes(data.readInt());
            int chunkType = Integer.reverseBytes(data.readInt());
            
            bytesRead += 8;
            
            byte[] chunkData = new byte[chunkLength];
            int read = data.read(chunkData);
            if (read != chunkLength)
            {
                 throw new IOException("Failed to read chunk data");
            }
            bytesRead += chunkLength;
            
            if (chunkType == CHUNK_JSON)
            {
                String json = new String(chunkData, StandardCharsets.UTF_8);
                gltf = gson.fromJson(json, GLTF.class);
            }
            else if (chunkType == CHUNK_BIN)
            {
                binData = chunkData;
            }
        }
        
        if (gltf != null && binData != null)
        {
            if (gltf.buffers != null && !gltf.buffers.isEmpty())
            {
                // Assign BIN chunk to the first buffer usually
                // GLB spec: The first buffer is the BIN chunk.
                gltf.buffers.get(0).data = binData;
            }
        }
        
        return gltf;
    }
}
