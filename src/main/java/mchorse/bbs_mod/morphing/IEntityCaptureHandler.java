package mchorse.bbs_mod.morphing;

import mchorse.bbs_mod.forms.forms.Form;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public interface IEntityCaptureHandler
{
    public Form capture(PlayerEntity player, Entity target);
}
