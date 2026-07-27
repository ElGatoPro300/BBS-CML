package mchorse.bbs_mod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import net.minecraft.class_2382;
import net.minecraft.class_3499;

@Mixin(class_3499.class)
public interface StructureTemplateAccessor
{
    @Accessor("blockInfoLists")
    List<class_3499.class_5162> bbs$getBlockInfoLists();

    @Accessor("blockInfoLists")
    @Mutable
    void bbs$setBlockInfoLists(List<class_3499.class_5162> lists);

    @Accessor("size")
    @Mutable
    void bbs$setSize(class_2382 size);
}
