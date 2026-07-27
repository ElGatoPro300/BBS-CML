package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.StructurePickerClient;
import mchorse.bbs_mod.entity.ActorEntity;
import mchorse.bbs_mod.entity.IEntityFormProvider;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.morphing.IMorphProvider;
import mchorse.bbs_mod.morphing.Morph;
import net.minecraft.class_1297;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * While Structure Picker is held, ignore film-pawn / form hitboxes so block clicks
 * (especially Model Blocks) are not stolen by entities in front of them.
 */
@Mixin(class_1297.class)
public class StructurePickerEntityHitMixin
{
    @Inject(method = "canHit", at = @At("HEAD"), cancellable = true)
    private void bbs$structurePickerIgnoreFormHitbox(CallbackInfoReturnable<Boolean> info)
    {
        if (!StructurePickerClient.isActive() || !this.bbs$hasFormHitbox())
        {
            return;
        }

        info.setReturnValue(false);
    }

    @Inject(method = "isCollidable", at = @At("HEAD"), cancellable = true)
    private void bbs$structurePickerDisableFormCollidable(CallbackInfoReturnable<Boolean> info)
    {
        if (!StructurePickerClient.isActive() || !this.bbs$hasFormHitbox())
        {
            return;
        }

        info.setReturnValue(false);
    }

    private boolean bbs$hasFormHitbox()
    {
        class_1297 self = (class_1297) (Object) this;

        if (self instanceof ActorEntity)
        {
            return true;
        }

        if (self instanceof IEntityFormProvider provider)
        {
            Form form = provider.getForm();

            return form != null && form.hitbox.get();
        }

        if (self instanceof IMorphProvider provider)
        {
            Morph morph = provider.getMorph();

            if (morph != null)
            {
                Form form = morph.getForm();

                return form != null && form.hitbox.get();
            }
        }

        return false;
    }
}
