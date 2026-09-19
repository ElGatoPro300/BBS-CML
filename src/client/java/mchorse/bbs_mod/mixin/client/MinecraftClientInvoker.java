package mchorse.bbs_mod.mixin.client;

import net.minecraft.client.MinecraftClient;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MinecraftClient.class)
public interface MinecraftClientInvoker
{
    @Invoker("doAttack")
    boolean bbs$invokeDoAttack();

    @Invoker("doItemUse")
    void bbs$invokeDoItemUse();
}
