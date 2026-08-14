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

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.TridentEntityModel;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Util;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Stack;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

public class FormUtilsClient
{
    private static Map<Class, IFormRendererFactory> map = new HashMap<>();
    private static CustomVertexConsumerProvider customVertexConsumerProvider;
    /** Isolated Immediate for MobForm morph draws (clothing / held items). */
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
     * Isolated Immediate for form/item/armor/block draws — same idea as original BBS.
     * <p>
     * Must NOT wrap {@code getEntityVertexConsumers()}: {@code draw()} would flush
     * pending vanilla entity layers (enchanted armor) while the form has the lightmap
     * off. Writing builtin meshes (trident) into the world Immediate instead makes
     * Iris draw a second, scaled copy. Pre-allocate entity/glint layers so
     * {@code ModelPart} can switch solid→glint without flushing mid-mesh.
     */
    public static CustomVertexConsumerProvider getProvider()
    {
        if (customVertexConsumerProvider == null)
        {
            customVertexConsumerProvider = FormUtilsClient.createIsolatedProvider();
        }

        return customVertexConsumerProvider;
    }

    /**
     * Isolated Immediate for MobForm morph geometry (villager clothing, piglin body,
     * held items). Separate from {@link #getProvider()} so clothing flushes do not mix
     * with form-item batches.
     */
    public static CustomVertexConsumerProvider getMobMorphProvider()
    {
        if (mobMorphVertexConsumerProvider == null)
        {
            mobMorphVertexConsumerProvider = FormUtilsClient.createIsolatedProvider();
        }

        return mobMorphVertexConsumerProvider;
    }

    /**
     * Original BBS layer map, plus the glint layers vanilla keeps on the entity
     * Immediate and the trident solid layer (per-texture, not in the atlas map).
     */
    private static CustomVertexConsumerProvider createIsolatedProvider()
    {
        SequencedMap<RenderLayer, BufferAllocator> layers = Util.make(new Object2ObjectLinkedOpenHashMap<>(), map ->
        {
            map.put(TexturedRenderLayers.getEntitySolid(), new BufferAllocator(RenderLayer.getSolid().getExpectedBufferSize()));
            map.put(TexturedRenderLayers.getEntityCutout(), new BufferAllocator(RenderLayer.getCutout().getExpectedBufferSize()));
            map.put(TexturedRenderLayers.getBannerPatterns(), new BufferAllocator(RenderLayer.getCutoutMipped().getExpectedBufferSize()));
            map.put(TexturedRenderLayers.getEntityTranslucentCull(), new BufferAllocator(RenderLayer.getTranslucent().getExpectedBufferSize()));
            FormUtilsClient.assignBuffer(map, RenderLayer.getSolid());
            FormUtilsClient.assignBuffer(map, RenderLayer.getCutout());
            FormUtilsClient.assignBuffer(map, RenderLayer.getTranslucent());
            FormUtilsClient.assignBuffer(map, RenderLayer.getCutoutMipped());
            FormUtilsClient.assignBuffer(map, TexturedRenderLayers.getShieldPatterns());
            FormUtilsClient.assignBuffer(map, TexturedRenderLayers.getBeds());
            FormUtilsClient.assignBuffer(map, TexturedRenderLayers.getShulkerBoxes());
            FormUtilsClient.assignBuffer(map, TexturedRenderLayers.getSign());
            FormUtilsClient.assignBuffer(map, TexturedRenderLayers.getHangingSign());
            map.put(TexturedRenderLayers.getChest(), new BufferAllocator(786432));
            FormUtilsClient.assignBuffer(map, RenderLayer.getArmorEntityGlint());
            FormUtilsClient.assignBuffer(map, RenderLayer.getGlint());
            FormUtilsClient.assignBuffer(map, RenderLayer.getGlintTranslucent());
            FormUtilsClient.assignBuffer(map, RenderLayer.getEntityGlint());
            FormUtilsClient.assignBuffer(map, RenderLayer.getDirectEntityGlint());
            FormUtilsClient.assignBuffer(map, RenderLayer.getWaterMask());
            FormUtilsClient.assignBuffer(map, RenderLayer.getEntitySolid(TridentEntityModel.TEXTURE));
            ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.forEach((layer) -> FormUtilsClient.assignBuffer(map, layer));
        });

        return new CustomVertexConsumerProvider(
            VertexConsumerProvider.immediate(layers, new BufferAllocator(512 * 1024))
        );
    }

    private static void assignBuffer(SequencedMap<RenderLayer, BufferAllocator> storage, RenderLayer layer)
    {
        storage.put(layer, new BufferAllocator(layer.getExpectedBufferSize()));
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

    public static interface IFormRendererFactory <T extends Form>
    {
        public FormRenderer<T> create(T form);
    }
}
