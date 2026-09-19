package mchorse.bbs_mod.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;

import org.lwjgl.glfw.GLFW;

public class WorldExportWindowSession
{
    private WindowSnapshot snapshot;
    private boolean changed;

    public void begin(int width, int height)
    {
        Window window = MinecraftClient.getInstance().getWindow();
        long handle = window.getHandle();

        if (this.snapshot == null)
        {
            this.snapshot = WindowSnapshot.capture(window, handle);
            this.changed = false;
        }

        if (this.snapshot.fullscreen)
        {
            return;
        }

        if (this.snapshot.maximized)
        {
            GLFW.glfwRestoreWindow(handle);
        }

        if (window.getWidth() != width || window.getHeight() != height)
        {
            window.setWindowedSize(width, height);
        }

        this.restoreOriginalPosition(handle);

        this.changed = this.snapshot.maximized
            || this.snapshot.width != width
            || this.snapshot.height != height;
    }

    public void restore()
    {
        if (this.snapshot == null)
        {
            return;
        }

        Window window = MinecraftClient.getInstance().getWindow();
        long handle = window.getHandle();

        if (!this.snapshot.fullscreen && this.changed)
        {
            int width = Math.max(this.snapshot.width, 2);
            int height = Math.max(this.snapshot.height, 2);

            if (window.getWidth() != width || window.getHeight() != height)
            {
                window.setWindowedSize(width, height);
            }

            this.restoreOriginalPosition(handle);

            if (this.snapshot.maximized)
            {
                GLFW.glfwMaximizeWindow(handle);
            }
        }

        this.clear();
    }

    public void clear()
    {
        this.snapshot = null;
        this.changed = false;
    }

    private void restoreOriginalPosition(long handle)
    {
        Position position = Position.capture(handle);

        if (position.x != this.snapshot.x || position.y != this.snapshot.y)
        {
            GLFW.glfwSetWindowPos(handle, this.snapshot.x, this.snapshot.y);
        }
    }

    private static class WindowSnapshot
    {
        private final int width;
        private final int height;
        private final int x;
        private final int y;
        private final boolean maximized;
        private final boolean fullscreen;

        private WindowSnapshot(int width, int height, int x, int y, boolean maximized, boolean fullscreen)
        {
            this.width = width;
            this.height = height;
            this.x = x;
            this.y = y;
            this.maximized = maximized;
            this.fullscreen = fullscreen;
        }

        private static WindowSnapshot capture(Window window, long handle)
        {
            Position position = Position.capture(handle);
            boolean maximized = GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_MAXIMIZED) == GLFW.GLFW_TRUE;
            boolean fullscreen = GLFW.glfwGetWindowMonitor(handle) != 0L;

            return new WindowSnapshot(window.getWidth(), window.getHeight(), position.x, position.y, maximized, fullscreen);
        }
    }

    private static class Position
    {
        private final int x;
        private final int y;

        private Position(int x, int y)
        {
            this.x = x;
            this.y = y;
        }

        private static Position capture(long handle)
        {
            int[] x = new int[1];
            int[] y = new int[1];

            GLFW.glfwGetWindowPos(handle, x, y);

            return new Position(x[0], y[0]);
        }
    }
}
