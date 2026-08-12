package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.morphing.IMorphProvider;
import mchorse.bbs_mod.morphing.Morph;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(Player.class)
public abstract class PlayerEntityMorphMixin extends LivingEntity implements IMorphProvider
{
    public Morph morph = new Morph(this);

    protected PlayerEntityMorphMixin(EntityType<? extends LivingEntity> entityType, Level world)
    {
        super(entityType, world);
    }

    @Override
    public Morph getMorph()
    {
        return this.morph;
    }

    @Override
    public void baseTick()
    {
        /* Tick living entity first so age/limbs are current, then advance the morph form
         * animator (previously morph ticked before super and could miss limb motion). */
        super.baseTick();
        this.morph.update();
    }
}