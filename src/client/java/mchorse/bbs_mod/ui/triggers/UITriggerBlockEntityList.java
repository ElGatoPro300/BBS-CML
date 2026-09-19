package mchorse.bbs_mod.ui.triggers;

import mchorse.bbs_mod.blocks.entities.TriggerBlockEntity;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;

import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.function.Consumer;

public class UITriggerBlockEntityList extends UIList<TriggerBlockEntity>
{
    public UITriggerBlockEntityList(Consumer<List<TriggerBlockEntity>> callback)
    {
        super(callback);

        this.scroll.scrollItemSize = UIStringList.DEFAULT_HEIGHT;
    }

    @Override
    protected String elementToString(UIContext context, int i, TriggerBlockEntity element)
    {
        BlockPos pos = element.getPos();
        
        return "(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")";
    }
}
