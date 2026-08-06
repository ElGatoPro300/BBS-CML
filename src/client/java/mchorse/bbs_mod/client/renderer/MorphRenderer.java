package mchorse.bbs_mod.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.selectors.ISelectorOwnerProvider;
import mchorse.bbs_mod.selectors.SelectorOwner;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.morphing.UIMorphingPanel;
import mchorse.bbs_mod.utils.interps.Lerps;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector3f;

public class MorphRenderer
{
    public static boolean hidePlayer = false;

    public static boolean renderPlayer(AbstractClientPlayerEntity player, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i)
    {
        if (hidePlayer)
        {
            if (FormUtilsClient.getCurrentForm() instanceof MobForm form && !form.isPlayer())
            {
                return true;
            }
        }

        Morph morph = Morph.getMorph(player);

        if (morph != null && morph.getForm() != null)
        {
            if (canRender())
            {
                RenderSystem.enableDepthTest();

                Vector3f a = new Vector3f(0.85F, 0.85F, -1F).normalize();
                Vector3f b = new Vector3f(-0.85F, 0.85F, 1F).normalize();
                RenderSystem.setupLevelDiffuseLighting(a, b);

                float bodyYaw = Lerps.lerp(player.prevBodyYaw, player.bodyYaw, g);
                int overlay = LivingEntityRenderer.getOverlay(player, 0F);

                matrixStack.push();
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-bodyYaw));

                FormUtilsClient.render(morph.getForm(), new FormRenderingContext()
                    .set(FormRenderType.ENTITY, morph.entity, matrixStack, i, overlay, g)
                    .camera(MinecraftClient.getInstance().gameRenderer.getCamera()));

                matrixStack.pop();

                RenderSystem.disableDepthTest();
            }

            return true;
        }

        return false;
    }

    private static boolean canRender()
    {
        UIBaseMenu menu = UIScreen.getCurrentMenu();
        
        if (menu instanceof UIDashboard dashboard)
        {
            UIDashboardPanel panel = dashboard.getPanels().panel;

            if (panel instanceof UIMorphingPanel morphingPanel)
            {
                return !morphingPanel.palette.editor.isEditing();
            }
        }

        return true;
    }

    public static boolean renderLivingEntity(LivingEntity livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, int o)
    {
        if (!(livingEntity instanceof ISelectorOwnerProvider))
        {
            return false;
        }

        SelectorOwner owner = ((ISelectorOwnerProvider) livingEntity).getOwner();

        owner.check();

        Form form = owner.getForm();

        if (form != null)
        {
            RenderSystem.enableDepthTest();

            float bodyYaw = Lerps.lerp(livingEntity.prevBodyYaw, livingEntity.bodyYaw, g);

            matrixStack.push();
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-bodyYaw));

            FormUtilsClient.render(form, new FormRenderingContext()
                .set(FormRenderType.ENTITY, owner.entity, matrixStack, i, o, g)
                .camera(MinecraftClient.getInstance().gameRenderer.getCamera()));

            matrixStack.pop();

            RenderSystem.disableDepthTest();

            return true;
        }

        return false;
    }
}