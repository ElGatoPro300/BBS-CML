package mchorse.bbs_mod.ui.framework.elements.input;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.Scroll;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.interps.Lerps;

import java.util.ArrayList;
import java.util.List;

/**
 * Animated height clip for disclosure bodies (Transform / Color grade).
 * <p>
 * Animation is ticked in {@link #tickAll()} before the UI tree renders so
 * parent layout is never resized mid-draw (that caused the close jump).
 * Scroll offset is compensated while collapsing so the panel does not snap.
 * <p>
 * Nested shells (Transform inside Color grade) must not call {@code resize()}
 * on an ancestor shell alone — that re-enters {@code ColumnResizer} with a
 * stale cursor and teleports/clips the outer body. Instead the outer shell
 * tracks live content height each frame while a nested shell is active.
 */
public class UIAnimatedCollapseShell extends UIElement
{
    private static final long DURATION_NS = 260_000_000L;
    private static final List<UIAnimatedCollapseShell> ACTIVE = new ArrayList<>();

    private final UIElement content;
    private boolean open;
    private boolean animating;
    private float progress;
    private float from;
    private float to;
    private long animStartNs;
    private int lastAppliedHeight = -1;
    private int naturalHeight;
    private UIElement scrollAnchor;

    public UIAnimatedCollapseShell(UIElement content)
    {
        super();

        this.content = content;
        this.content.relative(this).x(0).y(0).w(1F);
        this.add(this.content);
        this.h(0);
        this.culled = false;
    }

    /**
     * Run before {@code root.render} so height changes settle before draw.
     */
    public static void tickAll()
    {
        if (ACTIVE.isEmpty())
        {
            return;
        }

        List<UIAnimatedCollapseShell> snapshot = new ArrayList<>(ACTIVE);

        /* Nested Transform shells must tick before Color grade so the outer
         * clip follows the same-frame animated height. */
        snapshot.sort((a, b) -> Integer.compare(b.nestingDepth(), a.nestingDepth()));

        for (UIAnimatedCollapseShell shell : snapshot)
        {
            shell.tickAnimation();
        }
    }

    private int nestingDepth()
    {
        int depth = 0;
        UIElement current = this.getParent();

        while (current != null)
        {
            if (current instanceof UIAnimatedCollapseShell)
            {
                depth += 1;
            }

            current = current.getParent();
        }

        return depth;
    }

    public UIElement getContent()
    {
        return this.content;
    }

    public boolean isOpen()
    {
        return this.open;
    }

    public boolean isAnimating()
    {
        return this.animating;
    }

    /**
     * @param host toggle element; this shell is inserted as the next sibling under host's parent
     */
    public void setExpanded(boolean expanded, UIElement host)
    {
        this.scrollAnchor = host;

        if (expanded)
        {
            this.attachAfter(host);

            /* Host not parented yet — do not claim open (avoids ▼ with empty body). */
            if (!this.hasParent())
            {
                return;
            }
        }

        if (this.open == expanded && !this.animating && this.hasParent() == expanded)
        {
            return;
        }

        this.open = expanded;
        this.from = this.progress;
        this.to = expanded ? 1F : 0F;
        this.animStartNs = System.nanoTime();
        this.animating = true;
        this.registerActive();
        this.registerAncestorShells();

        if (expanded)
        {
            this.naturalHeight = this.measureNaturalHeightQuiet();
            this.applyHeight(true);
        }
        else if (this.naturalHeight <= 0)
        {
            this.naturalHeight = Math.max(1, Math.max(this.area.h, this.getFlex().h.offset));
        }
    }

    private void registerActive()
    {
        if (!ACTIVE.contains(this))
        {
            ACTIVE.add(this);
        }
    }

    private void unregisterActive()
    {
        ACTIVE.remove(this);
    }

    private void registerAncestorShells()
    {
        UIElement current = this.getParent();

        while (current != null)
        {
            if (current instanceof UIAnimatedCollapseShell shell && shell != this)
            {
                shell.registerActive();
            }

            current = current.getParent();
        }
    }

    private void attachAfter(UIElement host)
    {
        UIElement parent = host == null ? null : host.getParent();

        if (parent == null)
        {
            return;
        }

        if (!this.hasParent())
        {
            parent.addAfter(host, this);
        }
        else if (this.getParent() != parent)
        {
            this.removeFromParent();
            parent.addAfter(host, this);
        }

        this.resizeWithScrollCompensation(0);
    }

