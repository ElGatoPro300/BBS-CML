package mchorse.bbs_mod.ui.triggers;

import mchorse.bbs_mod.blocks.entities.TriggerBlockEntity;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import net.minecraft.class_2338;
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
        class_2338 pos = element.method_11016();
        
        return "(" + pos.method_10263() + ", " + pos.method_10264() + ", " + pos.method_10260() + ")";
    }
}
