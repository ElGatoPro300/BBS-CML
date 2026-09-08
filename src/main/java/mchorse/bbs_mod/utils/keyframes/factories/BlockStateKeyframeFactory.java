package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.interps.IInterp;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;

import java.util.Optional;

public class BlockStateKeyframeFactory implements IKeyframeFactory<BlockState>
{
    @Override
    public BlockState fromData(BaseType data)
    {
        if (data == null)
        {
            return Blocks.AIR.getDefaultState();
        }

        NbtElement nbt = DataStorageUtils.toNbt(data);

        if (nbt == null)
        {
            return Blocks.AIR.getDefaultState();
        }

        DataResult<Pair<BlockState, NbtElement>> decode = BlockState.CODEC.decode(NbtOps.INSTANCE, nbt);
        Optional<Pair<BlockState, NbtElement>> result = decode.result();

        return result.map(Pair::getFirst).orElse(Blocks.AIR.getDefaultState());
    }

    @Override
    public BaseType toData(BlockState value)
    {
        if (value == null)
        {
            value = Blocks.AIR.getDefaultState();
        }

        Optional<NbtElement> result = BlockState.CODEC.encodeStart(NbtOps.INSTANCE, value).result();

        return result.map(DataStorageUtils::fromNbt).orElseGet(MapType::new);
    }

    @Override
    public BlockState createEmpty()
    {
        return Blocks.AIR.getDefaultState();
    }

    @Override
    public BlockState copy(BlockState value)
    {
        return value == null ? Blocks.AIR.getDefaultState() : value;
    }

    @Override
    public BlockState interpolate(BlockState preA, BlockState a, BlockState b, BlockState postB, IInterp interpolation, float x)
    {
        return a == null ? Blocks.AIR.getDefaultState() : a;
    }
}