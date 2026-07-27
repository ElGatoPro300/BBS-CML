package mchorse.bbs_mod.mixin;

import net.minecraft.class_1940;
import net.minecraft.class_31;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(class_31.class)
public interface LevelPropertiesAccessor
{
    @Accessor("levelInfo")
    public void bbs$setLevelInfo(class_1940 info);
}