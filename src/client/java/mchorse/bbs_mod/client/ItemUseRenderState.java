package mchorse.bbs_mod.client;

import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.mixin.client.LivingEntityAccessor;
import mchorse.bbs_mod.mixin.client.LivingEntityItemAccessor;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

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

    private static RemotePlayer proxy;
    private static ClientLevel proxyWorld;

    private ItemUseRenderState()
    {}

    public static LivingEntity prepareProxy(Level world, IEntity source, EquipmentSlot slot, ItemStack stack)
    {
        if (!(world instanceof ClientLevel clientWorld) || stack == null || stack.isEmpty())
        {
            return null;
        }

        if (proxy == null || proxyWorld != clientWorld)
        {
            proxy = new RemotePlayer(clientWorld, new GameProfile(UUID.randomUUID(), "bbs_item_use"));
            proxy.noPhysics = true;
            proxyWorld = clientWorld;
        }

        InteractionHand hand = slot == EquipmentSlot.OFFHAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;

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

        living.setItemSlot(EquipmentSlot.MAINHAND, source.getEquipmentStack(EquipmentSlot.MAINHAND));
        living.setItemSlot(EquipmentSlot.OFFHAND, source.getEquipmentStack(EquipmentSlot.OFFHAND));
        living.setItemSlot(EquipmentSlot.HEAD, source.getEquipmentStack(EquipmentSlot.HEAD));
        living.setItemSlot(EquipmentSlot.CHEST, source.getEquipmentStack(EquipmentSlot.CHEST));
        living.setItemSlot(EquipmentSlot.LEGS, source.getEquipmentStack(EquipmentSlot.LEGS));
        living.setItemSlot(EquipmentSlot.FEET, source.getEquipmentStack(EquipmentSlot.FEET));
    }

    /**
     * Timeline {@link IEntity#getItemUseTimeLeft()} stores elapsed ticks on replay stubs,
     * but vanilla {@link LivingEntity#getUseItemRemainingTicks()} stores remaining ticks.
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

        int maxUseTime = stack.getUseDuration(living);
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
    public static void syncItemUse(LivingEntity living, IEntity source, InteractionHand hand, ItemStack stack)
    {
        if (source == null || stack == null || stack.isEmpty())
        {
            living.stopUsingItem();
            ((LivingEntityAccessor) living).invokeSetLivingFlag(USING_ITEM_FLAG, false);
            ((LivingEntityAccessor) living).invokeSetLivingFlag(OFF_HAND_ACTIVE_FLAG, false);

            return;
        }

        int itemUseElapsed = ItemUseRenderState.getItemUseElapsed(source, living, stack);
        boolean usingItem = source.isUsingItem() || itemUseElapsed > 0;

        if (!usingItem)
        {
            living.stopUsingItem();
            ((LivingEntityAccessor) living).invokeSetLivingFlag(USING_ITEM_FLAG, false);
            ((LivingEntityAccessor) living).invokeSetLivingFlag(OFF_HAND_ACTIVE_FLAG, false);

            return;
        }

        int maxUseTime = stack.getUseDuration(living);
        int itemUseTimeLeft = Math.max(0, maxUseTime - itemUseElapsed);

        living.startUsingItem(hand);
        living.setItemInHand(hand, stack);
        ((LivingEntityItemAccessor) living).setActiveItemStack(stack);
        ((LivingEntityItemAccessor) living).setItemUseTimeLeft(itemUseTimeLeft);
        ((LivingEntityAccessor) living).invokeSetLivingFlag(USING_ITEM_FLAG, true);
        ((LivingEntityAccessor) living).invokeSetLivingFlag(OFF_HAND_ACTIVE_FLAG, hand == InteractionHand.OFF_HAND);
    }
}
