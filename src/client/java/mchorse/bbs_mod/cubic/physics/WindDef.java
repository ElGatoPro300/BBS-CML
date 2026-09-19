package mchorse.bbs_mod.cubic.physics;

/**
 * Global wind for every spring chain: direction ({@code dirX}/{@code dirY}/{@code dirZ})
 * scaled by {@code power}. Gust fields modulate magnitude over space and film time.
 * {@code modelRelative} rotates the direction by the model world orientation.
 */
public record WindDef(float power, float dirX, float dirY, float dirZ, float gustiness, float gustSpeed, float gustScale, boolean modelRelative)
{
    public static final float DEFAULT_GUSTINESS = 0.5F;
    public static final float DEFAULT_GUST_SPEED = 1F;
    public static final float DEFAULT_GUST_SCALE = 1F;

    public static final WindDef NONE = new WindDef(0F, 1F, 0F, 0F, DEFAULT_GUSTINESS, DEFAULT_GUST_SPEED, DEFAULT_GUST_SCALE, false);

    public WindDef
    {
        power = Math.max(0F, power);
        gustiness = gustiness < 0F ? 0F : Math.min(gustiness, 1F);
        gustSpeed = Math.max(0F, gustSpeed);
        gustScale = Math.max(0F, gustScale);
    }

    public boolean active()
    {
        return this.power > 0F && (this.dirX != 0F || this.dirY != 0F || this.dirZ != 0F);
    }

    public boolean isDefault()
    {
        return this.equals(NONE);
    }
}
