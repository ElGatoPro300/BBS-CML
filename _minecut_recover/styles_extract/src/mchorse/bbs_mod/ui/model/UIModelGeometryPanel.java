package mchorse.bbs_mod.ui.model;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.cubic.CubicLoader;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelCube;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.model.ModelConfig;
import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
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
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;
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
import java.util.HashSet;
import java.util.List;
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
    private final UITrackpad cubeUvX;
    private final UITrackpad cubeUvY;
    private final UIToggle cubeMirror;
    private final UIIcon addCubeIcon;
    private final UIIcon addFolderIcon;
    private final UIIcon addIKLocatorIcon;
    private final Set<String> collapsedGroupIds = new HashSet<>();
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

        int sideMargin = 10;
        int leftWidth = 260;
        int rightWidth = 280;

        UILabel hierarchyTitle = UI.label(UIKeys.MODELS_GEOMETRY_BONE_HIERARCHY).background();
        hierarchyTitle.relative(this).x(sideMargin).y(10).w(leftWidth).h(12);

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
                int offset = element.depth * 10;
                int arrowX = x + 2 + offset;
                int iconX = x + 18 + offset;
                Icon icon = element.type == GeometryEntryType.BONE ? Icons.FOLDER : Icons.BLOCK;

                if (element.expandable)
                {
                    context.batcher.icon(UIModelGeometryPanel.this.collapsedGroupIds.contains(element.groupId) ? Icons.COLLAPSED : Icons.UNCOLLAPSED, arrowX, y + 1);
                }

                context.batcher.icon(icon, iconX, y + 1);
                context.batcher.textShadow(element.label, x + 36 + offset, textY, hover ? Colors.HIGHLIGHT : Colors.WHITE);
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
                if (!this.isFiltering() && this.area.isInside(context) && context.mouseButton == 0)
                {
                    int visibleIndex = this.scroll.getIndex(context.mouseX, context.mouseY);

                    if (this.exists(visibleIndex))
                    {
                        GeometryEntry entry = this.getList().get(visibleIndex);
                        int y = this.area.y + visibleIndex * this.scroll.scrollItemSize - (int) this.scroll.getScroll();
                        int offset = entry.depth * 10;
                        int arrowX = this.area.x + 2 + offset;

                        if (entry.expandable && context.mouseX >= arrowX && context.mouseX < arrowX + 16 && context.mouseY >= y + 1 && context.mouseY < y + 17)
                        {
                            UIModelGeometryPanel.this.toggleGroupCollapsed(entry.groupId);

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

                        this.setCurrentDirect(entry);
                        UIModelGeometryPanel.this.selectCurrentHierarchyEntry();
                        UIModelGeometryPanel.this.openHierarchyContextMenu(context, entry);

                        return true;
                    }
                }

                return super.subMouseClicked(context);
            }
        };
        this.hierarchyList.background();
        this.hierarchyList.sorting();
        this.hierarchyList.scroll.scrollItemSize = 18;
        this.hierarchySearch = new UISearchList<>(this.hierarchyList);
        this.hierarchySearch.label(UIKeys.GENERAL_SEARCH);
        this.hierarchySearch.relative(this).x(sideMargin).y(52).w(leftWidth).h(1F, -94);

        UILabel editorTitle = UI.label(UIKeys.MODELS_GEOMETRY_EDITOR).background();
        editorTitle.relative(this).x(1F, -rightWidth - sideMargin).y(10).w(rightWidth).h(12);

        this.selectedBoneLabel = UI.label(IKey.raw("-"));
        this.selectedBoneLabel.relative(editorTitle).y(1F, 4).w(1F).h(12);

        this.unifiedTransform = new UITransform()
        {
            {
                UIElement row = this.r2x.getParentContainer();

                if (row != null)
                {
                    row.removeFromParent();
                }

                this.iconR2.setEnabled(false);
                this.r2x.setEnabled(false);
                this.r2y.setEnabled(false);
                this.r2z.setEnabled(false);
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
        this.unifiedTransform.relative(this.selectedBoneLabel).y(1F, 6).w(1F).h(104);
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

        this.saveButton = new UIButton(UIKeys.GENERAL_SAVE, (b) -> this.saveModelFile());
        this.saveButton.w(1F).h(20);
        UIElement buttons = UI.row(this.saveButton);
        buttons.relative(this.unifiedTransform).y(1F, 10).w(1F).h(20);

        UILabel cubeInflateLabel = UI.label(UIKeys.MODELS_GEOMETRY_CUBE_INFLATE);
        cubeInflateLabel.w(0.4F, -4).h(20);
        this.cubeInflate = this.trackpad((v) -> this.updateCubeInflate(v.floatValue()));
        this.cubeInflate.w(0.6F, -2);
        UIElement cubeInflateRow = UI.row(6, cubeInflateLabel, this.cubeInflate);
        cubeInflateRow.relative(buttons).y(1F, 8).w(1F).h(20);

        UILabel cubeUvLabel = UI.label(UIKeys.MODELS_GEOMETRY_CUBE_UV);
        cubeUvLabel.w(0.25F, -4).h(20);
        this.cubeUvX = this.trackpad((v) -> this.updateCubeUV(0, v.floatValue()));
        this.cubeUvY = this.trackpad((v) -> this.updateCubeUV(1, v.floatValue()));
        this.cubeMirror = new UIToggle(UIKeys.MODELS_GEOMETRY_CUBE_MIRROR, (b) -> this.updateCubeMirror(b.getValue()));
        this.cubeUvX.w(0.25F, -3);
        this.cubeUvY.w(0.25F, -3);
        this.cubeMirror.w(0.25F, -3).h(20);
        UIElement cubeUvRow = UI.row(4, cubeUvLabel, this.cubeUvX, this.cubeUvY, this.cubeMirror);
        cubeUvRow.relative(cubeInflateRow).y(1F, 6).w(1F).h(20);

        UIElement editor = new UIElement();
        editor.relative(this).x(1F, -rightWidth - sideMargin).y(26).w(rightWidth).h(1F, -36);
        editor.add(editorTitle, this.selectedBoneLabel, this.unifiedTransform, buttons, cubeInflateRow, cubeUvRow, this.gizmoTransform);

        this.addCubeIcon = new UIIcon(Icons.BLOCK, (b) -> this.addCube());
        this.addFolderIcon = new UIIcon(Icons.FOLDER, (b) -> this.addFolder());
        this.addIKLocatorIcon = new UIIcon(Icons.POSE, (b) -> this.addIKLocator());
        this.addCubeIcon.tooltip(UIKeys.MODELS_GEOMETRY_ADD_CUBE);
        this.addFolderIcon.tooltip(UIKeys.MODELS_GEOMETRY_ADD_FOLDER);
        this.addIKLocatorIcon.tooltip(UIKeys.MODELS_IK_CREATE_LOCATOR_TOOLTIP);
        this.addCubeIcon.relative(this).x(sideMargin).y(26).w(20).h(20);
        this.addFolderIcon.relative(this.addCubeIcon).x(1F, 2).y(0).w(20).h(20);
        this.addIKLocatorIcon.relative(this.addFolderIcon).x(1F, 2).y(0).w(20).h(20);

        this.add(hierarchyTitle, this.addCubeIcon, this.addFolderIcon, this.addIKLocatorIcon, this.hierarchySearch, editor);

        this.fillControls();
        this.fillCubeControls();
    }

    private void fillCubeControls()
    {
        this.filling = true;

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
            this.cubeInflate.setEnabled(true);
            this.cubeUvX.setEnabled(true);
            this.cubeUvY.setEnabled(true);
            this.cubeMirror.setEnabled(true);
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
        }

        this.filling = false;
        this.syncGizmoTransformFromSelection();
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
        if (this.filling || (this.selectedGroup == null && this.selectedCube == null))
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
    }

    private void openHierarchyContextMenu(UIContext context, GeometryEntry entry)
    {
        context.replaceContextMenu((menu) ->
        {
            menu.action(Icons.COPY, UIKeys.GENERAL_COPY, () -> this.copyEntry(entry));
            menu.action(Icons.PASTE, UIKeys.GENERAL_PASTE, () -> this.pasteEntry(entry));
            menu.action(Icons.DUPE, UIKeys.GENERAL_DUPE, () -> this.duplicateEntry(entry));
            menu.action(Icons.EDIT, UIKeys.GENERAL_RENAME, () -> this.renameEntry(entry));
            menu.action(Icons.REMOVE, UIKeys.GENERAL_REMOVE, () -> this.deleteEntry(entry));
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

    private boolean isCubeMirrored(ModelCube cube)
    {
        return cube.front != null && cube.front.size.x < 0;
    }

    private Vector2f getBoxUV(ModelCube cube)
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
