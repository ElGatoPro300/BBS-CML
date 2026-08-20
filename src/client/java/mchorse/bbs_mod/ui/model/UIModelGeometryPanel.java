package mchorse.bbs_mod.ui.model;

import mchorse.bbs_mod.BBSFeatures;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.cubic.CubicLoader;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelCube;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.data.model.ModelUV;
import mchorse.bbs_mod.cubic.model.ModelConfig;
import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.controller.UIGizmoSizeContextMenu;
import mchorse.bbs_mod.ui.film.controller.UIGizmoThicknessContextMenu;
import mchorse.bbs_mod.ui.film.controller.UIGizmoTranslateSpeedContextMenu;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.UITransform;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIPromptOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.UIDraggable;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.framework.elements.utils.UIModelRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.UIRenderable;
import mchorse.bbs_mod.ui.utils.Gizmo;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;
import mchorse.bbs_mod.ui.utils.gizmo.TransformOrientation;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Axis;
import mchorse.bbs_mod.utils.IOUtils;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.pose.Transform;
import mchorse.bbs_mod.utils.undo.IUndo;
import mchorse.bbs_mod.utils.undo.UndoManager;

import org.joml.Vector2f;
import org.joml.Vector3f;

import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class UIModelGeometryPanel extends UIElement
{
    private final IUIModelPanelHost parent;
    private final UIList<GeometryEntry> hierarchyList;
    private final UISearchList<GeometryEntry> hierarchySearch;
    private final UILabel selectedBoneLabel;
    private final UITransform unifiedTransform;
    private final UIPropTransform gizmoTransform;
    private final Transform gizmoTransformData = new Transform();
    private final UITrackpad transformX;
    private final UITrackpad transformY;
    private final UITrackpad transformZ;
    private final UITrackpad rotateX;
    private final UITrackpad rotateY;
    private final UITrackpad rotateZ;
    private final UITrackpad pivotX;
    private final UITrackpad pivotY;
    private final UITrackpad pivotZ;
    private final UITrackpad scaleX;
    private final UITrackpad scaleY;
    private final UITrackpad scaleZ;
    private final UIButton saveButton;
    private final UITrackpad cubeInflate;
    private final UIButton uvModeButton;
    private final UIElement uvBoxRow;
    private final UITrackpad cubeUvX;
    private final UITrackpad cubeUvY;
    private final UIToggle cubeMirror;
    private final UIElement uvFaceSelectRow;
    private final UIButton uvFaceSelectButton;
    private final UIIcon uvFaceRotateButton;
    private final UIElement uvFaceCoordsRow;
    private final UITrackpad uvFaceX;
    private final UITrackpad uvFaceY;
    private final UITrackpad uvFaceW;
    private final UITrackpad uvFaceH;
    private final UIButton uvResetToBoxButton;
    private final UIModelUVEditor uvEditor;
    private boolean faceUVMode;
    private final UIIcon addCubeIcon;
    private final UIIcon addFolderIcon;
    private final UIIcon addIKLocatorIcon;
    private final UIElement leftPanel;
    private final UIElement rightPanel;
    private final UIIcon gizmoMove;
    private final UIIcon gizmoScale;
    private final UIIcon gizmoRotate;
    private final UIIcon gizmoCombined;
    private final UIIcon gizmoTop;
    private final UIIcon gizmoVisualSize;
    private final UIIcon gizmoThickness;
    private final UIIcon gizmoTranslateSpeed;
    private final UIIcon gizmoCoordinateSpace;
    private final UIIcon gizmoSnap;
    private final UIIcon gizmoViewportMode;
    private boolean snapEnabled = true;
    private float positionSnap = 1.0F;
    private float rotationSnap = 15.0F;
    private TransformOrientation coordinateSpace = TransformOrientation.LOCAL;
    private final Set<String> collapsedGroupIds = new HashSet<>();
    private final Set<String> lockedGroupIds = new HashSet<>();
    private final Set<String> lockedCubeKeys = new HashSet<>();
    private ModelGroup copiedGroup;
    private ModelCube copiedCube;

    private ModelConfig config;
    private ModelInstance instance;
    private ModelGroup selectedGroup;
    private ModelCube selectedCube;
    private UndoManager<UIModelGeometryPanel> undoManager = new UndoManager<>(200);
    private GeometryState lastUndoState;
    private boolean applyingUndo;
    private boolean cubeMirrorValue;
    private boolean filling;

    public UIModelGeometryPanel(IUIModelPanelHost parent)
    {
        this.parent = parent;
        this.relative(parent.getMainView()).w(1F).h(1F);

        int leftWidth = 220;
        int rightWidth = 230;
        if (BBSSettings.uiLayoutPreferences != null)
        {
            leftWidth = (int) BBSSettings.uiLayoutPreferences.getFormPanelWidth("UIModelPanel_left", 220F);
            rightWidth = (int) BBSSettings.uiLayoutPreferences.getFormPanelWidth("UIModelPanel_right", 230F);
        }

        this.leftPanel = new UIElement();
        this.leftPanel.relative(this).x(0).y(0).w(leftWidth).h(1F);

        this.rightPanel = new UIElement();
        this.rightPanel.relative(this).x(1F, -rightWidth).y(0).w(rightWidth).h(1F);

        /* ==================== LEFT PANEL: UV & TEXTURES ==================== */
        UILabel uvTitle = UI.label(UIKeys.MODELS_GEOMETRY_UV_TITLE).background();
        uvTitle.relative(this.leftPanel).x(10).y(8).w(1F, -20).h(12);

        this.uvEditor = new UIModelUVEditor(this);
        this.uvEditor.relative(this.leftPanel).x(10).y(24).w(1F, -20).h(180);

        UILabel texturePropsTitle = UI.label(UIKeys.MODELS_GEOMETRY_UV_PROPERTIES).background();
        texturePropsTitle.relative(this.leftPanel).x(10).y(210).w(1F, -20).h(12);

        /* UV Mode Header Row */
        this.uvModeButton = new UIButton(UIKeys.MODELS_GEOMETRY_UV_BOX, (b) -> this.toggleUVMode());
        this.uvModeButton.w(1F).h(20);
        UIElement uvHeaderRow = UI.row(this.uvModeButton);
        uvHeaderRow.relative(this.leftPanel).x(10).y(226).w(1F, -20).h(20);

        /* Box UV Row */
        UILabel cubeUvLabel = UI.label(UIKeys.MODELS_GEOMETRY_CUBE_UV);
        cubeUvLabel.w(0.25F, -4).h(20);
        this.cubeUvX = this.trackpad((v) -> this.updateCubeUV(0, v.floatValue()));
        this.cubeUvY = this.trackpad((v) -> this.updateCubeUV(1, v.floatValue()));
        this.cubeMirror = new UIToggle(UIKeys.MODELS_GEOMETRY_CUBE_MIRROR, (b) -> this.updateCubeMirror(b.getValue()));
        this.cubeUvX.w(0.25F, -3);
        this.cubeUvY.w(0.25F, -3);
        this.cubeMirror.w(0.25F, -3).h(20);
        this.uvBoxRow = UI.row(4, cubeUvLabel, this.cubeUvX, this.cubeUvY, this.cubeMirror);
        this.uvBoxRow.relative(this.leftPanel).x(10).y(250).w(1F, -20).h(20);

        /* Face UV Controls Rows */
        this.uvFaceSelectButton = new UIButton(UIKeys.MODELS_GEOMETRY_UV_FACE_FRONT, (b) -> this.openFaceSelectionMenu(b.getContext()));
        this.uvFaceSelectButton.w(0.75F, -4).h(20);
        this.uvFaceRotateButton = new UIIcon(Icons.REFRESH, (b) -> this.rotateSelectedFaceUV());
        this.uvFaceRotateButton.tooltip(UIKeys.MODELS_GEOMETRY_UV_ROTATE);
        this.uvFaceRotateButton.w(0.25F).h(20);
        this.uvFaceSelectRow = UI.row(4, this.uvFaceSelectButton, this.uvFaceRotateButton);
        this.uvFaceSelectRow.relative(this.leftPanel).x(10).y(250).w(1F, -20).h(20);

        this.uvFaceX = this.trackpad((v) -> this.updateFaceUVCoord(0, v.floatValue()));
        this.uvFaceY = this.trackpad((v) -> this.updateFaceUVCoord(1, v.floatValue()));
        this.uvFaceW = this.trackpad((v) -> this.updateFaceUVCoord(2, v.floatValue()));
        this.uvFaceH = this.trackpad((v) -> this.updateFaceUVCoord(3, v.floatValue()));
        this.uvFaceX.w(0.25F, -3);
        this.uvFaceY.w(0.25F, -3);
        this.uvFaceW.w(0.25F, -3);
        this.uvFaceH.w(0.25F, -3);
        this.uvFaceCoordsRow = UI.row(4, this.uvFaceX, this.uvFaceY, this.uvFaceW, this.uvFaceH);
        this.uvFaceCoordsRow.relative(this.leftPanel).x(10).y(274).w(1F, -20).h(20);

        this.uvResetToBoxButton = new UIButton(UIKeys.MODELS_GEOMETRY_UV_RESET_TO_BOX, (b) -> this.resetToBoxUV());
        this.uvResetToBoxButton.relative(this.leftPanel).x(10).y(298).w(1F, -20).h(20);

        this.leftPanel.add(uvTitle, this.uvEditor, texturePropsTitle, uvHeaderRow, this.uvBoxRow, this.uvFaceSelectRow, this.uvFaceCoordsRow, this.uvResetToBoxButton);

        /* ==================== RIGHT PANEL: TRANSFORM & HIERARCHY ==================== */
        UILabel editorTitle = UI.label(UIKeys.MODELS_GEOMETRY_TRANSFORM_TITLE).background();
        editorTitle.relative(this.rightPanel).x(10).y(8).w(1F, -20).h(12);

        this.selectedBoneLabel = UI.label(IKey.raw("-"));
        this.selectedBoneLabel.relative(this.rightPanel).x(10).y(22).w(1F, -20).h(12);

        this.unifiedTransform = new UITransform()
        {
            {
                UIElement row = this.r2x.getParent();

                if (row != null)
                {
                    row.removeFromParent();
                }

                this.iconR2.setEnabled(false);
                this.r2x.setEnabled(false);
                this.r2y.setEnabled(false);
                this.r2z.setEnabled(false);

                this.iconP.setEnabled(true);
                this.iconP.callback = (b) -> UIModelGeometryPanel.this.openPivotContextMenu(UIModelGeometryPanel.this.getContext());
                this.iconP.tooltip(UIKeys.MODELS_GEOMETRY_PIVOT_CENTER);
            }

            @Override
            public void setT(Axis axis, double x, double y, double z)
            {
                UIModelGeometryPanel.this.applyGizmoChange(0, axis, x, y, z);
            }

            @Override
            public void setS(Axis axis, double x, double y, double z)
            {
                UIModelGeometryPanel.this.applyGizmoChange(3, axis, x, y, z);
            }

            @Override
            public void setR(Axis axis, double x, double y, double z)
            {
                UIModelGeometryPanel.this.applyGizmoChange(1, axis, x, y, z);
            }

            @Override
            public void setR2(Axis axis, double x, double y, double z)
            {}

            @Override
            public void setP(Axis axis, double x, double y, double z)
            {
                UIModelGeometryPanel.this.applyGizmoChange(2, axis, x, y, z);
            }
        };
        this.unifiedTransform.relative(this.rightPanel).x(10).y(38).w(1F, -20).h(92);
        this.gizmoTransform = new UIPropTransform()
        {
            @Override
            public void setT(Axis axis, double x, double y, double z)
            {
                UIModelGeometryPanel.this.applyGizmoChange(0, axis, x, y, z);
            }

            @Override
            public void setS(Axis axis, double x, double y, double z)
            {
                UIModelGeometryPanel.this.applyGizmoChange(3, axis, x, y, z);
            }

            @Override
            public void setR(Axis axis, double x, double y, double z)
            {
                UIModelGeometryPanel.this.applyGizmoChange(1, axis, x, y, z);
            }

            @Override
            public void setR2(Axis axis, double x, double y, double z)
            {}

            @Override
            public void setP(Axis axis, double x, double y, double z)
            {
                UIModelGeometryPanel.this.applyGizmoChange(2, axis, x, y, z);
            }
        };
        this.gizmoTransform.translationScale(16F);
        this.gizmoTransform.setTransform(this.gizmoTransformData);
        this.gizmoTransform.noCulling();
        this.gizmoTransform.relative(this).xy(-1000, -1000).wh(1, 1);

        this.transformX = this.unifiedTransform.tx;
        this.transformY = this.unifiedTransform.ty;
        this.transformZ = this.unifiedTransform.tz;
        this.rotateX = this.unifiedTransform.rx;
        this.rotateY = this.unifiedTransform.ry;
        this.rotateZ = this.unifiedTransform.rz;
        this.pivotX = this.unifiedTransform.px;
        this.pivotY = this.unifiedTransform.py;
        this.pivotZ = this.unifiedTransform.pz;
        this.scaleX = this.unifiedTransform.sx;
        this.scaleY = this.unifiedTransform.sy;
        this.scaleZ = this.unifiedTransform.sz;

        UILabel cubeInflateLabel = UI.label(UIKeys.MODELS_GEOMETRY_CUBE_INFLATE);
        cubeInflateLabel.w(0.4F, -4).h(20);
        this.cubeInflate = this.trackpad((v) -> this.updateCubeInflate(v.floatValue()));
        this.cubeInflate.w(0.6F, -2);
        UIElement cubeInflateRow = UI.row(6, cubeInflateLabel, this.cubeInflate);
        cubeInflateRow.relative(this.rightPanel).x(10).y(138).w(1F, -20).h(20);

        this.saveButton = new UIButton(UIKeys.GENERAL_SAVE, (b) -> this.saveModelFile());
        this.saveButton.w(1F).h(20);
        UIElement buttons = UI.row(this.saveButton);
        buttons.relative(this.rightPanel).x(10).y(164).w(1F, -20).h(20);

        /* Hierarchy / Outliner */
        UILabel hierarchyTitle = UI.label(UIKeys.MODELS_GEOMETRY_OUTLINE_TITLE).background();
        hierarchyTitle.relative(this.rightPanel).x(10).y(192).w(1F, -20).h(12);

        this.addCubeIcon = new UIIcon(Icons.BLOCK, (b) -> this.addCube());
        this.addFolderIcon = new UIIcon(Icons.FOLDER, (b) -> this.addFolder());
        this.addIKLocatorIcon = new UIIcon(Icons.POSE, (b) -> this.addIKLocator());
        this.addCubeIcon.tooltip(UIKeys.MODELS_GEOMETRY_ADD_CUBE);
        this.addFolderIcon.tooltip(UIKeys.MODELS_GEOMETRY_ADD_FOLDER);
        this.addIKLocatorIcon.tooltip(UIKeys.MODELS_IK_CREATE_LOCATOR_TOOLTIP);

        UIElement actionButtonsRow = BBSFeatures.MODEL_IK_UI
            ? UI.row(this.addCubeIcon, this.addFolderIcon, this.addIKLocatorIcon)
            : UI.row(this.addCubeIcon, this.addFolderIcon);
        actionButtonsRow.relative(this.rightPanel).x(10).y(208).w(1F, -20).h(20);

        this.hierarchyList = new UIList<>((l) -> this.selectCurrentHierarchyEntry())
        {
            @Override
            protected boolean sortElements()
            {
                return false;
            }

            @Override
            protected void renderElementPart(UIContext context, GeometryEntry element, int i, int x, int y, boolean hover, boolean selected)
            {
                int textY = y + (this.scroll.scrollItemSize - context.batcher.getFont().getHeight()) / 2;
                int offset = element.depth * 8;
                int arrowX = x + 2 + offset;
                int iconX = x + (element.expandable ? 16 : 4) + offset;
                int textX = iconX + 16;
                int w = this.area.w - (this.scroll.hasScrollbar() ? this.scroll.scrollSize : 0);
                int eyeX = x + w - 30;
                int lockX = x + w - 16;
                boolean isVis = UIModelGeometryPanel.this.isEntryVisible(element);
                boolean isLock = UIModelGeometryPanel.this.isLocked(element);
                Icon icon = element.type == GeometryEntryType.BONE ? Icons.FOLDER : Icons.BLOCK;

                if (element.expandable)
                {
                    context.batcher.icon(UIModelGeometryPanel.this.collapsedGroupIds.contains(element.groupId) ? Icons.COLLAPSED : Icons.UNCOLLAPSED, arrowX, y + 1);
                }

                int mainIconColor = isVis ? (isLock ? 0xfff59e0b : Colors.WHITE) : Colors.A25;
                context.batcher.icon(icon, mainIconColor, iconX, y + 1);

                int textColor = hover ? Colors.HIGHLIGHT : (!isVis ? Colors.A50 : (isLock ? 0xfff59e0b : Colors.WHITE));

                context.batcher.textShadow(element.label, textX, textY, textColor);

                /* Visibility Icon (Eye) */
                Icon eyeIcon = isVis ? Icons.VISIBLE : Icons.INVISIBLE;
                int eyeColor = isVis ? (hover ? Colors.WHITE : Colors.A50) : 0xffff4444;
                context.batcher.icon(eyeIcon, eyeColor, eyeX, y + 1);

                /* Lock Icon (Padlock) */
                if (isLock || hover)
                {
                    Icon lockIcon = isLock ? Icons.LOCKED : Icons.UNLOCKED;
                    int lockColor = isLock ? 0xfff59e0b : Colors.A50;
                    context.batcher.icon(lockIcon, lockColor, lockX, y + 1);
                }
            }

            @Override
            protected String elementToString(UIContext context, int i, GeometryEntry element)
            {
                return element.label + " " + element.groupId;
            }

            @Override
            protected void handleSwap(int from, int to)
            {
                UIModelGeometryPanel.this.handleHierarchySwap(from, to);
            }

            @Override
            public boolean subMouseClicked(UIContext context)
            {
                if (this.area.isInside(context) && context.mouseButton == 0)
                {
                    int visibleIndex = this.scroll.getIndex(context.mouseX, context.mouseY);

                    if (this.exists(visibleIndex))
                    {
                        GeometryEntry entry = this.getList().get(visibleIndex);
                        int y = this.area.y + visibleIndex * this.scroll.scrollItemSize - (int) this.scroll.getScroll();
                        int offset = entry.depth * 8;
                        int arrowX = this.area.x + 2 + offset;
                        int w = this.area.w - (this.scroll.hasScrollbar() ? this.scroll.scrollSize : 0);
                        int eyeX = this.area.x + w - 30;
                        int lockX = this.area.x + w - 16;

                        if (!this.isFiltering() && entry.expandable && context.mouseX >= arrowX && context.mouseX < arrowX + 16 && context.mouseY >= y + 1 && context.mouseY < y + 17)
                        {
                            UIModelGeometryPanel.this.toggleGroupCollapsed(entry.groupId);

                            return true;
                        }

                        if (context.mouseX >= eyeX && context.mouseX < eyeX + 16 && context.mouseY >= y + 1 && context.mouseY < y + 17)
                        {
                            UIModelGeometryPanel.this.toggleVisibility(entry);
                            UIUtils.playClick();

                            return true;
                        }

                        if (context.mouseX >= lockX && context.mouseX < lockX + 16 && context.mouseY >= y + 1 && context.mouseY < y + 17)
                        {
                            UIModelGeometryPanel.this.toggleLock(entry);
                            UIUtils.playClick();

                            return true;
                        }
                    }
                }

                if (this.area.isInside(context) && context.mouseButton == 1)
                {
                    int visibleIndex = this.scroll.getIndex(context.mouseX, context.mouseY);

                    if (this.exists(visibleIndex))
                    {
                        GeometryEntry entry = this.getList().get(visibleIndex);

                        if (!this.getCurrent().contains(entry))
                        {
                            this.setCurrentDirect(entry);
                            UIModelGeometryPanel.this.selectCurrentHierarchyEntry();
                        }

                        UIModelGeometryPanel.this.openHierarchyContextMenu(context, entry);

                        return true;
                    }
                }

                return super.subMouseClicked(context);
            }
        };
        this.hierarchyList.multi();
        this.hierarchyList.background();
        this.hierarchyList.sorting();
        this.hierarchyList.scroll.scrollItemSize = 18;
        this.hierarchySearch = new UISearchList<>(this.hierarchyList);
        this.hierarchySearch.label(UIKeys.GENERAL_SEARCH);
        this.hierarchySearch.relative(this.rightPanel).x(10).y(234).w(1F, -20).h(1F, -242);

        this.rightPanel.add(editorTitle, this.selectedBoneLabel, this.unifiedTransform, cubeInflateRow, buttons, hierarchyTitle, actionButtonsRow, this.hierarchySearch);

        UIDraggable leftDraggable = new UIDraggable((context) ->
        {
            int diff = context.mouseX - this.leftPanel.area.x;
            int maxW = Math.max(140, this.area.w / 2 - 20);
            int w = MathUtils.clamp(diff, 140, maxW);
            this.leftPanel.w(w);
            this.resize();
        }).dragEnd(() ->
        {
            if (BBSSettings.uiLayoutPreferences != null)
            {
                BBSSettings.uiLayoutPreferences.setFormPanelWidth("UIModelPanel_left", this.leftPanel.area.w);
            }
        });
        leftDraggable.relative(this.leftPanel).x(1F).y(0).w(6).h(1F).anchorX(0.5F);
        leftDraggable.hoverOnly().rendering((context) ->
        {
            int color = leftDraggable.isDragging() ? BBSSettings.primaryColor.get() | Colors.A100 : (leftDraggable.area.isInside(context) ? Colors.A75 : Colors.A25);
            context.batcher.box(leftDraggable.area.x + 2, leftDraggable.area.y, leftDraggable.area.x + 4, leftDraggable.area.ey(), color);
        });

        UIDraggable rightDraggable = new UIDraggable((context) ->
        {
            int diff = this.area.ex() - context.mouseX;
            int maxW = Math.max(140, this.area.w / 2 - 20);
            int w = MathUtils.clamp(diff, 140, maxW);
            this.rightPanel.x(1F, -w).w(w);
            this.resize();
        }).dragEnd(() ->
        {
            if (BBSSettings.uiLayoutPreferences != null)
            {
                BBSSettings.uiLayoutPreferences.setFormPanelWidth("UIModelPanel_right", this.rightPanel.area.w);
            }
        });
        rightDraggable.relative(this.rightPanel).x(0F).y(0).w(6).h(1F).anchorX(0.5F);
        rightDraggable.hoverOnly().rendering((context) ->
        {
            int color = rightDraggable.isDragging() ? BBSSettings.primaryColor.get() | Colors.A100 : (rightDraggable.area.isInside(context) ? Colors.A75 : Colors.A25);
            context.batcher.box(rightDraggable.area.x + 2, rightDraggable.area.y, rightDraggable.area.x + 4, rightDraggable.area.ey(), color);
        });

        UIRenderable backgroundRenderable = new UIRenderable((context) ->
        {
            if (!this.isVisible())
            {
                return;
            }

            int leftX1 = this.leftPanel.area.x;
            int leftY1 = this.leftPanel.area.y;
            int leftX2 = this.leftPanel.area.ex();
            int leftY2 = this.leftPanel.area.ey();

            int rightX1 = this.rightPanel.area.x;
            int rightY1 = this.rightPanel.area.y;
            int rightX2 = this.rightPanel.area.ex();
            int rightY2 = this.rightPanel.area.ey();

            context.batcher.box(leftX1, leftY1, leftX2, leftY2, 0xFF181A1F);
            context.batcher.outline(leftX1 - 1, leftY1 - 1, leftX2 + 1, leftY2 + 1, 0xFF2A2D35);

            context.batcher.box(rightX1, rightY1, rightX2, rightY2, 0xFF181A1F);
            context.batcher.outline(rightX1 - 1, rightY1 - 1, rightX2 + 1, rightY2 + 1, 0xFF2A2D35);
        });

        /* Gizmo Toolbar */
        this.gizmoMove = this.createGizmoModeButton(Icons.ALL_DIRECTIONS, Gizmo.Mode.TRANSLATE, UIKeys.FILM_GIZMO_MOVE);
        this.gizmoScale = this.createGizmoModeButton(Icons.SCALE, Gizmo.Mode.SCALE, UIKeys.FILM_GIZMO_SCALE);
        this.gizmoRotate = this.createGizmoModeButton(Icons.ARC, Gizmo.Mode.ROTATE, UIKeys.FILM_GIZMO_ROTATE);
        this.gizmoCombined = this.createGizmoModeButton(Icons.SHAPES, Gizmo.Mode.COMBINED, UIKeys.FILM_GIZMO_COMBINED);
        this.gizmoTop = this.createGizmoModeButton(Icons.SPHERE, Gizmo.Mode.TOP, UIKeys.FILM_GIZMO_TOP);
        this.gizmoVisualSize = new UIIcon(Icons.MAXIMIZE, (b) ->
        {
            if (this.getContext() != null)
            {
                this.getContext().replaceContextMenu(new UIGizmoSizeContextMenu());
            }
        });
        this.gizmoVisualSize.tooltip(UIKeys.FILM_GIZMO_SIZE);

        this.gizmoThickness = new UIIcon(Icons.LINE, (b) ->
        {
            if (this.getContext() != null)
            {
                this.getContext().replaceContextMenu(new UIGizmoThicknessContextMenu());
            }
        });
        this.gizmoThickness.tooltip(UIKeys.FILM_GIZMO_THICKNESS);

        this.gizmoTranslateSpeed = new UIIcon(Icons.FORWARD, (b) ->
        {
            if (this.getContext() != null)
            {
                this.getContext().replaceContextMenu(new UIGizmoTranslateSpeedContextMenu());
            }
        });
        this.gizmoTranslateSpeed.tooltip(UIKeys.FILM_GIZMO_TRANSLATE_SPEED);

        this.gizmoCoordinateSpace = new UIIcon(Icons.SPHERE, (b) -> this.toggleCoordinateSpace());
        this.gizmoCoordinateSpace.tooltip(UIKeys.MODELS_GEOMETRY_SPACE);

        this.gizmoSnap = new UIIcon(Icons.BLOCK, (b) -> this.openSnapContextMenu(this.getContext()));
        this.gizmoSnap.tooltip(UIKeys.MODELS_GEOMETRY_SNAP);

        this.gizmoViewportMode = new UIIcon(Icons.MATERIAL, (b) -> this.openViewportModeContextMenu(this.getContext()));
        this.gizmoViewportMode.tooltip(UIKeys.MODELS_GEOMETRY_VIEWPORT_MODE);

        UIElement gizmoToolbar = new UIElement();
        gizmoToolbar.row(0);
        gizmoToolbar.relative(this).x(0.5F).y(4).wh(220, 20).anchorX(0.5F);

        UIRenderable toolbarBackground = new UIRenderable((context) ->
        {
            gizmoToolbar.area.render(context.batcher, Colors.A75);

            Gizmo.Mode gizmoMode = Gizmo.INSTANCE.getMode();

            this.gizmoMove.active(gizmoMode == Gizmo.Mode.TRANSLATE);
            this.gizmoScale.active(gizmoMode == Gizmo.Mode.SCALE);
            this.gizmoRotate.active(gizmoMode == Gizmo.Mode.ROTATE);
            this.gizmoCombined.active(gizmoMode == Gizmo.Mode.COMBINED);
            this.gizmoTop.active(gizmoMode == Gizmo.Mode.TOP);
            this.gizmoCoordinateSpace.active(this.coordinateSpace == TransformOrientation.GLOBAL);
            this.gizmoSnap.active(this.snapEnabled);

            UIModelRenderer mr = this.parent.getModelRenderer();

            if (mr instanceof UIModelEditorRenderer editorRenderer)
            {
                this.gizmoViewportMode.active(editorRenderer.viewportMode != UIModelEditorRenderer.ViewportMode.TEXTURED);
            }
        });

        gizmoToolbar.add(this.gizmoMove, this.gizmoScale, this.gizmoRotate, this.gizmoCombined, this.gizmoTop, this.gizmoCoordinateSpace, this.gizmoSnap, this.gizmoViewportMode, this.gizmoVisualSize, this.gizmoThickness, this.gizmoTranslateSpeed);

        this.add(backgroundRenderable, this.leftPanel, this.rightPanel, leftDraggable, rightDraggable, this.gizmoTransform, toolbarBackground, gizmoToolbar);

        this.fillControls();
        this.fillCubeControls();
    }

    private UIIcon createGizmoModeButton(Icon icon, Gizmo.Mode mode, IKey tooltip)
    {
        UIIcon button = new UIIcon(icon, (b) ->
        {
            Gizmo.INSTANCE.setMode(mode);
            UIUtils.playClick();
        });

        button.tooltip(tooltip);
        button.activeBackground(Colors.A50 | Colors.BLUE);

        return button;
    }

    private void fillCubeControls()
    {
        this.filling = true;

        boolean locked = this.isCurrentSelectionLocked();

        if (this.selectedCube == null && this.selectedGroup == null)
        {
            this.cubeMirrorValue = false;
            this.setTransformPads(new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f(1F, 1F, 1F));
            this.cubeInflate.setValue(0);
            this.cubeUvX.setValue(0);
            this.cubeUvY.setValue(0);
            this.cubeMirror.setValue(false);
            this.cubeInflate.setEnabled(false);
            this.cubeUvX.setEnabled(false);
            this.cubeUvY.setEnabled(false);
            this.cubeMirror.setEnabled(false);
            this.uvModeButton.setEnabled(false);
            this.uvFaceSelectButton.setEnabled(false);
            this.uvFaceRotateButton.setEnabled(false);
            this.uvFaceX.setEnabled(false);
            this.uvFaceY.setEnabled(false);
            this.uvFaceW.setEnabled(false);
            this.uvFaceH.setEnabled(false);
            this.uvResetToBoxButton.setEnabled(false);
            this.unifiedTransform.setEnabled(false);
        }
        else if (this.selectedCube != null)
        {
            Vector2f uv = this.getBoxUV(this.selectedCube);

            this.cubeMirrorValue = this.isCubeMirrored(this.selectedCube);
            this.setTransformPads(this.selectedCube.origin, this.selectedCube.rotate, this.selectedCube.pivot, this.selectedCube.size);
            this.cubeInflate.setValue(this.selectedCube.inflate);
            this.cubeUvX.setValue(uv.x);
            this.cubeUvY.setValue(uv.y);
            this.cubeMirror.setValue(this.cubeMirrorValue);
            this.cubeInflate.setEnabled(!locked);
            this.cubeUvX.setEnabled(!locked);
            this.cubeUvY.setEnabled(!locked);
            this.cubeMirror.setEnabled(!locked);
            this.uvModeButton.setEnabled(!locked);
            this.unifiedTransform.setEnabled(!locked);

            this.uvModeButton.label = this.faceUVMode ? UIKeys.MODELS_GEOMETRY_UV_FACE : UIKeys.MODELS_GEOMETRY_UV_BOX;
            this.uvBoxRow.setVisible(!this.faceUVMode);
            this.uvFaceSelectRow.setVisible(this.faceUVMode);
            this.uvFaceCoordsRow.setVisible(this.faceUVMode);
            this.uvResetToBoxButton.setVisible(this.faceUVMode);

            if (this.faceUVMode)
            {
                int faceIdx = this.uvEditor.getSelectedFace();
                this.uvFaceSelectButton.label = this.getFaceKey(faceIdx);
                ModelUV faceUV = this.uvEditor.getFaceUV(this.selectedCube, faceIdx);

                if (faceUV != null)
                {
                    this.uvFaceX.setValue(faceUV.origin.x);
                    this.uvFaceY.setValue(faceUV.origin.y);
                    this.uvFaceW.setValue(faceUV.size.x);
                    this.uvFaceH.setValue(faceUV.size.y);
                }

                this.uvFaceSelectButton.setEnabled(!locked);
                this.uvFaceRotateButton.setEnabled(!locked);
                this.uvFaceX.setEnabled(!locked);
                this.uvFaceY.setEnabled(!locked);
                this.uvFaceW.setEnabled(!locked);
                this.uvFaceH.setEnabled(!locked);
                this.uvResetToBoxButton.setEnabled(!locked);
            }
        }
        else
        {
            this.setTransformPads(this.selectedGroup.initial.translate, this.selectedGroup.initial.rotate, this.selectedGroup.initial.pivot, this.selectedGroup.initial.scale);
            this.cubeInflate.setValue(0);
            this.cubeUvX.setValue(0);
            this.cubeUvY.setValue(0);
            this.cubeMirror.setValue(false);
            this.cubeInflate.setEnabled(false);
            this.cubeUvX.setEnabled(false);
            this.cubeUvY.setEnabled(false);
            this.cubeMirror.setEnabled(false);
            this.uvModeButton.setEnabled(false);
            this.uvFaceSelectButton.setEnabled(false);
            this.uvFaceRotateButton.setEnabled(false);
            this.uvFaceX.setEnabled(false);
            this.uvFaceY.setEnabled(false);
            this.uvFaceW.setEnabled(false);
            this.uvFaceH.setEnabled(false);
            this.uvResetToBoxButton.setEnabled(false);
            this.unifiedTransform.setEnabled(!locked);
        }

        this.filling = false;
        this.syncGizmoTransformFromSelection();
        this.syncUVEditor();
    }

    private UITrackpad trackpad(Consumer<Double> callback)
    {
        UITrackpad pad = new UITrackpad((v) -> callback.accept(v.doubleValue())).increment(1);

        pad.w(0.333F, -6);

        return pad;
    }

    public void setConfig(ModelConfig config)
    {
        this.config = config;
        this.undoManager = new UndoManager<>(200);
        this.lastUndoState = null;
        this.reloadModelData();
    }

    @Override
    protected boolean subKeyPressed(UIContext context)
    {
        if (!context.isFocused() && Window.isCtrlPressed() && context.isPressed(GLFW.GLFW_KEY_Z))
        {
            boolean ok = Window.isShiftPressed() ? this.undoManager.redo(this) : this.undoManager.undo(this);

            if (ok)
            {
                UIUtils.playClick();
            }

            return ok;
        }

        if (!context.isFocused() && Window.isCtrlPressed() && context.isPressed(GLFW.GLFW_KEY_Y))
        {
            boolean ok = this.undoManager.redo(this);

            if (ok)
            {
                UIUtils.playClick();
            }

            return ok;
        }

        if (!context.isFocused())
        {
            if (context.isPressed(GLFW.GLFW_KEY_DELETE) || context.isPressed(GLFW.GLFW_KEY_BACKSPACE))
            {
                if (!this.hierarchyList.getCurrent().isEmpty())
                {
                    this.deleteSelection();
                    UIUtils.playClick();
                    return true;
                }
            }

            if (Window.isCtrlPressed() && Window.isShiftPressed() && context.isPressed(GLFW.GLFW_KEY_D))
            {
                if (!this.hierarchyList.getCurrent().isEmpty())
                {
                    this.duplicateAndMirrorX();
                    return true;
                }
            }

            if (Window.isCtrlPressed() && context.isPressed(GLFW.GLFW_KEY_D))
            {
                if (!this.hierarchyList.getCurrent().isEmpty())
                {
                    this.duplicateSelection();
                    UIUtils.playClick();
                    return true;
                }
            }

            if (context.isPressed(GLFW.GLFW_KEY_F2))
            {
                GeometryEntry first = this.hierarchyList.getCurrentFirst();

                if (first != null)
                {
                    this.renameEntry(first);
                    return true;
                }
            }

            if (context.isPressed(GLFW.GLFW_KEY_F))
            {
                this.focusSelection();
                return true;
            }

            if (context.isPressed(GLFW.GLFW_KEY_G) || context.isPressed(GLFW.GLFW_KEY_T))
            {
                Gizmo.INSTANCE.setMode(Gizmo.Mode.TRANSLATE);
                UIUtils.playClick();
                return true;
            }

            if (context.isPressed(GLFW.GLFW_KEY_R))
            {
                Gizmo.INSTANCE.setMode(Gizmo.Mode.ROTATE);
                UIUtils.playClick();
                return true;
            }

            if (context.isPressed(GLFW.GLFW_KEY_S))
            {
                Gizmo.INSTANCE.setMode(Gizmo.Mode.SCALE);
                UIUtils.playClick();
                return true;
            }
        }

        return super.subKeyPressed(context);
    }

    @Override
    protected boolean subMouseReleased(UIContext context)
    {
        if (!this.applyingUndo)
        {
            this.undoManager.markLastUndoNoMerging();
        }

        return super.subMouseReleased(context);
    }

    public void selectBone(String bone)
    {
        if (bone == null || bone.isEmpty())
        {
            return;
        }

        for (GeometryEntry entry : this.hierarchyList.getList())
        {
            if (entry.type == GeometryEntryType.BONE && entry.groupId.equals(bone))
            {
                this.hierarchyList.setCurrentDirect(entry);
                this.selectCurrentHierarchyEntry();

                break;
            }
        }
    }

    private void reloadModelData()
    {
        this.instance = null;
        this.selectedGroup = null;
        this.selectedCube = null;
        this.hierarchyList.clear();
        this.parent.getModelRenderer().setSelectedCube(null);

        if (this.config == null)
        {
            this.fillControls();
            this.fillCubeControls();
            this.lastUndoState = null;
            return;
        }

        this.instance = this.parent.getModelRenderer().getPreviewModelInstance();

        if (this.instance == null)
        {
            this.instance = BBSModClient.getModels().loadModel(this.config.getId());
            this.parent.getModelRenderer().invalidatePreviewModel();
            this.instance = this.parent.getModelRenderer().getPreviewModelInstance();
        }

        if (this.instance == null || !(this.instance.model instanceof Model model))
        {
            this.fillControls();
            this.fillCubeControls();
            this.lastUndoState = null;
            return;
        }

        for (ModelGroup group : model.topGroups)
        {
            this.collectHierarchy(group, 0);
        }

        if (!this.hierarchyList.getList().isEmpty())
        {
            this.hierarchyList.setCurrent(this.hierarchyList.getList().get(0));
            this.selectCurrentHierarchyEntry();
        }
        else
        {
            this.fillControls();
            this.fillCubeControls();
        }

        this.lastUndoState = this.captureState();
    }

    private void collectHierarchy(ModelGroup group, int depth)
    {
        boolean expandable = !group.children.isEmpty() || !group.cubes.isEmpty();

        this.hierarchyList.add(new GeometryEntry(GeometryEntryType.BONE, group.id, -1, depth, group.id, expandable));

        if (this.collapsedGroupIds.contains(group.id))
        {
            return;
        }

        for (int i = 0; i < group.cubes.size(); i++)
        {
            this.hierarchyList.add(new GeometryEntry(GeometryEntryType.CUBE, group.id, i, depth + 1, this.getCubeLabel(group.cubes.get(i)), false));
        }

        for (ModelGroup child : group.children)
        {
            this.collectHierarchy(child, depth + 1);
        }
    }

    private void selectCurrentHierarchyEntry()
    {
        this.selectedGroup = null;
        this.selectedCube = null;

        if (this.instance != null && this.instance.model instanceof Model model)
        {
            GeometryEntry entry = this.hierarchyList.getCurrentFirst();

            if (entry != null)
            {
                this.selectedGroup = model.getGroup(entry.groupId);

                if (this.selectedGroup != null)
                {
                    this.parent.getModelRenderer().setSelectedBone(this.selectedGroup.id);

                    if (entry.type == GeometryEntryType.CUBE && entry.cubeIndex >= 0 && entry.cubeIndex < this.selectedGroup.cubes.size())
                    {
                        this.selectedCube = this.selectedGroup.cubes.get(entry.cubeIndex);
                    }
                }
            }
        }

        this.parent.getModelRenderer().setSelectedCube(this.selectedCube);

        if (this.selectedCube != null)
        {
            this.faceUVMode = this.isCustomFaceUV(this.selectedCube);
        }

        if (this.parent.getMainView().getChildren().contains(this))
        {
            this.parent.getModelRenderer().transform = this.gizmoTransform;
        }

        this.fillControls();
        this.fillCubeControls();
    }

    private void fillControls()
    {
        this.filling = true;

        if (this.selectedGroup == null)
        {
            this.selectedBoneLabel.label = IKey.raw("-");
            this.setPads(new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f(1F, 1F, 1F));
        }
        else
        {
            this.selectedBoneLabel.label = IKey.raw(this.selectedGroup.id);
            this.setPads(this.selectedGroup.initial.translate, this.selectedGroup.initial.rotate, this.selectedGroup.initial.pivot, this.selectedGroup.initial.scale);
        }

        this.filling = false;
    }

    private void setPads(Vector3f origin, Vector3f rotate, Vector3f pivot, Vector3f scale)
    {
        this.setTransformPads(origin, rotate, pivot, scale);
    }

    private void setTransformPads(Vector3f origin, Vector3f rotate, Vector3f pivot, Vector3f scale)
    {
        this.transformX.setValue(origin.x);
        this.transformY.setValue(origin.y);
        this.transformZ.setValue(origin.z);
        this.rotateX.setValue(rotate.x);
        this.rotateY.setValue(rotate.y);
        this.rotateZ.setValue(rotate.z);
        this.pivotX.setValue(pivot.x);
        this.pivotY.setValue(pivot.y);
        this.pivotZ.setValue(pivot.z);
        this.scaleX.setValue(scale.x);
        this.scaleY.setValue(scale.y);
        this.scaleZ.setValue(scale.z);
    }

    private void syncGizmoTransformFromSelection()
    {
        if (this.selectedCube != null)
        {
            this.gizmoTransformData.translate.set(this.selectedCube.origin);
            this.gizmoTransformData.scale.set(this.selectedCube.size);
            this.gizmoTransformData.rotate.set(
                MathUtils.toRad(this.selectedCube.rotate.x),
                MathUtils.toRad(this.selectedCube.rotate.y),
                MathUtils.toRad(this.selectedCube.rotate.z)
            );
            this.gizmoTransformData.pivot.set(this.selectedCube.pivot);
        }
        else if (this.selectedGroup != null)
        {
            this.gizmoTransformData.translate.set(this.selectedGroup.initial.translate);
            this.gizmoTransformData.scale.set(this.selectedGroup.initial.scale);
            this.gizmoTransformData.rotate.set(
                MathUtils.toRad(this.selectedGroup.initial.rotate.x),
                MathUtils.toRad(this.selectedGroup.initial.rotate.y),
                MathUtils.toRad(this.selectedGroup.initial.rotate.z)
            );
            this.gizmoTransformData.pivot.set(this.selectedGroup.initial.pivot);
        }
        else
        {
            this.gizmoTransformData.translate.zero();
            this.gizmoTransformData.scale.set(1F, 1F, 1F);
            this.gizmoTransformData.rotate.zero();
            this.gizmoTransformData.pivot.zero();
        }

        this.gizmoTransform.setTransform(this.gizmoTransformData);
    }

    private void applyGizmoChange(int type, Axis axis, double x, double y, double z)
    {
        if (this.isCurrentSelectionLocked())
        {
            return;
        }

        boolean snap = this.snapEnabled ^ Window.isShiftPressed();

        if (snap)
        {
            if (type == 0 || type == 2 || type == 3)
            {
                if (this.positionSnap > 0F)
                {
                    x = Math.round(x / this.positionSnap) * this.positionSnap;
                    y = Math.round(y / this.positionSnap) * this.positionSnap;
                    z = Math.round(z / this.positionSnap) * this.positionSnap;
                }
            }
            else if (type == 1)
            {
                if (this.rotationSnap > 0F)
                {
                    x = Math.round(x / this.rotationSnap) * this.rotationSnap;
                    y = Math.round(y / this.rotationSnap) * this.rotationSnap;
                    z = Math.round(z / this.rotationSnap) * this.rotationSnap;
                }
            }
        }

        if (axis == null)
        {
            this.updateTransformVector(type, 0, (float) x);
            this.updateTransformVector(type, 1, (float) y);
            this.updateTransformVector(type, 2, (float) z);
        }
        else
        {
            this.updateTransformVector(type, this.axisIndex(axis), (float) (axis == Axis.X ? x : axis == Axis.Y ? y : z));
        }

        this.filling = true;

        if (this.selectedCube != null)
        {
            this.setTransformPads(this.selectedCube.origin, this.selectedCube.rotate, this.selectedCube.pivot, this.selectedCube.size);
        }
        else if (this.selectedGroup != null)
        {
            this.setTransformPads(this.selectedGroup.initial.translate, this.selectedGroup.initial.rotate, this.selectedGroup.initial.pivot, this.selectedGroup.initial.scale);
        }

        this.filling = false;
        this.syncGizmoTransformFromSelection();
    }

    private int axisIndex(Axis axis)
    {
        if (axis == null)
        {
            return 0;
        }

        return switch (axis)
        {
            case X -> 0;
            case Y -> 1;
            case Z -> 2;
        };
    }

    private void updateTransformVector(int type, int axis, float value)
    {
        if (this.filling || (this.selectedGroup == null && this.selectedCube == null) || this.isCurrentSelectionLocked())
        {
            return;
        }

        Vector3f vector;

        if (this.selectedCube != null)
        {
            vector = switch (type)
            {
                case 0 -> this.selectedCube.origin;
                case 1 -> this.selectedCube.rotate;
                case 2 -> this.selectedCube.pivot;
                default -> this.selectedCube.size;
            };
        }
        else
        {
            vector = switch (type)
            {
                case 0 -> this.selectedGroup.initial.translate;
                case 1 -> this.selectedGroup.initial.rotate;
                case 2 -> this.selectedGroup.initial.pivot;
                default -> this.selectedGroup.initial.scale;
            };
        }

        if (axis == 0)
        {
            vector.x = value;
        }
        else if (axis == 1)
        {
            vector.y = value;
        }
        else
        {
            vector.z = value;
        }

        if (this.selectedCube == null && this.selectedGroup != null)
        {
            this.selectedGroup.current.copy(this.selectedGroup.initial);
        }

        this.refreshCubeRenderAndSave();
    }

    private void updateCubeInflate(float value)
    {
        if (this.filling || this.selectedCube == null)
        {
            return;
        }

        this.selectedCube.inflate = value;
        this.refreshCubeRenderAndSave();
    }

    private void updateCubeUV(int axis, float value)
    {
        if (this.filling || this.selectedCube == null)
        {
            return;
        }

        Vector2f uv = this.getBoxUV(this.selectedCube);

        if (axis == 0)
        {
            uv.x = value;
        }
        else
        {
            uv.y = value;
        }

        this.selectedCube.setupBoxUV(uv, this.cubeMirrorValue);
        this.refreshCubeRenderAndSave();
        this.syncUVEditor();
    }

    private void updateCubeMirror(boolean mirror)
    {
        if (this.filling || this.selectedCube == null)
        {
            return;
        }

        this.cubeMirrorValue = mirror;
        this.selectedCube.setupBoxUV(this.getBoxUV(this.selectedCube), this.cubeMirrorValue);
        this.refreshCubeRenderAndSave();
        this.syncUVEditor();
    }

    public boolean isFaceUVMode()
    {
        return this.faceUVMode;
    }

    public void toggleUVMode()
    {
        if (this.selectedCube == null)
        {
            return;
        }

        if (!this.faceUVMode)
        {
            this.selectedCube.setupBoxUV(this.getBoxUV(this.selectedCube), this.cubeMirrorValue);
            this.faceUVMode = true;
        }
        else
        {
            this.faceUVMode = false;
            this.selectedCube.setupBoxUV(this.getBoxUV(this.selectedCube), this.cubeMirrorValue);
        }

        this.refreshCubeRenderAndSave();
        this.fillCubeControls();
    }

    public void resetToBoxUV()
    {
        if (this.selectedCube == null)
        {
            return;
        }

        this.faceUVMode = false;
        this.selectedCube.setupBoxUV(this.getBoxUV(this.selectedCube), this.cubeMirrorValue);
        this.refreshCubeRenderAndSave();
        this.fillCubeControls();
    }

    public void toggleUVEditorVisible()
    {
        this.uvEditor.setVisible(!this.uvEditor.isVisible());
    }

    public void rotateSelectedFaceUV()
    {
        if (this.selectedCube == null)
        {
            return;
        }

        if (!this.faceUVMode)
        {
            this.selectedCube.setupBoxUV(this.getBoxUV(this.selectedCube), this.cubeMirrorValue);
            this.faceUVMode = true;
        }

        ModelUV uv = this.uvEditor.getFaceUV(this.selectedCube, this.uvEditor.getSelectedFace());

        if (uv != null)
        {
            uv.rotation = (uv.rotation + 90F) % 360F;
            this.refreshCubeRenderAndSave();
            this.fillCubeControls();
        }
    }

    public void onUVFaceSelected(int faceIndex)
    {
        this.uvEditor.setSelectedFace(faceIndex);
        this.fillCubeControls();
    }

    public void recordUVUndo()
    {
        this.recordUndoState();
    }

    public void refreshCubeRenderAndSyncControls()
    {
        this.refreshCubeRenderAndSave();
        this.fillCubeControls();
    }

    public void setBoxUVDirect(float x, float y)
    {
        if (this.selectedCube == null)
        {
            return;
        }

        this.selectedCube.setupBoxUV(new Vector2f(x, y), this.cubeMirrorValue);
        this.refreshCubeRenderAndSave();
        this.fillCubeControls();
    }

    public void syncUVEditor()
    {
        if (this.uvEditor == null)
        {
            return;
        }

        Link textureLink = this.config != null ? this.config.texture.get() : null;
        Model model = this.instance != null && this.instance.model instanceof Model m ? m : null;

        this.uvEditor.setModelAndCube(model, this.selectedCube, textureLink, this.faceUVMode);
    }

    private void openFaceSelectionMenu(UIContext context)
    {
        context.replaceContextMenu((menu) ->
        {
            for (int i = 0; i < 6; i++)
            {
                final int faceIndex = i;
                menu.action(this.getFaceKey(faceIndex), () -> this.onUVFaceSelected(faceIndex));
            }
        });
    }

    private void updateFaceUVCoord(int coord, float value)
    {
        if (this.filling || this.selectedCube == null)
        {
            return;
        }

        ModelUV uv = this.uvEditor.getFaceUV(this.selectedCube, this.uvEditor.getSelectedFace());

        if (uv != null)
        {
            if (coord == 0)
            {
                uv.origin.x = value;
            }
            else if (coord == 1)
            {
                uv.origin.y = value;
            }
            else if (coord == 2)
            {
                uv.size.x = value;
            }
            else if (coord == 3)
            {
                uv.size.y = value;
            }

            this.refreshCubeRenderAndSave();
            this.syncUVEditor();
        }
    }

    public IKey getFaceKey(int index)
    {
        switch (index)
        {
            case UIModelUVEditor.FACE_FRONT: return UIKeys.MODELS_GEOMETRY_UV_FACE_FRONT;
            case UIModelUVEditor.FACE_BACK: return UIKeys.MODELS_GEOMETRY_UV_FACE_BACK;
            case UIModelUVEditor.FACE_RIGHT: return UIKeys.MODELS_GEOMETRY_UV_FACE_RIGHT;
            case UIModelUVEditor.FACE_LEFT: return UIKeys.MODELS_GEOMETRY_UV_FACE_LEFT;
            case UIModelUVEditor.FACE_TOP: return UIKeys.MODELS_GEOMETRY_UV_FACE_TOP;
            case UIModelUVEditor.FACE_BOTTOM: return UIKeys.MODELS_GEOMETRY_UV_FACE_BOTTOM;
            default: return UIKeys.MODELS_GEOMETRY_UV_FACE_FRONT;
        }
    }

    public boolean isCustomFaceUV(ModelCube cube)
    {
        if (cube == null)
        {
            return false;
        }

        if ((cube.front != null && cube.front.rotation != 0) ||
            (cube.back != null && cube.back.rotation != 0) ||
            (cube.right != null && cube.right.rotation != 0) ||
            (cube.left != null && cube.left.rotation != 0) ||
            (cube.top != null && cube.top.rotation != 0) ||
            (cube.bottom != null && cube.bottom.rotation != 0))
        {
            return true;
        }

        return false;
    }

    private void openHierarchyContextMenu(UIContext context, GeometryEntry entry)
    {
        context.replaceContextMenu((menu) ->
        {
            menu.action(Icons.COPY, UIKeys.GENERAL_COPY, () -> this.copyEntry(entry));
            menu.action(Icons.PASTE, UIKeys.GENERAL_PASTE, () -> this.pasteEntry(entry));
            menu.action(Icons.DUPE, UIKeys.GENERAL_DUPE, () -> this.duplicateSelection());
            menu.action(Icons.ALL_DIRECTIONS, UIKeys.MODELS_GEOMETRY_FLIP_X, () -> this.flipSelectedX());
            menu.action(Icons.DUPE, UIKeys.MODELS_GEOMETRY_DUPE_MIRROR_X, () -> this.duplicateAndMirrorX());
            menu.action(Icons.EDIT, UIKeys.GENERAL_RENAME, () -> this.renameEntry(entry));
            menu.action(this.isEntryVisible(entry) ? Icons.INVISIBLE : Icons.VISIBLE, this.isEntryVisible(entry) ? UIKeys.MODELS_GEOMETRY_HIDE : UIKeys.MODELS_GEOMETRY_SHOW, () -> this.toggleVisibility(entry));
            menu.action(this.isLocked(entry) ? Icons.UNLOCKED : Icons.LOCKED, this.isLocked(entry) ? UIKeys.MODELS_GEOMETRY_UNLOCK : UIKeys.MODELS_GEOMETRY_LOCK, () -> this.toggleLock(entry));
            menu.action(Icons.MOVE_TO, UIKeys.MODELS_GEOMETRY_FOCUS, () -> this.focusSelection());
            menu.action(Icons.REMOVE, UIKeys.GENERAL_REMOVE, () -> this.deleteSelection());
        });
    }

    private void copyEntry(GeometryEntry entry)
    {
        if (entry.type == GeometryEntryType.BONE)
        {
            ModelGroup group = this.selectedGroup;

            if (group != null)
            {
                this.copiedGroup = this.cloneGroupTree(group, null, group.id, false, null);
                this.copiedCube = null;
            }
        }
        else if (this.selectedCube != null)
        {
            this.copiedCube = this.selectedCube.copy();
            this.copiedGroup = null;
        }
    }

    private void pasteEntry(GeometryEntry entry)
    {
        if (this.instance == null || !(this.instance.model instanceof Model model) || this.selectedGroup == null)
        {
            return;
        }

        GeometryEntry preferred = null;

        if (this.copiedCube != null)
        {
            ModelCube cube = this.copiedCube.copy();
            ModelGroup destination = this.selectedGroup;
            int insertIndex = destination.cubes.size();

            if (entry.type == GeometryEntryType.CUBE)
            {
                insertIndex = Math.min(entry.cubeIndex + 1, destination.cubes.size());
            }

            destination.cubes.add(insertIndex, cube);
            preferred = new GeometryEntry(GeometryEntryType.CUBE, destination.id, insertIndex, 0, this.getCubeLabel(cube), false);
        }
        else if (this.copiedGroup != null)
        {
            Set<String> used = new HashSet<>(model.getAllGroupKeys());
            ModelGroup destination = this.selectedGroup;
            ModelGroup clone = this.cloneGroupTree(this.copiedGroup, destination, this.copiedGroup.id, true, used);

            if (clone != null)
            {
                if (entry.type == GeometryEntryType.BONE)
                {
                    destination.children.add(clone);
                }
                else
                {
                    destination.children.add(clone);
                }

                preferred = new GeometryEntry(GeometryEntryType.BONE, clone.id, -1, 0, clone.id, true);
            }
        }

        if (preferred != null)
        {
            model.initialize();
            this.reloadHierarchyPreserveSelection(preferred);
            this.refreshCubeRenderAndSave();
        }
    }

    private void duplicateEntry(GeometryEntry entry)
    {
        this.copyEntry(entry);
        this.pasteEntry(entry);
    }

    private void renameEntry(GeometryEntry entry)
    {
        if (entry.type == GeometryEntryType.BONE)
        {
            this.renameBone(entry);
        }
        else
        {
            this.renameCube(entry);
        }
    }

    private void renameBone(GeometryEntry entry)
    {
        if (this.instance == null || !(this.instance.model instanceof Model model) || this.selectedGroup == null)
        {
            return;
        }

        UIPromptOverlayPanel panel = new UIPromptOverlayPanel(UIKeys.GENERAL_RENAME, UIKeys.GENERAL_RENAME, (newName) ->
        {
            String sanitized = this.sanitizeName(newName);

            if (sanitized.isEmpty())
            {
                return;
            }

            Set<String> used = new HashSet<>(model.getAllGroupKeys());
            used.remove(this.selectedGroup.id);
            String unique = this.makeUniqueGroupId(sanitized, used);
            ModelGroup replacement = this.cloneGroupTree(this.selectedGroup, this.selectedGroup.parent, unique, false, null);

            if (replacement == null)
            {
                return;
            }

            this.replaceGroup(model, this.selectedGroup, replacement);
            model.initialize();
            this.reloadHierarchyPreserveSelection(new GeometryEntry(GeometryEntryType.BONE, replacement.id, -1, 0, replacement.id, true));
            this.refreshCubeRenderAndSave();
        });

        panel.text.setText(entry.groupId);
        panel.text.filename();
        UIOverlay.addOverlay(this.getContext(), panel);
    }

    private void renameCube(GeometryEntry entry)
    {
        if (this.selectedCube == null || this.selectedGroup == null)
        {
            return;
        }

        UIPromptOverlayPanel panel = new UIPromptOverlayPanel(UIKeys.GENERAL_RENAME, UIKeys.GENERAL_RENAME, (newName) ->
        {
            this.selectedCube.name = this.sanitizeCubeName(newName);
            this.reloadHierarchyPreserveSelection(new GeometryEntry(GeometryEntryType.CUBE, this.selectedGroup.id, entry.cubeIndex, 0, this.getCubeLabel(this.selectedCube), false));
            this.refreshCubeRenderAndSave();
        });

        panel.text.setText(this.getCubeLabel(this.selectedCube));
        panel.text.filename();
        UIOverlay.addOverlay(this.getContext(), panel);
    }

    private void deleteEntry(GeometryEntry entry)
    {
        if (this.instance == null || !(this.instance.model instanceof Model model))
        {
            return;
        }

        if (entry.type == GeometryEntryType.CUBE)
        {
            this.removeCube();
            return;
        }

        ModelGroup group = model.getGroup(entry.groupId);

        if (group == null)
        {
            return;
        }

        this.removeGroupFromParent(model, group);
        model.initialize();
        this.reloadHierarchyPreserveSelection(null);
        this.refreshCubeRenderAndSave();
    }

    public boolean isEntryVisible(GeometryEntry entry)
    {
        if (entry == null || this.instance == null || !(this.instance.model instanceof Model model))
        {
            return true;
        }

        ModelGroup group = model.getGroup(entry.groupId);

        if (group == null)
        {
            return true;
        }

        if (entry.type == GeometryEntryType.BONE)
        {
            return group.visible;
        }
        else if (entry.type == GeometryEntryType.CUBE && entry.cubeIndex >= 0 && entry.cubeIndex < group.cubes.size())
        {
            ModelCube cube = group.cubes.get(entry.cubeIndex);

            return cube.visible && group.visible;
        }

        return true;
    }

    public void toggleVisibility(GeometryEntry entry)
    {
        if (entry == null || this.instance == null || !(this.instance.model instanceof Model model))
        {
            return;
        }

        ModelGroup group = model.getGroup(entry.groupId);

        if (group == null)
        {
            return;
        }

        if (entry.type == GeometryEntryType.BONE)
        {
            group.visible = !group.visible;
        }
        else if (entry.type == GeometryEntryType.CUBE && entry.cubeIndex >= 0 && entry.cubeIndex < group.cubes.size())
        {
            ModelCube cube = group.cubes.get(entry.cubeIndex);
            cube.visible = !cube.visible;
        }

        this.refreshCubeRenderAndSave();
    }

    public boolean isLocked(GeometryEntry entry)
    {
        if (entry == null)
        {
            return false;
        }

        if (this.lockedGroupIds.contains(entry.groupId))
        {
            return true;
        }

        if (entry.type == GeometryEntryType.CUBE)
        {
            return this.lockedCubeKeys.contains(entry.groupId + ":" + entry.cubeIndex);
        }

        return false;
    }

    public void toggleLock(GeometryEntry entry)
    {
        if (entry == null)
        {
            return;
        }

        if (entry.type == GeometryEntryType.BONE)
        {
            if (this.lockedGroupIds.contains(entry.groupId))
            {
                this.lockedGroupIds.remove(entry.groupId);
            }
            else
            {
                this.lockedGroupIds.add(entry.groupId);
            }
        }
        else
        {
            String key = entry.groupId + ":" + entry.cubeIndex;

            if (this.lockedCubeKeys.contains(key))
            {
                this.lockedCubeKeys.remove(key);
            }
            else
            {
                this.lockedCubeKeys.add(key);
            }
        }

        this.fillControls();
        this.fillCubeControls();
    }

    public boolean isCurrentSelectionLocked()
    {
        GeometryEntry entry = this.hierarchyList.getCurrentFirst();

        return this.isLocked(entry);
    }

    public void focusSelection()
    {
        if (this.instance == null || this.selectedGroup == null)
        {
            return;
        }

        Vector3f target = new Vector3f();

        if (this.selectedCube != null)
        {
            target.set(
                this.selectedCube.origin.x + this.selectedCube.size.x / 2F,
                this.selectedCube.origin.y + this.selectedCube.size.y / 2F,
                this.selectedCube.origin.z + this.selectedCube.size.z / 2F
            );
        }
        else
        {
            target.set(this.selectedGroup.initial.translate);
        }

        UIModelRenderer renderer = this.parent.getModelRenderer();

        if (renderer != null)
        {
            renderer.pos.set(-target.x / 16F, -target.y / 16F, -target.z / 16F);
            UIUtils.playClick();
        }
    }

    public void deleteSelection()
    {
        List<GeometryEntry> selected = new ArrayList<>(this.hierarchyList.getCurrent());

        if (selected.isEmpty())
        {
            return;
        }

        if (selected.size() == 1)
        {
            this.deleteEntry(selected.get(0));
            return;
        }

        if (this.instance == null || !(this.instance.model instanceof Model model))
        {
            return;
        }

        /* 1. Delete cubes grouped by ModelGroup in descending order of indices */
        Map<ModelGroup, List<Integer>> cubesPerGroup = new HashMap<>();

        for (GeometryEntry entry : selected)
        {
            if (entry.type == GeometryEntryType.CUBE)
            {
                ModelGroup group = model.getGroup(entry.groupId);

                if (group != null)
                {
                    cubesPerGroup.computeIfAbsent(group, (k) -> new ArrayList<>()).add(entry.cubeIndex);
                }
            }
        }

        for (Map.Entry<ModelGroup, List<Integer>> pair : cubesPerGroup.entrySet())
        {
            ModelGroup group = pair.getKey();
            List<Integer> indices = pair.getValue();
            indices.sort((a, b) -> Integer.compare(b, a));

            for (int idx : indices)
            {
                if (idx >= 0 && idx < group.cubes.size())
                {
                    group.cubes.remove(idx);
                }
            }
        }

        /* 2. Delete bones */
        for (GeometryEntry entry : selected)
        {
            if (entry.type == GeometryEntryType.BONE)
            {
                ModelGroup group = model.getGroup(entry.groupId);

                if (group != null)
                {
                    this.removeGroupFromParent(model, group);
                }
            }
        }

        model.initialize();
        this.reloadHierarchyPreserveSelection(null);
        this.refreshCubeRenderAndSave();
    }

    public void duplicateSelection()
    {
        List<GeometryEntry> selected = new ArrayList<>(this.hierarchyList.getCurrent());

        if (selected.isEmpty())
        {
            return;
        }

        if (selected.size() == 1)
        {
            this.duplicateEntry(selected.get(0));
            return;
        }

        for (GeometryEntry entry : selected)
        {
            this.copyEntry(entry);
            this.pasteEntry(entry);
        }
    }

    public void toggleCoordinateSpace()
    {
        this.coordinateSpace = this.coordinateSpace == TransformOrientation.LOCAL ? TransformOrientation.GLOBAL : TransformOrientation.LOCAL;
        this.gizmoTransform.setOrientation(this.coordinateSpace);
        Gizmo.INSTANCE.setActiveOrientation(this.coordinateSpace);
        UIUtils.playClick();
    }

    public void openSnapContextMenu(UIContext context)
    {
        if (context == null)
        {
            return;
        }

        context.replaceContextMenu((menu) ->
        {
            menu.action(this.snapEnabled ? Icons.SAVED : Icons.NONE, this.snapEnabled ? UIKeys.MODELS_GEOMETRY_SNAP_ON : UIKeys.MODELS_GEOMETRY_SNAP_OFF, () ->
            {
                this.snapEnabled = !this.snapEnabled;
            });

            menu.action(this.positionSnap == 1.0F ? Icons.SAVED : Icons.NONE, UIKeys.MODELS_GEOMETRY_SNAP_POS_1, () ->
            {
                this.positionSnap = 1.0F;
                this.snapEnabled = true;
            });
            menu.action(this.positionSnap == 0.5F ? Icons.SAVED : Icons.NONE, UIKeys.MODELS_GEOMETRY_SNAP_POS_05, () ->
            {
                this.positionSnap = 0.5F;
                this.snapEnabled = true;
            });
            menu.action(this.positionSnap == 0.25F ? Icons.SAVED : Icons.NONE, UIKeys.MODELS_GEOMETRY_SNAP_POS_025, () ->
            {
                this.positionSnap = 0.25F;
                this.snapEnabled = true;
            });
            menu.action(this.positionSnap == 0.125F ? Icons.SAVED : Icons.NONE, UIKeys.MODELS_GEOMETRY_SNAP_POS_0125, () ->
            {
                this.positionSnap = 0.125F;
                this.snapEnabled = true;
            });
            menu.action(this.positionSnap == 0.0625F ? Icons.SAVED : Icons.NONE, UIKeys.MODELS_GEOMETRY_SNAP_POS_00625, () ->
            {
                this.positionSnap = 0.0625F;
                this.snapEnabled = true;
            });

            menu.action(this.rotationSnap == 15.0F ? Icons.SAVED : Icons.NONE, UIKeys.MODELS_GEOMETRY_SNAP_ANGLE_15, () ->
            {
                this.rotationSnap = 15.0F;
                this.snapEnabled = true;
            });
            menu.action(this.rotationSnap == 22.5F ? Icons.SAVED : Icons.NONE, UIKeys.MODELS_GEOMETRY_SNAP_ANGLE_225, () ->
            {
                this.rotationSnap = 22.5F;
                this.snapEnabled = true;
            });
            menu.action(this.rotationSnap == 45.0F ? Icons.SAVED : Icons.NONE, UIKeys.MODELS_GEOMETRY_SNAP_ANGLE_45, () ->
            {
                this.rotationSnap = 45.0F;
                this.snapEnabled = true;
            });
            menu.action(this.rotationSnap == 90.0F ? Icons.SAVED : Icons.NONE, UIKeys.MODELS_GEOMETRY_SNAP_ANGLE_90, () ->
            {
                this.rotationSnap = 90.0F;
                this.snapEnabled = true;
            });
        });
    }

    public void openPivotContextMenu(UIContext context)
    {
        if (context == null)
        {
            return;
        }

        context.replaceContextMenu((menu) ->
        {
            if (this.selectedCube != null)
            {
                menu.action(Icons.SPHERE, UIKeys.MODELS_GEOMETRY_PIVOT_CENTER, () -> this.centerPivotToSelection());
                menu.action(Icons.MOVE_TO, UIKeys.MODELS_GEOMETRY_PIVOT_BOTTOM, () -> this.bottomCenterPivotToSelection());
                menu.action(Icons.REFRESH, UIKeys.MODELS_GEOMETRY_PIVOT_RESET, () -> this.resetPivot());
            }
            else if (this.selectedGroup != null)
            {
                menu.action(Icons.SPHERE, UIKeys.MODELS_GEOMETRY_PIVOT_CENTER_BONE, () -> this.centerBonePivotToCubes());
                menu.action(Icons.REFRESH, UIKeys.MODELS_GEOMETRY_PIVOT_RESET, () -> this.resetPivot());
            }
        });
    }

    public void centerPivotToSelection()
    {
        if (this.selectedCube == null || this.isCurrentSelectionLocked())
        {
            return;
        }

        this.selectedCube.pivot.set(
            this.selectedCube.origin.x + this.selectedCube.size.x / 2F,
            this.selectedCube.origin.y + this.selectedCube.size.y / 2F,
            this.selectedCube.origin.z + this.selectedCube.size.z / 2F
        );

        this.refreshCubeRenderAndSave();
        this.fillCubeControls();
        UIUtils.playClick();
    }

    public void bottomCenterPivotToSelection()
    {
        if (this.selectedCube == null || this.isCurrentSelectionLocked())
        {
            return;
        }

        this.selectedCube.pivot.set(
            this.selectedCube.origin.x + this.selectedCube.size.x / 2F,
            this.selectedCube.origin.y,
            this.selectedCube.origin.z + this.selectedCube.size.z / 2F
        );

        this.refreshCubeRenderAndSave();
        this.fillCubeControls();
        UIUtils.playClick();
    }

    public void resetPivot()
    {
        if (this.isCurrentSelectionLocked())
        {
            return;
        }

        if (this.selectedCube != null)
        {
            this.selectedCube.pivot.set(0, 0, 0);
        }
        else if (this.selectedGroup != null)
        {
            this.selectedGroup.initial.pivot.set(0, 0, 0);
            this.selectedGroup.current.pivot.set(0, 0, 0);
        }

        this.refreshCubeRenderAndSave();
        this.fillCubeControls();
        UIUtils.playClick();
    }

    public void centerBonePivotToCubes()
    {
        if (this.selectedGroup == null || this.selectedGroup.cubes.isEmpty() || this.isCurrentSelectionLocked())
        {
            return;
        }

        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;

        for (ModelCube cube : this.selectedGroup.cubes)
        {
            minX = Math.min(minX, cube.origin.x);
            minY = Math.min(minY, cube.origin.y);
            minZ = Math.min(minZ, cube.origin.z);
            maxX = Math.max(maxX, cube.origin.x + cube.size.x);
            maxY = Math.max(maxY, cube.origin.y + cube.size.y);
            maxZ = Math.max(maxZ, cube.origin.z + cube.size.z);
        }

        this.selectedGroup.initial.pivot.set((minX + maxX) / 2F, (minY + maxY) / 2F, (minZ + maxZ) / 2F);
        this.selectedGroup.current.pivot.set(this.selectedGroup.initial.pivot);

        this.refreshCubeRenderAndSave();
        this.fillCubeControls();
        UIUtils.playClick();
    }

    public void openViewportModeContextMenu(UIContext context)
    {
        if (context == null)
        {
            return;
        }

        UIModelRenderer mr = this.parent.getModelRenderer();

        if (!(mr instanceof UIModelEditorRenderer renderer))
        {
            return;
        }

        context.replaceContextMenu((menu) ->
        {
            menu.action(renderer.viewportMode == UIModelEditorRenderer.ViewportMode.TEXTURED ? Icons.SAVED : Icons.NONE, UIKeys.MODELS_GEOMETRY_VIEWPORT_TEXTURED, () ->
            {
                renderer.viewportMode = UIModelEditorRenderer.ViewportMode.TEXTURED;
            });
            menu.action(renderer.viewportMode == UIModelEditorRenderer.ViewportMode.WIREFRAME ? Icons.SAVED : Icons.NONE, UIKeys.MODELS_GEOMETRY_VIEWPORT_WIREFRAME, () ->
            {
                renderer.viewportMode = UIModelEditorRenderer.ViewportMode.WIREFRAME;
            });
            menu.action(renderer.viewportMode == UIModelEditorRenderer.ViewportMode.XRAY ? Icons.SAVED : Icons.NONE, UIKeys.MODELS_GEOMETRY_VIEWPORT_XRAY, () ->
            {
                renderer.viewportMode = UIModelEditorRenderer.ViewportMode.XRAY;
            });
        });
    }

    public void flipSelectedX()
    {
        if (this.instance == null || !(this.instance.model instanceof Model model) || this.isCurrentSelectionLocked())
        {
            return;
        }

        if (this.selectedCube != null)
        {
            this.flipCubeX(this.selectedCube, model);
        }
        else if (this.selectedGroup != null)
        {
            this.flipGroupTreeX(this.selectedGroup, model);
        }

        model.initialize();
        this.refreshCubeRenderAndSave();
        this.fillControls();
        this.fillCubeControls();
        UIUtils.playClick();
    }

    private void flipCubeX(ModelCube cube, Model model)
    {
        cube.origin.x = -cube.origin.x - cube.size.x;
        cube.pivot.x = -cube.pivot.x;
        cube.rotate.y = -cube.rotate.y;
        cube.rotate.z = -cube.rotate.z;

        if (!this.isCustomFaceUV(cube))
        {
            boolean mirror = !this.isCubeMirrored(cube);
            cube.setupBoxUV(this.getBoxUV(cube), mirror);
        }
        else
        {
            ModelUV temp = cube.left;
            cube.left = cube.right;
            cube.right = temp;
        }

        cube.generateQuads(model.textureWidth, model.textureHeight);
    }

    private void flipGroupTreeX(ModelGroup group, Model model)
    {
        group.initial.translate.x = -group.initial.translate.x;
        group.initial.rotate.y = -group.initial.rotate.y;
        group.initial.rotate.z = -group.initial.rotate.z;
        group.initial.pivot.x = -group.initial.pivot.x;
        group.current.copy(group.initial);

        for (ModelCube cube : group.cubes)
        {
            this.flipCubeX(cube, model);
        }

        for (ModelGroup child : group.children)
        {
            this.flipGroupTreeX(child, model);
        }
    }

    public void duplicateAndMirrorX()
    {
        if (this.instance == null || !(this.instance.model instanceof Model model) || this.isCurrentSelectionLocked())
        {
            return;
        }

        if (this.selectedCube != null && this.selectedGroup != null)
        {
            ModelCube clone = this.selectedCube.copy();
            this.flipCubeX(clone, model);
            clone.name = this.getMirroredName(clone.name);
            int insertIndex = this.selectedGroup.cubes.indexOf(this.selectedCube) + 1;

            if (insertIndex <= 0 || insertIndex > this.selectedGroup.cubes.size())
            {
                insertIndex = this.selectedGroup.cubes.size();
            }

            this.selectedGroup.cubes.add(insertIndex, clone);
            GeometryEntry entry = new GeometryEntry(GeometryEntryType.CUBE, this.selectedGroup.id, insertIndex, 0, this.getCubeLabel(clone), false);

            model.initialize();
            this.reloadHierarchyPreserveSelection(entry);
            this.refreshCubeRenderAndSave();
            UIUtils.playClick();
        }
        else if (this.selectedGroup != null)
        {
            Set<String> used = new HashSet<>(model.getAllGroupKeys());
            String newId = this.makeUniqueGroupId(this.getMirroredName(this.selectedGroup.id), used);
            ModelGroup clone = this.cloneGroupTree(this.selectedGroup, this.selectedGroup.parent, newId, true, used);

            this.flipGroupTreeX(clone, model);

            if (this.selectedGroup.parent != null)
            {
                this.selectedGroup.parent.children.add(clone);
            }
            else
            {
                model.topGroups.add(clone);
            }

            GeometryEntry entry = new GeometryEntry(GeometryEntryType.BONE, clone.id, -1, 0, clone.id, true);

            model.initialize();
            this.reloadHierarchyPreserveSelection(entry);
            this.refreshCubeRenderAndSave();
            UIUtils.playClick();
        }
    }

    public String getMirroredName(String name)
    {
        if (name == null || name.isEmpty())
        {
            return "mirrored";
        }

        if (name.contains("_right")) return name.replace("_right", "_left");
        if (name.contains("_left")) return name.replace("_left", "_right");
        if (name.contains("_r")) return name.replace("_r", "_l");
        if (name.contains("_l")) return name.replace("_l", "_r");
        if (name.contains("right")) return name.replace("right", "left");
        if (name.contains("left")) return name.replace("left", "right");
        if (name.contains("Right")) return name.replace("Right", "Left");
        if (name.contains("Left")) return name.replace("Left", "Right");
        if (name.contains("_der")) return name.replace("_der", "_izq");
        if (name.contains("_izq")) return name.replace("_izq", "_der");

        return name + "_mirror";
    }

    private void addFolder()
    {
        if (this.instance == null || !(this.instance.model instanceof Model model))
        {
            return;
        }

        Set<String> used = new HashSet<>(model.getAllGroupKeys());
        String id = this.makeUniqueGroupId("folder", used);
        ModelGroup group = new ModelGroup(id);
        ModelGroup parent = this.selectedGroup;

        if (parent == null)
        {
            model.topGroups.add(group);
        }
        else
        {
            group.parent = parent;
            parent.children.add(group);
        }

        model.initialize();
        this.reloadHierarchyPreserveSelection(new GeometryEntry(GeometryEntryType.BONE, id, -1, 0, id, true));
        this.refreshCubeRenderAndSave();
    }

    private void addIKLocator()
    {
        if (this.instance == null || !(this.instance.model instanceof Model model))
        {
            return;
        }

        Set<String> used = new HashSet<>(model.getAllGroupKeys());
        String id = this.makeUniqueGroupId("ik_locator", used);
        ModelGroup group = new ModelGroup(id);
        group.ikLocator = true;
        ModelGroup parent = this.selectedGroup;

        if (parent == null)
        {
            model.topGroups.add(group);
        }
        else
        {
            group.parent = parent;
            parent.children.add(group);
        }

        model.initialize();
        this.reloadHierarchyPreserveSelection(new GeometryEntry(GeometryEntryType.BONE, id, -1, 0, id, false));
        this.refreshCubeRenderAndSave();
    }

    private void addCube()
    {
        if (this.selectedGroup == null)
        {
            return;
        }

        ModelCube cube = this.selectedCube == null ? new ModelCube() : this.selectedCube.copy();

        if (this.selectedCube == null)
        {
            cube.size.set(1F, 1F, 1F);
            cube.pivot.set(this.selectedGroup.initial.pivot);
            cube.setupBoxUV(new Vector2f(0F, 0F), false);
        }

        this.selectedGroup.cubes.add(cube);
        this.selectedCube = cube;
        this.reloadModelData();

        for (GeometryEntry entry : this.hierarchyList.getList())
        {
            if (entry.type == GeometryEntryType.CUBE && entry.groupId.equals(this.selectedGroup.id) && entry.cubeIndex == this.selectedGroup.cubes.indexOf(cube))
            {
                this.hierarchyList.setCurrentDirect(entry);
                this.selectCurrentHierarchyEntry();

                break;
            }
        }

        this.refreshCubeRenderAndSave();
    }

    private void removeCube()
    {
        if (this.selectedGroup == null || this.selectedCube == null)
        {
            return;
        }

        int index = this.selectedGroup.cubes.indexOf(this.selectedCube);

        if (index < 0)
        {
            return;
        }

        this.selectedGroup.cubes.remove(index);
        this.selectedCube = null;
        this.reloadModelData();
        this.refreshCubeRenderAndSave();
    }

    private void refreshCubeRenderAndSave()
    {
        if (this.selectedCube != null && this.selectedGroup != null && this.selectedGroup.owner != null)
        {
            int tw = Math.max(1, this.selectedGroup.owner.textureWidth);
            int th = Math.max(1, this.selectedGroup.owner.textureHeight);

            this.selectedCube.generateQuads(tw, th);
        }

        if (this.instance != null)
        {
            this.instance.delete();
            this.instance.setup();
        }

        this.parent.dirty();
        this.recordUndoState();
    }

    private void toggleGroupCollapsed(String groupId)
    {
        if (this.collapsedGroupIds.contains(groupId))
        {
            this.collapsedGroupIds.remove(groupId);
        }
        else
        {
            this.collapsedGroupIds.add(groupId);
        }

        GeometryEntry current = this.hierarchyList.getCurrentFirst();
        GeometryEntry preferred = current;

        if (current != null && current.type == GeometryEntryType.CUBE && current.groupId.equals(groupId))
        {
            preferred = new GeometryEntry(GeometryEntryType.BONE, groupId, -1, 0, groupId, true);
        }

        this.reloadHierarchyPreserveSelection(preferred);
    }

    private void reloadHierarchyPreserveSelection(GeometryEntry preferred)
    {
        if (this.instance == null || !(this.instance.model instanceof Model model))
        {
            return;
        }

        this.hierarchyList.clear();

        for (ModelGroup top : model.topGroups)
        {
            this.collectHierarchy(top, 0);
        }

        GeometryEntry selected = null;

        if (preferred != null)
        {
            for (GeometryEntry entry : this.hierarchyList.getList())
            {
                if (entry.type == preferred.type && entry.groupId.equals(preferred.groupId) && entry.cubeIndex == preferred.cubeIndex)
                {
                    selected = entry;
                    break;
                }
            }
        }

        if (selected == null && !this.hierarchyList.getList().isEmpty())
        {
            selected = this.hierarchyList.getList().get(0);
        }

        if (selected != null)
        {
            this.hierarchyList.setCurrentDirect(selected);
            this.selectCurrentHierarchyEntry();
        }
        else
        {
            this.selectedGroup = null;
            this.selectedCube = null;
            this.parent.getModelRenderer().setSelectedCube(null);
            this.fillControls();
            this.fillCubeControls();
        }
    }

    private void handleHierarchySwap(int from, int to)
    {
        if (this.instance == null || !(this.instance.model instanceof Model model))
        {
            return;
        }

        List<GeometryEntry> entries = this.hierarchyList.getList();

        if (from < 0 || from >= entries.size() || to < 0 || to >= entries.size() || from == to)
        {
            return;
        }

        GeometryEntry source = entries.get(from);
        GeometryEntry destination = entries.get(to);
        GeometryEntry preferred = null;

        if (source.type == GeometryEntryType.BONE)
        {
            preferred = this.reorderBoneByDrag(model, source, destination, to > from);
        }
        else if (source.type == GeometryEntryType.CUBE)
        {
            preferred = this.reorderCubeByDrag(model, source, destination, to > from);
        }

        if (preferred == null)
        {
            this.reloadHierarchyPreserveSelection(source);
            return;
        }

        model.initialize();
        this.reloadHierarchyPreserveSelection(preferred);
        this.refreshCubeRenderAndSave();
    }

    private GeometryEntry reorderBoneByDrag(Model model, GeometryEntry source, GeometryEntry destination, boolean moveAfter)
    {
        ModelGroup sourceGroup = model.getGroup(source.groupId);

        if (sourceGroup == null)
        {
            return null;
        }

        ModelGroup destinationGroup = model.getGroup(destination.groupId);

        if (destinationGroup == null || sourceGroup == destinationGroup || this.isDescendantGroup(sourceGroup, destinationGroup))
        {
            return null;
        }

        this.removeGroupFromParent(model, sourceGroup);

        if (destination.type == GeometryEntryType.BONE)
        {
            sourceGroup.parent = destinationGroup;

            if (moveAfter)
            {
                destinationGroup.children.add(sourceGroup);
            }
            else
            {
                destinationGroup.children.add(0, sourceGroup);
            }
        }
        else
        {
            sourceGroup.parent = destinationGroup;

            if (moveAfter)
            {
                destinationGroup.children.add(sourceGroup);
            }
            else
            {
                destinationGroup.children.add(0, sourceGroup);
            }
        }

        return new GeometryEntry(GeometryEntryType.BONE, sourceGroup.id, -1, 0, sourceGroup.id, true);
    }

    private GeometryEntry reorderCubeByDrag(Model model, GeometryEntry source, GeometryEntry destination, boolean moveAfter)
    {
        ModelGroup sourceGroup = model.getGroup(source.groupId);
        ModelGroup destinationGroup = model.getGroup(destination.groupId);

        if (sourceGroup == null || destinationGroup == null || source.cubeIndex < 0 || source.cubeIndex >= sourceGroup.cubes.size())
        {
            return null;
        }

        ModelCube cube = sourceGroup.cubes.remove(source.cubeIndex);
        int insertIndex;

        if (destination.type == GeometryEntryType.BONE)
        {
            insertIndex = moveAfter ? destinationGroup.cubes.size() : 0;
        }
        else
        {
            int destinationIndex = destination.cubeIndex;

            if (destinationIndex < 0 || destinationIndex >= destinationGroup.cubes.size())
            {
                sourceGroup.cubes.add(Math.min(source.cubeIndex, sourceGroup.cubes.size()), cube);

                return null;
            }

            insertIndex = destinationIndex + (moveAfter ? 1 : 0);

            if (sourceGroup == destinationGroup && source.cubeIndex < destinationIndex)
            {
                insertIndex -= 1;
            }
        }

        if (insertIndex < 0)
        {
            insertIndex = 0;
        }

        if (insertIndex > destinationGroup.cubes.size())
        {
            insertIndex = destinationGroup.cubes.size();
        }

        destinationGroup.cubes.add(insertIndex, cube);

        return new GeometryEntry(GeometryEntryType.CUBE, destinationGroup.id, insertIndex, 0, this.getCubeLabel(cube), false);
    }

    private ModelGroup cloneGroupTree(ModelGroup source, ModelGroup parent, String requestedId, boolean uniquify, Set<String> usedIds)
    {
        if (source == null)
        {
            return null;
        }

        String id = requestedId == null ? source.id : requestedId;

        if (uniquify)
        {
            id = this.makeUniqueGroupId(id, usedIds);
        }

        if (usedIds != null)
        {
            usedIds.add(id);
        }

        ModelGroup group = new ModelGroup(id);

        group.parent = parent;
        group.visible = source.visible;
        group.lighting = source.lighting;
        group.color.copy(source.color);
        group.textureOverride = source.textureOverride;
        group.initial.copy(source.initial);
        group.current.copy(source.current);

        for (ModelCube cube : source.cubes)
        {
            group.cubes.add(cube.copy());
        }

        group.meshes.addAll(source.meshes.stream().map((m) -> m.copy()).toList());

        for (ModelGroup child : source.children)
        {
            ModelGroup childCopy = this.cloneGroupTree(child, group, child.id, uniquify, usedIds);

            if (childCopy != null)
            {
                group.children.add(childCopy);
            }
        }

        return group;
    }

    private void replaceGroup(Model model, ModelGroup oldGroup, ModelGroup replacement)
    {
        if (oldGroup.parent == null)
        {
            int index = model.topGroups.indexOf(oldGroup);

            if (index >= 0)
            {
                model.topGroups.set(index, replacement);
            }
            else
            {
                model.topGroups.add(replacement);
            }
        }
        else
        {
            List<ModelGroup> siblings = oldGroup.parent.children;
            int index = siblings.indexOf(oldGroup);

            if (index >= 0)
            {
                siblings.set(index, replacement);
            }
            else
            {
                siblings.add(replacement);
            }
        }
    }

    private String sanitizeName(String name)
    {
        if (name == null)
        {
            return "";
        }

        return name.trim().replace(" ", "_");
    }

    private String sanitizeCubeName(String name)
    {
        if (name == null)
        {
            return "";
        }

        return name.trim();
    }

    private String makeUniqueGroupId(String base, Set<String> used)
    {
        String source = this.sanitizeName(base);

        if (source.isEmpty())
        {
            source = "group";
        }

        String candidate = source;
        int i = 1;

        while (used != null && used.contains(candidate))
        {
            candidate = source + "_" + i;
            i++;
        }

        return candidate;
    }

    private void removeGroupFromParent(Model model, ModelGroup group)
    {
        if (group.parent == null)
        {
            model.topGroups.remove(group);
        }
        else
        {
            group.parent.children.remove(group);
        }
    }

    private boolean isDescendantGroup(ModelGroup source, ModelGroup candidateParent)
    {
        for (ModelGroup cursor = candidateParent; cursor != null; cursor = cursor.parent)
        {
            if (cursor == source)
            {
                return true;
            }
        }

        return false;
    }

    private String getCubeLabel(ModelCube cube)
    {
        if (cube == null || cube.name == null || cube.name.isBlank())
        {
            return UIKeys.MODELS_GEOMETRY_CUBE.get();
        }

        return cube.name;
    }

    private GeometryState captureState()
    {
        if (this.instance == null || !(this.instance.model instanceof Model model))
        {
            return null;
        }

        MapType data = model.toData();
        GeometryEntry selected = this.hierarchyList.getCurrentFirst();
        String selectedGroupId = selected == null ? null : selected.groupId;
        int selectedCubeIndex = selected == null ? -1 : selected.cubeIndex;
        boolean selectedCubeEntry = selected != null && selected.type == GeometryEntryType.CUBE;

        return new GeometryState(data, selectedGroupId, selectedCubeIndex, selectedCubeEntry, new HashSet<>(this.collapsedGroupIds));
    }

    private void recordUndoState()
    {
        if (this.applyingUndo)
        {
            return;
        }

        GeometryState current = this.captureState();

        if (current == null)
        {
            this.lastUndoState = null;
            return;
        }

        if (this.lastUndoState == null)
        {
            this.lastUndoState = current;
            return;
        }

        if (this.lastUndoState.same(current))
        {
            return;
        }

        this.undoManager.pushUndo(new GeometryStateUndo(this.lastUndoState, current));
        this.lastUndoState = current;
    }

    private void applyState(GeometryState state)
    {
        if (state == null || this.instance == null || !(this.instance.model instanceof Model model))
        {
            return;
        }

        this.applyingUndo = true;

        try
        {
            model.topGroups.clear();
            model.fromData((MapType) state.model.copy());
            model.initialize();

            this.collapsedGroupIds.clear();
            this.collapsedGroupIds.addAll(state.collapsedGroupIds);

            GeometryEntry preferred = null;

            if (state.selectedGroupId != null)
            {
                preferred = new GeometryEntry(state.selectedCube ? GeometryEntryType.CUBE : GeometryEntryType.BONE, state.selectedGroupId, state.selectedCubeIndex, 0, "", state.selectedCube ? false : true);
            }

            this.reloadHierarchyPreserveSelection(preferred);
            this.refreshCubeRenderAndSave();
            this.lastUndoState = this.captureState();
        }
        finally
        {
            this.applyingUndo = false;
        }
    }

    public boolean isCubeMirrored(ModelCube cube)
    {
        return cube.front != null && cube.front.size.x < 0;
    }

    public Vector2f getBoxUV(ModelCube cube)
    {
        Vector2f uv = new Vector2f();

        if (cube.front != null)
        {
            float depth = (float) Math.floor(Math.abs(cube.size.z));
            float width = (float) Math.floor(Math.abs(cube.size.x));

            uv.x = this.isCubeMirrored(cube) ? cube.front.origin.x - depth - width : cube.front.origin.x - depth;
            uv.y = cube.front.origin.y - depth;
        }

        return uv;
    }

    private void saveModelFile()
    {
        if (this.config == null || this.instance == null)
        {
            return;
        }

        File file = this.findModelFile(this.config.getId());

        if (file == null)
        {
            return;
        }

        MapType map = CubicLoader.toData(this.instance);

        try
        {
            IOUtils.writeText(file, DataToString.toString(map, true));
            BBSModClient.getModels().loadModel(this.config.getId());
            this.parent.getModelRenderer().invalidatePreviewModel();
            this.parent.getModelRenderer().setModel(this.config.getId());
            this.parent.getModelRenderer().setConfig(this.config);
            this.reloadModelData();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private File findModelFile(String id)
    {
        Link root = Link.assets("models/" + id);
        File direct = BBSMod.getProvider().getFile(root.combine("model.bbs.json"));

        if (direct != null && direct.exists())
        {
            return direct;
        }

        File folder = BBSMod.getProvider().getFile(root);

        if (folder == null || !folder.exists())
        {
            return null;
        }

        return this.findBbsRecursively(folder);
    }

    private File findBbsRecursively(File folder)
    {
        File[] files = folder.listFiles();

        if (files == null)
        {
            return null;
        }

        for (File file : files)
        {
            if (file.isFile() && file.getName().endsWith(".bbs.json"))
            {
                return file;
            }
        }

        for (File file : files)
        {
            if (file.isDirectory())
            {
                File result = this.findBbsRecursively(file);

                if (result != null)
                {
                    return result;
                }
            }
        }

        return null;
    }

    private static class GeometryState
    {
        private final MapType model;
        private final String selectedGroupId;
        private final int selectedCubeIndex;
        private final boolean selectedCube;
        private final Set<String> collapsedGroupIds;

        private GeometryState(MapType model, String selectedGroupId, int selectedCubeIndex, boolean selectedCube, Set<String> collapsedGroupIds)
        {
            this.model = model;
            this.selectedGroupId = selectedGroupId;
            this.selectedCubeIndex = selectedCubeIndex;
            this.selectedCube = selectedCube;
            this.collapsedGroupIds = collapsedGroupIds;
        }

        private boolean same(GeometryState state)
        {
            return state != null
                && this.selectedCube == state.selectedCube
                && this.selectedCubeIndex == state.selectedCubeIndex
                && ((this.selectedGroupId == null && state.selectedGroupId == null) || (this.selectedGroupId != null && this.selectedGroupId.equals(state.selectedGroupId)))
                && this.collapsedGroupIds.equals(state.collapsedGroupIds)
                && this.model.equals(state.model);
        }
    }

    private static class GeometryStateUndo implements IUndo<UIModelGeometryPanel>
    {
        private final GeometryState before;
        private GeometryState after;
        private boolean mergeable = true;

        private GeometryStateUndo(GeometryState before, GeometryState after)
        {
            this.before = before;
            this.after = after;
        }

        @Override
        public IUndo<UIModelGeometryPanel> noMerging()
        {
            this.mergeable = false;

            return this;
        }

        @Override
        public boolean isMergeable(IUndo<UIModelGeometryPanel> undo)
        {
            return this.mergeable && undo instanceof GeometryStateUndo;
        }

        @Override
        public void merge(IUndo<UIModelGeometryPanel> undo)
        {
            if (undo instanceof GeometryStateUndo stateUndo)
            {
                this.after = stateUndo.after;
            }
        }

        @Override
        public void undo(UIModelGeometryPanel context)
        {
            context.applyState(this.before);
        }

        @Override
        public void redo(UIModelGeometryPanel context)
        {
            context.applyState(this.after);
        }
    }

    private enum GeometryEntryType
    {
        BONE,
        CUBE
    }

    private static class GeometryEntry
    {
        private final GeometryEntryType type;
        private final String groupId;
        private final int cubeIndex;
        private final int depth;
        private final String label;
        private final boolean expandable;

        private GeometryEntry(GeometryEntryType type, String groupId, int cubeIndex, int depth, String label, boolean expandable)
        {
            this.type = type;
            this.groupId = groupId;
            this.cubeIndex = cubeIndex;
            this.depth = depth;
            this.label = label;
            this.expandable = expandable;
        }
    }

    public UIPropTransform getGizmoTransformEditor()
    {
        return this.gizmoTransform;
    }
}
