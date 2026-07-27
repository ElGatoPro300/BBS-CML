package mchorse.bbs_mod.ui.forms.editors.forms;

import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCacheEntry;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.UIFormEditor;
import mchorse.bbs_mod.ui.forms.editors.panels.UIFormPanel;
import mchorse.bbs_mod.ui.forms.editors.panels.UIGeneralFormPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIPanelBase;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.gizmo.GizmoMatrixUtils;
import mchorse.bbs_mod.ui.utils.gizmo.TransformOrientation;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.joml.Matrices;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public abstract class UIForm <T extends Form> extends UIPanelBase<UIFormPanel<T>>
{
    public UIFormEditor editor;

    public T form;
    public UIFormPanel<T> defaultPanel;
    public UIGeneralFormPanel generalPanel;

    private UIPropTransform general;

    public UIForm()
    {
        super(Direction.LEFT);

        this.keys().register(Keys.FILM_CONTROLLER_CYCLE_EDITORS, this::cyclePanels);
    }

    @Override
    public void setPanel(UIFormPanel<T> panel)
    {
        super.setPanel(panel);

        if (panel == this.generalPanel && this.editor != null)
        {
            this.editor.enableFormTransformGizmoFromGeneralPanel();
        }
    }

    public UIPropTransform getEditableTransform()
    {
        this.setPanel(this.generalPanel);

        return this.general;
    }

    private void cyclePanels()
    {
        int index = this.panels.indexOf(this.view);
        int newIndex = MathUtils.cycler(index + (Window.isShiftPressed() ? -1 : 1), this.panels);

        this.setPanel(this.panels.get(newIndex));
        UIUtils.playClick();
    }

    public Matrix4f getOrigin(float transition)
    {
        TransformOrientation orientation = this.generalPanel != null ? this.generalPanel.transform.getOrientation() : TransformOrientation.PARENT;

        return this.getOrigin(transition, FormUtils.getPath(this.form), orientation);
    }

    public Matrix4f getOrigin(float transition, String path, boolean local)
    {
        return this.getOrigin(transition, path, local ? TransformOrientation.LOCAL : TransformOrientation.PARENT);
    }

    public Matrix4f getOrigin(float transition, String path, TransformOrientation orientation)
    {
        if (path == null)
        {
            return Matrices.EMPTY_4F;
        }

        Form root = FormUtils.getRoot(this.form);
        MatrixCache map = FormUtilsClient.getRenderer(root).collectMatrices(this.editor.renderer.getTargetEntity(), transition);
        
        boolean forceOrigin = path.endsWith("#origin");
        
        if (forceOrigin) path = path.substring(0, path.length() - 7);
        
        MatrixCacheEntry entry = map.get(path);

        if (entry == null)
        {
            return Matrices.EMPTY_4F;
        }

        Matrix4f matrix;
        boolean local = orientation != null && orientation.usesLocalBoneBasis();

        if (forceOrigin)
        {
            matrix = entry.origin();
        }
        else if (local)
        {
            Matrix4f localMatrix = entry.matrix();
            Matrix4f originMatrix = entry.origin();

            if (localMatrix != null && originMatrix != null)
            {
                matrix = new Matrix4f(localMatrix);
                matrix.setTranslation(originMatrix.getTranslation(new Vector3f()));
            }
            else
            {
                matrix = localMatrix != null ? localMatrix : originMatrix;
            }
        }
        else
        {
            matrix = entry.origin();
        }

        matrix = matrix == null ? null : new Matrix4f(matrix);
        matrix = GizmoMatrixUtils.applyOrientationSpace(matrix, orientation);

        return matrix == null ? Matrices.EMPTY_4F : matrix;
    }

    protected void registerDefaultPanels()
    {
        UIGeneralFormPanel panel = new UIGeneralFormPanel(this);

        this.registerPanel(panel, UIKeys.FORMS_EDITORS_GENERAL, Icons.GEAR);

        this.generalPanel = panel;
        this.general = panel.transform;
    }

    public void setEditor(UIFormEditor editor)
    {
        this.editor = editor;
    }

    public void startEdit(T form)
    {
        this.form = form;

        this.setPanel(this.defaultPanel);

        for (UIFormPanel<T> panel : this.panels)
        {
            panel.startEdit(form);
        }
    }

    public void finishEdit()
    {
        for (UIFormPanel<T> panel : this.panels)
        {
            panel.finishEdit();
        }
    }

    public void pickBone(String bone)
    {
        if (this.view != null)
        {
            this.view.pickBone(bone);
        }
    }

    @Override
    protected void renderBackground(UIContext context, int x, int y, int w, int h)
    {
        context.batcher.box(x, y, x + w, y + h, Colors.A100);
    }

    @Override
    public void collectUndoData(MapType data)
    {
        super.collectUndoData(data);

        int panelIndex = this.panels.indexOf(this.view);
        data.putInt("panel", panelIndex);

        double scroll = 0D;
        if (this.view != null && this.view.options != null)
        {
            scroll = this.view.options.scroll.getScroll();
        }

        data.putDouble("scroll", scroll);
    }

    @Override
    public void applyUndoData(MapType data)
    {
        super.applyUndoData(data);

        int panelIndex = data.getInt("panel");
        if (panelIndex >= 0 && panelIndex < this.panels.size())
        {
            this.setPanel(this.panels.get(panelIndex));
        }
        else
        {
            this.setPanel(this.defaultPanel);
        }

        if (this.view != null && this.view.options != null)
        {
            this.view.options.scroll.setScroll(data.getDouble("scroll"));
        }
    }
}
