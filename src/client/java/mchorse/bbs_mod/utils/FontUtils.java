package mchorse.bbs_mod.utils;

import mchorse.bbs_mod.BBSMod;

import java.awt.Font;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FontUtils
{
    public static final File FONTS_FOLDER = BBSMod.getAssetsPath("fonts");
    private static final Map<String, TextureFont> fonts = new HashMap<>();

    public static void createFontsFolder()
    {
        if (!FONTS_FOLDER.exists())
        {
            FONTS_FOLDER.mkdirs();
        }
    }

    public static TextureFont getFont(String name, int style)
    {
        String key = name + ":" + style;
        
        if (fonts.containsKey(key))
        {
            return fonts.get(key);
        }
        
        /* Case-insensitive search */
        File[] files = FONTS_FOLDER.listFiles((dir, f) -> f.toLowerCase().startsWith(name.toLowerCase() + ".") && (f.toLowerCase().endsWith(".ttf") || f.toLowerCase().endsWith(".otf")));
        
        if (files != null && files.length > 0)
        {
            TextureFont font = new TextureFont(files[0], style);
            fonts.put(key, font);
            return font;
        }
        
        return null;
    }

    public static TextureFont getFont(String name)
    {
        return getFont(name, Font.PLAIN);
    }

    public static List<String> getAvailableFonts()
    {
        createFontsFolder();
        
        File[] files = FONTS_FOLDER.listFiles((dir, name) -> name.toLowerCase().endsWith(".ttf") || name.toLowerCase().endsWith(".otf"));
        
        if (files == null)
        {
            return new ArrayList<>();
        }
        
        return Arrays.stream(files)
            .map(File::getName)
            .map(name -> name.substring(0, name.lastIndexOf('.')))
            .collect(Collectors.toList());
    }
}