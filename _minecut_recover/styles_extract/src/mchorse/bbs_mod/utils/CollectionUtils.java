package mchorse.bbs_mod.utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CollectionUtils
{
    public static <T> Set<T> setOf(T... values)
    {
        Set<T> set = new HashSet<>();

        for (T value : values)
        {
            set.add(value);
        }

        return set;
    }

    public static boolean inRange(Collection collection, int index)
    {
        return index >= 0 && index < collection.size();
    }

    public static <T> T getSafe(List<T> list, int index)
    {
        return getSafe(list, index, null);
    }

    public static <T> T getSafe(List<T> list, int index, T defaultValue)
    {
        if (inRange(list, index))
        {
            return list.get(index);
        }

        return defaultValue;
    }

    public static <K, V> K getKey(Map<K, V> map, V value)
    {
        for (Map.Entry<K, V> entry : map.entrySet())
        {
            if (entry.getValue() == value)
            {
                return entry.getKey();
            }
        }

        return null;
    }

    public static <T> int getIndex(List<T> list, T value)
    {
        for (int i = 0; i < list.size(); i++)
        {
            if (list.get(i) == value)
            {
                return i;
            }
        }

        return -1;
    }

    public static float[] toArray(List<Float> floats)
    {
        float[] array = new float[floats.size()];

        for (int i = 0; i < array.length; i++)
        {
            array[i] = floats.get(i);
        }

        return array;
    }
}