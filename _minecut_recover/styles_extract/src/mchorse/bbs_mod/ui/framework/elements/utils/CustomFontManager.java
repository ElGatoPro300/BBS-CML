package mchorse.bbs_mod.ui.framework.elements.utils;

import mchorse.bbs_mod.BBSSettings;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_377;
import net.minecraft.class_390;
import net.minecraft.class_395;
import net.minecraft.class_9111;
import net.minecraft.class_9243;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FreeType;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

/**
 * Loads a user-selected TrueType (.ttf) font at runtime and exposes it as a Minecraft {@link class_327}
 * so the whole BBS/CML UI can be drawn with it (see {@link Batcher2D#getDefaultTextRenderer()}).
 */
public class CustomFontManager
{
    private static final class_2960 FONT_ID = class_2960.method_60655("bbs", "custom_ui_font");

    private static final class_2960 BUNDLED_FONT_ID = class_2960.method_60655("bbs", "rtl_ui_font");

    private static class_327 customRenderer;

    private static class_377 fontStorage;

    private static String attemptedPath;

    private static float attemptedSize;

    private static String bundledFontId;

    private static class_327 bundledRenderer;

    private static class_377 bundledFontStorage;

    public static float getFontScale()
    {
        return BBSSettings.uiFontSize == null ? 1F : BBSSettings.uiFontSize.get();
    }

    private static float getFontPointSize()
    {
        return 11F * getFontScale();
    }

    public static class_327 getCustomRenderer()
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
        void accept(class_377 storage, class_327 renderer);
    }

    private static void loadFontBytes(byte[] bytes, class_2960 fontId, FontLoadCallback callback)
    {
        ByteBuffer buffer = null;
        boolean ownedByFont = false;

        try
        {
            buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes);
            buffer.flip();

            FT_Face face;

            synchronized (class_9111.field_51483)
            {
                long library = class_9111.method_56143();
                PointerBuffer pointer = MemoryUtil.memAllocPointer(1);

                try
                {
                    class_9111.method_59837(FreeType.FT_New_Memory_Face(library, buffer, 0L, pointer), "Initializing font face");
                    face = FT_Face.create(pointer.get(0));
                }
                finally
                {
                    MemoryUtil.memFree(pointer);
                }
            }

            class_395 font = new class_395(buffer, face, getFontPointSize(), 2F, 0F, 0F, "");

            ownedByFont = true;

            class_377 storage = new class_377(class_310.method_1551().method_1531(), fontId);

            storage.method_2004(List.of(new class_390.class_9241(font, class_9243.class_9244.field_49118)), Set.of());

            class_327 renderer = new class_327((id) -> storage, false);

            callback.accept(storage, renderer);
        }
        catch (Throwable t)
        {
            t.printStackTrace();

            if (!ownedByFont && buffer != null)
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
