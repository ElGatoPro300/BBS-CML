package mchorse.bbs_mod.client.renderer.entity;

import mchorse.bbs_mod.client.renderer.MorphFireRenderer;
import mchorse.bbs_mod.cubic.render.vanilla.ArmorRenderer;
import mchorse.bbs_mod.entity.ActorEntity;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_4050;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_5602;
import net.minecraft.class_5617;
import net.minecraft.class_7833;
import net.minecraft.class_8136;
import net.minecraft.class_897;
import net.minecraft.class_922;
import com.mojang.blaze3d.systems.RenderSystem;

public class ActorEntityRenderer extends class_897<ActorEntity>
{
    public static ArmorRenderer armorRenderer;

    public ActorEntityRenderer(class_5617.class_5618 ctx)
    {
        super(ctx);

        armorRenderer = new ArmorRenderer(
            new class_8136(ctx.method_32167(class_5602.field_27579)),
            new class_8136(ctx.method_32167(class_5602.field_27580)),
            ctx.method_48481()
        );

        this.field_4673 = 0.5F;
    }

    @Override
    public class_2960 getTexture(ActorEntity entity)
    {
        return class_2960.method_60654("minecraft:textures/entity/player/wide/steve.png");
    }

    @Override
    public void render(ActorEntity livingEntity, float yaw, float tickDelta, class_4587 matrices, class_4597 vertexConsumers, int light)
    {
        matrices.method_22903();

        float bodyYaw = class_3532.method_17821(tickDelta, livingEntity.field_6220, livingEntity.field_6283);
        int overlay = class_922.method_23622(livingEntity, 0F);

        this.setupTransforms(livingEntity, matrices, bodyYaw, tickDelta);

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        FormUtilsClient.render(livingEntity.getForm(), new FormRenderingContext()
            .set(FormRenderType.ENTITY, livingEntity.getEntity(), matrices, light, overlay, tickDelta)
            .camera(class_310.method_1551().field_1773.method_19418()));

        if (livingEntity.getEntity().getFireTicks() > 0)
        {
            MorphFireRenderer.render(
                matrices,
                vertexConsumers,
                livingEntity.getEntity(),
                livingEntity.getForm(),
                tickDelta,
                class_310.method_1551().field_1773.method_19418(),
                false
            );
        }

        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();

        matrices.method_22909();

        super.method_3936(livingEntity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    protected boolean isVisible(ActorEntity entity)
    {
        return !entity.method_5767();
    }

    protected void setupTransforms(ActorEntity entity, class_4587 matrices, float bodyYaw, float tickDelta)
    {
        if (!entity.method_41328(class_4050.field_18078))
        {
            matrices.method_22907(class_7833.field_40716.rotationDegrees(-bodyYaw));
        }

        if (entity.field_6213 > 0)
        {
            float deathAngle = (entity.field_6213 + tickDelta - 1F) / 20F * 1.6F;

            matrices.method_22907(class_7833.field_40718.rotationDegrees(Math.min(class_3532.method_15355(deathAngle), 1F) * 90F));
        }
    }
}