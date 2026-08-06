package mchorse.bbs_mod.ui.framework.elements.input;

import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.PaintMaskShape;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanels;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcons;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Direction;

import java.util.function.Consumer;

/**
 * Compact Transform toggle: a fixed-size icon docked at the trailing edge of a
 * field. The field keeps all remaining horizontal space; only the icon takes a
 * minimal 20px. When selected, shows the primary-color bottom bar + gradient and
 * reveals shape + transform grid on a full-width row underneath.
 * <p>
 * Always place this control as a full-width column child. Use
 * {@link #withHeaderExtras} to keep siblings like Lighting on the same header
 * row while the grid still opens below at full width.
 */
public class UIEffectTransformCollapse extends UIElement
{
    private final UIIcon toggle;
    private final UIAnimatedCollapseShell shell;
    private final UIIcons shapeIcons;
    private final UIEffectKeyframeTransform transform;
    private boolean expanded;
    private boolean manageOwnShell = true;
    private Consumer<UIEffectTransformCollapse> toggleHandler;
    private UIElement leading;
    private UIElement[] headerExtras = new UIElement[0];
    private IKey fieldLabel;

    public UIEffectTransformCollapse(Consumer<Consumer<EffectTransform>> apply)
    {
        super();

        this.column().vertical().stretch();

        this.toggle = new UIIcon(Icons.SCALE, (b) -> this.requestToggle());
        this.toggle.tooltip(UIKeys.TIMELINE_TOOLBAR_TRANSFORM);
        this.toggle.wh(20, 20);

        this.shapeIcons = new UIIcons((b) -> apply.accept((effect) ->
            effect.shape = PaintMaskShape.fromId(b.getValue())));
        this.shapeIcons.add(Icons.SQUARE, UIKeys.FORMS_EDITORS_PAINT_SHAPE_BOX);
        this.shapeIcons.add(Icons.CIRCLE, UIKeys.FORMS_EDITORS_PAINT_SHAPE_CIRCLE);
        this.shapeIcons.add(Icons.TRIANGLE, UIKeys.FORMS_EDITORS_PAINT_SHAPE_TRIANGLE);
        this.shapeIcons.h(20);

        this.transform = new UIEffectKeyframeTransform(apply);
        /* Extra bottom room so the last transform row is not scissored by nested
           disclosure shells (Color grade / Extra, model block editor, etc.). */
        this.transform.marginBottom(2);
        this.shell = new UIAnimatedCollapseShell(UI.column(
            UI.label(UIKeys.FORMS_EDITORS_PAINT_SHAPE).marginTop(8),
            this.shapeIcons,
            this.transform
        ));

        this.add(this.toggle);
        this.h(20);
    }

    /**
     * Place the Transform icon at the trailing edge of {@code field}. The field
     * fills all remaining width; the icon stays a fixed 20px.
     */
    public UIEffectTransformCollapse withLeading(UIElement field)
    {
        this.leading = field;
        this.fieldLabel = null;
        this.rebuildHeader();

        return this;
    }

    /**
     * Label above {@code field}, with the Transform icon at the field's trailing edge.
     */
    public UIEffectTransformCollapse withLabeledField(IKey label, UIElement field)
    {
        this.leading = field;
        this.fieldLabel = label;
        this.rebuildHeader();

        return this;
    }

    /**
     * Keep extra widgets (e.g. Lighting) on the same header row. The field+icon
     * cluster stays on the left; extras share the remaining width. The transform
     * grid still opens full-width underneath this control.
     */
    public UIEffectTransformCollapse withHeaderExtras(UIElement... extras)
    {
        this.headerExtras = extras == null ? new UIElement[0] : extras;
        this.rebuildHeader();

        return this;
    }

    /**
     * Color + intensity style: icon at the trailing edge of the color field;
     * value column on the right. Grid opens full-width below.
     * <p>
     * Inner columns use {@code stretch()} so the swatch/cluster receive the
     * column width (plain {@link UI#column} leaves child width at 0).
     */
    public UIEffectTransformCollapse withLabeledColorValue(IKey colorLabel, UIElement colorField, IKey valueLabel, UIElement valueField)
    {
        this.leading = colorField;
        this.fieldLabel = null;
        this.headerExtras = new UIElement[0];

        UIElement colorColumn = this.stretchedColumn(UI.label(colorLabel), this.buildCluster(colorField));
        UIElement valueColumn = this.stretchedColumn(UI.label(valueLabel), valueField);

        this.removeAll();
        this.add(UI.row(colorColumn, valueColumn));
        this.getFlex().h.reset();

        return this;
    }

