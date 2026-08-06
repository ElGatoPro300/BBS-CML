package mchorse.bbs_mod.ui.framework.elements.utils;

import mchorse.bbs_mod.BBSSettings;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.Font;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.font.TrueTypeFont;
import net.minecraft.util.Identifier;

import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.List;

/**
 * Loads a user-selected TrueType (.ttf) font at runtime using LWJGL STB bindings
 * and exposes it as a Minecraft {@link TextRenderer} for 1.20.4 compatibility.
 */
public class CustomFontManager
{
    private static final Identifier FONT_ID = Identifier.of("bbs", "custom_ui_font");

    private static final Identifier BUNDLED_FONT_ID = Identifier.of("bbs", "rtl_ui_font");

    private static TextRenderer customRenderer;

    private static FontStorage fontStorage;

    private static String attemptedPath;

    private static float attemptedSize;

    private static String bundledFontId;

    private static TextRenderer bundledRenderer;

    private static FontStorage bundledFontStorage;

    public static float getFontScale()
    {
        return BBSSettings.uiFontSize == null ? 1F : BBSSettings.uiFontSize.get();
    }

    private static float getFontPointSize()
    {
        return 11F * getFontScale();
    }

    public static TextRenderer getCustomRenderer()
    {
        if (customRenderer != null)
        {
            return customRenderer;
        }

        return bundledRenderer;
    }

    public static boolean hasCustomFont()
    {
        return customRenderer != null || bundledRenderer != null;
    }

    public static boolean hasUserCustomFont()
    {
        return customRenderer != null;
    }

    public static void ensureLoaded()
    {
        float size = getFontScale();

        if (BBSSettings.uiFont == null || BBSSettings.uiFont.get().trim().isEmpty())
        {
            if (attemptedPath != null && attemptedPath.isEmpty() && attemptedSize == size && customRenderer == null)
            {
                return;
            }
        }

        applyPath(BBSSettings.uiFont == null ? "" : BBSSettings.uiFont.get(), size);
    }

    public static String getConfiguredFontPath()
    {
        if (BBSSettings.uiFont == null)
        {
            return "";
        }

        return BBSSettings.uiFont.get().trim();
    }

    public static byte[] readConfiguredFontBytes()
    {
        String path = getConfiguredFontPath();

        if (path.isEmpty())
        {
            return null;
        }

        File file = new File(path);

        if (!file.isFile())
        {
            return null;
        }

        try
        {
            return Files.readAllBytes(file.toPath());
        }
        catch (Throwable t)
        {
            t.printStackTrace();

            return null;
        }
    }

    public static void invalidate()
    {
        attemptedPath = null;
        attemptedSize = -1F;
    }

    public static void invalidateBundledFont()
    {
        bundledFontId = null;
        disposeBundledFont();
    }

    public static void loadBundledFont(byte[] bytes, String sourceId)
    {
        if (sourceId != null && sourceId.equals(bundledFontId) && bundledRenderer != null)
        {
            return;
        }

        bundledFontId = sourceId;

        loadFontBytes(bytes, BUNDLED_FONT_ID, (storage, renderer) ->
        {
            disposeBundledFont();
            bundledFontStorage = storage;
            bundledRenderer = renderer;
        });
    }

    private static void applyPath(String path, float size)
    {
        String normalized = path == null ? "" : path.trim();

        if (normalized.equals(attemptedPath) && size == attemptedSize)
        {
            return;
        }

        attemptedPath = normalized;
        attemptedSize = size;

        if (normalized.isEmpty())
        {
            disposeFont();
            customRenderer = null;

            return;
        }

        File file = new File(normalized);

        if (!file.isFile())
        {
            disposeFont();
            customRenderer = null;

            return;
        }

        try
        {
            byte[] bytes = Files.readAllBytes(file.toPath());

            loadFontBytes(bytes, FONT_ID, (storage, renderer) ->
            {
                disposeFont();
                fontStorage = storage;
                customRenderer = renderer;
            });
        }
        catch (Throwable t)
        {
            t.printStackTrace();
            disposeFont();
            customRenderer = null;
        }
    }

    private interface FontLoadCallback
    {
        void accept(FontStorage storage, TextRenderer renderer);
    }

    private static void loadFontBytes(byte[] bytes, Identifier fontId, FontLoadCallback callback)
    {
        ByteBuffer buffer = null;
        STBTTFontinfo info = null;
        boolean ownedByFont = false;

        try
        {
            buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes);
            buffer.flip();

            info = STBTTFontinfo.malloc();
            if (!STBTruetype.stbtt_InitFont(info, buffer))
            {
                throw new IOException("Failed to initialize TrueType font via STB Truetype");
            }

            TrueTypeFont font = new TrueTypeFont(buffer, info, getFontPointSize(), 2F, 0F, 0F, "");

            ownedByFont = true;

            FontStorage storage = new FontStorage(MinecraftClient.getInstance().getTextureManager(), fontId);

            storage.setFonts(List.of(font));

            TextRenderer renderer = new TextRenderer((id) -> storage, false);

            callback.accept(storage, renderer);
        }
        catch (Throwable t)
        {
            t.printStackTrace();

            if (info != null && !ownedByFont)
            {
                info.free();
            }

            if (buffer != null && !ownedByFont)
            {
                MemoryUtil.memFree(buffer);
            }
        }
    }

    private static void disposeFont()
    {
        if (fontStorage != null)
        {
            try
            {
                fontStorage.close();
            }
            catch (Exception e)
            {}

            fontStorage = null;
        }
    }

    private static void disposeBundledFont()
    {
        if (bundledFontStorage != null)
        {
            try
            {
                bundledFontStorage.close();
            }
            catch (Exception e)
            {}

            bundledFontStorage = null;
        }

        bundledRenderer = null;
    }
}
