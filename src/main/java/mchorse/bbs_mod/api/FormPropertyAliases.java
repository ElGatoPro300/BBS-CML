package mchorse.bbs_mod.api;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for migrating legacy or old unnamespaced property keys to namespaced ones.
 */
public class FormPropertyAliases
{
    private static final Map<String, String> ALIASES = new HashMap<>();

    public static void register(String oldName, String canonicalName)
    {
        if (oldName != null && canonicalName != null)
        {
            ALIASES.put(oldName, canonicalName);
        }
    }

    public static String get(String key)
    {
        return ALIASES.getOrDefault(key, key);
    }

    public static boolean has(String key)
    {
        return ALIASES.containsKey(key);
    }
}
