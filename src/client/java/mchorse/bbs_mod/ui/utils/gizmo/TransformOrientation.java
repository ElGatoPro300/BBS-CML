package mchorse.bbs_mod.ui.utils.gizmo;

public enum TransformOrientation
{
    PARENT,
    LOCAL,
    GLOBAL,
    VIEW;

    public boolean isLocal()
    {
        return this == LOCAL;
    }

    public boolean usesLocalBoneBasis()
    {
        return this == LOCAL;
    }

    public TransformOrientation next()
    {
        TransformOrientation[] values = values();

        return values[(this.ordinal() + 1) % values.length];
    }
}
