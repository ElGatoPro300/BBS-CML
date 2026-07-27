package mchorse.bbs_mod.ui.framework.elements.utils;

import mchorse.bbs_mod.text.RtlAwtTextRenderer;
import mchorse.bbs_mod.text.RtlFontManager;
import mchorse.bbs_mod.text.RtlTextEngine;
import net.minecraft.class_2561;
import net.minecraft.class_2583;
import net.minecraft.class_327;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FontRenderer
{
    private class_327 renderer;

    public static List<String> wrap(class_327 renderer, String string, int width)
    {
        return renderer.method_1728(class_2561.method_43470(string), width).stream().map((ot) ->
        {
            StringBuilder builder = new StringBuilder();
            StyleHolder holder = new StyleHolder(class_2583.field_24360);

            ot.accept((a, b, c) ->
            {
                if (!Objects.equals(b, holder.style))
                {
                    styleToString(builder, b);

                    holder.style = b;
                }

                builder.appendCodePoint(c);

                return true;
            });

            return builder.toString();
        }).collect(Collectors.toList());
    }

    private static void styleToString(StringBuilder b, class_2583 style)
    {
        /* Ew... */
        if (!style.method_10967())
        {
            b.append("\u00A7r");
        }

        if (style.method_10973() != null)
        {
            switch (style.method_10973().method_27721())
            {
                case "black": b.append("\u00A70"); break;
                case "dark_blue": b.append("\u00A71"); break;
                case "dark_green": b.append("\u00A72"); break;
                case "dark_aqua": b.append("\u00A73"); break;
                case "dark_red": b.append("\u00A74"); break;
                case "dark_purple": b.append("\u00A75"); break;
                case "gold": b.append("\u00A76"); break;
                case "gray": b.append("\u00A77"); break;
                case "dark_gray": b.append("\u00A78"); break;
                case "blue": b.append("\u00A79"); break;
                case "green": b.append("\u00A7a"); break;
                case "aqua": b.append("\u00A7b"); break;
                case "red": b.append("\u00A7c"); break;
                case "light_purple": b.append("\u00A7d"); break;
                case "yellow": b.append("\u00A7e"); break;
                case "white": b.append("\u00A7f"); break;
            }
        }

        if (style.method_10987()) b.append("\u00A7k");
        if (style.method_10984()) b.append("\u00A7l");
        if (style.method_10986()) b.append("\u00A7m");
        if (style.method_10965()) b.append("\u00A7n");
        if (style.method_10966()) b.append("\u00A7o");
    }

    public void setRenderer(class_327 renderer)
    {
        this.renderer = renderer;
    }

    public class_327 getRenderer()
    {
        return this.renderer;
    }

    public int getWidth(String string)
    {
        if (RtlTextEngine.isActive())
        {
            RtlFontManager.ensureLoaded();

            if (RtlAwtTextRenderer.isReady())
            {
                return RtlAwtTextRenderer.getWidth(string);
            }
        }

        float scale = CustomFontManager.hasCustomFont() ? 1F : CustomFontManager.getFontScale();

        return Math.round(this.renderer.method_1727(string) * scale);
    }

    public int getHeight()
    {
        if (RtlTextEngine.isActive())
        {
            RtlFontManager.ensureLoaded();

            if (RtlAwtTextRenderer.isReady())
            {
                return RtlAwtTextRenderer.getHeight();
            }
        }

        float scale = CustomFontManager.hasCustomFont() ? 1F : CustomFontManager.getFontScale();

        return Math.max(1, Math.round((this.renderer.field_2000 - 2) * scale));
    }

    public List<String> wrap(String string, int width)
    {
        if (RtlTextEngine.isActive())
        {
            RtlFontManager.ensureLoaded();

            if (RtlAwtTextRenderer.isReady())
            {
                return RtlAwtTextRenderer.wrap(string, width);
            }
        }

        return wrap(this.renderer, string, width);
    }

    public String limitToWidth(String str, int width)
    {
        return limitToWidth(str, "...", width);
    }

    public String limitToWidth(String str, String suffix, int width)
    {
        if (str.isEmpty())
        {
            return str;
        }

        if (RtlTextEngine.isActive())
        {
            RtlFontManager.ensureLoaded();

            if (RtlAwtTextRenderer.isReady())
            {
                int w = RtlAwtTextRenderer.getWidth(str);

                if (w < width)
                {
                    return str;
                }

                int sw = RtlAwtTextRenderer.getWidth(suffix);
                int i = str.length() - 1;

                while (w + sw >= width && i > 0)
                {
                    w -= RtlAwtTextRenderer.getWidth(String.valueOf(str.charAt(i)));
                    i -= 1;
                }

                str = str.substring(0, i);

                return str.isEmpty() ? str : str + suffix;
            }
        }

        int w = this.renderer.method_1727(str);

        if (w < width)
        {
            return str;
        }

        int sw = this.renderer.method_1727(suffix);
        int i = str.length() - 1;

        while (w + sw >= width && i > 0)
        {
            w -= this.renderer.method_1727(String.valueOf(str.charAt(i)));
            i -= 1;
        }

        str = str.substring(0, i);

        return str.isEmpty() ? str : str + suffix;
    }

    public String prepare(String string)
    {
        if (RtlTextEngine.isActive())
        {
            RtlFontManager.ensureLoaded();

            if (!RtlAwtTextRenderer.isReady())
            {
                return RtlTextEngine.prepare(string);
            }
        }

        return string;
    }

    private static class StyleHolder
    {
        public class_2583 style;

        public StyleHolder(class_2583 style)
        {
            this.style = style;
        }
    }
}