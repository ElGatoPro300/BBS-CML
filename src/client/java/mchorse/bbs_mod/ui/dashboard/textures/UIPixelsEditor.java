package mchorse.bbs_mod.ui.dashboard.textures;

import mchorse.bbs_mod.BBSSettings;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
        FILL,
        SHAPE,
        GRADIENT
    }

    public enum BrushShape
    {
        SQUARE,
        CIRCLE
    }

    public enum ShapeType
    {
        RECTANGLE,
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
    private Supplier<Color> secondaryColorSupplier = () -> new Color(0, 0, 0, 1F);
    private Consumer<Color> pickColorCallback;
    private BiConsumer<Vector2i, Boolean> fillColorCallback;
    private Tool activeTool = Tool.BRUSH;
    private boolean lockAlpha = false;
    private boolean mirrorX = false;
    private boolean mirrorY = false;
    private boolean pixelPerfect = false;
    private ShapeType shapeType = ShapeType.RECTANGLE;
    private boolean shapeFilled = false;
    private Vector2i dragStartPixel;
    private final List<Vector2i> strokePoints = new ArrayList<>();
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

    public boolean isLockAlpha()
    {
        return this.lockAlpha;
    }

    public UIPixelsEditor setLockAlpha(boolean lockAlpha)
    {
        this.lockAlpha = lockAlpha;

        return this;
    }

    public boolean isMirrorX()
    {
        return this.mirrorX;
    }

    public UIPixelsEditor setMirrorX(boolean mirrorX)
    {
        this.mirrorX = mirrorX;

        return this;
    }

    public boolean isMirrorY()
    {
        return this.mirrorY;
    }

    public UIPixelsEditor setMirrorY(boolean mirrorY)
    {
        this.mirrorY = mirrorY;

        return this;
    }

    public boolean isPixelPerfect()
    {
        return this.pixelPerfect;
    }

    public UIPixelsEditor setPixelPerfect(boolean pixelPerfect)
    {
        this.pixelPerfect = pixelPerfect;

        return this;
    }

    public ShapeType getShapeType()
    {
        return this.shapeType;
    }

    public UIPixelsEditor setShapeType(ShapeType shapeType)
    {
        this.shapeType = shapeType == null ? ShapeType.RECTANGLE : shapeType;

        return this;
    }

    public boolean isShapeFilled()
    {
        return this.shapeFilled;
    }

    public UIPixelsEditor setShapeFilled(boolean shapeFilled)
    {
        this.shapeFilled = shapeFilled;

        return this;
    }

    public UIPixelsEditor secondaryColorSupplier(Supplier<Color> supplier)
    {
        this.secondaryColorSupplier = supplier == null ? () -> new Color(0, 0, 0, 1F) : supplier;

        return this;
    }

    public void flipHorizontal()
    {
        if (this.pixels == null || this.pixels.width <= 0 || this.pixels.height <= 0)
        {
            return;
        }

        PixelsUndo undo = new PixelsUndo();

        for (int y = 0; y < this.pixels.height; y++)
        {
            for (int x = 0; x < this.pixels.width / 2; x++)
            {
                int ox = this.pixels.width - 1 - x;
                Color left = this.pixels.getColor(x, y);
                Color right = this.pixels.getColor(ox, y);

                undo.setColor(this.pixels, x, y, right == null ? new Color(0, 0, 0, 0) : right.copy());
                undo.setColor(this.pixels, ox, y, left == null ? new Color(0, 0, 0, 0) : left.copy());
            }
        }

        if (!undo.pixels.isEmpty())
        {
            this.undoManager.pushUndo(undo);
            this.wasChanged();
            this.updateTexture();
        }
    }

    public void flipVertical()
    {
        if (this.pixels == null || this.pixels.width <= 0 || this.pixels.height <= 0)
        {
            return;
        }

        PixelsUndo undo = new PixelsUndo();

        for (int x = 0; x < this.pixels.width; x++)
        {
            for (int y = 0; y < this.pixels.height / 2; y++)
            {
                int oy = this.pixels.height - 1 - y;
                Color top = this.pixels.getColor(x, y);
                Color bottom = this.pixels.getColor(x, oy);

                undo.setColor(this.pixels, x, y, bottom == null ? new Color(0, 0, 0, 0) : bottom.copy());
                undo.setColor(this.pixels, x, oy, top == null ? new Color(0, 0, 0, 0) : top.copy());
            }
        }

        if (!undo.pixels.isEmpty())
        {
            this.undoManager.pushUndo(undo);
            this.wasChanged();
            this.updateTexture();
        }
    }

    public void rotate90(boolean clockwise)
    {
        if (this.pixels == null || this.pixels.width <= 0 || this.pixels.height <= 0)
        {
            return;
        }

        Pixels copy = Pixels.fromSize(this.pixels.width, this.pixels.height);
        copy.draw(this.pixels, 0, 0, copy.width, copy.height);

        PixelsUndo undo = new PixelsUndo();

        for (int x = 0; x < this.pixels.width; x++)
        {
            for (int y = 0; y < this.pixels.height; y++)
            {
                int srcX = clockwise ? y : this.pixels.width - 1 - y;
                int srcY = clockwise ? this.pixels.height - 1 - x : x;

                if (srcX >= 0 && srcX < copy.width && srcY >= 0 && srcY < copy.height)
                {
                    Color color = copy.getColor(srcX, srcY);
                    undo.setColor(this.pixels, x, y, color == null ? new Color(0, 0, 0, 0) : color.copy());
                }
            }
        }

        copy.delete();

        if (!undo.pixels.isEmpty())
        {
            this.undoManager.pushUndo(undo);
            this.wasChanged();
            this.updateTexture();
        }
    }

    public void rotate180()
    {
        if (this.pixels == null || this.pixels.width <= 0 || this.pixels.height <= 0)
        {
            return;
        }

        Pixels copy = Pixels.fromSize(this.pixels.width, this.pixels.height);
        copy.draw(this.pixels, 0, 0, copy.width, copy.height);

        PixelsUndo undo = new PixelsUndo();

        for (int x = 0; x < this.pixels.width; x++)
        {
            for (int y = 0; y < this.pixels.height; y++)
            {
                int srcX = this.pixels.width - 1 - x;
                int srcY = this.pixels.height - 1 - y;
                Color color = copy.getColor(srcX, srcY);

                undo.setColor(this.pixels, x, y, color == null ? new Color(0, 0, 0, 0) : color.copy());
            }
        }

        copy.delete();

        if (!undo.pixels.isEmpty())
        {
            this.undoManager.pushUndo(undo);
            this.wasChanged();
            this.updateTexture();
        }
    }

    public void invertColors()
    {
        if (this.pixels == null || this.pixels.width <= 0 || this.pixels.height <= 0)
        {
            return;
        }

        PixelsUndo undo = new PixelsUndo();

        for (int x = 0; x < this.pixels.width; x++)
        {
            for (int y = 0; y < this.pixels.height; y++)
            {
                Color color = this.pixels.getColor(x, y);

                if (color != null && color.a > 0F)
                {
                    Color inverted = new Color(1F - color.r, 1F - color.g, 1F - color.b, color.a);
                    undo.setColor(this.pixels, x, y, inverted);
                }
            }
        }

        if (!undo.pixels.isEmpty())
        {
            this.undoManager.pushUndo(undo);
            this.wasChanged();
            this.updateTexture();
        }
    }

    public void grayscale()
    {
        if (this.pixels == null || this.pixels.width <= 0 || this.pixels.height <= 0)
        {
            return;
        }

        PixelsUndo undo = new PixelsUndo();

        for (int x = 0; x < this.pixels.width; x++)
        {
            for (int y = 0; y < this.pixels.height; y++)
            {
                Color color = this.pixels.getColor(x, y);

                if (color != null && color.a > 0F)
                {
                    float lum = 0.299F * color.r + 0.587F * color.g + 0.114F * color.b;
                    Color gray = new Color(lum, lum, lum, color.a);
                    undo.setColor(this.pixels, x, y, gray);
                }
            }
        }

        if (!undo.pixels.isEmpty())
        {
            this.undoManager.pushUndo(undo);
            this.wasChanged();
            this.updateTexture();
        }
    }

    public void adjustBrightnessContrast(float brightness, float contrast)
    {
        if (this.pixels == null || this.pixels.width <= 0 || this.pixels.height <= 0)
        {
            return;
        }

        PixelsUndo undo = new PixelsUndo();

        for (int x = 0; x < this.pixels.width; x++)
        {
            for (int y = 0; y < this.pixels.height; y++)
            {
                Color color = this.pixels.getColor(x, y);

                if (color != null && color.a > 0F)
                {
                    float r = (color.r - 0.5F) * (1F + contrast) + 0.5F + brightness;
                    float g = (color.g - 0.5F) * (1F + contrast) + 0.5F + brightness;
                    float b = (color.b - 0.5F) * (1F + contrast) + 0.5F + brightness;

                    r = Math.max(0F, Math.min(1F, r));
                    g = Math.max(0F, Math.min(1F, g));
                    b = Math.max(0F, Math.min(1F, b));

                    Color adjusted = new Color(r, g, b, color.a);
                    undo.setColor(this.pixels, x, y, adjusted);
                }
            }
        }

        if (!undo.pixels.isEmpty())
        {
            this.undoManager.pushUndo(undo);
            this.wasChanged();
            this.updateTexture();
        }
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

        this.strokePoints.clear();

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
        else if (this.editing && (this.activeTool == Tool.SHAPE || this.activeTool == Tool.GRADIENT) && context.mouseButton == 0)
        {
            this.dragStartPixel = this.getHoverPixel(context.mouseX, context.mouseY);
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
            this.strokePoints.clear();
        }
        else if (this.dragging && this.dragStartPixel != null)
        {
            Vector2i hoverPixel = this.getHoverPixel(context.mouseX, context.mouseY);

            if (this.activeTool == Tool.SHAPE)
            {
                this.rasterizeShape(this.dragStartPixel, hoverPixel, Window.isShiftPressed());
            }
            else if (this.activeTool == Tool.GRADIENT)
            {
                this.rasterizeGradient(this.dragStartPixel, hoverPixel);
            }

            this.dragStartPixel = null;
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

        /* Render symmetry guides */
        if (this.pixels != null)
        {
            if (this.mirrorX)
            {
                int midX = (int) Math.round(this.scaleX.to(this.pixels.width / 2F));
                int topY = (int) Math.round(this.scaleY.to(0));
                int botY = (int) Math.round(this.scaleY.to(this.pixels.height));

                context.batcher.box(midX, topY, midX + 1, botY, 0x8840a0ff);
            }

            if (this.mirrorY)
            {
                int midY = (int) Math.round(this.scaleY.to(this.pixels.height / 2F));
                int leftX = (int) Math.round(this.scaleX.to(0));
                int rightX = (int) Math.round(this.scaleX.to(this.pixels.width));

                context.batcher.box(leftX, midY, rightX, midY + 1, 0x8840a0ff);
            }
        }

        /* Draw current pixel preview */
        int pixelX = (int) Math.floor(this.scaleX.from(context.mouseX));
        int pixelY = (int) Math.floor(this.scaleY.from(context.mouseY));

        if (this.activeTool == Tool.BRUSH || this.activeTool == Tool.ERASER)
        {
            this.renderBrushPreview(context, pixelX, pixelY);
        }

        /* Continuous brush dragging */
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

        /* Drag preview for Shapes and Gradients */
        if (this.editing && this.dragging && this.dragStartPixel != null)
        {
            Vector2i hover = this.getHoverPixel(context.mouseX, context.mouseY);

            if (this.activeTool == Tool.SHAPE)
            {
                int x1 = this.dragStartPixel.x;
                int y1 = this.dragStartPixel.y;
                int x2 = hover.x;
                int y2 = hover.y;

                if (Window.isShiftPressed())
                {
                    int sx = Math.abs(x2 - x1);
                    int sy = Math.abs(y2 - y1);
                    int side = Math.max(sx, sy);

                    x2 = x1 + (x2 >= x1 ? side : -side);
                    y2 = y1 + (y2 >= y1 ? side : -side);
                }

                int minX = Math.min(x1, x2);
                int maxX = Math.max(x1, x2) + 1;
                int minY = Math.min(y1, y2);
                int maxY = Math.max(y1, y2) + 1;

                int sx1 = (int) Math.round(this.scaleX.to(minX));
                int sy1 = (int) Math.round(this.scaleY.to(minY));
                int sx2 = (int) Math.round(this.scaleX.to(maxX));
                int sy2 = (int) Math.round(this.scaleY.to(maxY));

                context.batcher.outline(sx1, sy1, sx2, sy2, 0xff000000 | BBSSettings.primaryColor.get());
            }
            else if (this.activeTool == Tool.GRADIENT)
            {
                int sx1 = (int) Math.round(this.scaleX.to(this.dragStartPixel.x + 0.5F));
                int sy1 = (int) Math.round(this.scaleY.to(this.dragStartPixel.y + 0.5F));
                int sx2 = (int) Math.round(this.scaleX.to(hover.x + 0.5F));
                int sy2 = (int) Math.round(this.scaleY.to(hover.y + 0.5F));

                context.batcher.box(sx1 - 2, sy1 - 2, sx1 + 2, sy1 + 2, Colors.WHITE);
                context.batcher.box(sx2 - 2, sy2 - 2, sx2 + 2, sy2 + 2, 0xff000000 | BBSSettings.primaryColor.get());
            }
        }
    }

    protected Texture getRenderTexture(UIContext context)
    {
        return this.temporary;
    }

    private void rasterizeShape(Vector2i p1, Vector2i p2, boolean square)
    {
        if (this.pixels == null)
        {
            return;
        }

        int x1 = p1.x;
        int y1 = p1.y;
        int x2 = p2.x;
        int y2 = p2.y;

        if (square)
        {
            int dx = Math.abs(x2 - x1);
            int dy = Math.abs(y2 - y1);
            int side = Math.max(dx, dy);

            x2 = x1 + (x2 >= x1 ? side : -side);
            y2 = y1 + (y2 >= y1 ? side : -side);
        }

        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);

        PixelsUndo undo = new PixelsUndo();
        Color color = this.colorSupplier.get();

        if (this.shapeType == ShapeType.RECTANGLE)
        {
            if (this.shapeFilled)
            {
                for (int x = minX; x <= maxX; x++)
                {
                    for (int y = minY; y <= maxY; y++)
                    {
                        this.applyBrush(undo, x, y, color);
                    }
                }
            }
            else
            {
                for (int x = minX; x <= maxX; x++)
                {
                    this.applyBrush(undo, x, minY, color);
                    this.applyBrush(undo, x, maxY, color);
                }

                for (int y = minY; y <= maxY; y++)
                {
                    this.applyBrush(undo, minX, y, color);
                    this.applyBrush(undo, maxX, y, color);
                }
            }
        }
        else if (this.shapeType == ShapeType.CIRCLE)
        {
            double cx = (minX + maxX) / 2.0;
            double cy = (minY + maxY) / 2.0;
            double rx = Math.max(0.5, (maxX - minX) / 2.0);
            double ry = Math.max(0.5, (maxY - minY) / 2.0);

            for (int x = minX; x <= maxX; x++)
            {
                for (int y = minY; y <= maxY; y++)
                {
                    double dx = (x - cx) / rx;
                    double dy = (y - cy) / ry;
                    double distSq = dx * dx + dy * dy;

                    if (this.shapeFilled)
                    {
                        if (distSq <= 1.05)
                        {
                            this.applyBrush(undo, x, y, color);
                        }
                    }
                    else
                    {
                        double innerRx = Math.max(0.01, rx - this.brushSize);
                        double innerRy = Math.max(0.01, ry - this.brushSize);
                        double innerDistSq = ((x - cx) / innerRx) * ((x - cx) / innerRx) + ((y - cy) / innerRy) * ((y - cy) / innerRy);

                        if (distSq <= 1.05 && innerDistSq >= 0.85)
                        {
                            this.applyBrush(undo, x, y, color);
                        }
                    }
                }
            }
        }

        if (!undo.pixels.isEmpty())
        {
            this.undoManager.pushUndo(undo);
            this.wasChanged();
            this.updateTexture();
        }
    }

    private void rasterizeGradient(Vector2i p1, Vector2i p2)
    {
        if (this.pixels == null)
        {
            return;
        }

        int x1 = p1.x;
        int y1 = p1.y;
        int x2 = p2.x;
        int y2 = p2.y;

        int dx = x2 - x1;
        int dy = y2 - y1;
        int lenSq = dx * dx + dy * dy;

        Color c1 = this.colorSupplier.get();
        Color c2 = this.secondaryColorSupplier.get();

        PixelsUndo undo = new PixelsUndo();

        for (int x = 0; x < this.pixels.width; x++)
        {
            for (int y = 0; y < this.pixels.height; y++)
            {
                if (this.lockAlpha)
                {
                    Color current = this.pixels.getColor(x, y);

                    if (current == null || current.a <= 0F)
                    {
                        continue;
                    }
                }

                float t = 0F;

                if (lenSq > 0)
                {
                    t = (float) ((x - x1) * dx + (y - y1) * dy) / (float) lenSq;
                    t = Math.max(0F, Math.min(1F, t));
                }

                Color blended = new Color(
                    Lerps.lerp(c1.r, c2.r, t),
                    Lerps.lerp(c1.g, c2.g, t),
                    Lerps.lerp(c1.b, c2.b, t),
                    Lerps.lerp(c1.a, c2.a, t)
                );

                undo.setColor(this.pixels, x, y, blended);
            }
        }

        if (!undo.pixels.isEmpty())
        {
            this.undoManager.pushUndo(undo);
            this.wasChanged();
            this.updateTexture();
        }
    }

    private void applyBrushRaw(PixelsUndo undo, int x, int y, Color color)
    {
        int minX = x - (this.brushSize - 1) / 2;
        int minY = y - (this.brushSize - 1) / 2;

        for (int i = 0; i < this.brushSize; i++)
        {
            for (int j = 0; j < this.brushSize; j++)
            {
                if (this.isBrushOffsetInside(i, j))
                {
                    int px = minX + i;
                    int py = minY + j;

                    if (this.lockAlpha)
                    {
                        Color current = this.pixels.getColor(px, py);

                        if (current == null || current.a <= 0F)
                        {
                            continue;
                        }
                    }

                    undo.setColor(this.pixels, px, py, color);
                }
            }
        }
    }

    private void applyBrush(PixelsUndo undo, int x, int y, Color color)
    {
        if (this.pixelPerfect && this.brushSize == 1 && this.activeTool == Tool.BRUSH && this.pixels != null)
        {
            if (this.strokePoints.size() >= 2)
            {
                Vector2i p0 = this.strokePoints.get(this.strokePoints.size() - 2);
                Vector2i p1 = this.strokePoints.get(this.strokePoints.size() - 1);

                if (p0.x != x && p0.y != y)
                {
                    if ((p1.x == p0.x && p1.y == y) || (p1.x == x && p1.y == p0.y))
                    {
                        undo.revertColor(this.pixels, p1.x, p1.y);

                        if (this.mirrorX)
                        {
                            undo.revertColor(this.pixels, this.pixels.width - 1 - p1.x, p1.y);
                        }

                        if (this.mirrorY)
                        {
                            undo.revertColor(this.pixels, p1.x, this.pixels.height - 1 - p1.y);
                        }

                        if (this.mirrorX && this.mirrorY)
                        {
                            undo.revertColor(this.pixels, this.pixels.width - 1 - p1.x, this.pixels.height - 1 - p1.y);
                        }

                        this.strokePoints.remove(this.strokePoints.size() - 1);
                    }
                }
            }

            this.strokePoints.add(new Vector2i(x, y));
        }

        this.applyBrushRaw(undo, x, y, color);

        if (this.pixels == null)
        {
            return;
        }

        int mx = this.pixels.width - 1 - x;
        int my = this.pixels.height - 1 - y;

        if (this.mirrorX && mx != x)
        {
            this.applyBrushRaw(undo, mx, y, color);
        }

        if (this.mirrorY && my != y)
        {
            this.applyBrushRaw(undo, x, my, color);
        }

        if (this.mirrorX && this.mirrorY && mx != x && my != y)
        {
            this.applyBrushRaw(undo, mx, my, color);
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

    private void renderBrushPreviewSingle(UIContext context, int pixelX, int pixelY)
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

    private void renderBrushPreview(UIContext context, int pixelX, int pixelY)
    {
        this.renderBrushPreviewSingle(context, pixelX, pixelY);

        if (this.pixels == null)
        {
            return;
        }

        int mx = this.pixels.width - 1 - pixelX;
        int my = this.pixels.height - 1 - pixelY;

        if (this.mirrorX && mx != pixelX)
        {
            this.renderBrushPreviewSingle(context, mx, pixelY);
        }

        if (this.mirrorY && my != pixelY)
        {
            this.renderBrushPreviewSingle(context, pixelX, my);
        }

        if (this.mirrorX && this.mirrorY && mx != pixelX && my != pixelY)
        {
            this.renderBrushPreviewSingle(context, mx, my);
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
