package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.selectors.ISelectorOwnerProvider;
import mchorse.bbs_mod.selectors.SelectorOwner;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements ISelectorOwnerProvider
{
    public SelectorOwner selector = new SelectorOwner((LivingEntity) (Object) this);

    protected LivingEntityMixin(EntityType<?> type, Level world)
    {
        super(type, world);
    }

    @Override
    public SelectorOwner getOwner()
    {
        return this.selector;
    }
}