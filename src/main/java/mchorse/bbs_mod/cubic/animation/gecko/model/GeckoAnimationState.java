package mchorse.bbs_mod.cubic.animation.gecko.model;

public enum GeckoAnimationState
{
    IDLE("idle"),
    WALK("walk"),
    RUN("run"),
    JUMP("jump"),
    FALL("fall"),
    ATTACK("attack"),
    SWIM("swim"),
    FLY("fly"),
    WHEEL("wheel");

    public final String id;

    GeckoAnimationState(String id)
    {
        this.id = id;
    }
}
