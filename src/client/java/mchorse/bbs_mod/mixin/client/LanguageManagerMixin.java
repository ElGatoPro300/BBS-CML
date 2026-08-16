package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.BBSModClient;
import net.minecraft.client.resource.language.LanguageManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LanguageManager.class)
public class LanguageManagerMixin
{
    @Inject(method = "reload", at = @At("TAIL"))
    public void onReload(CallbackInfo info)
    {
        BBSModClient.reloadLanguage(BBSModClient.getLanguageKey());
    }
}