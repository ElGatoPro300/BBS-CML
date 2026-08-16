package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.morphing.IMorphProvider;
import mchorse.bbs_mod.morphing.Morph;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMorphMixin extends LivingEntity implements IMorphProvider
{
    public Morph morph = new Morph(this);

    protected PlayerEntityMorphMixin(EntityType<? extends LivingEntity> entityType, World world)
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
        this.morph.update();

        super.baseTick();
    }
}