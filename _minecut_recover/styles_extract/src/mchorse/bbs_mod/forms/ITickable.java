package mchorse.bbs_mod.forms;

import mchorse.bbs_mod.forms.entities.IEntity;

public interface ITickable
{
    public void tick(IEntity entity);
}