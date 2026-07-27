package mchorse.bbs_mod.mixin.client;

import net.minecraft.class_1297;
import net.minecraft.class_4048;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(class_1297.class)
public interface EntityAccessor
{
    @Accessor("dimensions")
    void bbs$setDimensions(class_4048 dimensions);
}
