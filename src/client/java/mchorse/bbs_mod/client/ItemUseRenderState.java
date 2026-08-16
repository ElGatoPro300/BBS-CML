package mchorse.bbs_mod.client;

import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.mixin.client.LivingEntityItemAccessor;

import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import com.mojang.authlib.GameProfile;

import java.util.UUID;

/**
 * Syncs item use state onto a {@link LivingEntity} so vanilla item model stages
 * (bow pull, crossbow charge, trident throw, etc.) resolve correctly during {@code renderItem}.
 */
public final class ItemUseRenderState
{
    private static final int USING_ITEM_FLAG = 1;
    private static final int OFF_HAND_ACTIVE_FLAG = 2;

    private static OtherClientPlayerEntity proxy;

    private ItemUseRenderState()
    {}

    public static LivingEntity prepareProxy(World world, IEntity source, EquipmentSlot slot, ItemStack stack)
    {
        if (!(world instanceof ClientWorld clientWorld) || stack == null || stack.isEmpty())
        {
            return null;
        }

        if (proxy == null || proxy.getWorld() != clientWorld)
        {
            proxy = new OtherClientPlayerEntity(clientWorld, new GameProfile(UUID.randomUUID(), "bbs_item_use"));
            proxy.noClip = true;
        }

        Hand hand = slot == EquipmentSlot.OFFHAND ? Hand.OFF_HAND : Hand.MAIN_HAND;

        ItemUseRenderState.syncEquipment(proxy, source);
        ItemUseRenderState.syncItemUse(proxy, source, hand, stack);

        return proxy;
    }

    public static void syncEquipment(LivingEntity living, IEntity source)
    {
        if (source == null)
        {
            return;
        }

        living.equipStack(EquipmentSlot.MAINHAND, source.getEquipmentStack(EquipmentSlot.MAINHAND));
        living.equipStack(EquipmentSlot.OFFHAND, source.getEquipmentStack(EquipmentSlot.OFFHAND));
        living.equipStack(EquipmentSlot.HEAD, source.getEquipmentStack(EquipmentSlot.HEAD));
        living.equipStack(EquipmentSlot.CHEST, source.getEquipmentStack(EquipmentSlot.CHEST));
        living.equipStack(EquipmentSlot.LEGS, source.getEquipmentStack(EquipmentSlot.LEGS));
        living.equipStack(EquipmentSlot.FEET, source.getEquipmentStack(EquipmentSlot.FEET));
    }

    /**
     * Timeline {@link IEntity#getItemUseTimeLeft()} stores elapsed ticks on replay stubs,
     * but vanilla {@link LivingEntity#getItemUseTimeLeft()} stores remaining ticks.
     */
    public static int getItemUseElapsed(IEntity source, LivingEntity living, ItemStack stack)
    {
        if (source == null)
        {
            return 0;
        }

        boolean usingItem = source.isUsingItem() || source.getItemUseTimeLeft() > 0;

        if (!usingItem)
        {
            return 0;
        }

        if (source instanceof StubEntity)
        {
            return Math.max(0, source.getItemUseTimeLeft());
        }

        if (stack == null || stack.isEmpty())
        {
            return 0;
        }

        int maxUseTime = stack.getItem().getMaxUseTime(stack);
        int remaining = source.getItemUseTimeLeft();

        if (maxUseTime <= 0)
        {
            return Math.max(0, remaining);
        }

        return Math.max(0, maxUseTime - remaining);
    }

    /**
     * Applies item-use fields on {@code living}. {@code stack} must be the same reference
     * that will be passed to {@code ItemRenderer.renderItem} for model predicates.
     */
    public static void syncItemUse(LivingEntity living, IEntity source, Hand hand, ItemStack stack)
    {
        if (source == null || stack == null || stack.isEmpty())
        {
            living.clearActiveItem();
            living.setLivingFlag(USING_ITEM_FLAG, false);
            living.setLivingFlag(OFF_HAND_ACTIVE_FLAG, false);

            return;
        }

        int itemUseElapsed = ItemUseRenderState.getItemUseElapsed(source, living, stack);
        boolean usingItem = source.isUsingItem() || itemUseElapsed > 0;

        if (!usingItem)
        {
            living.clearActiveItem();
            living.setLivingFlag(USING_ITEM_FLAG, false);
            living.setLivingFlag(OFF_HAND_ACTIVE_FLAG, false);

            return;
        }

        int maxUseTime = stack.getItem().getMaxUseTime(stack);
        int itemUseTimeLeft = Math.max(0, maxUseTime - itemUseElapsed);

        living.setCurrentHand(hand);
        living.setStackInHand(hand, stack);
        ((LivingEntityItemAccessor) living).setActiveItemStack(stack);
        ((LivingEntityItemAccessor) living).setItemUseTimeLeft(itemUseTimeLeft);
        living.setLivingFlag(USING_ITEM_FLAG, true);
        living.setLivingFlag(OFF_HAND_ACTIVE_FLAG, hand == Hand.OFF_HAND);
    }
}
