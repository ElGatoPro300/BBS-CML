package mchorse.bbs_mod.mixin;

import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(StructureTemplate.Palette.class)
public interface StructureTemplatePalettedListAccessor
{
    @Accessor("blocks")
    List<StructureTemplate.StructureBlockInfo> bbs$getInfos();
}
