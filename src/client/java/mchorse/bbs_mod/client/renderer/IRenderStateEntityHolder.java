package mchorse.bbs_mod.client.renderer;

import net.minecraft.entity.Entity;

/**
 * Duck interface implemented on EntityRenderState so the morph render path can recover the live entity + tickDelta.
 */
public interface IRenderStateEntityHolder
{
    Entity bbs$getRenderedEntity();

    float bbs$getRenderedTickDelta();

    void bbs$setRenderedEntity(Entity entity, float tickDelta);
}
