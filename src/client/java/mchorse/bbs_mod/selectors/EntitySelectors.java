package mchorse.bbs_mod.selectors;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import net.minecraft.entity.LivingEntity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EntitySelectors implements IMapSerializable
{
    public List<EntitySelector> selectors = new ArrayList<>();

    private long update;

    private static File getFile()
    {
        return BBSMod.getSettingsPath("selectors.json");
    }

    public EntitySelectors()
    {
        this.update();
    }

    public long getLastUpdate()
    {
        return this.update;
    }

    public void update()
    {
        this.update = System.currentTimeMillis();
    }

    public EntitySelector getSelectorFor(LivingEntity mcEntity)
    {
        for (EntitySelector selector : this.selectors)
        {
            if (selector.matches(mcEntity))
            {
                return selector;
            }
        }

        return null;
    }

    public void read()
    {
        try
        {
            File file = getFile();

            if (file.isFile())
            {
                this.fromData(DataToString.read(file).asMap());
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void save()
    {
        try
        {
            DataToString.write(getFile(), this.toData());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void fromData(MapType data)
    {
        ListType selectors = data.getList("selectors");

        for (BaseType selectorType : selectors)
        {
            if (selectorType.isMap())
            {
                EntitySelector selector = new EntitySelector();

                selector.fromData(selectorType.asMap());
                this.selectors.add(selector);
            }
        }
    }

    @Override
    public void toData(MapType data)
    {
        ListType selectors = new ListType();

        for (EntitySelector selector : this.selectors)
        {
            selectors.add(selector.toData());
        }

        data.put("selectors", selectors);
    }
}