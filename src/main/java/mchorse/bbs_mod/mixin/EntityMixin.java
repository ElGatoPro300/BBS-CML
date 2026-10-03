package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.entity.IEntityFormProvider;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.structure.ModelBlockSolidCollisions;
import mchorse.bbs_mod.morphing.IMorphProvider;
import mchorse.bbs_mod.morphing.Morph;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.jetbrains.annotations.Nullable;

import java.util.List;

@Mixin(Entity.class)
public class EntityMixin
{
    @Shadow
    private int id;

    @Inject(method = "getId", at = @At("HEAD"), cancellable = true)
    public void bbs$safeGetId(CallbackInfoReturnable<Integer> info)
    {
        if (this.id == 0)
        {
            this.id = -100000 - Math.abs(System.identityHashCode(this) % 800000);
            info.setReturnValue(this.id);
        }
    }

    @Inject(method = "getEyeHeight(Lnet/minecraft/world/entity/Pose;)F", at = @At("HEAD"), cancellable = true)
    public void getEyeHeight(Pose pose, CallbackInfoReturnable<Float> info)
    {
        if (this instanceof IMorphProvider provider)
        {
            Morph morph = provider.getMorph();

            if (morph != null)
            {
                Form form = morph.getForm();

                if (form != null && form.hitbox.get())
                {
                    Player player = (Player) (Object) this;
                    float height = form.hitboxHeight.get() * (player.isShiftKeyDown() ? form.hitboxSneakMultiplier.get() : 1F);

                    info.setReturnValue(form.hitboxEyeHeight.get() * height);
                }
            }
        }
        else if (this instanceof IEntityFormProvider provider)
        {
            Form form = provider.getForm();

            if (form != null && form.hitbox.get())
            {
                Entity entity = (Entity) (Object) this;
                float height = form.hitboxHeight.get() * (entity.isShiftKeyDown() ? form.hitboxSneakMultiplier.get() : 1F);

                info.setReturnValue(form.hitboxEyeHeight.get() * height);
            }
        }
    }

    @Inject(method = "canBeCollidedWith", at = @At("HEAD"), cancellable = true)
    public void onIsCollidable(Entity other, CallbackInfoReturnable<Boolean> info)
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
    @Inject(method = "collectAllColliders", at = @At("RETURN"), cancellable = true)
    private static void bbs$appendSolidHitboxes(
        @Nullable Entity entity,
        Level world,
        AABB movingEntityBoundingBox,
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
