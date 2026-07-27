package mchorse.bbs_mod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import net.minecraft.class_3499;

@Mixin(class_3499.class_5162.class)
public interface StructureTemplatePalettedListAccessor
{
    @Accessor("infos")
    List<class_3499.class_3501> bbs$getInfos();
}
