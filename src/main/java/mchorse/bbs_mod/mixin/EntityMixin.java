package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.morphing.IMorphProvider;
import mchorse.bbs_mod.morphing.Morph;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin
{
    @Inject(method = "getEyeHeight", at = @At("HEAD"), cancellable = true)
    public void getEyeHeight(EntityPose pose, CallbackInfoReturnable<Float> info)
    {
        if (this instanceof IMorphProvider provider)
        {
            Morph morph = provider.getMorph();

            if (morph != null)
            {
                Form form = morph.getForm();

                if (form != null && form.hitbox.get())
                {
                    PlayerEntity player = (PlayerEntity) (Object) this;
                    float height = form.hitboxHeight.get() * (player.isSneaking() ? form.hitboxSneakMultiplier.get() : 1F);

                    info.setReturnValue(form.hitboxEyeHeight.get() * height);
                }
            }
        }
    }
}