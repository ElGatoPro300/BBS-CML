package mchorse.bbs_mod.utils;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

public class UnsafeUtils
{
    public static Unsafe getUnsafe()
    {
        try
        {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");

            theUnsafe.setAccessible(true);

            return (Unsafe) theUnsafe.get(null);
        }
        catch (Exception e)
        {}

        return null;
    }
}