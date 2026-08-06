package mchorse.bbs_mod.film;

import com.mojang.blaze3d.systems.RenderSystem;
import io.netty.util.collection.IntObjectMap;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

public class FilmControllerContext
{
    public final static FilmControllerContext instance = new FilmControllerContext();

    public IntObjectMap<IEntity> entities;
    public IEntity entity;
    public Replay replay;
    public Camera camera;
    public MatrixStack stack;
    public VertexConsumerProvider consumers;
    public StencilMap map;

    public float transition;
    public int color;
    public float shadowRadius;

    public String bone;
    public boolean local;

    public String bone2;
    public boolean local2;

    public String nameTag = "";
    public boolean relative;

    private FilmControllerContext()
    {}

    private void reset()
    {
        this.map = null;
        this.shadowRadius = 0F;
        this.color = Colors.WHITE;
        this.bone = null;
        this.local = false;
        this.nameTag = "";
        this.relative = false;
    }

    public FilmControllerContext setup(IntObjectMap<IEntity> entities, IEntity entity, Replay replay, WorldRenderContext context)
    {
        this.reset();

        this.entities = entities;
        this.entity = entity;
        this.replay = replay;
        this.camera = context.camera();
        this.stack = context.matrixStack();
        if (this.stack == null)
        {
            this.stack = new MatrixStack();
            MatrixStackUtils.multiply(this.stack, RenderSystem.getModelViewMatrix());
        }
        this.consumers = context.consumers();
        this.transition = context.tickCounter().getTickDelta(false);

        return this;
    }

    public FilmControllerContext setup(IntObjectMap<IEntity> entities, IEntity entity, Replay replay, Camera camera, MatrixStack stack, VertexConsumerProvider consumers, float transition)
    {
        this.reset();

        this.entities = entities;
        this.entity = entity;
        this.replay = replay;
        this.camera = camera;
        this.stack = stack;
        this.consumers = consumers;
        this.transition = transition;

        return this;
    }

    public FilmControllerContext transition(float transition)
    {
        this.transition = transition;

        return this;
    }

    public FilmControllerContext stencil(StencilMap map)
    {
        this.map = map;

        return this;
    }

    public FilmControllerContext shadow(boolean shadow, float shadowRadius)
    {
        this.shadowRadius = shadow ? shadowRadius : 0F;

        return this;
    }

    public FilmControllerContext shadow(float shadowRadius)
    {
        this.shadowRadius = shadowRadius;

        return this;
    }

    public FilmControllerContext color(int overlayColor)
    {
        this.color = overlayColor;

        return this;
    }

    public FilmControllerContext bone(String bone, boolean local)
    {
        this.bone = bone;
        this.local = local;

        return this;
    }

    public FilmControllerContext bone2(String bone, boolean local)
    {
        this.bone2 = bone;
        this.local2 = local;

        return this;
    }

    public FilmControllerContext nameTag(String nameTag)
    {
        this.nameTag = nameTag;

        return this;
    }

    public FilmControllerContext relative(boolean relative)
    {
        this.relative = relative;

        return this;
    }
}