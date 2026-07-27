package mchorse.bbs_mod.l10n.keys;

import java.util.List;

public interface IKey
{
    public static final IKey EMPTY = new StringKey("");

    /**
     * This method is used to create an IKey that contains translated string, but I'm too lazy
     * to extract it at the moment...
     */
    public static IKey raw(String string)
    {
        return new StringKey(string);
    }

    /**
     * This method is used to create an IKey that contains raw string data.
     */
    public static IKey constant(String string)
    {
        return new StringKey(string);
    }

    public static IKey comp(List<IKey> keys)
    {
        return new CompoundKey(keys);
    }

    public String get();

    public default IKey format(Object... args)
    {
        return new FormatKey(this, args);
    }
}