package mchorse.bbs_mod.mixin;

import net.minecraft.core.Vec3i;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(StructureTemplate.class)
public interface StructureTemplateAccessor
{
    @Accessor("palettes")
    List<StructureTemplate.Palette> bbs$getBlockInfoLists();

    @Accessor("palettes")
    @Mutable
    void bbs$setBlockInfoLists(List<StructureTemplate.Palette> lists);

    @Accessor("size")
    @Mutable
    void bbs$setSize(Vec3i size);
}
