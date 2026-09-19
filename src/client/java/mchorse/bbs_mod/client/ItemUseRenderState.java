package mchorse.bbs_mod.client;

import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.mixin.client.LivingEntityItemAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
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
    private static boolean drivingLocalPlayerUse;

    private ItemUseRenderState()
    {}

    public static boolean isDrivingLocalPlayerUse()
    {
        return drivingLocalPlayerUse;
    }

    /**
     * Called at the start of {@code Films.updateEndWorld} so a missing FP replay
     * this tick can release last tick's driven use instead of leaving it stuck.
     */
    public static void beginEndWorldUpdate()
    {
        drivingLocalPlayerUse = false;
    }

    public static void releaseLocalPlayerUse()
    {
        drivingLocalPlayerUse = false;

        Minecraft client = Minecraft.getInstance();

        if (client.player == null)
        {
            return;
        }

        ItemUseRenderState.clearUseFlags(client.player);
    }

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

        if (!source.isUsingItem())
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
        if (living == null)
        {
            return;
        }

        if (source == null || stack == null || stack.isEmpty() || !source.isUsingItem())
        {
            ItemUseRenderState.clearUseFlags(living);

            return;
        }

        int itemUseElapsed = ItemUseRenderState.getItemUseElapsed(source, living, stack);

        int maxUseTime = stack.getUseDuration(living);
        int itemUseTimeLeft = Math.max(0, maxUseTime - itemUseElapsed);
        boolean localPlayer = living instanceof LocalPlayer;
        ItemStack active = stack;

        if (localPlayer)
        {
            /* Never write into the real hotbar — setStackInHand copies the use
             * item into whichever slot is selected, and a new interpolate() copy
             * every tick resets HeldItemRenderer's identity-based equip pose. */
            drivingLocalPlayerUse = true;
            active = living.getItemInHand(hand);

            if (active.isEmpty() || !ItemStack.isSameItemSameComponents(active, stack))
            {
                active = stack.copy();
            }
        }
        else
        {
            ItemStack current = living.getItemInHand(hand);

            if (!ItemStack.matches(current, stack))
            {
                living.setItemInHand(hand, stack.copy());
                current = living.getItemInHand(hand);
            }

            active = current.isEmpty() ? stack.copy() : current;

            if (!living.isUsingItem() || living.getUsedItemHand() != hand)
            {
                living.startUsingItem(hand);
            }
        }

        ((LivingEntityItemAccessor) living).setActiveItemStack(active);
        ((LivingEntityItemAccessor) living).setItemUseTimeLeft(itemUseTimeLeft);
        living.setLivingEntityFlag(USING_ITEM_FLAG, true);
        living.setLivingEntityFlag(OFF_HAND_ACTIVE_FLAG, hand == InteractionHand.OFF_HAND);
    }

    private static void clearUseFlags(LivingEntity living)
    {
        living.stopUsingItem();
        living.setLivingEntityFlag(USING_ITEM_FLAG, false);
        living.setLivingEntityFlag(OFF_HAND_ACTIVE_FLAG, false);
    }
}
