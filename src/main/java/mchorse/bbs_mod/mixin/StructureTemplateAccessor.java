package mchorse.bbs_mod.mixin;

import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.Vec3i;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StructureTemplate.class)
public interface StructureTemplateAccessor
{
    @Accessor("blockInfoLists")
    List<StructureTemplate.PalettedBlockInfoList> bbs$getBlockInfoLists();

    @Accessor("blockInfoLists")
    @Mutable
    void bbs$setBlockInfoLists(List<StructureTemplate.PalettedBlockInfoList> lists);

    @Accessor("size")
    @Mutable
    void bbs$setSize(Vec3i size);
}
