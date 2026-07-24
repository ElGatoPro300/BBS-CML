package mchorse.bbs_mod.forms;

import mchorse.bbs_mod.forms.forms.AnchorForm;
import mchorse.bbs_mod.forms.forms.BillboardForm;
import mchorse.bbs_mod.forms.forms.BlockForm;
import mchorse.bbs_mod.forms.forms.ExtrudedForm;
import mchorse.bbs_mod.forms.forms.FluidForm;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.FramebufferForm;
import mchorse.bbs_mod.forms.forms.ItemForm;
import mchorse.bbs_mod.forms.forms.LabelForm;
import mchorse.bbs_mod.forms.forms.LightForm;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.forms.ParticleForm;
import mchorse.bbs_mod.forms.forms.ShapeForm;
import mchorse.bbs_mod.forms.forms.StructureForm;
import mchorse.bbs_mod.forms.forms.TrailForm;
import mchorse.bbs_mod.forms.forms.VanillaParticleForm;
import mchorse.bbs_mod.forms.renderers.AnchorFormRenderer;
import mchorse.bbs_mod.forms.renderers.BillboardFormRenderer;
import mchorse.bbs_mod.forms.renderers.BlockFormRenderer;
import mchorse.bbs_mod.forms.renderers.ExtrudedFormRenderer;
import mchorse.bbs_mod.forms.renderers.FluidFormRenderer;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.forms.renderers.FramebufferFormRenderer;
import mchorse.bbs_mod.forms.renderers.ItemFormRenderer;
import mchorse.bbs_mod.forms.renderers.LabelFormRenderer;
import mchorse.bbs_mod.forms.renderers.LightFormRenderer;
import mchorse.bbs_mod.forms.renderers.MobFormRenderer;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.forms.renderers.ParticleFormRenderer;
import mchorse.bbs_mod.forms.renderers.ShapeFormRenderer;
import mchorse.bbs_mod.forms.renderers.StructureFormRenderer;
import mchorse.bbs_mod.forms.renderers.TrailFormRenderer;
import mchorse.bbs_mod.forms.renderers.VanillaParticleFormRenderer;
import mchorse.bbs_mod.ui.framework.UIContext;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Util;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.Stack;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

public class FormUtilsClient
{
    private static Map<Class, IFormRendererFactory> map = new HashMap<>();
    private static CustomVertexConsumerProvider customVertexConsumerProvider;
    private static Stack<Form> currentForm = new Stack<>();
    private static final ThreadLocal<Boolean> UI_PREVIEW_ANIMATE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    static
    {
        register(BillboardForm.class, BillboardFormRenderer::new);
        register(FluidForm.class, FluidFormRenderer::new);
        register(ExtrudedForm.class, ExtrudedFormRenderer::new);
        register(LabelForm.class, LabelFormRenderer::new);
        register(ModelForm.class, ModelFormRenderer::new);
        register(ParticleForm.class, ParticleFormRenderer::new);
        register(BlockForm.class, BlockFormRenderer::new);
        register(ItemForm.class, ItemFormRenderer::new);
        register(AnchorForm.class, AnchorFormRenderer::new);
        register(MobForm.class, MobFormRenderer::new);
        register(VanillaParticleForm.class, VanillaParticleFormRenderer::new);
        register(TrailForm.class, TrailFormRenderer::new);
        register(FramebufferForm.class, FramebufferFormRenderer::new);
        register(StructureForm.class, StructureFormRenderer::new);
        register(ShapeForm.class, ShapeFormRenderer::new);
        register(LightForm.class, LightFormRenderer::new);
    }

    public static CustomVertexConsumerProvider getProvider()
    {
        if (customVertexConsumerProvider == null)
        {
            customVertexConsumerProvider = new CustomVertexConsumerProvider(MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers());
        }

        return customVertexConsumerProvider;
    }

    public static <T extends Form> void register(Class<T> clazz, IFormRendererFactory<T> function)
    {
        map.put(clazz, function);
    }

    public static Form getCurrentForm()
    {
        return currentForm.isEmpty() ? null : currentForm.peek();
    }

    public static FormRenderer getRenderer(Form form)
    {
        if (form == null)
        {
            return null;
        }

        if (form.getRenderer() instanceof FormRenderer renderer)
        {
            return renderer;
        }

        IFormRendererFactory factory = map.get(form.getClass());

        if (factory != null)
        {
            FormRenderer formRenderer = factory.create(form);

            form.setRenderer(formRenderer);

            return formRenderer;
        }

        return null;
    }

    public static boolean isUIPreviewAnimating()
    {
        return Boolean.TRUE.equals(UI_PREVIEW_ANIMATE.get());
    }

    public static void renderUI(Form form, UIContext context, int x1, int y1, int x2, int y2)
    {
        /* List / morph thumbnails default to a frozen pose (no idle). Pass true to animate. */
        renderUI(form, context, x1, y1, x2, y2, false);
    }

    public static void renderUI(Form form, UIContext context, int x1, int y1, int x2, int y2, boolean animate)
    {
        FormRenderer renderer = getRenderer(form);

        if (renderer != null)
        {
            UI_PREVIEW_ANIMATE.set(animate);

            try
            {
                renderer.renderUI(context, x1, y1, x2, y2);
            }
            finally
            {
                UI_PREVIEW_ANIMATE.set(Boolean.FALSE);
            }
        }
    }

    /**
     * Cached variant of {@link #renderUI} for list thumbnails and HUD overlays.
     * Always renders a static pose into the cache (mouse orbit still updates via angle buckets).
     */
    public static void renderUICached(Form form, UIContext context, int x1, int y1, int x2, int y2)
    {
        FormUIPreviewCache.render(form, context, x1, y1, x2, y2, true);
    }

    /**
     * Cached thumbnail at a fixed orbit angle — for category-card previews that must not
     * refill on every mouse move.
     */
    public static void renderUICachedStatic(Form form, UIContext context, int x1, int y1, int x2, int y2)
    {
        FormUIPreviewCache.render(form, context, x1, y1, x2, y2, false);
    }

    public static void render(Form form, FormRenderingContext context)
    {
        FormRenderer renderer = getRenderer(form);

        if (renderer != null)
        {
            currentForm.push(form);

            try
            {
                renderer.render(context);
            }
            catch (Exception e)
            {}

            currentForm.pop();
        }
    }

    public static List<String> getBones(Form form)
    {
        FormRenderer renderer = getRenderer(form);

        if (renderer != null)
        {
            return renderer.getBones();
        }

        return Collections.emptyList();
    }

    public static interface IFormRendererFactory <T extends Form>
    {
        public FormRenderer<T> create(T form);
    }
}