    private void detachIfClosed()
    {
        if (this.open || this.progress > 0.001F)
        {
            return;
        }

        this.progress = 0F;
        this.animating = false;
        this.h(0);
        this.lastAppliedHeight = 0;
        this.scrollAnchor = null;
        this.unregisterActive();

        if (this.hasParent())
        {
            UIElement parent = this.getParent();

            this.removeFromParent();
            this.resizeLayoutRoot(parent);
            this.syncAncestorShellsToContent();
            this.resizeLayoutRoot(parent);
        }
    }

    private void tickAnimation()
    {
        if (!this.hasParent() && !this.animating)
        {
            this.unregisterActive();

            return;
        }

        if (this.animating)
        {
            float t = (System.nanoTime() - this.animStartNs) / (float) DURATION_NS;

            if (t >= 1F)
            {
                this.progress = this.to;
                this.animating = false;
            }
            else
            {
                float clamped = MathUtils.clamp(t, 0F, 1F);
                float eased = this.to < this.from
                    ? Interpolations.CUBIC_IN.interpolate(0F, 1F, clamped)
                    : Interpolations.CUBIC_OUT.interpolate(0F, 1F, clamped);

                this.progress = Lerps.lerp(this.from, this.to, eased);
            }
        }

        /* Outer shells follow nested Transform height both up and down. */
        if (this.open && !this.animating)
        {
            this.followLiveContentHeight();
        }

        this.applyHeight(false);

        if (!this.open && !this.animating)
        {
            this.detachIfClosed();
        }
        else if (this.animating || this.hasNestedActiveShell())
        {
            this.registerActive();
        }
        else if (this.open)
        {
            this.unregisterActive();
        }
        else
        {
            this.registerActive();
        }
    }

    private boolean hasNestedActiveShell()
    {
        for (UIAnimatedCollapseShell shell : ACTIVE)
        {
            if (shell != this && this.isAncestorOf(shell))
            {
                return true;
            }
        }

        return false;
    }

    private boolean isAncestorOf(UIAnimatedCollapseShell other)
    {
        UIElement current = other.getParent();

        while (current != null)
        {
            if (current == this)
            {
                return true;
            }

            current = current.getParent();
        }

        return false;
    }

    /**
     * Match clip height to current column content (grow or shrink).
     */
    private void followLiveContentHeight()
    {
        int live = this.measureLiveContentHeight();

        if (live > 0 && Math.abs(live - this.naturalHeight) > 1)
        {
            this.naturalHeight = live;
        }
    }

    private int measureLiveContentHeight()
    {
        int flexH = this.content.getFlex().getH();

        if (flexH > 0)
        {
            return flexH;
        }

        return Math.max(this.content.area.h, 0);
    }

    private void syncAncestorShellsToContent()
    {
        UIElement current = this.getParent();

        while (current != null)
        {
            if (current instanceof UIAnimatedCollapseShell shell && shell != this)
            {
                if (shell.open && !shell.animating)
                {
                    shell.followLiveContentHeight();
                    shell.applyHeight(true);
                }

                shell.registerActive();
            }

            current = current.getParent();
        }
    }

    private int measureNaturalHeightQuiet()
    {
        int flexH = this.content.getFlex().getH();

        if (flexH > 0)
        {
            return flexH;
        }

        if (this.content.area.h > 0)
        {
            return this.content.area.h;
        }

        int shellW = this.area.w > 0 ? this.area.w : (this.getParent() == null ? 200 : Math.max(1, this.getParent().area.w));
        int savedX = this.area.x;
        int savedY = this.area.y;
        int savedW = this.area.w;
        int savedH = this.area.h;

        this.area.set(savedX, savedY, Math.max(shellW, 1), 4096);
        this.content.resize();

        int measured = Math.max(this.content.getFlex().getH(), this.content.area.h);

        this.area.set(savedX, savedY, savedW, savedH);

        return Math.max(measured, 1);
    }

