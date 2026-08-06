package mchorse.bbs_mod.forms.forms.utils;

/**
 * Spatial paint mask silhouette in effect-local space (BOX remains the default).
 */
public enum PaintMaskShape
{
    BOX(0),
    CIRCLE(1),
    TRIANGLE(2);

    public final int id;

    PaintMaskShape(int id)
    {
        this.id = id;
    }

    public static PaintMaskShape fromId(int id)
    {
        if (id == CIRCLE.id)
        {
            return CIRCLE;
        }

        if (id == TRIANGLE.id)
        {
            return TRIANGLE;
        }

        return BOX;
    }

    public static PaintMaskShape fromName(String name)
    {
        if (name == null || name.isEmpty())
        {
            return BOX;
        }

        try
        {
            return PaintMaskShape.valueOf(name.toUpperCase());
        }
        catch (IllegalArgumentException ignored)
        {
            return BOX;
        }
    }
}
