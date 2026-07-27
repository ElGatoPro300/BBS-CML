package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.BBSModClient;
import net.minecraft.class_1076;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_1076.class)
public class LanguageManagerMixin
{
    @Inject(method = "reload", at = @At("TAIL"))
    public void onReload(CallbackInfo info)
    {
        BBSModClient.reloadLanguage(BBSModClient.getLanguageKey());
    }
}