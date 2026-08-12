package mchorse.bbs_mod.items;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class MobKillerItem extends Item
{
    public MobKillerItem(Properties settings)
    {
        super(settings);
    }

    @Override
    public boolean isFoil(ItemStack stack)
    {
        return true;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker)
    {
        if (!target.level().isClientSide() && !(target instanceof Player) && target.level() instanceof ServerLevel serverWorld)
        {
            target.kill(serverWorld);
        }

        super.hurtEnemy(stack, target, attacker);
    }
}
