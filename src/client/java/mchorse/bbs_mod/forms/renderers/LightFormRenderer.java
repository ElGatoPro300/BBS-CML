package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.LightForm;
import mchorse.bbs_mod.ui.framework.UIContext;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;

import com.mojang.blaze3d.vertex.PoseStack;

import java.util.Map;

public class LightFormRenderer extends FormRenderer<LightForm>
{
    private final ItemStack stack;

    public LightFormRenderer(LightForm form)
    {
        super(form);
        this.stack = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecraft", "light")));
    }

    @Override
    protected void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        // context.batcher.getContext().draw();

        int level = Math.max(0, Math.min(15, this.form.level.get()));
        ItemStack stack = this.stack.copy();

        if (!stack.isEmpty())
        {
            stack.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(Map.of("level", Integer.toString(level))));
        }

        if (stack.isEmpty())
        {
            return;
        }

        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        PoseStack matrices = new PoseStack();

        float cellW = x2 - x1;
        float cellH = y2 - y1;
        float scale = Math.min(cellW, cellH) / 16F * 0.8F * this.form.uiScale.get();
        float centerX = x1 + cellW / 2F;
        float centerY = y1 + cellH / 2F;

        matrices.pushPose();
        matrices.translate(centerX, centerY, 0F);
        matrices.scale(scale, scale, 1F);

        consumers.setUI(true);
        context.batcher.getContext().drawItem(stack, -8, -8);
        context.batcher.getContext().drawStackOverlay(context.batcher.getFont().getRenderer(), stack, -8, -8);
        consumers.setUI(false);
        matrices.popPose();
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
    }
}
