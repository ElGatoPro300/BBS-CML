package mchorse.bbs_mod.client;

import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.mixin.client.LivingEntityItemAccessor;
import net.minecraft.class_1268;
import net.minecraft.class_1304;
import net.minecraft.class_1309;
import net.minecraft.class_1799;
import net.minecraft.class_1937;
import net.minecraft.class_638;
import net.minecraft.class_745;
import com.mojang.authlib.GameProfile;

import java.util.UUID;

/**
 * Syncs item use state onto a {@link class_1309} so vanilla item model stages
 * (bow pull, crossbow charge, trident throw, etc.) resolve correctly during {@code renderItem}.
 */
public final class ItemUseRenderState
{
    private static final int USING_ITEM_FLAG = 1;
    private static final int OFF_HAND_ACTIVE_FLAG = 2;

    private static class_745 proxy;

    private ItemUseRenderState()
    {}

    public static class_1309 prepareProxy(class_1937 world, IEntity source, class_1304 slot, class_1799 stack)
    {
        if (!(world instanceof class_638 clientWorld) || stack == null || stack.method_7960())
        {
            return null;
        }

        if (proxy == null || proxy.method_37908() != clientWorld)
        {
            proxy = new class_745(clientWorld, new GameProfile(UUID.randomUUID(), "bbs_item_use"));
            proxy.field_5960 = true;
        }

        class_1268 hand = slot == class_1304.field_6171 ? class_1268.field_5810 : class_1268.field_5808;

        ItemUseRenderState.syncEquipment(proxy, source);
        ItemUseRenderState.syncItemUse(proxy, source, hand, stack);

        return proxy;
    }

    public static void syncEquipment(class_1309 living, IEntity source)
    {
        if (source == null)
        {
            return;
        }

        living.method_5673(class_1304.field_6173, source.getEquipmentStack(class_1304.field_6173));
        living.method_5673(class_1304.field_6171, source.getEquipmentStack(class_1304.field_6171));
        living.method_5673(class_1304.field_6169, source.getEquipmentStack(class_1304.field_6169));
        living.method_5673(class_1304.field_6174, source.getEquipmentStack(class_1304.field_6174));
        living.method_5673(class_1304.field_6172, source.getEquipmentStack(class_1304.field_6172));
        living.method_5673(class_1304.field_6166, source.getEquipmentStack(class_1304.field_6166));
    }

    /**
     * Timeline {@link IEntity#getItemUseTimeLeft()} stores elapsed ticks on replay stubs,
     * but vanilla {@link class_1309#method_6014()} stores remaining ticks.
     */
    public static int getItemUseElapsed(IEntity source, class_1309 living, class_1799 stack)
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

        if (stack == null || stack.method_7960())
        {
            return 0;
        }

        int maxUseTime = stack.method_7935(living);
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
    public static void syncItemUse(class_1309 living, IEntity source, class_1268 hand, class_1799 stack)
    {
        if (source == null || stack == null || stack.method_7960())
        {
            living.method_6021();
            living.method_6085(USING_ITEM_FLAG, false);
            living.method_6085(OFF_HAND_ACTIVE_FLAG, false);

            return;
        }

        int itemUseElapsed = ItemUseRenderState.getItemUseElapsed(source, living, stack);
        boolean usingItem = source.isUsingItem() || itemUseElapsed > 0;

        if (!usingItem)
        {
            living.method_6021();
            living.method_6085(USING_ITEM_FLAG, false);
            living.method_6085(OFF_HAND_ACTIVE_FLAG, false);

            return;
        }

        int maxUseTime = stack.method_7935(living);
        int itemUseTimeLeft = Math.max(0, maxUseTime - itemUseElapsed);

        living.method_6019(hand);
        living.method_6122(hand, stack);
        ((LivingEntityItemAccessor) living).setActiveItemStack(stack);
        ((LivingEntityItemAccessor) living).setItemUseTimeLeft(itemUseTimeLeft);
        living.method_6085(USING_ITEM_FLAG, true);
        living.method_6085(OFF_HAND_ACTIVE_FLAG, hand == class_1268.field_5810);
    }
}
