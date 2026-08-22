package mchorse.bbs_mod.ui.model;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelCube;
import mchorse.bbs_mod.cubic.data.model.ModelUV;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.utils.UICanvasEditor;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

import org.joml.Vector2f;
import org.joml.Vector2i;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class UIModelUVEditor extends UICanvasEditor
{
    public static final int FACE_FRONT = 0;
    public static final int FACE_BACK = 1;
    public static final int FACE_RIGHT = 2;
    public static final int FACE_LEFT = 3;
    public static final int FACE_TOP = 4;
    public static final int FACE_BOTTOM = 5;

    public static final String[] FACE_NAMES = new String[]
    {
        "front", "back", "right", "left", "top", "bottom"
    };

    public static final String[] FACE_LABELS = new String[]
    {
        "F", "B", "R", "L", "T", "D"
    };

    public static final int[] FACE_COLORS = new int[]
    {
        0x503b82f6, /* Front: Blue */
        0x5006b6d4, /* Back: Cyan */
        0x5022c55e, /* Right: Green */
        0x50a855f7, /* Left: Purple */
        0x50eab308, /* Top: Yellow */
        0x50f97316  /* Bottom: Orange */
    };

    public static final int[] FACE_OUTLINE_COLORS = new int[]
    {
        0xff3b82f6,
        0xff06b6d4,
        0xff22c55e,
        0xffa855f7,
        0xffeab308,
        0xfff97316
    };

    private final UIModelGeometryPanel geometryPanel;
    private ModelCube cube;
    private Model model;
    private Link textureLink;
    private boolean faceUVMode;
    private int selectedFace = FACE_FRONT;

    /* Dragging state */
    private boolean draggingUV;
    private int dragFace = -1;
    private final Vector2f dragStartUV = new Vector2f();
    private final Vector2i dragStartPixel = new Vector2i();

    /* Controls header overlay */
    private final UIElement header;
    private final UIIcon toggleMode;
    private final UIIcon rotateUV;
    private final UIIcon closeButton;
    private final UIIcon resetZoom;

    public UIModelUVEditor(UIModelGeometryPanel geometryPanel)
    {
        super();

        this.geometryPanel = geometryPanel;

        this.header = new UIElement();
        this.header.relative(this).x(0).y(0).w(1F).h(24);

        this.toggleMode = new UIIcon(Icons.REFRESH, (b) ->
        {
            this.geometryPanel.toggleUVMode();
        });
        this.toggleMode.tooltip(UIKeys.MODELS_GEOMETRY_UV_MODE_TOGGLE);

        this.rotateUV = new UIIcon(Icons.REVERSE, (b) ->
        {
            this.geometryPanel.rotateSelectedFaceUV();
        });
        this.rotateUV.tooltip(UIKeys.MODELS_GEOMETRY_UV_ROTATE);

        this.resetZoom = new UIIcon(Icons.MOVE_TO, (b) ->
        {
            this.resetView();
        });
        this.resetZoom.tooltip(UIKeys.MODELS_GEOMETRY_UV_RESET_VIEW);

        this.closeButton = new UIIcon(Icons.CLOSE, (b) ->
        {
            this.geometryPanel.toggleUVEditorVisible();
        });
        this.closeButton.tooltip(UIKeys.GENERAL_CLOSE);

        UIElement buttons = UI.row(this.toggleMode, this.rotateUV, this.resetZoom, this.closeButton);
        buttons.relative(this.header).x(1F, -86).y(2).wh(84, 20);

        this.header.add(buttons);
        this.add(this.header);

        this.setSize(64, 64);
    }

    public void setModelAndCube(Model model, ModelCube cube, Link textureLink, boolean faceUVMode)
    {
        this.model = model;
        this.cube = cube;
        this.textureLink = textureLink;
        this.faceUVMode = faceUVMode;

        int tw = 64;
        int th = 64;

        if (model != null)
        {
            tw = Math.max(1, model.textureWidth);
            th = Math.max(1, model.textureHeight);
        }

        if (tw != this.w || th != this.h)
        {
            this.setSize(tw, th);
        }

        this.toggleMode.active(this.faceUVMode);
    }

    public void setSelectedFace(int faceIndex)
    {
        this.selectedFace = Math.max(0, Math.min(FACE_BOTTOM, faceIndex));
    }

    public int getSelectedFace()
    {
        return this.selectedFace;
    }

    public void resetView()
    {
        this.setSize(this.w, this.h);
    }

    @Override
    protected boolean isMouseButtonAllowed(int mouseButton)
    {
        return mouseButton == 0 || mouseButton == 1 || mouseButton == 2;
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (this.header.area.isInside(context))
        {
            return super.subMouseClicked(context);
        }

        if (this.area.isInside(context) && context.mouseButton == 0 && !Window.isCtrlPressed())
        {
            Vector2i pixel = this.getHoverPixel(context.mouseX, context.mouseY);

            if (this.cube != null)
            {
                int clickedFace = this.getFaceAtPixel(pixel.x, pixel.y);

                if (clickedFace >= 0)
                {
                    this.draggingUV = true;
                    this.dragFace = clickedFace;
                    this.selectedFace = clickedFace;
                    this.dragStartPixel.set(pixel.x, pixel.y);

                    if (this.faceUVMode)
                    {
                        ModelUV uv = this.getFaceUV(this.cube, clickedFace);

                        if (uv != null)
                        {
                            this.dragStartUV.set(uv.origin);
                        }
                    }
                    else
                    {
                        Vector2f boxUV = this.geometryPanel.getBoxUV(this.cube);
                        this.dragStartUV.set(boxUV);
                    }

                    this.geometryPanel.onUVFaceSelected(clickedFace);

                    return true;
                }
            }
        }

        return super.subMouseClicked(context);
    }

    @Override
    public boolean subMouseReleased(UIContext context)
    {
        if (this.draggingUV)
        {
            this.draggingUV = false;
            this.dragFace = -1;
            this.geometryPanel.recordUVUndo();
        }

        return super.subMouseReleased(context);
    }

    @Override
    public void render(UIContext context)
    {
        if (this.draggingUV && this.cube != null)
        {
            Vector2i pixel = this.getHoverPixel(context.mouseX, context.mouseY);
            int dx = pixel.x - this.dragStartPixel.x;
            int dy = pixel.y - this.dragStartPixel.y;

            if (dx != 0 || dy != 0)
            {
                if (this.faceUVMode)
                {
                    ModelUV uv = this.getFaceUV(this.cube, this.dragFace >= 0 ? this.dragFace : this.selectedFace);

                    if (uv != null)
                    {
                        float newX = (float) Math.floor(this.dragStartUV.x + dx);
                        float newY = (float) Math.floor(this.dragStartUV.y + dy);

                        if (newX != uv.origin.x || newY != uv.origin.y)
                        {
                            uv.origin.x = newX;
                            uv.origin.y = newY;
                            this.geometryPanel.refreshCubeRenderAndSyncControls();
                        }
                    }
                }
                else
                {
                    float newX = (float) Math.floor(this.dragStartUV.x + dx);
                    float newY = (float) Math.floor(this.dragStartUV.y + dy);
                    Vector2f currentBox = this.geometryPanel.getBoxUV(this.cube);

                    if (newX != currentBox.x || newY != currentBox.y)
                    {
                        this.geometryPanel.setBoxUVDirect(newX, newY);
                    }
                }
            }
        }

        super.render(context);
    }

    @Override
    protected void renderCanvasFrame(UIContext context)
    {
        int x = -this.w / 2;
        int y = -this.h / 2;
        Area area = this.calculate(x, y, x + this.w, y + this.h);

        /* 1. Render Texture */
        Texture texture = null;

        if (this.textureLink != null)
        {
            texture = BBSModClient.getTextures().getTexture(this.textureLink);
        }

        if (texture != null && texture != BBSModClient.getTextures().getError())
        {
            context.batcher.fullTexturedBox(texture, area.x, area.y, area.w, area.h);
        }

        /* 2. Pixel Grid (when zoomed in) */
        if (this.scaleX.getZoom() >= 4.0)
        {
            float halfW = this.w / 2F;
            float halfH = this.h / 2F;

            for (int px = 0; px <= this.w; px++)
            {
                int sx = (int) Math.round(this.scaleX.to(px - halfW));
                context.batcher.box(sx, area.y, sx + 1, area.ey(), 0x18ffffff);
            }

            for (int py = 0; py <= this.h; py++)
            {
                int sy = (int) Math.round(this.scaleY.to(py - halfH));
                context.batcher.box(area.x, sy, area.ex(), sy + 1, 0x18ffffff);
            }
        }

        /* 3. Render Cube UV layout */
        if (this.cube != null)
        {
            this.renderCubeUVs(context);
        }

        /* 4. Canvas border */
        context.batcher.outline(area.x, area.y, area.ex(), area.ey(), Colors.A75);
    }

    private void renderCubeUVs(UIContext context)
    {
        float halfW = this.w / 2F;
        float halfH = this.h / 2F;
        Vector2i hoverPixel = this.getHoverPixel(context.mouseX, context.mouseY);
        int hoveredFace = this.getFaceAtPixel(hoverPixel.x, hoverPixel.y);

        List<FaceRect> rects = this.calculateFaceRects();

        for (int i = 0; i < rects.size(); i++)
        {
            FaceRect rect = rects.get(i);
            int faceIndex = rect.faceIndex;
            boolean isSelected = this.faceUVMode && faceIndex == this.selectedFace;
            boolean isHovered = faceIndex == hoveredFace;

            int sx1 = (int) Math.round(this.scaleX.to(rect.x - halfW));
            int sy1 = (int) Math.round(this.scaleY.to(rect.y - halfH));
            int sx2 = (int) Math.round(this.scaleX.to(rect.x + rect.w - halfW));
            int sy2 = (int) Math.round(this.scaleY.to(rect.y + rect.h - halfH));

            int minSx = Math.min(sx1, sx2);
            int maxSx = Math.max(sx1, sx2);
            int minSy = Math.min(sy1, sy2);
            int maxSy = Math.max(sy1, sy2);

            int fillColor = isSelected ? (FACE_COLORS[faceIndex] | 0x60000000) : (isHovered ? (FACE_COLORS[faceIndex] | 0x40000000) : FACE_COLORS[faceIndex]);
            int outlineColor = isSelected ? Colors.WHITE : (isHovered ? Colors.A100 | FACE_OUTLINE_COLORS[faceIndex] : FACE_OUTLINE_COLORS[faceIndex]);

            context.batcher.box(minSx, minSy, maxSx, maxSy, fillColor);
            context.batcher.outline(minSx, minSy, maxSx, maxSy, outlineColor);

            if (isSelected)
            {
                context.batcher.outline(minSx - 1, minSy - 1, maxSx + 1, maxSy + 1, Colors.A50 | Colors.BLUE);
            }

            /* Draw face label */
            String label = FACE_LABELS[faceIndex];

            if (this.faceUVMode && rect.rotation != 0)
            {
                label += " " + (int) rect.rotation + "°";
            }

            int textW = context.batcher.getFont().getWidth(label);
            int textX = minSx + (maxSx - minSx - textW) / 2;
            int textY = minSy + (maxSy - minSy - context.batcher.getFont().getHeight()) / 2;

            if (maxSx - minSx >= 10 && maxSy - minSy >= 8)
            {
                context.batcher.text(label, textX, textY, Colors.WHITE);
            }
        }

        /* Draw overall bounding box for Box UV */
        if (!this.faceUVMode && !rects.isEmpty())
        {
            Vector2f boxUV = this.geometryPanel.getBoxUV(this.cube);
            float w = (float) Math.floor(Math.abs(this.cube.size.x));
            float h = (float) Math.floor(Math.abs(this.cube.size.y));
            float d = (float) Math.floor(Math.abs(this.cube.size.z));
            float totalW = d * 2F + w * 2F;
            float totalH = d + h;

            int bx1 = (int) Math.round(this.scaleX.to(boxUV.x - halfW));
            int by1 = (int) Math.round(this.scaleY.to(boxUV.y - halfH));
            int bx2 = (int) Math.round(this.scaleX.to(boxUV.x + totalW - halfW));
            int by2 = (int) Math.round(this.scaleY.to(boxUV.y + totalH - halfH));

            context.batcher.outline(bx1 - 1, by1 - 1, bx2 + 1, by2 + 1, 0x88ffffff);
        }
    }

    private int getFaceAtPixel(int px, int py)
    {
        if (this.cube == null)
        {
            return -1;
        }

        List<FaceRect> rects = this.calculateFaceRects();

        /* If Face UV mode and clicking directly on active face, prioritize it */
        if (this.faceUVMode && this.selectedFace >= 0 && this.selectedFace < rects.size())
        {
            for (FaceRect rect : rects)
            {
                if (rect.faceIndex == this.selectedFace && rect.contains(px, py))
                {
                    return rect.faceIndex;
                }
            }
        }

        for (int i = rects.size() - 1; i >= 0; i--)
        {
            FaceRect rect = rects.get(i);

            if (rect.contains(px, py))
            {
                return rect.faceIndex;
            }
        }

        return -1;
    }

    public List<FaceRect> calculateFaceRects()
    {
        List<FaceRect> list = new ArrayList<>();

        if (this.cube == null)
        {
            return list;
        }

        if (this.faceUVMode)
        {
            for (int i = 0; i < 6; i++)
            {
                ModelUV uv = this.getFaceUV(this.cube, i);

                if (uv != null)
                {
                    float x = Math.min(uv.origin.x, uv.origin.x + uv.size.x);
                    float y = Math.min(uv.origin.y, uv.origin.y + uv.size.y);
                    float w = Math.abs(uv.size.x);
                    float h = Math.abs(uv.size.y);

                    list.add(new FaceRect(i, x, y, w, h, uv.rotation));
                }
            }
        }
        else
        {
            Vector2f boxUV = this.geometryPanel.getBoxUV(this.cube);
            boolean mirror = this.geometryPanel.isCubeMirrored(this.cube);

            float w = (float) Math.floor(Math.abs(this.cube.size.x));
            float h = (float) Math.floor(Math.abs(this.cube.size.y));
            float d = (float) Math.floor(Math.abs(this.cube.size.z));

            /* Top */
            list.add(new FaceRect(FACE_TOP, boxUV.x + d, boxUV.y, w, d, 0));
            /* Bottom */
            list.add(new FaceRect(FACE_BOTTOM, boxUV.x + d + w, boxUV.y, w, d, 0));
            /* Right (East) */
            float rx = mirror ? (boxUV.x + d + w) : boxUV.x;
            list.add(new FaceRect(FACE_RIGHT, rx, boxUV.y + d, d, h, 0));
            /* Front (North) */
            list.add(new FaceRect(FACE_FRONT, boxUV.x + d, boxUV.y + d, w, h, 0));
            /* Left (West) */
            float lx = mirror ? boxUV.x : (boxUV.x + d + w);
            list.add(new FaceRect(FACE_LEFT, lx, boxUV.y + d, d, h, 0));
            /* Back (South) */
            list.add(new FaceRect(FACE_BACK, boxUV.x + d * 2F + w, boxUV.y + d, w, h, 0));
        }

        return list;
    }

    public ModelUV getFaceUV(ModelCube cube, int faceIndex)
    {
        if (cube == null)
        {
            return null;
        }

        switch (faceIndex)
        {
            case FACE_FRONT: return cube.front;
            case FACE_BACK: return cube.back;
            case FACE_RIGHT: return cube.right;
            case FACE_LEFT: return cube.left;
            case FACE_TOP: return cube.top;
            case FACE_BOTTOM: return cube.bottom;
            default: return null;
        }
    }

    public static class FaceRect
    {
        public int faceIndex;
        public float x;
        public float y;
        public float w;
        public float h;
        public float rotation;

        public FaceRect(int faceIndex, float x, float y, float w, float h, float rotation)
        {
            this.faceIndex = faceIndex;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.rotation = rotation;
        }

        public boolean contains(float px, float py)
        {
            return px >= this.x && px <= this.x + this.w && py >= this.y && py <= this.y + this.h;
        }
    }
}
