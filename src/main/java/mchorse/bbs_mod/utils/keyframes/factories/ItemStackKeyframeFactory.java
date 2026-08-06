package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.interps.IInterp;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import java.util.Optional;

public class ItemStackKeyframeFactory implements IKeyframeFactory<ItemStack>
{
    @Override
    public ItemStack fromData(BaseType data)
    {
        RegistryWrapper.WrapperLookup registries = BBSMod.getRegistryManager();
        DynamicOps<NbtElement> ops = registries != null ? RegistryOps.of(NbtOps.INSTANCE, registries) : NbtOps.INSTANCE;
        DataResult<Pair<ItemStack, NbtElement>> decode = ItemStack.CODEC.decode(ops, DataStorageUtils.toNbt(data));
        Optional<Pair<ItemStack, NbtElement>> result = decode.result();

        return result.map(Pair::getFirst).orElse(ItemStack.EMPTY);
    }

    @Override
    public BaseType toData(ItemStack value)
    {
        RegistryWrapper.WrapperLookup registries = BBSMod.getRegistryManager();
        DynamicOps<NbtElement> ops = registries != null ? RegistryOps.of(NbtOps.INSTANCE, registries) : NbtOps.INSTANCE;
        Optional<NbtElement> result = ItemStack.CODEC.encodeStart(ops, value).result();

        return result.map(DataStorageUtils::fromNbt).orElse(new MapType());
    }

    @Override
    public ItemStack createEmpty()
    {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean compare(Object a, Object b)
    {
        if (a instanceof ItemStack itemA && b instanceof ItemStack itemB)
        {
            return ItemStack.areEqual(itemA, itemB);
        }

        return false;
    }

    @Override
    public ItemStack copy(ItemStack value)
    {
        return value.copy();
    }

    @Override
    public ItemStack interpolate(ItemStack preA, ItemStack a, ItemStack b, ItemStack postB, IInterp interpolation, float x)
    {
        if (a == null || b == null)
        {
            return a == null ? ItemStack.EMPTY : a;
        }

        if (a.isEmpty() || b.isEmpty())
        {
            return x < 1F ? a : b;
        }

        if (!ItemStack.canCombine(a, b))
        {
            return x < 1F ? a : b;
        }

        int aCount = a.getCount();
        int bCount = b.getCount();
        int count = (int) Math.round(interpolation.interpolate(aCount, bCount, x));

        if (count < 0)
        {
            count = 0;
        }

        ItemStack copy = a.copy();
        copy.setCount(count);

        return copy;
    }
}
