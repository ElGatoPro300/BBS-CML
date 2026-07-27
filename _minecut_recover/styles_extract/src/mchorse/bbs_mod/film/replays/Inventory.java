package mchorse.bbs_mod.film.replays;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Inventory extends BaseValue
{
    private List<class_1799> stacks = new ArrayList<>();

    public Inventory(String id)
    {
        super(id);
    }

    public List<class_1799> getStacks()
    {
        return Collections.unmodifiableList(this.stacks);
    }

    public void fromPlayer(class_1657 player)
    {
        this.stacks.clear();

        for (int i = 0; i < player.method_31548().method_5439(); i++)
        {
            this.stacks.add(player.method_31548().method_5438(i).method_7972());
        }
    }

    @Override
    public BaseType toData()
    {
        ListType data = new ListType();

        for (class_1799 stack : this.stacks)
        {
            if (stack == null)
            {
                stack = class_1799.field_8037;
            }

            data.add(KeyframeFactories.ITEM_STACK.toData(stack));
        }

        return data;
    }

    @Override
    public void fromData(BaseType data)
    {
        this.stacks.clear();

        if (data.isList())
        {
            ListType list = data.asList();

            for (BaseType type : list)
            {
                class_1799 stack = KeyframeFactories.ITEM_STACK.fromData(type);

                if (stack == null)
                {
                    stack = class_1799.field_8037;
                }

                this.stacks.add(stack);
            }
        }
    }
}