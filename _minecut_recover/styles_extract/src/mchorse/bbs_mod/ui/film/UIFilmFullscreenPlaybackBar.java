package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.camera.utils.TimeUtils;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.interps.Lerps;
import mchorse.bbs_mod.utils.joml.Vectors;

import org.joml.Vector2i;

import org.lwjgl.glfw.GLFW;

public class UIFilmFullscreenPlaybackBar extends UIElement
{
    private static final int HEIGHT = 28;
    private static final int PLAY_WIDTH = 24;
    private static final int PADDING = 6;
    private static final int BOTTOM_MARGIN = 8;

    private UIFilmPanel panel;
    private boolean scrubbing;
    private float hoverAlpha;

    public UIFilmFullscreenPlaybackBar(UIFilmPanel panel)
    {
        this.panel = panel;
    }

    public boolean isKeybindActive()
    {
        return this.shouldBeVisible() && !this.panel.isFlying();
    }

    public void attachToRoot()
    {
        this.panel.getContext().menu.getRoot().add(this);
        this.syncLayout();
    }

    @Override
    public boolean canBeRendered(Area viewport)
    {
        if (this.shouldBeVisible())
        {
            return true;
        }

        return super.canBeRendered(viewport);
    }

    private void syncLayout()
    {
        UIContext context = this.panel.getContext();

        if (context != null)
        {
            this.applyLayout(context);
        }
    }

    private boolean shouldBeVisible()
    {
        return this.panel.getData() != null
            && this.panel.canToggleVisibility()
            && !this.panel.dashboard.main.isVisible();
    }

    private void applyLayout(UIContext context)
    {
        int screenW = context.menu.width;
        int screenH = context.menu.height;
        Texture texture = BBSRendering.getTexture();
        int barW;
        int barX;
        int barY;

        if (texture != null && texture.width > 0 && texture.height > 0)
        {
            Vector2i viewport = Vectors.resize(texture.width / (float) texture.height, screenW, screenH);
            int viewportX = (screenW - viewport.x) / 2;
            int viewportY = (screenH - viewport.y) / 2;

            barW = Math.max(200, viewport.x - 16);
            barX = viewportX + (viewport.x - barW) / 2;
            barY = viewportY + viewport.y - HEIGHT - BOTTOM_MARGIN;
        }
        else
        {
            barW = Math.max(200, screenW - 80);
            barX = (screenW - barW) / 2;
            barY = screenH - HEIGHT - 12;
        }

        this.area.set(barX, barY, barW, HEIGHT);
    }

    private int getDuration()
    {
        if (this.panel.getData() == null)
        {
            return 1;
        }

        return Math.max(1, this.panel.getData().camera.calculateDuration());
    }

    private int getTimelineX(int labelWidth)
    {
        return this.area.x + PLAY_WIDTH + labelWidth + PADDING;
    }

    private int getTimelineWidth(int labelWidth)
    {
        return Math.max(0, this.area.w - PLAY_WIDTH - labelWidth - PADDING * 2);
    }

    private int cursorToX(int cursor, int duration, int timelineX, int timelineW)
    {
        if (timelineW <= 0)
        {
            return timelineX;
        }

        return timelineX + (int) ((long) cursor * timelineW / duration);
    }

    private int xToCursor(int mouseX, int duration, int timelineX, int timelineW)
    {
        if (timelineW <= 0)
        {
            return 0;
        }

        float t = (mouseX - timelineX) / (float) timelineW;

        t = Math.max(0F, Math.min(1F, t));

        return (int) (t * duration);
    }

    private String getTimeLabel()
    {
        float current = TimeUtils.toSeconds(this.panel.getCursor());
        float total = TimeUtils.toSeconds(this.getDuration());

        return String.format("%.1f / %.1f", current, total);
    }

    @Override
    protected void afterResizeApplied()
    {
        this.syncLayout();
    }

