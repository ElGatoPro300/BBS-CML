package mchorse.bbs_mod.client.renderer;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.entity.ActorEntity;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.mixin.client.EntityAccessor;
import mchorse.bbs_mod.mixin.client.EntityRendererDispatcherInvoker;
import mchorse.bbs_mod.utils.AABB;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.interps.Lerps;
import net.minecraft.class_1297;
import net.minecraft.class_1299;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_4048;
import net.minecraft.class_4050;
import net.minecraft.class_4184;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_638;
import net.minecraft.class_7833;
import net.minecraft.class_7923;
import net.minecraft.class_898;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Renders vanilla entity fire overlay on morph replays.
 */
public final class MorphFireRenderer
{
    private static final Quaternionf TEMP_QUATERNION = new Quaternionf();

    private static ActorEntity proxy;

    private MorphFireRenderer()
    {}

    public static void render(class_4587 matrices, class_4597 consumers, IEntity morph, Form form, float tickDelta, class_4184 camera, boolean relative)
    {
        if (morph.getFireTicks() <= 0 || consumers == null)
        {
            return;
        }

        class_310 mc = class_310.method_1551();
        class_638 world = mc.field_1687;

        if (world == null)
        {
            return;
        }

        if (MorphFireRenderer.proxy == null || MorphFireRenderer.proxy.method_37908() != world)
        {
            MorphFireRenderer.proxy = new ActorEntity(BBSMod.ACTOR_ENTITY, world);
        }

        ActorEntity entity = MorphFireRenderer.proxy;
        float[] size = MorphFireRenderer.getFireDimensions(morph, form);
        class_4050 pose = morph.isSneaking() ? class_4050.field_18081 : class_4050.field_18076;

        entity.method_20803(morph.getFireTicks());
        entity.field_6012 = Math.max(entity.field_6012, morph.getAge());
        entity.method_18380(pose);
        entity.method_5660(morph.isSneaking());
        ((EntityAccessor) entity).bbs$setDimensions(class_4048.method_18385(size[0], size[1]));
        entity.method_18382();
        entity.method_23327(0D, 0D, 0D);
        entity.field_6038 = 0D;
        entity.field_5971 = 0D;
        entity.field_5989 = 0D;
        entity.field_6014 = 0D;
        entity.field_6036 = 0D;
        entity.field_5969 = 0D;
        entity.method_5648(false);

        float bodyYaw = Lerps.lerp(morph.getPrevBodyYaw(), morph.getBodyYaw(), tickDelta);
        class_898 dispatcher = mc.method_1561();
        boolean irisWorld = BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld();

        matrices.method_22903();

        if (irisWorld && !relative)
        {
            /* Iris bakes the terrain matrix into the stack; strip it and rebuild the
             * camera-relative entity transform the same way as ParticleFormRenderer. */
            Matrix4f composed = BBSRendering.stripTerrainPositionMatrix(new Matrix4f(matrices.method_23760().method_23761()));
            Matrix4f oriented = new Matrix4f(MatrixStackUtils.getInverseViewRotationMatrix());

            oriented.mul(composed);

            matrices.method_34426();
            matrices.method_34425(MatrixStackUtils.getViewRotationMatrix());
            MatrixStackUtils.multiply(matrices, oriented);
        }
        else if (relative)
        {
            matrices.method_22907(camera.method_23767().conjugate(MorphFireRenderer.TEMP_QUATERNION));
        }

        matrices.method_22907(class_7833.field_40716.rotation(MathUtils.toRad(bodyYaw)));

        ((EntityRendererDispatcherInvoker) dispatcher).bbs$renderFire(matrices, consumers, entity, dispatcher.method_24197());

        matrices.method_22909();

        entity.method_20803(0);
    }

    private static float[] getFireDimensions(IEntity morph, Form form)
    {
        if (form instanceof MobForm mobForm)
        {
            class_1299<?> type = class_7923.field_41177.method_10223(class_2960.method_60654(mobForm.mobID.get()));

            if (type != null)
            {
                class_4048 dimensions = type.method_18386();

                if (morph.isSneaking())
                {
                    dimensions = dimensions.method_18383(0.8F);
                }

                return new float[] {dimensions.comp_2185(), dimensions.comp_2186()};
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
            class_1297 mc = mcEntity.getMcEntity();

            return new float[] {mc.method_17681(), mc.method_17682()};
        }

        AABB hitbox = morph.getPickingHitbox();

        return new float[] {(float) hitbox.w, (float) hitbox.h};
    }
}
