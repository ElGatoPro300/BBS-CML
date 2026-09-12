package mchorse.bbs_mod.mixin;

import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StructureTemplate.Palette.class)
public interface StructureTemplatePalettedListAccessor
{
    @Accessor("blocks")
    List<StructureTemplate.StructureBlockInfo> bbs$getInfos();
}
