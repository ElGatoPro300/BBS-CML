package mchorse.bbs_mod.settings.values.core;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.base.BaseValueGroup;
import mchorse.bbs_mod.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ValueList <T extends BaseValue> extends BaseValueGroup
{
    protected final List<T> list = new ArrayList<T>();

    public ValueList(String id)
    {
        super(id);
    }

    public List<T> getList()
    {
        return Collections.unmodifiableList(this.list);
    }

    public void add(T value)
    {
        this.preNotify(0);
        this.list.add(value);
        value.setParent(this);
        this.postNotify(0);
    }

    public void add(int index, T value)
    {
        if (!CollectionUtils.inRange(this.list, index))
        {
            this.add(value);

            return;
        }

        this.preNotify(0);
        this.list.add(index, value);
        value.setParent(this);
        this.postNotify(0);
    }

    public void remove(T value)
    {
        if (this.list.contains(value))
        {
            this.preNotify(0);
            this.list.remove(value);
            this.postNotify(0);
        }
    }

    public void remove(int index)
    {
        if (CollectionUtils.inRange(this.list, index))
        {
            this.preNotify(0);
            this.list.remove(index);
            this.postNotify(0);
        }
    }

    @Override
    public List<BaseValue> getAll()
    {
        return (List<BaseValue>) this.list;
    }

    public List<T> getAllTyped()
    {
        return this.list;
    }

    @Override
    public BaseValue get(String key)
    {
        try
        {
            int index = Integer.parseInt(key);

            return CollectionUtils.getSafe(this.list, index);
        }
        catch (Exception e)
        {}

        return null;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj.getClass() == this.getClass())
        {
            ValueList<?> list = (ValueList<?>) obj;

            return list.list.equals(this.list);
        }

        return super.equals(obj);
    }

    @Override
    public void copy(BaseValueGroup group)
    {
        this.list.clear();

        for (BaseValue value : group.getAll())
        {
            T newValue = this.create(value.getId());

            this.add(newValue);
            newValue.copy(value);
        }
    }

    public void sync()
    {
        int i = 0;

        for (T value : this.list)
        {
            value.setId(String.valueOf(i));
            value.setParent(this);

            i += 1;
        }
    }

    protected abstract T create(String id);

    @Override
    public BaseType toData()
    {
        if (this.list.isEmpty())
        {
            return null;
        }

        ListType list = new ListType();

        for (T value : this.list)
        {
            BaseType data = value.toData();

            if (data != null)
            {
                list.add(data);
            }
        }

        return list.isEmpty() ? null : list;
    }

    @Override
    public void fromData(BaseType data)
    {
        this.list.clear();

        if (data == null || !data.isList())
        {
            return;
        }

        ListType list = data.asList();

        for (int i = 0; i < list.size(); i++)
        {
            T value = this.create(String.valueOf(i));

            this.add(value);
            value.fromData(list.get(i));
        }
    }
}