    public UIEffectTransformCollapse label(IKey label)
    {
        this.toggle.tooltip(label);

        return this;
    }

    /**
     * When false, expanding only updates the icon highlight; the caller must
     * open {@link #getShell()} against a shared host (e.g. Color Grade accordion).
     */
    public UIEffectTransformCollapse manageOwnShell(boolean manage)
    {
        this.manageOwnShell = manage;

        return this;
    }

    /**
     * Replaces the default toggle behavior. Used for exclusive groups where a
     * parent closes siblings before opening this control.
     */
    public UIEffectTransformCollapse onToggle(Consumer<UIEffectTransformCollapse> handler)
    {
        this.toggleHandler = handler;

        return this;
    }

    public UIAnimatedCollapseShell getShell()
    {
        return this.shell;
    }

    public void registerUndo(UIKeyframes editor)
    {
        this.transform.registerUndo(editor);
    }

    public void setEffectTransform(EffectTransform effect)
    {
        EffectTransform value = effect == null ? new EffectTransform() : effect;

        this.shapeIcons.setValue(value.shape == null ? 0 : value.shape.id);
        this.transform.setEffectTransform(value);
    }

    public boolean isExpanded()
    {
        return this.expanded;
    }

    public void setExpanded(boolean expanded)
    {
        if (this.manageOwnShell)
        {
            if (this.expanded == expanded && this.shell.isOpen() == expanded)
            {
                return;
            }
        }
        else if (this.expanded == expanded)
        {
            return;
        }

        this.expanded = expanded;
        this.toggle.active(expanded);

        if (this.manageOwnShell)
        {
            this.shell.setExpanded(expanded, this);
        }
    }

    /**
     * Opens or closes this control's shell under {@code host} (sibling after host).
     * Used when {@link #manageOwnShell(boolean)} is false.
     */
    public void setShellExpanded(boolean expanded, UIElement host)
    {
        this.setShellExpanded(expanded, host, true);
    }

    public void setShellExpanded(boolean expanded, UIElement host, boolean animate)
    {
        this.shell.setExpanded(expanded, host, animate);
    }

    private void requestToggle()
    {
        if (this.toggleHandler != null)
        {
            this.toggleHandler.accept(this);

            return;
        }

        this.setExpanded(!this.expanded);
    }

    private void rebuildHeader()
    {
        this.removeAll();

        UIElement cluster = this.buildCluster(this.leading != null ? this.leading : this.toggle);
        UIElement header = cluster;

        if (this.fieldLabel != null)
        {
            /* Must stretch: otherwise the cluster/field keep width 0 inside the column.
               Taller label + slight text inset keeps Spanish accents inside the
               disclosure shell scissor (Color grade brightness/contrast/etc.). */
            int fontH = Batcher2D.getDefaultTextRenderer().getHeight();
            UILabel label = UI.label(this.fieldLabel, fontH + 3).textOffsetY(2);

            header = this.stretchedColumn(label, cluster);
        }

        if (this.headerExtras.length > 0)
        {
            UIElement[] row = new UIElement[this.headerExtras.length + 1];

            row[0] = header;
            System.arraycopy(this.headerExtras, 0, row, 1, this.headerExtras.length);
            header = UI.row(row);
        }

        this.add(header);
        this.getFlex().h.reset();
    }

    private UIElement stretchedColumn(UIElement... children)
    {
        UIElement column = new UIElement();

        column.column(5).vertical().stretch();
        column.add(children);

        return column;
    }

    /**
     * Field expands to fill the cluster; the Transform icon keeps a fixed 20px
     * on the right and never steals horizontal space from the field.
     */
    private UIElement buildCluster(UIElement field)
    {
        if (field == this.toggle)
        {
            return this.toggle;
        }

        /* Undo any prior compact-width so the swatch/input can fill again. */
        if (field instanceof UIColor)
        {
            field.getFlex().w.reset();
        }

        /* Ensure toggle is not left parented under this collapse from the ctor. */
        this.toggle.removeFromParent();

        UIElement cluster = new UIElement();

        cluster.row(0);
        cluster.h(20);
        cluster.add(field, this.toggle);

        return cluster;
    }

    @Override
    public void render(UIContext context)
    {
        super.render(context);

        if (this.expanded)
        {
            UIDashboardPanels.renderHighlight(context.batcher, this.toggle.area, Direction.BOTTOM);
        }
    }
}
