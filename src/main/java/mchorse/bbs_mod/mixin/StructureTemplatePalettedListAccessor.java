package mchorse.bbs_mod.mixin;

import net.minecraft.structure.StructureTemplate;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StructureTemplate.PalettedBlockInfoList.class)
public interface StructureTemplatePalettedListAccessor
{
    @Accessor("infos")
    List<StructureTemplate.StructureBlockInfo> bbs$getInfos();
}
