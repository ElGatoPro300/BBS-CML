package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.morphing.IMorphProvider;
import mchorse.bbs_mod.morphing.Morph;
import net.minecraft.class_1657;
import net.minecraft.class_2487;
import net.minecraft.class_4048;
import net.minecraft.class_4050;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * For some unknown reason to me, if these methods are used in {@link PlayerEntityMorphMixin}
 * then the world will be locked for some reason... by extracting write/read NBT method to
 * a separate mixin fixes it...
 */
@Mixin(class_1657.class)
public class PlayerEntityMixin
{
    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    public void onWriteCustomDataToNbt(class_2487 nbt, CallbackInfo info)
    {
        if (this instanceof IMorphProvider provider)
        {
            nbt.method_10566("BBSMorph", provider.getMorph().toNbt());
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    public void onReadCustomDataFromNbt(class_2487 nbt, CallbackInfo info)
    {
        if (this instanceof IMorphProvider provider)
        {
            if (nbt.method_10545("BBSMorph"))
            {
                provider.getMorph().fromNbt(nbt.method_10562("BBSMorph"));
            }
        }
    }

    @Inject(method = "getBaseDimensions", at = @At("RETURN"), cancellable = true)
    public void onGetDimensions(class_4050 pose, CallbackInfoReturnable<class_4048> info)
    {
        if (this instanceof IMorphProvider provider)
        {
            Form form = provider.getMorph().getForm();

            if (form != null && form.hitbox.get())
            {
                class_1657 player = (class_1657) (Object) this;
                class_4048 dimensions = info.getReturnValue();
                float height = form.hitboxHeight.get() * (player.method_5715() ? form.hitboxSneakMultiplier.get() : 1F);

                if (dimensions.comp_2189())
                {
                    info.setReturnValue(class_4048.method_18385(form.hitboxWidth.get(), height));
                }
                else
                {
                    info.setReturnValue(class_4048.method_18384(form.hitboxWidth.get(), height));
                }
            }
        }
    }

}