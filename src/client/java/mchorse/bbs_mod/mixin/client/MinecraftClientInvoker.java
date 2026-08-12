package mchorse.bbs_mod.mixin.client;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface MinecraftClientInvoker
{
    @Invoker("startAttack")
    boolean bbs$invokeDoAttack();

    @Invoker("startUseItem")
    void bbs$invokeDoItemUse();
}
