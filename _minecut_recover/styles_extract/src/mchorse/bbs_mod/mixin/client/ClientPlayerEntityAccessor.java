package mchorse.bbs_mod.mixin.client;

import net.minecraft.class_746;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(class_746.class)
public interface ClientPlayerEntityAccessor
{
    @Accessor("inSneakingPose")
    public void bbs$setIsSneakingPose(boolean sneaking);
}