package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.entity.IEntityFormProvider;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.structure.ModelBlockSolidCollisions;
import mchorse.bbs_mod.morphing.IMorphProvider;
import mchorse.bbs_mod.morphing.Morph;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_1937;
import net.minecraft.class_238;
import net.minecraft.class_265;
import net.minecraft.class_4050;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(class_1297.class)
public class EntityMixin
{
    @Inject(method = "getEyeHeight", at = @At("HEAD"), cancellable = true)
    public void getEyeHeight(class_4050 pose, CallbackInfoReturnable<Float> info)
    {
        if (this instanceof IMorphProvider provider)
        {
            Morph morph = provider.getMorph();

            if (morph != null)
            {
                Form form = morph.getForm();

                if (form != null && form.hitbox.get())
                {
                    class_1657 player = (class_1657) (Object) this;
                    float height = form.hitboxHeight.get() * (player.method_5715() ? form.hitboxSneakMultiplier.get() : 1F);

                    info.setReturnValue(form.hitboxEyeHeight.get() * height);
                }
            }
        }
        else if (this instanceof IEntityFormProvider provider)
        {
            Form form = provider.getForm();

            if (form != null && form.hitbox.get())
            {
                class_1297 entity = (class_1297) (Object) this;
                float height = form.hitboxHeight.get() * (entity.method_5715() ? form.hitboxSneakMultiplier.get() : 1F);

                info.setReturnValue(form.hitboxEyeHeight.get() * height);
            }
        }
    }

    @Inject(method = "isCollidable", at = @At("HEAD"), cancellable = true)
    public void onIsCollidable(CallbackInfoReturnable<Boolean> info)
    {
        if ((Object) this instanceof IMorphProvider provider)
        {
            Form form = provider.getMorph().getForm();

            if (form != null && form.hitbox.get())
            {
                info.setReturnValue(true);
            }
        }
        else if ((Object) this instanceof IEntityFormProvider provider)
        {
            Form form = provider.getForm();

            if (form != null && form.hitbox.get())
            {
                info.setReturnValue(true);
            }
        }
    }

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    public void onIsPushable(CallbackInfoReturnable<Boolean> info)
    {
        if ((Object) this instanceof IMorphProvider provider)
        {
            Form form = provider.getMorph().getForm();

            if (form != null && form.hitbox.get())
            {
                info.setReturnValue(false);
            }
        }
        else if ((Object) this instanceof IEntityFormProvider provider)
        {
            Form form = provider.getForm();

            if (form != null && form.hitbox.get())
            {
                info.setReturnValue(false);
            }
        }
    }

    /**
     * Inject solid model/structure hitboxes into every movement collision list,
     * including the step-up pass ({@code list2}) which previously only saw block shapes.
     */
    @Inject(method = "findCollisionsForMovement", at = @At("RETURN"), cancellable = true)
    private static void bbs$appendSolidHitboxes(
        @Nullable class_1297 entity,
        class_1937 world,
        List<class_265> regularCollisions,
        class_238 movingEntityBoundingBox,
        CallbackInfoReturnable<List<class_265>> info)
    {
        if (entity == null || world == null || movingEntityBoundingBox == null)
        {
            return;
        }

        List<class_265> mutable = ModelBlockSolidCollisions.wrapMutable(info.getReturnValue());

        ModelBlockSolidCollisions.appendShapes(entity, movingEntityBoundingBox, world, mutable);
        info.setReturnValue(mutable);
    }
}
