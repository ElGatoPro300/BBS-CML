package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.utils.interps.IInterp;
import net.minecraft.class_2246;
import net.minecraft.class_2509;
import net.minecraft.class_2520;
import net.minecraft.class_2680;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;

import java.util.Optional;

public class BlockStateKeyframeFactory implements IKeyframeFactory<class_2680>
{
    @Override
    public class_2680 fromData(BaseType data)
    {
        DataResult<Pair<class_2680, class_2520>> decode = class_2680.field_24734.decode(class_2509.field_11560, DataStorageUtils.toNbt(data));
        Optional<Pair<class_2680, class_2520>> result = decode.result();

        return result.map(Pair::getFirst).orElse(null);
    }

    @Override
    public BaseType toData(class_2680 value)
    {
        class_2680 safe = value != null ? value : this.createEmpty();
        Optional<class_2520> result = class_2680.field_24734.encodeStart(class_2509.field_11560, safe).result();

        return result.map(DataStorageUtils::fromNbt).orElse(null);
    }

    @Override
    public class_2680 createEmpty()
    {
        return class_2246.field_10124.method_9564();
    }

    @Override
    public class_2680 copy(class_2680 value)
    {
        return value;
    }

    @Override
    public class_2680 interpolate(class_2680 preA, class_2680 a, class_2680 b, class_2680 postB, IInterp interpolation, float x)
    {
        return a;
    }
}
