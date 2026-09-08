package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.BBSModClient;

import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.server.packs.resources.ResourceManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LanguageManager.class)
public class LanguageManagerMixin
{
    @Inject(method = "onResourceManagerReload", at = @At("TAIL"))
    public void onReload(ResourceManager resourceManager, CallbackInfo info)
    {
        BBSModClient.reloadLanguage(BBSModClient.getLanguageKey());
    }
}