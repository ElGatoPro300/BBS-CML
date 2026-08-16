package mchorse.bbs_mod.particles.components.appearance;

/**
 * Source of the direction vector used by the {@code direction_x/y/z} camera facing modes.
 */
public enum BillboardDirection
{
    DERIVE_FROM_VELOCITY("derive_from_velocity"), CUSTOM_DIRECTION("custom_direction");

    public final String id;

    public static BillboardDirection fromString(String string)
    {
        for (BillboardDirection direction : values())
        {
            if (direction.id.equals(string))
            {
                return direction;
            }
        }

        return DERIVE_FROM_VELOCITY;
    }

    private BillboardDirection(String id)
    {
        this.id = id;
    }
}
