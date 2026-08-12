package mchorse.bbs_mod.ui.dashboard.textures;

import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.textures.undo.PixelsUndo;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.utils.UICanvasEditor;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.interps.Lerps;
import mchorse.bbs_mod.utils.interps.rasterizers.LineRasterizer;
import mchorse.bbs_mod.utils.resources.Pixels;
import mchorse.bbs_mod.utils.undo.IUndo;
import mchorse.bbs_mod.utils.undo.UndoManager;

import org.joml.Vector2d;
import org.joml.Vector2i;

import org.lwjgl.opengl.GL11;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class UIPixelsEditor extends UICanvasEditor
{
    public enum Tool
    {
        BRUSH,
        ERASER,
        PICK,
        FILL
    }

    public enum BrushShape
    {
        SQUARE,
        CIRCLE
    }

    public UIElement toolbar;

    /* Tools */
    public UIIcon undo;
    public UIIcon redo;

    private Texture temporary;
    private Pixels pixels;

    private boolean editing;
    private Color drawColor;
    private Vector2i lastPixel;
    private int brushSize = 1;
    private BrushShape brushShape = BrushShape.SQUARE;

    protected UndoManager<Pixels> undoManager;
    private PixelsUndo pixelsUndo;

    private Supplier<Float> backgroundSupplier = () -> 0.7F;
    private Supplier<Color> colorSupplier = Color::white;
    private Consumer<Color> pickColorCallback;
    private BiConsumer<Vector2i, Boolean> fillColorCallback;
    private Tool activeTool = Tool.BRUSH;
    protected boolean showInternalToolbar = true;

    public UIPixelsEditor()
    {
        super();

        this.toolbar = new UIElement();
        this.toolbar.relative(this).w(1F).h(30).row(0).resize().padding(5);

        this.undo = new UIIcon(Icons.UNDO, (b) -> this.undo());
        this.undo.tooltip(UIKeys.TEXTURES_KEYS_UNDO, Direction.BOTTOM);
        this.redo = new UIIcon(Icons.REDO, (b) -> this.redo());
        this.redo.tooltip(UIKeys.TEXTURES_KEYS_REDO, Direction.BOTTOM);

        this.toolbar.add(this.undo, this.redo);

        this.add(this.toolbar);

        IKey category = UIKeys.TEXTURES_KEYS_CATEGORY;
        Supplier<Boolean> texture = () -> this.pixels != null;
        Supplier<Boolean> editing = () -> this.editing;

        this.keys().register(Keys.COPY, this::copyPixel).label(UIKeys.TEXTURES_VIEWER_CONTEXT_COPY_HEX).inside().active(texture).category(category);
        this.keys().register(Keys.UNDO, this::undo).inside().active(editing).category(category);
        this.keys().register(Keys.REDO, this::redo).inside().active(editing).category(category);

        this.setEditing(false);
    }

    public UIPixelsEditor colorSupplier(Supplier<Color> supplier)
    {
        this.colorSupplier = supplier;

        return this;
    }

    public UIPixelsEditor backgroundSupplier(Supplier<Float> supplier)
    {
        this.backgroundSupplier = supplier;

        return this;
    }

    public Pixels getPixels()
    {
        return this.pixels;
    }

    public int getBrushSize()
    {
        return this.brushSize;
    }

    public BrushShape getBrushShape()
    {
        return this.brushShape;
    }

    public UIPixelsEditor useExternalToolbar()
    {
        this.showInternalToolbar = false;
        this.toolbar.setVisible(false);

        return this;
    }

    public UIPixelsEditor onPickColor(Consumer<Color> callback)
    {
        this.pickColorCallback = callback;

        return this;
    }

    public UIPixelsEditor onFillColor(BiConsumer<Vector2i, Boolean> callback)
    {
        this.fillColorCallback = callback;

        return this;
    }

    public UIPixelsEditor setTool(Tool tool)
    {
        this.activeTool = tool == null ? Tool.BRUSH : tool;

        return this;
    }

    public Tool getTool()
    {
        return this.activeTool;
    }

    public void setBrushSize(int brushSize)
    {
        this.brushSize = Math.max(1, brushSize);
    }

    public UIPixelsEditor setBrushShape(BrushShape brushShape)
    {
        this.brushShape = brushShape == null ? BrushShape.SQUARE : brushShape;

        return this;
    }

    protected void wasChanged()
    {}

    public boolean isEditing()
    {
        return this.editing;
    }

    public void toggleEditor()
    {
        this.setEditing(!this.editing);
    }

    public void setEditing(boolean editing)
    {
        this.editing = editing;

        this.toolbar.setVisible(this.showInternalToolbar && editing);

        if (editing)
        {
            this.undoManager = new UndoManager<>();
            this.undoManager.setCallback(this::handleUndo);
        }
        else
        {
            this.undoManager = null;
        }

        this.pixelsUndo = null;
    }

    private void handleUndo(IUndo<Pixels> pixelsIUndo, boolean redo)
    {
        this.updateTexture();
    }

    private void copyPixel()
    {
        UIContext context = this.getContext();
        int pixelX = (int) Math.floor(this.scaleX.from(context.mouseX)) + this.w / 2;
        int pixelY = (int) Math.floor(this.scaleY.from(context.mouseY)) + this.h / 2;
        Color color = this.pixels.getColor(pixelX, pixelY);

        if (color != null)
        {
            Window.setClipboard(color.stringify());

            UIUtils.playClick();
        }
    }

    protected void updateTexture()
    {
        this.pixels.rewindBuffer();
        this.temporary.bind();
        this.temporary.updateTexture(this.pixels);
    }

    private void undo()
    {
        if (this.undoManager.undo(this.pixels))
        {
            UIUtils.playClick();
        }
    }

    private void redo()
    {
        if (this.undoManager.redo(this.pixels))
        {
            UIUtils.playClick();
        }
    }

    public UndoManager<Pixels> exportUndoManager()
    {
        return this.undoManager;
    }

    public void importUndoManager(UndoManager<Pixels> undoManager)
    {
        this.undoManager = undoManager == null ? new UndoManager<>() : undoManager;
        this.undoManager.setCallback(this::handleUndo);
        this.pixelsUndo = null;
    }

    public void fillPixels(Pixels pixels)
    {
        this.fillPixels(pixels, false);
    }

    public void fillPixels(Pixels pixels, boolean preserveView)
    {
        this.lastPixel = null;
        double oldZoomX = this.scaleX.getZoom();
        double oldZoomY = this.scaleY.getZoom();
        double oldShiftX = this.scaleX.getShift();
        double oldShiftY = this.scaleY.getShift();
        int oldW = this.w;
        int oldH = this.h;

        if (this.temporary != null)
        {
            this.temporary.delete();
            this.temporary = null;
        }

        this.setEditing(false);

        this.pixels = pixels;

        if (pixels != null)
        {
            this.temporary = new Texture();
            this.temporary.setFilter(GL11.GL_NEAREST);

            this.updateTexture();
            this.setSize(pixels.width, pixels.height);

            if (preserveView && oldW == pixels.width && oldH == pixels.height)
            {
                this.scaleX.setZoom(oldZoomX);
                this.scaleY.setZoom(oldZoomY);
                this.scaleX.setShift(oldShiftX);
                this.scaleY.setShift(oldShiftY);
            }
        }
    }

    @Override
    protected boolean isMouseButtonAllowed(int mouseButton)
    {
        return super.isMouseButtonAllowed(mouseButton) || mouseButton == 1;
    }

    @Override
    protected void startDragging(UIContext context)
    {
        super.startDragging(context);

        boolean canPaint = this.mouse == 1 || this.activeTool == Tool.BRUSH || this.activeTool == Tool.ERASER;

        if (this.editing && canPaint && (this.mouse == 0 || this.mouse == 1) && this.pixelsUndo == null)
        {
            this.pixelsUndo = new PixelsUndo();
            this.drawColor = (this.mouse == 1 || this.activeTool == Tool.ERASER) ? new Color(0, 0, 0, 0) : this.colorSupplier.get();

            Vector2i pixel = this.getHoverPixel(context.mouseX, context.mouseY);

            this.applyBrush(this.pixelsUndo, pixel.x, pixel.y, this.drawColor);
            this.updateTexture();

            this.wasChanged();
        }
    }

    @Override
    public boolean subMouseReleased(UIContext context)
    {
        if (this.dragging && this.pixelsUndo != null)
        {
            Vector2i hoverPixel = this.getHoverPixel(context.mouseX, context.mouseY);

            if (Window.isShiftPressed() && this.lastPixel != null)
            {
                LineRasterizer rasterizer = new LineRasterizer(
                    new Vector2d(this.lastPixel.x, this.lastPixel.y),
                    new Vector2d(hoverPixel.x, hoverPixel.y)
                );
                Set<Vector2i> pixels = new HashSet<>();

                rasterizer.setupRange(0F, 1F, 1F / (float) this.lastPixel.distance(hoverPixel));
                rasterizer.solve(pixels);

                for (Vector2i pixel : pixels)
                {
                    this.applyBrush(this.pixelsUndo, pixel.x, pixel.y, this.drawColor);
                }

                this.updateTexture();
            }

            this.undoManager.pushUndo(this.pixelsUndo);

            this.pixelsUndo = null;
            this.lastPixel = hoverPixel;
        }

        return super.subMouseReleased(context);
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (this.editing && this.pixels != null && this.area.isInside(context) && context.mouseButton == 0)
        {
            Vector2i pixel = this.getHoverPixel(context.mouseX, context.mouseY);

            if (this.activeTool == Tool.PICK)
            {
                if (this.pickColorCallback != null)
                {
                    Color color = this.pixels.getColor(pixel.x, pixel.y);

                    if (color != null)
                    {
                        this.pickColorCallback.accept(color.copy());
                    }
                }

                return true;
            }

            if (this.activeTool == Tool.FILL)
            {
                if (this.fillColorCallback != null)
                {
                    this.fillColorCallback.accept(pixel, Window.isShiftPressed());
                }

                return true;
            }
        }

        return super.subMouseClicked(context);
    }

    @Override
    protected void renderBackground(UIContext context)
    {}

    @Override
    protected void renderCanvasFrame(UIContext context)
    {
        int x = -this.w / 2;
        int y = -this.h / 2;
        Area area = this.calculate(x, y, x + this.w, y + this.h);
        Texture texture = this.getRenderTexture(context);

        context.batcher.fullTexturedBox(texture, area.x, area.y, area.w, area.h);

        /* Draw current pixel */
        int pixelX = (int) Math.floor(this.scaleX.from(context.mouseX));
        int pixelY = (int) Math.floor(this.scaleY.from(context.mouseY));

        this.renderBrushPreview(context, pixelX, pixelY);

        if (this.editing && this.dragging && this.pixelsUndo != null && (this.lastX != context.mouseX || this.lastY != context.mouseY) && (this.mouse == 0 || this.mouse == 1))
        {
            Vector2i last = this.getHoverPixel(this.lastX, this.lastY);
            Vector2i current = this.getHoverPixel(context.mouseX, context.mouseY);

            double distance = Math.max(new Vector2d(current.x, current.y).distance(last.x, last.y), 1);

            for (int i = 0; i <= distance; i++)
            {
                int xx = (int) Lerps.lerp(last.x, current.x, i / distance);
                int yy = (int) Lerps.lerp(last.y, current.y, i / distance);

                this.applyBrush(this.pixelsUndo, xx, yy, this.drawColor);
            }

            this.wasChanged();
            this.updateTexture();

            this.lastX = context.mouseX;
            this.lastY = context.mouseY;
        }
    }

    protected Texture getRenderTexture(UIContext context)
    {
        return this.temporary;
    }

    private void applyBrush(PixelsUndo undo, int x, int y, Color color)
    {
        int minX = x - (this.brushSize - 1) / 2;
        int minY = y - (this.brushSize - 1) / 2;

        for (int i = 0; i < this.brushSize; i++)
        {
            for (int j = 0; j < this.brushSize; j++)
            {
                if (this.isBrushOffsetInside(i, j))
                {
                    undo.setColor(this.pixels, minX + i, minY + j, color);
                }
            }
        }
    }

    private boolean isBrushOffsetInside(int offsetX, int offsetY)
    {
        if (this.brushShape == BrushShape.SQUARE)
        {
            return true;
        }

        float center = (this.brushSize - 1) / 2F;
        float radius = Math.max(0.5F, this.brushSize / 2F);
        float dx = offsetX - center;
        float dy = offsetY - center;

        return dx * dx + dy * dy <= radius * radius;
    }

    private void renderBrushPreview(UIContext context, int pixelX, int pixelY)
    {
        int brushMinX = pixelX - (this.brushSize - 1) / 2;
        int brushMinY = pixelY - (this.brushSize - 1) / 2;

        if (this.brushShape == BrushShape.SQUARE)
        {
            int brushMaxX = brushMinX + this.brushSize;
            int brushMaxY = brushMinY + this.brushSize;

            context.batcher.outline(
                (int) Math.round(this.scaleX.to(brushMinX)), (int) Math.round(this.scaleY.to(brushMinY)),
                (int) Math.round(this.scaleX.to(brushMaxX)), (int) Math.round(this.scaleY.to(brushMaxY)),
                Colors.A50
            );

            return;
        }

        for (int i = 0; i < this.brushSize; i++)
        {
            for (int j = 0; j < this.brushSize; j++)
            {
                if (!this.isBrushOffsetInside(i, j))
                {
                    continue;
                }

                int cellMinX = (int) Math.round(this.scaleX.to(brushMinX + i));
                int cellMinY = (int) Math.round(this.scaleY.to(brushMinY + j));
                int cellMaxX = (int) Math.round(this.scaleX.to(brushMinX + i + 1));
                int cellMaxY = (int) Math.round(this.scaleY.to(brushMinY + j + 1));

                if (!this.isBrushOffsetInsideBounds(i - 1, j))
                {
                    context.batcher.box(cellMinX, cellMinY, cellMinX + 1, cellMaxY, Colors.A50);
                }

                if (!this.isBrushOffsetInsideBounds(i + 1, j))
                {
                    context.batcher.box(cellMaxX - 1, cellMinY, cellMaxX, cellMaxY, Colors.A50);
                }

                if (!this.isBrushOffsetInsideBounds(i, j - 1))
                {
                    context.batcher.box(cellMinX, cellMinY, cellMaxX, cellMinY + 1, Colors.A50);
                }

                if (!this.isBrushOffsetInsideBounds(i, j + 1))
                {
                    context.batcher.box(cellMinX, cellMaxY - 1, cellMaxX, cellMaxY, Colors.A50);
                }
            }
        }
    }

    private boolean isBrushOffsetInsideBounds(int offsetX, int offsetY)
    {
        if (offsetX < 0 || offsetY < 0 || offsetX >= this.brushSize || offsetY >= this.brushSize)
        {
            return false;
        }

        return this.isBrushOffsetInside(offsetX, offsetY);
    }

    @Override
    protected void renderCheckboard(UIContext context, Area area)
    {
        int brightness = (int) (this.backgroundSupplier.get() * 255);
        int color = Colors.setA(brightness << 16 | brightness << 8 | brightness, 1F);

        context.batcher.iconArea(Icons.CHECKBOARD, color, area.x, area.y, area.w, area.h);
    }

    @Override
    protected void renderForeground(UIContext context)
    {
        super.renderForeground(context);

        if (this.editing)
        {
            if (this.showInternalToolbar)
            {
                context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.y + 10, Colors.A50);
                context.batcher.gradientVBox(this.area.x, this.area.y + 10, this.area.ex(), this.area.y + 30, Colors.A50, 0);
            }
        }
    }
}
