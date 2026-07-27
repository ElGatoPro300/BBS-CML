package mchorse.bbs_mod.utils.resources;

import mchorse.bbs_mod.data.IDataSerializable;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.resources.Link;

public interface IWritableLink extends IDataSerializable<BaseType>
{
    public Link copy();
}