package mchorse.bbs_mod.settings.values.mc;

import mchorse.bbs_mod.settings.values.base.BaseKeyframeFactoryValue;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ValueBlockState extends BaseKeyframeFactoryValue<BlockState>
{
    public ValueBlockState(String id)
    {
        super(id, KeyframeFactories.BLOCK_STATE, Blocks.AIR.defaultBlockState());
    }
}