    @Override
    protected boolean subMouseClicked(UIContext context)
    {
        if (!this.shouldBeVisible())
        {
            return false;
        }

        if (context.mouseButton != 0)
        {
            return super.subMouseClicked(context);
        }

        if (context.mouseX >= this.area.x && context.mouseX < this.area.x + PLAY_WIDTH && context.mouseY >= this.area.y && context.mouseY < this.area.ey())
        {
            this.panel.togglePlayback();

            return true;
        }

        FontRenderer font = context.batcher.getFont();
        String label = this.getTimeLabel();
        int labelWidth = font.getWidth(label) + PADDING;
        int timelineX = this.getTimelineX(labelWidth);
        int timelineW = this.getTimelineWidth(labelWidth);

        if (timelineW > 0 && context.mouseX >= timelineX && context.mouseX < timelineX + timelineW && context.mouseY >= this.area.y && context.mouseY < this.area.ey())
        {
            this.scrubbing = true;
            this.panel.setCursor(this.xToCursor(context.mouseX, this.getDuration(), timelineX, timelineW));

            return true;
        }

        return super.subMouseClicked(context);
    }

    @Override
    protected boolean subMouseReleased(UIContext context)
    {
        if (this.scrubbing && context.mouseButton == 0)
        {
            this.scrubbing = false;

            return true;
        }

        return super.subMouseReleased(context);
    }

    private void handleScrubbing(UIContext context, int timelineX, int timelineW)
    {
        if (this.scrubbing && Window.isMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_1))
        {
            this.panel.setCursor(this.xToCursor(context.mouseX, this.getDuration(), timelineX, timelineW));
        }
        else if (this.scrubbing)
        {
            this.scrubbing = false;
        }
    }

    @Override
    public void render(UIContext context)
    {
        if (!this.shouldBeVisible())
        {
            return;
        }

        this.applyLayout(context);

        Area.SHARED.set(this.area.x, this.area.y, this.area.w, this.area.h);
        boolean hovered = Area.SHARED.isInside(context);

        this.hoverAlpha = Lerps.lerp(this.hoverAlpha, hovered ? 1F : 0F, 0.2F);

        float bgAlpha = Lerps.lerp(0.35F, 0.9F, this.hoverAlpha);
        float textAlpha = Lerps.lerp(0.65F, 1F, this.hoverAlpha);
        float trackAlpha = Lerps.lerp(0.3F, 0.5F, this.hoverAlpha);
        float progressAlpha = Lerps.lerp(0.7F, 1F, this.hoverAlpha);

        int background = Colors.setA(Colors.CONTROL_BAR, bgAlpha);
        int textColor = Colors.mulA(Colors.WHITE, textAlpha);
        int trackColor = Colors.setA(Colors.WHITE, trackAlpha);
        int progressColor = Colors.mulA(Colors.CURSOR, progressAlpha);
        int cursorColor = Colors.mulA(Colors.CURSOR, progressAlpha);

        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), background);

        Icon playIcon = this.panel.isRunning() ? Icons.PAUSE : Icons.PLAY;
        int iconColor = Colors.mulA(Colors.WHITE, textAlpha);

        context.batcher.icon(playIcon, iconColor, this.area.x + PLAY_WIDTH / 2, this.area.my(), 0.5F, 0.5F);

        FontRenderer font = context.batcher.getFont();
        String label = this.getTimeLabel();
        int labelWidth = font.getWidth(label) + PADDING;
        int textY = this.area.y + (this.area.h - font.getHeight()) / 2;

        context.batcher.text(label, this.area.x + PLAY_WIDTH + 2, textY, textColor);

        int timelineX = this.getTimelineX(labelWidth);
        int timelineW = this.getTimelineWidth(labelWidth);
        int timelineY = this.area.y + this.area.h / 2;
        int cursor = this.panel.getCursor();
        int duration = this.getDuration();
        int progressX = this.cursorToX(cursor, duration, timelineX, timelineW);

        if (timelineW > 0)
        {
            context.batcher.box(timelineX, timelineY, timelineX + timelineW, timelineY + 2, trackColor);

            if (progressX > timelineX)
            {
                context.batcher.box(timelineX, timelineY, progressX, timelineY + 2, progressColor);
            }

            context.batcher.box(progressX, this.area.y + 4, progressX + 2, this.area.ey() - 4, cursorColor);
        }

        this.handleScrubbing(context, timelineX, timelineW);

        super.render(context);
    }
}
