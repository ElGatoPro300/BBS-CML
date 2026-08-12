package mchorse.bbs_mod.cubic.model.fbx;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.Inflater;

public class FBXBinaryReader
{
    private final byte[] data;
    private int pos;
    private int version;

    public FBXBinaryReader(byte[] data)
    {
        this.data = data;
    }

    public static boolean isBinary(byte[] data)
    {
        if (data.length < 27)
        {
            return false;
        }

        String magic = new String(data, 0, 20);

        return magic.startsWith("Kaydara FBX Binary");
    }

    public FBXNode read()
    {
        this.pos = 21;
        this.pos = 23;

        this.version = this.readUInt32();

        boolean wide = this.version >= 7500;

        FBXNode root = new FBXNode("__root__");

        while (this.pos < this.data.length)
        {
            FBXNode node = this.readNode(wide);

            if (node == null)
            {
                break;
            }

            root.children.add(node);
        }

        return root;
    }

    private FBXNode readNode(boolean wide)
    {
        long endOffset = wide ? this.readUInt64() : this.readUInt32();
        long numProperties = wide ? this.readUInt64() : this.readUInt32();
        long propertyListLen = wide ? this.readUInt64() : this.readUInt32();

        int nameLen = this.readUInt8();

        if (endOffset == 0 && numProperties == 0 && propertyListLen == 0 && nameLen == 0)
        {
            return null;
        }

        String name = this.readString(nameLen);
        FBXNode node = new FBXNode(name);

        for (int i = 0; i < numProperties; i++)
        {
            node.properties.add(this.readProperty());
        }

        while (this.pos < endOffset)
        {
            FBXNode child = this.readNode(wide);

            if (child == null)
            {
                break;
            }

            node.children.add(child);
        }

        this.pos = (int) endOffset;

        return node;
    }

    private Object readProperty()
    {
        char type = (char) this.readUInt8();

        switch (type)
        {
            case 'Y': return (int) this.readInt16();
            case 'C': return this.readUInt8() != 0;
            case 'I': return this.readInt32();
            case 'F': return this.readFloat32();
            case 'D': return this.readFloat64();
            case 'L': return this.readInt64();
            case 'f': return this.readArrayFloat();
            case 'd': return this.readArrayDouble();
            case 'l': return this.readArrayLong();
            case 'i': return this.readArrayInt();
            case 'b': return this.readArrayBool();
            case 'S':
            {
                int len = this.readInt32();
                return this.readString(len);
            }
            case 'R':
            {
                int len = this.readInt32();
                byte[] raw = new byte[len];
                System.arraycopy(this.data, this.pos, raw, 0, len);
                this.pos += len;
                return raw;
            }
            default:
                throw new IllegalStateException("Unknown FBX property type '" + type + "' at offset " + this.pos);
        }
    }

    private byte[] readArrayPayload(int arrayLength, int elementSize)
    {
        long arrayLen = arrayLength & 0xFFFFFFFFL;
        int encoding = this.readInt32();
        int compressedLength = this.readInt32();

        if (encoding == 0)
        {
            byte[] raw = new byte[(int) (arrayLen * elementSize)];
            System.arraycopy(this.data, this.pos, raw, 0, raw.length);
            this.pos += raw.length;

            return raw;
        }
        else
        {
            byte[] compressed = new byte[compressedLength];
            System.arraycopy(this.data, this.pos, compressed, 0, compressedLength);
            this.pos += compressedLength;

            return this.zlibInflate(compressed, (int) (arrayLen * elementSize));
        }
    }

    private byte[] zlibInflate(byte[] input, int expectedSize)
    {
        try
        {
            Inflater inflater = new Inflater();
            inflater.setInput(input);

            ByteArrayOutputStream out = new ByteArrayOutputStream(expectedSize > 0 ? expectedSize : input.length * 4);
            byte[] buffer = new byte[8192];

            while (!inflater.finished())
            {
                int n = inflater.inflate(buffer);

                if (n == 0)
                {
                    if (inflater.needsInput() || inflater.needsDictionary())
                    {
                        break;
                    }
                }

                out.write(buffer, 0, n);
            }

            inflater.end();

            return out.toByteArray();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to inflate FBX array (zlib)", e);
        }
    }

    private float[] readArrayFloat()
    {
        int arrayLength = this.readInt32();
        byte[] raw = this.readArrayPayload(arrayLength, 4);
        ByteBuffer bb = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
        float[] out = new float[arrayLength];

        for (int i = 0; i < arrayLength; i++)
        {
            out[i] = bb.getFloat();
        }

        return out;
    }

    private double[] readArrayDouble()
    {
        int arrayLength = this.readInt32();
        byte[] raw = this.readArrayPayload(arrayLength, 8);
        ByteBuffer bb = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
        double[] out = new double[arrayLength];

        for (int i = 0; i < arrayLength; i++)
        {
            out[i] = bb.getDouble();
        }

        return out;
    }

    private long[] readArrayLong()
    {
        int arrayLength = this.readInt32();
        byte[] raw = this.readArrayPayload(arrayLength, 8);
        ByteBuffer bb = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
        long[] out = new long[arrayLength];

        for (int i = 0; i < arrayLength; i++)
        {
            out[i] = bb.getLong();
        }

        return out;
    }

    private int[] readArrayInt()
    {
        int arrayLength = this.readInt32();
        byte[] raw = this.readArrayPayload(arrayLength, 4);
        ByteBuffer bb = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
        int[] out = new int[arrayLength];

        for (int i = 0; i < arrayLength; i++)
        {
            out[i] = bb.getInt();
        }

        return out;
    }

    private boolean[] readArrayBool()
    {
        int arrayLength = this.readInt32();
        byte[] raw = this.readArrayPayload(arrayLength, 1);
        boolean[] out = new boolean[arrayLength];

        for (int i = 0; i < arrayLength; i++)
        {
            out[i] = raw[i] != 0;
        }

        return out;
    }

    private int readUInt8()
    {
        return this.data[this.pos++] & 0xFF;
    }

    private short readInt16()
    {
        int v = (this.data[this.pos] & 0xFF) | ((this.data[this.pos + 1] & 0xFF) << 8);
        this.pos += 2;

        return (short) v;
    }

    private int readInt32()
    {
        int v = (this.data[this.pos] & 0xFF) | ((this.data[this.pos + 1] & 0xFF) << 8) | ((this.data[this.pos + 2] & 0xFF) << 16) | ((this.data[this.pos + 3] & 0xFF) << 24);
        this.pos += 4;

        return v;
    }

    private int readUInt32()
    {
        return this.readInt32();
    }

    private long readInt64()
    {
        long v = 0;

        for (int i = 0; i < 8; i++)
        {
            v |= (this.data[this.pos + i] & 0xFFL) << (8 * i);
        }

        this.pos += 8;

        return v;
    }

    private long readUInt64()
    {
        return this.readInt64();
    }

    private float readFloat32()
    {
        return Float.intBitsToFloat(this.readInt32());
    }

    private double readFloat64()
    {
        return Double.longBitsToDouble(this.readInt64());
    }

    private String readString(int len)
    {
        String s = new String(this.data, this.pos, len);
        this.pos += len;

        return s;
    }
}
