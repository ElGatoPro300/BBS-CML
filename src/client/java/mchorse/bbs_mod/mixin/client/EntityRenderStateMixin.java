package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.bridge.IEntityRenderState;
import mchorse.bbs_mod.client.renderer.IRenderStateEntityHolder;

import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements IEntityRenderState, IRenderStateEntityHolder
{
    @Unique
    private Entity bbs$entity;

    @Unique
    private float bbs$tickDelta;

    @Override
    public void bbs$setEntity(Entity entity)
    {
        this.bbs$entity = entity;
    }

    @Override
    public Entity bbs$getEntity()
    {
        return this.bbs$entity;
    }

    @Override
    public Entity bbs$getRenderedEntity()
    {
        return this.bbs$entity;
    }

    @Override
    public float bbs$getRenderedTickDelta()
    {
        return this.bbs$tickDelta;
    }

    @Override
    public void bbs$setRenderedEntity(Entity entity, float tickDelta)
    {
        this.bbs$entity = entity;
        this.bbs$tickDelta = tickDelta;
    }
}
