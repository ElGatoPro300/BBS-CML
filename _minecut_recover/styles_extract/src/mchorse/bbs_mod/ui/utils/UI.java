package mchorse.bbs_mod.ui.utils;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.utils.colors.Colors;

public class UI
{
    public static UIElement row(UIElement... elements)
    {
        return row(5, elements);
    }

    public static UIElement row(int margin, UIElement... elements)
    {
        return row(margin, 0, elements);
    }

    public static UIElement row(int margin, int padding, UIElement... elements)
    {
        return row(margin, padding, 0, elements);
    }

    public static UIElement row(int margin, int padding, int height, UIElement... elements)
    {
        UIElement element = new UIElement();

        element.row(margin).padding(padding).height(height);
        element.add(elements);

        return element;
    }

    public static UIElement column(UIElement... elements)
    {
        return column(5, elements);
    }

    public static UIElement column(int margin, UIElement... elements)
    {
        return column(margin, 0, elements);
    }

    public static UIElement column(int margin, int padding, UIElement... elements)
    {
        return column(margin, padding, 0, elements);
    }

    public static UIElement column(int margin, int padding, int height, UIElement... elements)
    {
        UIElement element = new UIElement();

        element.column(margin).vertical().stretch().padding(padding).height(height);
        element.add(elements);

        return element;
    }

    public static UILabel label(IKey label)
    {
        return label(label, Batcher2D.getDefaultTextRenderer().getHeight());
    }

    public static UILabel label(IKey label, int height)
    {
        return label(label, height, Colors.WHITE);
    }

    public static UILabel label(IKey label, int height, int color)
    {
        UILabel element = new UILabel(label, color);

        element.h(height);

        return element;
    }

    public static UIScrollView scrollView(UIElement... elements)
    {
        return scrollView(5, elements);
    }

    public static UIScrollView scrollView(int margin, UIElement... elements)
    {
        return scrollView(margin, 0, elements);
    }

    public static UIScrollView scrollView(int margin, int padding, UIElement... elements)
    {
        return scrollView(margin, padding, 0, elements);
    }

    public static UIScrollView scrollView(int margin, int padding, int width, UIElement... elements)
    {
        UIScrollView scrollView = new UIScrollView();

        scrollView.column(margin).vertical().stretch().scroll().width(width).padding(padding);
        scrollView.add(elements);

        return scrollView;
    }
}