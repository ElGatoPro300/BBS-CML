package mchorse.bbs_mod.mixin.client;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPlayerEntity.class)
public interface ClientPlayerEntityAccessor
{
    @Accessor("inSneakingPose")
    public void bbs$setIsSneakingPose(boolean sneaking);
}