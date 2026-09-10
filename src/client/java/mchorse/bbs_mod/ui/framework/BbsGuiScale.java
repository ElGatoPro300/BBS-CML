package mchorse.bbs_mod.ui.framework;

import mchorse.bbs_mod.BBSSettings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PerspectiveProjectionMatrixBuffer;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;

/**
 * BBS UI scale helpers. By default the BBS GUI uses {@link BBSSettings#userIntefaceScale}
 * without writing Minecraft's GUI scale. Linking to the game (legacy) is opt-in.
 */
public final class BbsGuiScale
{
    private static boolean restoringGameScale;
    private static PerspectiveProjectionMatrixBuffer bbsGuiProjection;

    private BbsGuiScale()
    {}

    public static boolean isLinkedToGame()
    {
        return BBSSettings.isUiScaleLinkedToGame();
    }

    public static boolean isRestoringGameScale()
    {
        return restoringGameScale;
    }

    public static int getGameScaleFactor()
    {
        Minecraft mc = Minecraft.getInstance();
        Window window = mc.getWindow();
        final int[] scale = { 1 };

        restoringGameScale(() -> scale[0] = window.getGuiScale());

        return scale[0];
    }

    /**
     * Exact BBS scale factor. {@code 0} means “use the window's current (game) scale”.
     */
    public static double getFactor()
    {
        float scale = BBSSettings.getUIScaleFactor();

        if (scale <= 0F)
        {
            Minecraft mc = Minecraft.getInstance();

            return mc == null || mc.getWindow() == null ? 1D : mc.getWindow().getGuiScale();
        }

        return scale;
    }

    public static int getScaledWidth()
    {
        Window window = Minecraft.getInstance().getWindow();
        double factor = getFactor();

        return scaledSize(window.getWidth(), factor <= 0 ? window.getGuiScale() : factor);
    }

    public static int getScaledHeight()
    {
        Window window = Minecraft.getInstance().getWindow();
        double factor = getFactor();

        return scaledSize(window.getHeight(), factor <= 0 ? window.getGuiScale() : factor);
    }

    public static void resizeMenu(UIBaseMenu menu)
    {
        if (menu != null)
        {
            menu.resize(getScaledWidth(), getScaledHeight());
        }
    }

    public static int toBbsMouseX(double mouseX)
    {
        return toBbsMouse(mouseX);
    }

    public static int toBbsMouseY(double mouseY)
    {
        return toBbsMouse(mouseY);
    }

    public static void restoringGameScale(Runnable runnable)
    {
        restoringGameScale = true;

        try
        {
            runnable.run();
        }
        finally
        {
            restoringGameScale = false;
        }
    }

    /**
     * Runs {@code draw} under BBS GUI scale: sets the Minecraft window scale factor so Spruche
     * and GUI projection match BBS coordinates. Restores the game scale afterward
     * so the hotbar / vanilla menus stay on Minecraft's GUI scale.
     */
    public static void withBbsWindowScale(Runnable draw)
    {
        Minecraft mc = Minecraft.getInstance();
        Window window = mc.getWindow();
        int saved = getGameScaleFactor();
        boolean linked = isLinkedToGame();
        int targetScale = linked || getFactor() <= 0D ? saved : (int) getFactor();

        try
        {
            if (!linked)
            {
                window.setGuiScale(targetScale);
            }

            int sw = window.getGuiScaledWidth();
            int sh = window.getGuiScaledHeight();

            if (bbsGuiProjection == null)
            {
                bbsGuiProjection = new PerspectiveProjectionMatrixBuffer("bbs_gui");
            }

            RenderSystem.backupProjectionMatrix();
            RenderSystem.setProjectionMatrix(bbsGuiProjection.getBuffer(new Matrix4f().ortho(0, sw, sh, 0, -1000, 3000)), ProjectionType.ORTHOGRAPHIC);

            /* GameRenderer's GUI pass leaves modelView at z=-11000; with ortho
             * -1000..3000 that clips every vertex. Identity matches HUD overlays. */
            Matrix4fStack modelView = RenderSystem.getModelViewStack();

            modelView.pushMatrix();
            modelView.identity();

            try
            {
                draw.run();
            }
            finally
            {
                modelView.popMatrix();
            }
        }
        finally
        {
            if (!linked)
            {
                restoringGameScale(() -> window.setGuiScale(saved));
            }

            RenderSystem.restoreProjectionMatrix();
        }
    }

    private static int toBbsMouse(double mouse)
    {
        if (isLinkedToGame())
        {
            return (int) mouse;
        }

        Window window = Minecraft.getInstance().getWindow();
        double bbs = getFactor();

        if (bbs <= 0D)
        {
            return (int) mouse;
        }

        return (int) (mouse * window.getGuiScale() / bbs);
    }

    private static int scaledSize(int framebuffer, double factor)
    {
        if (factor <= 0D)
        {
            return Math.max(1, framebuffer);
        }

        int i = (int) (framebuffer / factor);

        return framebuffer / factor > i ? i + 1 : i;
    }
}
