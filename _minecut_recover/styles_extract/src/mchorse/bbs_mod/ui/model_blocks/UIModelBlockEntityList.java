package mchorse.bbs_mod.ui.model_blocks;

import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import java.util.List;
import java.util.function.Consumer;

public class UIModelBlockEntityList extends UIList<ModelBlockEntity>
{
    public UIModelBlockEntityList(Consumer<List<ModelBlockEntity>> callback)
    {
        super(callback);

        this.scroll.scrollItemSize = UIStringList.DEFAULT_HEIGHT;
    }

    @Override
    protected String elementToString(UIContext context, int i, ModelBlockEntity element)
    {
        return element.getName();
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (this.area.isInside(context) && context.mouseButton == 1)
        {
            int index = this.scroll.getIndex(context.mouseX, context.mouseY);

            if (this.exists(index))
            {
                this.pick(index);
            }
        }

        return super.subMouseClicked(context);
    }
}