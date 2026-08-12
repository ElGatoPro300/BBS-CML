package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.utils.context.ColorfulContextAction;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.keys.KeyCombo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * A single entry inside a toolbar popup menu. Can be:
 * <ul>
 *     <li>A leaf action (label + optional icon + optional keycombo + runnable)</li>
 *     <li>A submenu (label + optional icon + children list)</li>
 *     <li>A separator (obtained via {@link #separator()})</li>
 * </ul>
 * Phase 1 does not require a {@code runnable} to be set; the item still renders
 * and shows its shortcut, but clicking it is a no-op.
 */
public class ToolbarItem
{
    /* Static factories */

    public static ToolbarItem separator()
    {
        ToolbarItem item = new ToolbarItem(null);
        item.separator = true;

        return item;
    }

    public static ToolbarItem action(IKey label)
    {
        return new ToolbarItem(label);
    }

    public static ToolbarItem submenu(IKey label, ToolbarItem... items)
    {
        ToolbarItem item = new ToolbarItem(label);

        for (ToolbarItem child : items)
        {
            item.children.add(child);
        }

        return item;
    }

    /* Fields */

    /**
     * If {@code true}, this row is a separator (all other fields ignored).
     */
    public boolean separator;

    /**
     * Localized label displayed in the row.
     */
    public IKey label;

    /**
     * Optional leading icon.
     */
    public Icon icon;

    /**
     * Optional shortcut combination whose {@link KeyCombo#getKeyCombo()} text
     * is drawn right-aligned in the row.
     */
    public KeyCombo keyCombo;

    /**
     * If {@code true}, draws a vertical red bar on the left of the row (same
     * visual cue used by the existing remove/trash icons in context menus).
     */
    public boolean destructive;

    /**
     * When non-zero, draws a vertical color bar on the left of the row (same
     * visual as {@link ColorfulContextAction}).
     */
    public int accentColor;

    /**
     * When present, this row expands into a submenu on hover / click.
     */
    public final List<ToolbarItem> children = new ArrayList<>();

    /**
     * Runnable executed on click when this row is enabled. In Phase 1 this is
     * typically {@code null} — rows still render, but clicks do nothing.
     */
    public Runnable runnable;

    /**
     * If present and returns {@code false}, the row is drawn in the disabled
     * style. When {@code null} the row is always enabled.
     */
    public BooleanSupplier enabled;

    /**
     * When this row is disabled by an external, "critical" cause (e.g. the
     * viewport panel is hidden), returning a non-null key here makes the
     * disabled tooltip render in red with the given reason.
     */
    public Supplier<IKey> disabledReason;

    /* Constructor */

    private ToolbarItem(IKey label)
    {
        this.label = label;
    }

    /* Builder-style setters */

    public ToolbarItem icon(Icon icon)
    {
        this.icon = icon;

        return this;
    }

    public ToolbarItem shortcut(KeyCombo combo)
    {
        this.keyCombo = combo;

        return this;
    }

    public ToolbarItem destructive()
    {
        this.destructive = true;

        return this;
    }

    public ToolbarItem accentColor(int color)
    {
        this.accentColor = color & 0xffffff;

        return this;
    }

    public ToolbarItem run(Runnable runnable)
    {
        this.runnable = runnable;

        return this;
    }

    public ToolbarItem enabledIf(BooleanSupplier supplier)
    {
        this.enabled = supplier;

        return this;
    }

    public ToolbarItem disabledReason(Supplier<IKey> reason)
    {
        this.disabledReason = reason;

        return this;
    }

    public ToolbarItem child(ToolbarItem child)
    {
        this.children.add(child);

        return this;
    }

    /* Queries */

    public boolean hasChildren()
    {
        return !this.children.isEmpty();
    }

    /**
     * Submenu rows that also trigger a primary action on click (wired
     * {@link #runnable} in Phase 2, or {@link #keyCombo} as the default
     * shortcut shown on the parent row).
     */
    public boolean hasDefaultAction()
    {
        return this.runnable != null || this.keyCombo != null;
    }

    /**
     * Submenu row that only groups child actions (hover opens the submenu)
     * without a default shortcut/action on the parent row itself.
     */
    public boolean isPureSubmenuContainer()
    {
        return this.hasChildren() && !this.hasDefaultAction();
    }

    public boolean isEnabled()
    {
        return this.enabled == null || this.enabled.getAsBoolean();
    }

    public IKey getDisabledReason()
    {
        if (this.disabledReason == null)
        {
            return null;
        }

        return this.disabledReason.get();
    }
}
