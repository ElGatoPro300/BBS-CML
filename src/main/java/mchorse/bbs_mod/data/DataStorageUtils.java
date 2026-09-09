package mchorse.bbs_mod.data;

import mchorse.bbs_mod.data.storage.DataStorage;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ByteArrayType;
import mchorse.bbs_mod.data.types.ByteType;
import mchorse.bbs_mod.data.types.DoubleType;
import mchorse.bbs_mod.data.types.FloatType;
import mchorse.bbs_mod.data.types.IntArrayType;
import mchorse.bbs_mod.data.types.IntType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.LongType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.data.types.ShortArrayType;
import mchorse.bbs_mod.data.types.ShortType;
import mchorse.bbs_mod.data.types.StringType;

import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import org.joml.Matrix3f;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DataStorageUtils
{
    private static final byte[] EMPTY = new byte[0];

    /* PacketByteBuf */

    public static byte[] writeToBytes(BaseType type)
    {
        if (type == null)
        {
            return EMPTY;
        }

        try
        {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            DataStorage.writeToStream(stream, type);

            return stream.toByteArray();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return EMPTY;
    }

    public static BaseType readFromBytes(byte[] bytes)
    {
        if (bytes == null)
        {
            return null;
        }

        try
        {
            ByteArrayInputStream stream = new ByteArrayInputStream(bytes);

            return DataStorage.readFromStream(stream);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public static void writeToPacket(FriendlyByteBuf packet, BaseType type)
    {
        try
        {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            DataStorage.writeToStream(stream, type);

            packet.writeByteArray(stream.toByteArray());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static BaseType readFromPacket(FriendlyByteBuf packet)
    {
        try
        {
            ByteArrayInputStream stream = new ByteArrayInputStream(packet.readByteArray());

            return DataStorage.readFromStream(stream);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    /* NBT */

    public static Tag toNbt(BaseType type)
    {
        if (type == null)
        {
            return null;
        }

        if (type instanceof ByteType byteType)
        {
            return ByteTag.valueOf(byteType.value);
        }
        else if (type instanceof DoubleType doubleType)
        {
            return DoubleTag.valueOf(doubleType.value);
        }
        else if (type instanceof FloatType floatType)
        {
            return FloatTag.valueOf(floatType.value);
        }
        else if (type instanceof IntType intType)
        {
            return IntTag.valueOf(intType.value);
        }
        else if (type instanceof LongType longType)
        {
            return LongTag.valueOf(longType.value);
        }
        else if (type instanceof ShortType shortType)
        {
            return ShortTag.valueOf(shortType.value);
        }
        else if (type instanceof StringType stringType)
        {
            return StringTag.valueOf(stringType.value);
        }
        else if (type instanceof ByteArrayType byteArrayType)
        {
            return new ByteArrayTag(byteArrayType.value);
        }
        else if (type instanceof IntArrayType intArrayType)
        {
            return new IntArrayTag(intArrayType.value);
        }
        else if (type instanceof ShortArrayType shortArrayType)
        {
            return new ListTag(); // Minecraft doesn't have NbtShortArray, it usually uses NbtList or NbtIntArray
        }
        else if (type instanceof ListType listType)
        {
            ListTag list = new ListTag();

            for (BaseType baseType : listType)
            {
                Tag element = toNbt(baseType);

                if (element != null)
                {
                    list.add(element);
                }
            }

            return list;
        }
        else if (type instanceof MapType mapType)
        {
            CompoundTag compound = new CompoundTag();

            for (String key : mapType.keys())
            {
                Tag element = toNbt(mapType.get(key));

                if (element != null)
                {
                    compound.put(key, element);
                }
            }

            return compound;
        }

        return null;
    }

    public static BaseType fromNbt(Tag element)
    {
        if (element instanceof ByteTag nbtByte)
        {
            return new ByteType(nbtByte.byteValue());
        }
        else if (element instanceof DoubleTag nbtDouble)
        {
            return new DoubleType(nbtDouble.doubleValue());
        }
        else if (element instanceof FloatTag nbtFloat)
        {
            return new FloatType(nbtFloat.floatValue());
        }
        else if (element instanceof IntTag nbtInt)
        {
            return new IntType(nbtInt.intValue());
        }
        else if (element instanceof LongTag nbtLong)
        {
            return new LongType(nbtLong.longValue());
        }
        else if (element instanceof ShortTag nbtShort)
        {
            return new ShortType(nbtShort.shortValue());
        }
        else if (element instanceof StringTag nbtString)
        {
            return new StringType(nbtString.value());
        }
        else if (element instanceof ByteArrayTag nbtByteArray)
        {
            return new ByteArrayType(nbtByteArray.getAsByteArray());
        }
        else if (element instanceof IntArrayTag nbtIntArray)
        {
            return new IntArrayType(nbtIntArray.getAsIntArray());
        }
        else if (element instanceof ListTag nbtList)
        {
            ListType list = new ListType();

            for (Tag nbtElement : nbtList)
            {
                list.add(fromNbt(nbtElement));
            }

            return list;
        }
        else if (element instanceof CompoundTag nbtCompound)
        {
            MapType map = new MapType();

            for (String key : nbtCompound.keySet())
            {
                map.put(key, fromNbt(nbtCompound.get(key)));
            }

            return map;
        }

        return null;
    }

    public static void writeToNbtCompound(CompoundTag compound, String key, BaseType data)
    {
        Tag nbt = toNbt(data);

        if (nbt != null)
        {
            compound.put(key, nbt);
        }
    }

    public static BaseType readFromNbtCompound(CompoundTag compound, String key)
    {
        BaseType baseType = DataStorageUtils.fromNbt(compound.get(key));

        if (baseType != null)
        {
            return baseType;
        }

        return null;
    }

    /* Vector2i */

    public static ListType vector2iToData(Vector2i vector)
    {
        ListType list = new ListType();

        list.addInt(vector.x);
        list.addInt(vector.y);

        return list;
    }

    public static Vector2i vector2iFromData(ListType element)
    {
        return vector2iFromData(element, new Vector2i());
    }

    public static Vector2i vector2iFromData(ListType element, Vector2i defaultValue)
    {
        if (element != null && element.size() >= 2)
        {
            return new Vector2i(element.getInt(0), element.getInt(1));
        }

        return defaultValue;
    }

    /* Vector3f */

    public static ListType vector3fToData(Vector3f vector)
    {
        ListType list = new ListType();

        list.addFloat(vector.x);
        list.addFloat(vector.y);
        list.addFloat(vector.z);

        return list;
    }

    public static Vector3f vector3fFromData(ListType element)
    {
        return vector3fFromData(element, new Vector3f());
    }

    public static Vector3f vector3fFromData(ListType element, Vector3f defaultValue)
    {
        if (element != null && element.size() >= 3)
        {
            return new Vector3f(element.getFloat(0), element.getFloat(1), element.getFloat(2));
        }

        return defaultValue;
    }

    /* Vector3d */

    public static ListType vector3dToData(Vector3d vector)
    {
        ListType list = new ListType();

        list.addDouble(vector.x);
        list.addDouble(vector.y);
        list.addDouble(vector.z);

        return list;
    }

    public static Vector3d vector3dFromData(ListType element)
    {
        return vector3dFromData(element, new Vector3d());
    }

    public static Vector3d vector3dFromData(ListType element, Vector3d defaultValue)
    {
        if (element != null && element.size() >= 3)
        {
            return new Vector3d(element.getDouble(0), element.getDouble(1), element.getDouble(2));
        }

        return defaultValue;
    }

    /* Vector4f */

    public static ListType vector4fToData(Vector4f vector)
    {
        ListType list = new ListType();

        list.addFloat(vector.x);
        list.addFloat(vector.y);
        list.addFloat(vector.z);
        list.addFloat(vector.w);

        return list;
    }

    public static Vector4f vector4fFromData(ListType element)
    {
        return vector4fFromData(element, new Vector4f());
    }

    public static Vector4f vector4fFromData(ListType element, Vector4f defaultValue)
    {
        if (element != null && element.size() >= 4)
        {
            return new Vector4f(element.getFloat(0), element.getFloat(1), element.getFloat(2), element.getFloat(3));
        }

        return defaultValue;
    }

    /* Matrix3f */

    public static ListType matrix3fToData(Matrix3f matrix)
    {
        ListType list = new ListType();

        list.addFloat(matrix.m00);
        list.addFloat(matrix.m01);
        list.addFloat(matrix.m02);
        list.addFloat(matrix.m10);
        list.addFloat(matrix.m11);
        list.addFloat(matrix.m12);
        list.addFloat(matrix.m20);
        list.addFloat(matrix.m21);
        list.addFloat(matrix.m22);

        return list;
    }

    public static Matrix3f matrix3fFromData(ListType element)
    {
        return matrix3fFromData(element, new Matrix3f());
    }

    public static Matrix3f matrix3fFromData(ListType element, Matrix3f defaultValue)
    {
        if (element != null && element.size() >= 9)
        {
            return new Matrix3f(
                element.getFloat(0), element.getFloat(1), element.getFloat(2),
                element.getFloat(3), element.getFloat(4), element.getFloat(5),
                element.getFloat(6), element.getFloat(7), element.getFloat(8)
            );
        }

        return defaultValue;
    }

    /* List<String> */

    public static ListType stringListToData(Collection<String> strings)
    {
        ListType list = new ListType();

        for (String string : strings)
        {
            list.addString(string);
        }

        return list;
    }

    public static List<String> stringListFromData(BaseType type)
    {
        ArrayList<String> strings = new ArrayList<>();

        if (type.isList())
        {
            for (BaseType baseType : type.asList())
            {
                if (baseType.isString())
                {
                    strings.add(baseType.asString());
                }
            }
        }

        return strings;
    }

    public static ListType intListToData(Collection<Integer> ints)
    {
        ListType list = new ListType();

        for (Integer i : ints)
        {
            list.addInt(i);
        }

        return list;
    }

    public static List<Integer> intListFromData(BaseType type)
    {
        ArrayList<Integer> ints = new ArrayList<>();

        if (type.isList())
        {
            for (BaseType baseType : type.asList())
            {
                if (baseType.isNumeric())
                {
                    ints.add(baseType.asNumeric().intValue());
                }
            }
        }

        return ints;
    }
}