package mchorse.bbs_mod.client.renderer.entity;

import mchorse.bbs_mod.entity.GunProjectileEntity;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.items.GunProperties;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.interps.Lerps;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_4608;
import net.minecraft.class_5617;
import net.minecraft.class_7833;
import net.minecraft.class_897;
import com.mojang.blaze3d.systems.RenderSystem;

public class GunProjectileEntityRenderer extends class_897<GunProjectileEntity>
{
    public GunProjectileEntityRenderer(class_5617.class_5618 ctx)
    {
        super(ctx);
    }

    @Override
    public class_2960 getTexture(GunProjectileEntity entity)
    {
        return class_2960.method_60655("minecraft", "textures/entity/player/wide/steve.png");
    }

    @Override
    public void render(GunProjectileEntity projectile, float yaw, float tickDelta, class_4587 matrices, class_4597 vertexConsumers, int light)
    {
        matrices.method_22903();

        GunProperties properties = projectile.getProperties();
        int out = properties.lifeSpan - 2;

        float bodyYaw = class_3532.method_17821(tickDelta, projectile.field_5982, projectile.method_36454());
        float pitch = class_3532.method_17821(tickDelta, projectile.field_6004, projectile.method_36455());
        float scale = Lerps.envelope(projectile.field_6012 + tickDelta, 0, properties.fadeIn, out - properties.fadeOut, out);

        if (properties.yaw) matrices.method_22907(class_7833.field_40716.rotationDegrees(bodyYaw));
        if (properties.pitch) matrices.method_22907(class_7833.field_40714.rotationDegrees(-pitch));
        matrices.method_22905(scale, scale, scale);
        MatrixStackUtils.applyTransform(matrices, properties.projectileTransform);

        RenderSystem.enableDepthTest();
        FormUtilsClient.render(projectile.getForm(), new FormRenderingContext()
            .set(FormRenderType.ENTITY, projectile.getEntity(), matrices, light, class_4608.field_21444, tickDelta)
            .camera(class_310.method_1551().field_1773.method_19418()));
        RenderSystem.disableDepthTest();

        matrices.method_22909();

        super.method_3936(projectile, yaw, tickDelta, matrices, vertexConsumers, light);
    }
}