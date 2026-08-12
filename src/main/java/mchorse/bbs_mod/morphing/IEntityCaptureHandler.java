package mchorse.bbs_mod.morphing;

import mchorse.bbs_mod.forms.forms.Form;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public interface IEntityCaptureHandler
{
    public Form capture(Player player, Entity target);
}
