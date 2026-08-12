package mchorse.bbs_mod.mixin;

import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.PrimaryLevelData;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PrimaryLevelData.class)
public interface LevelPropertiesAccessor
{
    @Accessor("settings")
    public void bbs$setLevelInfo(LevelSettings info);
}