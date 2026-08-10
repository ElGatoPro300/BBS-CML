package mchorse.bbs_mod.actions;

import mchorse.bbs_mod.items.MobKillerItem;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;

/**
 * Resolves melee attack damage from the attacker's held item / attributes so
 * film Attack clips match swords (incl. Sharpness / {@link MobKillerItem})
 * instead of a flat stub value.
 */
public final class AttackDamage
{
    /**
     * Stored on Attack clips for Mob Killer hits. Playback treats this (or a
     * Mob Killer in-hand) as {@link LivingEntity#kill()} for non-players.
     */
    public static final float MOB_KILLER_DAMAGE = 1000000F;

    private AttackDamage()
    {}

    public static boolean isMobKiller(ItemStack stack)
    {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof MobKillerItem;
    }

    public static float fromAttacker(LivingEntity attacker, Entity target)
    {
        if (attacker == null)
        {
            return 1F;
        }

        ItemStack stack = attacker.getMainHandStack();

        /* Mob Killer does not use attribute damage — postHit calls kill(). */
        if (isMobKiller(stack))
        {
            return MOB_KILLER_DAMAGE;
        }

        float base = (float) attacker.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);

        if (stack.isEmpty() || !(attacker.getWorld() instanceof ServerWorld serverWorld))
        {
            return Math.max(1F, base);
        }

        if (target == null)
        {
            return Math.max(1F, base);
        }

        DamageSource source = serverWorld.getDamageSources().mobAttack(attacker);

        if (attacker instanceof PlayerEntity player)
        {
            source = serverWorld.getDamageSources().playerAttack(player);
        }

        float enchanted = EnchantmentHelper.getDamage(serverWorld, stack, target, source, base);

        return Math.max(base, enchanted);
    }

    public static float resolve(float clipDamage, LivingEntity attacker, Entity target)
    {
        float weapon = fromAttacker(attacker, target);

        return Math.max(Math.max(0F, clipDamage), weapon);
    }

    /**
     * Applies film attack damage, honoring Mob Killer's instakill behavior.
     */
    public static void applyHit(LivingEntity attacker, Entity target, float clipDamage)
    {
        if (target == null || attacker == null)
        {
            return;
        }

        ItemStack stack = attacker.getMainHandStack();

        if (isMobKiller(stack) || clipDamage >= MOB_KILLER_DAMAGE)
        {
            if (target instanceof LivingEntity living && !(living instanceof PlayerEntity))
            {
                living.kill();
            }

            return;
        }

        float damage = resolve(clipDamage, attacker, target);

        if (damage <= 0F)
        {
            return;
        }

        target.damage(attacker.getWorld().getDamageSources().mobAttack(attacker), damage);
    }
}
