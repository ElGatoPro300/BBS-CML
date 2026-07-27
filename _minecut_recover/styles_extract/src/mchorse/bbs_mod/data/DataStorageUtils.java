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
import net.minecraft.class_2479;
import net.minecraft.class_2481;
import net.minecraft.class_2487;
import net.minecraft.class_2489;
import net.minecraft.class_2494;
import net.minecraft.class_2495;
import net.minecraft.class_2497;
import net.minecraft.class_2499;
import net.minecraft.class_2503;
import net.minecraft.class_2516;
import net.minecraft.class_2519;
import net.minecraft.class_2520;
import net.minecraft.class_2540;
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

    public static void writeToPacket(class_2540 packet, BaseType type)
    {
        try
        {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            DataStorage.writeToStream(stream, type);

            packet.method_10813(stream.toByteArray());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static BaseType readFromPacket(class_2540 packet)
    {
        try
        {
            ByteArrayInputStream stream = new ByteArrayInputStream(packet.method_10795());

            return DataStorage.readFromStream(stream);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    /* NBT */

    public static class_2520 toNbt(BaseType type)
    {
        if (type == null)
        {
            return null;
        }

        if (type instanceof ByteType byteType)
        {
            return class_2481.method_23233(byteType.value);
        }
        else if (type instanceof DoubleType doubleType)
        {
            return class_2489.method_23241(doubleType.value);
        }
        else if (type instanceof FloatType floatType)
        {
            return class_2494.method_23244(floatType.value);
        }
        else if (type instanceof IntType intType)
        {
            return class_2497.method_23247(intType.value);
        }
        else if (type instanceof LongType longType)
        {
            return class_2503.method_23251(longType.value);
        }
        else if (type instanceof ShortType shortType)
        {
            return class_2516.method_23254(shortType.value);
        }
        else if (type instanceof StringType stringType)
        {
            return class_2519.method_23256(stringType.value);
        }
        else if (type instanceof ByteArrayType byteArrayType)
        {
            return new class_2479(byteArrayType.value);
        }
        else if (type instanceof IntArrayType intArrayType)
        {
            return new class_2495(intArrayType.value);
        }
        else if (type instanceof ShortArrayType shortArrayType)
        {
            return new class_2499(); // Minecraft doesn't have NbtShortArray, it usually uses NbtList or NbtIntArray
        }
        else if (type instanceof ListType listType)
        {
            class_2499 list = new class_2499();

            for (BaseType baseType : listType)
            {
                class_2520 element = toNbt(baseType);

                if (element != null)
                {
                    list.add(element);
                }
            }

            return list;
        }
        else if (type instanceof MapType mapType)
        {
            class_2487 compound = new class_2487();

            for (String key : mapType.keys())
            {
                class_2520 element = toNbt(mapType.get(key));

                if (element != null)
                {
                    compound.method_10566(key, element);
                }
            }

            return compound;
        }

        return null;
    }

    public static BaseType fromNbt(class_2520 element)
    {
        if (element instanceof class_2481 nbtByte)
        {
            return new ByteType(nbtByte.method_10698());
        }
        else if (element instanceof class_2489 nbtDouble)
        {
            return new DoubleType(nbtDouble.method_10697());
        }
        else if (element instanceof class_2494 nbtFloat)
        {
            return new FloatType(nbtFloat.method_10700());
        }
        else if (element instanceof class_2497 nbtInt)
        {
            return new IntType(nbtInt.method_10701());
        }
        else if (element instanceof class_2503 nbtLong)
        {
            return new LongType(nbtLong.method_10699());
        }
        else if (element instanceof class_2516 nbtShort)
        {
            return new ShortType(nbtShort.method_10696());
        }
        else if (element instanceof class_2519 nbtString)
        {
            return new StringType(nbtString.method_10714());
        }
        else if (element instanceof class_2479 nbtByteArray)
        {
            return new ByteArrayType(nbtByteArray.method_10521());
        }
        else if (element instanceof class_2495 nbtIntArray)
        {
            return new IntArrayType(nbtIntArray.method_10588());
        }
        else if (element instanceof class_2499 nbtList)
        {
            ListType list = new ListType();

            for (class_2520 nbtElement : nbtList)
            {
                list.add(fromNbt(nbtElement));
            }

            return list;
        }
        else if (element instanceof class_2487 nbtCompound)
        {
            MapType map = new MapType();

            for (String key : nbtCompound.method_10541())
            {
                map.put(key, fromNbt(nbtCompound.method_10580(key)));
            }

            return map;
        }

        return null;
    }

    public static void writeToNbtCompound(class_2487 compound, String key, BaseType data)
    {
        class_2520 nbt = toNbt(data);

        if (nbt != null)
        {
            compound.method_10566(key, nbt);
        }
    }

    public static BaseType readFromNbtCompound(class_2487 compound, String key)
    {
        BaseType baseType = DataStorageUtils.fromNbt(compound.method_10580(key));

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