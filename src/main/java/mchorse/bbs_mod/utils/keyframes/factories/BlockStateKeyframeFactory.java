package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.utils.interps.IInterp;

import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;

import java.util.Optional;

public class BlockStateKeyframeFactory implements IKeyframeFactory<BlockState>
{
    @Override
    public BlockState fromData(BaseType data)
    {
        DataResult<Pair<BlockState, Tag>> decode = BlockState.CODEC.decode(NbtOps.INSTANCE, DataStorageUtils.toNbt(data));
        Optional<Pair<BlockState, Tag>> result = decode.result();

        return result.map(Pair::getFirst).orElse(null);
    }

    @Override
    public BaseType toData(BlockState value)
    {
        Optional<Tag> result = BlockState.CODEC.encodeStart(NbtOps.INSTANCE, value).result();

        return result.map(DataStorageUtils::fromNbt).orElse(null);
    }

    @Override
    public BlockState createEmpty()
    {
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public BlockState copy(BlockState value)
    {
        return value;
    }

    @Override
    public BlockState interpolate(BlockState preA, BlockState a, BlockState b, BlockState postB, IInterp interpolation, float x)
    {
        return a;
    }
}