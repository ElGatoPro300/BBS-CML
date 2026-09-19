package mchorse.bbs_mod.ui.framework.elements.utils;

import mchorse.bbs_mod.BBSSettings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.FontOption;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.GlyphStitcher;
import net.minecraft.client.gui.font.glyphs.EffectGlyph;
import net.minecraft.client.gui.font.providers.FreeTypeUtil;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.TrueTypeGlyphProvider;

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
 * Loads a user-selected TrueType (.ttf) font at runtime and exposes it as a Minecraft {@link Font}
 * so the whole BBS/CML UI can be drawn with it (see {@link Batcher2D#getDefaultTextRenderer()}).
 */
public class CustomFontManager
{
    private static final Identifier FONT_ID = Identifier.fromNamespaceAndPath("bbs", "custom_ui_font");

    private static final Identifier BUNDLED_FONT_ID = Identifier.fromNamespaceAndPath("bbs", "rtl_ui_font");

    private static Font customRenderer;

    private static FontSet fontStorage;

    private static String attemptedPath;

    private static float attemptedSize;

    private static String bundledFontId;

    private static Font bundledRenderer;

    private static FontSet bundledFontStorage;

    public static float getFontScale()
    {
        return BBSSettings.uiFontSize == null ? 1F : BBSSettings.uiFontSize.get();
    }

    private static float getFontPointSize()
    {
        return 11F * getFontScale();
    }

    public static Font getCustomRenderer()
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
        void accept(FontSet storage, Font renderer);
    }

    private static void loadFontBytes(byte[] bytes, Identifier fontId, FontLoadCallback callback)
    {
        ByteBuffer buffer = null;
        boolean ownedByFont = false;

        try
        {
            buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes);
            buffer.flip();

            FT_Face face;

            synchronized (FreeTypeUtil.LIBRARY_LOCK)
            {
                long library = FreeTypeUtil.getLibrary();
                PointerBuffer pointer = MemoryUtil.memAllocPointer(1);

                try
                {
                    FreeTypeUtil.assertError(FreeType.FT_New_Memory_Face(library, buffer, 0L, pointer), "Initializing font face");
                    face = FT_Face.create(pointer.get(0));
                }
                finally
                {
                    MemoryUtil.memFree(pointer);
                }
            }

            TrueTypeGlyphProvider font = new TrueTypeGlyphProvider(buffer, face, getFontPointSize(), 2F, 0F, 0F, "");

            ownedByFont = true;

            GlyphStitcher baker = new GlyphStitcher(Minecraft.getInstance().getTextureManager(), FONT_ID);
            FontSet storage = new FontSet(baker);

            storage.reload(List.of(new GlyphProvider.Conditional(font, FontOption.Filter.ALWAYS_PASS)), Set.of());

            Font renderer = new Font(new Font.Provider()
            {
                @Override
                public GlyphSource glyphs(FontDescription styleSpriteSource)
                {
                    return storage.source(false);
                }

                @Override
                public EffectGlyph effect()
                {
                    return null;
                }
            });

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
