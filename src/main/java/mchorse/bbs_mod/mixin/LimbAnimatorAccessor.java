package mchorse.bbs_mod.mixin;

import net.minecraft.world.entity.WalkAnimationState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WalkAnimationState.class)
public interface LimbAnimatorAccessor
{
    @Accessor("speedOld")
    public float getPrevSpeed();

    @Accessor("speedOld")
    public void setPrevSpeed(float v);

    @Accessor("speed")
    public float getSpeed();

    @Accessor("speed")
    public void setSpeed(float v);

    @Accessor("position")
    public float getPos();

    @Accessor("position")
    public void setPos(float v);
}