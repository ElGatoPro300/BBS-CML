package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.entity.IEntityFormProvider;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.structure.ModelBlockSolidCollisions;
import mchorse.bbs_mod.morphing.IMorphProvider;
import mchorse.bbs_mod.morphing.Morph;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin
{
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
        @Nullable Entity entity,
        World world,
        List<VoxelShape> regularCollisions,
        Box movingEntityBoundingBox,
        CallbackInfoReturnable<List<VoxelShape>> info)
    {
        if (entity == null || world == null || movingEntityBoundingBox == null)
        {
            return;
        }

        List<VoxelShape> mutable = ModelBlockSolidCollisions.wrapMutable(info.getReturnValue());

        ModelBlockSolidCollisions.appendShapes(entity, movingEntityBoundingBox, world, mutable);
        info.setReturnValue(mutable);
    }
}