    private void applyHeight(boolean forceResize)
    {
        int natural = Math.max(this.naturalHeight, 0);
        int height = Math.round(natural * MathUtils.clamp(this.progress, 0F, 1F));

        if (this.animating && this.open && height <= 0 && natural > 0)
        {
            height = 1;
        }

        if (!forceResize && height == this.lastAppliedHeight && this.getFlex().h.offset == height)
        {
            return;
        }

        int previousHeight = this.lastAppliedHeight < 0 ? height : this.lastAppliedHeight;

        this.lastAppliedHeight = height;
        this.h(height);
        this.resizeWithScrollCompensation(previousHeight - height);
    }

    /**
     * Keep the disclosure header visually fixed while the body grows/shrinks.
     * Previous shrink-scroll compensation pulled the whole panel upward on close.
     */
    private void resizeWithScrollCompensation(int shrinkPx)
    {
        UIElement parent = this.getParent();

        if (parent == null)
        {
            return;
        }

        UIScrollView scrollView = this.findScrollView();
        Scroll scroll = scrollView == null ? null : scrollView.scroll;
        double scrollBefore = scroll == null ? 0D : scroll.getScroll();
        UIElement anchor = this.resolveScrollAnchor();
        int anchorYBefore = anchor == null ? Integer.MIN_VALUE : anchor.area.y;

        /* Resize the column that owns this shell (or scroll content), never an
         * ancestor shell alone — isolated shell.resize() corrupts ColumnResizer. */
        this.resizeLayoutRoot(parent);
        this.syncAncestorShellsToContent();
        this.resizeLayoutRoot(parent);

        if (scroll == null)
        {
            return;
        }

        if (anchor != null && anchorYBefore != Integer.MIN_VALUE)
        {
            /* visualY ~= layoutY - scroll — keep the header's visualY stable. */
            int dy = anchor.area.y - anchorYBefore;

            scroll.setScroll(scrollBefore + dy);
        }
        else
        {
            scroll.setScroll(scrollBefore);
        }

        scroll.clamp();
    }

    private UIElement resolveScrollAnchor()
    {
        if (this.scrollAnchor != null && this.scrollAnchor.hasParent())
        {
            return this.scrollAnchor;
        }

        UIElement parent = this.getParent();

        if (parent == null)
        {
            return null;
        }

        List<IUIElement> children = parent.getChildren();
        int index = children.indexOf(this);

        if (index > 0 && children.get(index - 1) instanceof UIElement previous)
        {
            return previous;
        }

        return null;
    }

    /**
     * Walk up past nested shell content to the first layout parent that is not
     * itself an animated shell (usually the options column / scroll content).
     */
    private void resizeLayoutRoot(UIElement from)
    {
        UIElement root = from;

        while (root != null)
        {
            UIElement parent = root.getParent();

            if (parent instanceof UIAnimatedCollapseShell)
            {
                root = parent.getParent();

                continue;
            }

            break;
        }

        if (root == null)
        {
            root = from;
        }

        this.resizeElement(root);

        UIScrollView scrollView = this.findScrollViewFrom(root);

        if (scrollView != null && scrollView != root)
        {
            this.resizeElement(scrollView);
        }
    }

    private UIScrollView findScrollView()
    {
        return this.findScrollViewFrom(this.getParent());
    }

    private UIScrollView findScrollViewFrom(UIElement start)
    {
        UIElement current = start;

        while (current != null)
        {
            if (current instanceof UIScrollView)
            {
                return (UIScrollView) current;
            }

            current = current.getParent();
        }

        return null;
    }

    private void resizeElement(UIElement element)
    {
        if (element != null)
        {
            element.resize();
        }
    }

    @Override
    public boolean canBeRendered(Area viewport)
    {
        if (this.animating || this.open)
        {
            return true;
        }

        return super.canBeRendered(viewport);
    }

    @Override
    public void render(UIContext context)
    {
        if (this.area.h <= 0)
        {
            return;
        }

        context.batcher.clip(this.area, context);
        super.render(context);
        context.batcher.unclip(context);
    }

    @Override
    protected IUIElement childrenMouseClicked(UIContext context)
    {
        if (!this.area.isInside(context))
        {
            return null;
        }

        return super.childrenMouseClicked(context);
    }

    @Override
    protected IUIElement childrenMouseScrolled(UIContext context)
    {
        if (!this.area.isInside(context))
        {
            return null;
        }

        return super.childrenMouseScrolled(context);
    }
}
