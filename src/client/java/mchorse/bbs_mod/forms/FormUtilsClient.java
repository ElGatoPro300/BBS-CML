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
import mchorse.bbs_mod.forms.renderers.FormIllusionRenderer;
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
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.item.ItemStack;
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
    /** Isolated Immediate for MobForm morph draws — avoids flushing world entity leftovers. */
    private static CustomVertexConsumerProvider mobMorphVertexConsumerProvider;
    private static Stack<Form> currentForm = new Stack<>();
    /** Guards against recursive illusion copies spawning more illusions. */
    private static int illusionDepth;

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

    /**
     * Private Immediate for form/item/armor/block draws.
     * <p>
     * Must NOT wrap {@code getEntityVertexConsumers()}: model blocks and forms call
     * {@link CustomVertexConsumerProvider#draw()} after {@code ModelForm} turns the lightmap
     * off, which would flush pending vanilla entity layers (enchanted armor + glint) black
     * and z-fighting. MobForm world morphs already use {@link #getMobMorphProvider()}.
     */
    public static CustomVertexConsumerProvider getProvider()
    {
        if (customVertexConsumerProvider == null)
        {
            customVertexConsumerProvider = new CustomVertexConsumerProvider(
                VertexConsumerProvider.immediate(new BufferBuilder(512 * 1024))
            );
        }

        return customVertexConsumerProvider;
    }

    /**
     * Private Immediate for MobForm morph geometry. Villager clothing uses several dynamic
     * cutout layers; flushing them on the shared world Immediate mixed in leftover entity
     * layers and deferred the last clothing pass past held-item/shadow with bad lighting.
     */
    public static CustomVertexConsumerProvider getMobMorphProvider()
    {
        if (mobMorphVertexConsumerProvider == null)
        {
            mobMorphVertexConsumerProvider = new CustomVertexConsumerProvider(
                VertexConsumerProvider.immediate(new BufferBuilder(2048))
            );
        }

        return mobMorphVertexConsumerProvider;
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

    public static void renderUI(Form form, UIContext context, int x1, int y1, int x2, int y2)
    {
        FormRenderer renderer = getRenderer(form);

        if (renderer != null)
        {
            renderer.renderUI(context, x1, y1, x2, y2);
        }
    }

    /**
     * Cached variant of {@link #renderUI} for list thumbnails and HUD overlays.
     */
    public static void renderUICached(Form form, UIContext context, int x1, int y1, int x2, int y2)
    {
        FormUIPreviewCache.render(form, context, x1, y1, x2, y2);
    }

    public static void render(Form form, FormRenderingContext context)
    {
        render(form, context, null);
    }

    /**
     * Renders a form and, at the outermost call, any configured illusions.
     * {@code extras} carries film-only delay hooks (replay property ticks).
     */
    public static void render(Form form, FormRenderingContext context, FormIllusionRenderer.Extras extras)
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

            if (illusionDepth == 0)
            {
                illusionDepth++;

                try
                {
                    FormIllusionRenderer.render(form, context, extras);
                }
                finally
                {
                    illusionDepth--;
                }
            }
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

    public static void renderUI(Form form, UIContext context, int x1, int y1, int x2, int y2, boolean allowCached)
    {
        if (allowCached)
        {
            renderUICached(form, context, x1, y1, x2, y2);
        }
        else
        {
            renderUI(form, context, x1, y1, x2, y2);
        }
    }

    public static void renderUICachedStatic(Form form, UIContext context, int x1, int y1, int x2, int y2)
    {
        renderUICached(form, context, x1, y1, x2, y2);
    }

    public static boolean isUIPreviewAnimate()
    {
        return false;
    }

    public static boolean isMobFormEquipmentLayer(RenderLayer layer)
    {
        return false;
    }

    public static boolean shouldFlushMobFormFeatureLayers()
    {
        return false;
    }

    public static void flushMobFormFeatureLayers(VertexConsumerProvider consumers)
    {
    }

    public static VertexConsumerProvider routeMobFormBuiltinItemConsumers(ItemStack stack, ModelTransformationMode mode, VertexConsumerProvider consumers)
    {
        return consumers;
    }

    public static interface IFormRendererFactory <T extends Form>
    {
        public FormRenderer<T> create(T form);
    }
}
