package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.interps.IInterp;
import net.minecraft.class_1799;
import net.minecraft.class_2509;
import net.minecraft.class_2520;
import net.minecraft.class_6903;
import net.minecraft.class_7225;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import java.util.Optional;

public class ItemStackKeyframeFactory implements IKeyframeFactory<class_1799>
{
    @Override
    public class_1799 fromData(BaseType data)
    {
        class_7225.class_7874 registries = BBSMod.getRegistryManager();
        DynamicOps<class_2520> ops = registries != null ? class_6903.method_46632(class_2509.field_11560, registries) : class_2509.field_11560;
        DataResult<Pair<class_1799, class_2520>> decode = class_1799.field_24671.decode(ops, DataStorageUtils.toNbt(data));
        Optional<Pair<class_1799, class_2520>> result = decode.result();

        return result.map(Pair::getFirst).orElse(class_1799.field_8037);
    }

    @Override
    public BaseType toData(class_1799 value)
    {
        class_7225.class_7874 registries = BBSMod.getRegistryManager();
        DynamicOps<class_2520> ops = registries != null ? class_6903.method_46632(class_2509.field_11560, registries) : class_2509.field_11560;
        Optional<class_2520> result = class_1799.field_24671.encodeStart(ops, value).result();

        return result.map(DataStorageUtils::fromNbt).orElse(new MapType());
    }

    @Override
    public class_1799 createEmpty()
    {
        return class_1799.field_8037;
    }

    @Override
    public boolean compare(Object a, Object b)
    {
        if (a instanceof class_1799 itemA && b instanceof class_1799 itemB)
        {
            return class_1799.method_7973(itemA, itemB);
        }

        return false;
    }

    @Override
    public class_1799 copy(class_1799 value)
    {
        return value.method_7972();
    }

    @Override
    public class_1799 interpolate(class_1799 preA, class_1799 a, class_1799 b, class_1799 postB, IInterp interpolation, float x)
    {
        if (a == null || b == null)
        {
            return a == null ? class_1799.field_8037 : a;
        }

        if (a.method_7960() || b.method_7960())
        {
            return x < 1F ? a : b;
        }

        if (!class_1799.method_31577(a, b))
        {
            return x < 1F ? a : b;
        }

        int aCount = a.method_7947();
        int bCount = b.method_7947();
        int count = (int) Math.round(interpolation.interpolate(aCount, bCount, x));

        if (count < 0)
        {
            count = 0;
        }

        class_1799 copy = a.method_7972();
        copy.method_7939(count);

        return copy;
    }
}
