package mchorse.bbs_mod.client.renderer;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.entity.ActorEntity;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.mixin.client.EntityAccessor;
import mchorse.bbs_mod.utils.AABB;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.interps.Lerps;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;

import org.joml.Matrix4f;
import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

/**
 * Renders vanilla entity fire overlay on morph replays.
 */
public final class MorphFireRenderer
{
    private static final Quaternionf TEMP_QUATERNION = new Quaternionf();

    private static ActorEntity proxy;

    private MorphFireRenderer()
    {}

    public static void render(PoseStack matrices, MultiBufferSource consumers, IEntity morph, Form form, float tickDelta, Camera camera, boolean relative)
    {
        if (morph.getFireTicks() <= 0 || consumers == null)
        {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel world = mc.level;

        if (world == null)
        {
            return;
        }

        if (MorphFireRenderer.proxy == null || MorphFireRenderer.proxy.getEntityWorld() != world)
        {
            MorphFireRenderer.proxy = new ActorEntity(BBSMod.ACTOR_ENTITY, world);
        }

        ActorEntity entity = MorphFireRenderer.proxy;
        float[] size = MorphFireRenderer.getFireDimensions(morph, form);
        Pose pose = morph.isSneaking() ? Pose.CROUCHING : Pose.STANDING;

        entity.setFireTicks(morph.getFireTicks());
        entity.age = Math.max(entity.age, morph.getAge());
        entity.setPose(pose);
        entity.setSneaking(morph.isSneaking());
        ((EntityAccessor) entity).bbs$setDimensions(EntityDimensions.fixed(size[0], size[1]));
        entity.calculateDimensions();
        entity.setPos(0D, 0D, 0D);
        entity.lastRenderX = 0D;
        entity.lastRenderY = 0D;
        entity.lastRenderZ = 0D;
        entity.lastX = 0D;
        entity.lastY = 0D;
        entity.lastZ = 0D;
        entity.setInvisible(false);

        float bodyYaw = Lerps.lerp(morph.getPrevBodyYaw(), morph.getBodyYaw(), tickDelta);
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        boolean irisWorld = BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld();

        matrices.pushPose();

        if (irisWorld && !relative)
        {
            /* Iris bakes the terrain matrix into the stack; strip it and rebuild the
             * camera-relative entity transform the same way as ParticleFormRenderer. */
            Matrix4f composed = BBSRendering.stripTerrainPositionMatrix(new Matrix4f(matrices.last().pose()));
            Matrix4f oriented = new Matrix4f(MatrixStackUtils.getInverseViewRotationMatrix());

            oriented.mul(composed);

            matrices.setIdentity();
            matrices.mulPose(MatrixStackUtils.getViewRotationMatrix());
            MatrixStackUtils.multiply(matrices, oriented);
        }
        else if (relative)
        {
            matrices.mulPose(camera.rotation().conjugate(MorphFireRenderer.TEMP_QUATERNION));
        }

        matrices.mulPose(Axis.YP.rotation(MathUtils.toRad(bodyYaw)));

        /* TODO 1.21.4: EntityRenderDispatcher.renderFire removed */
        matrices.popPose();

        entity.setFireTicks(0);
    }

    private static float[] getFireDimensions(IEntity morph, Form form)
    {
        if (form instanceof MobForm mobForm)
        {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(mobForm.mobID.get()));

            if (type != null)
            {
                EntityDimensions dimensions = type.getDimensions();

                if (morph.isSneaking())
                {
                    dimensions = dimensions.scale(0.8F);
                }

                return new float[] {dimensions.width(), dimensions.height()};
            }
        }

        if (form != null && form.hitbox.get())
        {
            float height = form.hitboxHeight.get();

            if (morph.isSneaking())
            {
                height *= form.hitboxSneakMultiplier.get();
            }

            return new float[] {form.hitboxWidth.get(), height};
        }

        if (morph instanceof MCEntity mcEntity)
        {
            Entity mc = mcEntity.getMcEntity();

            return new float[] {mc.getBbWidth(), mc.getBbHeight()};
        }

        AABB hitbox = morph.getPickingHitbox();

        return new float[] {(float) hitbox.w, (float) hitbox.h};
    }
}
