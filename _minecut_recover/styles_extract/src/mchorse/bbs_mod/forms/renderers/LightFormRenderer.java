package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.LightForm;
import mchorse.bbs_mod.ui.framework.UIContext;
import net.minecraft.class_1799;
import net.minecraft.class_2960;
import net.minecraft.class_4587;
import net.minecraft.class_7923;
import net.minecraft.class_9275;
import net.minecraft.class_9334;
import java.util.Map;

public class LightFormRenderer extends FormRenderer<LightForm>
{
    private final class_1799 stack;

    public LightFormRenderer(LightForm form)
    {
        super(form);
        this.stack = new class_1799(class_7923.field_41178.method_10223(class_2960.method_60655("minecraft", "light")));
    }

    @Override
    protected void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        context.batcher.getContext().method_51452();

        int level = Math.max(0, Math.min(15, this.form.level.get()));
        class_1799 stack = this.stack.method_7972();

        if (!stack.method_7960())
        {
            stack.method_57379(class_9334.field_49623, new class_9275(Map.of("level", Integer.toString(level))));
        }

        if (stack.method_7960())
        {
            return;
        }

        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        class_4587 matrices = context.batcher.getContext().method_51448();

        float cellW = x2 - x1;
        float cellH = y2 - y1;
        float scale = Math.min(cellW, cellH) / 16F * 0.8F * this.form.uiScale.get();
        float centerX = x1 + cellW / 2F;
        float centerY = y1 + cellH / 2F;

        matrices.method_22903();
        matrices.method_46416(centerX, centerY, 0F);
        matrices.method_22905(scale, scale, 1F);

        consumers.setUI(true);
        context.batcher.getContext().method_51427(stack, -8, -8);
        context.batcher.getContext().method_51431(context.batcher.getFont().getRenderer(), stack, -8, -8);
        consumers.setUI(false);
        matrices.method_22909();
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
    }
}
