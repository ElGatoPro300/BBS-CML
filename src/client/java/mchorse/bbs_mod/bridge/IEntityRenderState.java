package mchorse.bbs_mod.bridge;

import net.minecraft.world.entity.Entity;

public interface IEntityRenderState {
    void bbs$setEntity(Entity entity);
    Entity bbs$getEntity();
}
