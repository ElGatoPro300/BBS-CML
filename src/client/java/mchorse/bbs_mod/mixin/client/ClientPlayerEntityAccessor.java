package mchorse.bbs_mod.mixin.client;

import net.minecraft.client.player.LocalPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LocalPlayer.class)
public interface ClientPlayerEntityAccessor
{
    @Accessor("crouching")
    public void bbs$setIsSneakingPose(boolean sneaking);
}