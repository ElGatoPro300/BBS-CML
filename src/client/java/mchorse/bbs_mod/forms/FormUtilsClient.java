package mchorse.bbs_mod.forms;

import mchorse.bbs_mod.client.BBSRendering;
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
import mchorse.bbs_mod.forms.renderers.utils.RecolorVertexConsumer;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.TridentEntityModel;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Util;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.Stack;
import java.util.function.Function;

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
                VertexConsumerProvider.immediate(new BufferAllocator(512 * 1024))
            );
        }

        return customVertexConsumerProvider;
    }

    /**
     * Trident/shield/etc. use vanilla {@code ModelPart} + Sodium's entity vertex path.
     * That only works on the world entity Immediate (the setup before f3e3a39).
     * Do not {@code draw()} this — that flush with the lightmap off is what turned
     * vanilla enchanted armor black. WorldRenderer draws it later with lightmap on.
     */
    public static VertexConsumerProvider tintWorldEntityConsumers(Function<VertexConsumer, VertexConsumer> substitute)
    {
        VertexConsumerProvider.Immediate world = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

        if (substitute == null)
        {
            return world;
        }

        return (layer) ->
        {
            VertexConsumer buffer = world.getBuffer(layer);
            VertexConsumer apply = substitute.apply(buffer);

            return apply != null ? apply : buffer;
        };
    }

    /**
     * Same special case as {@code ItemRenderer.renderItem}: trident is a 2D inventory
     * model but the third-person renderer uses the builtin entity mesh.
     */
    public static boolean usesBuiltinItemRenderer(ItemStack stack, ModelTransformationMode mode)
    {
        if (stack == null || stack.isEmpty())
        {
            return false;
        }

        if (stack.isOf(Items.TRIDENT))
        {
            return mode != ModelTransformationMode.GUI
                && mode != ModelTransformationMode.GROUND
                && mode != ModelTransformationMode.FIXED;
        }

        return MinecraftClient.getInstance().getItemRenderer().getModel(stack, null, null, 0).isBuiltin();
    }

    /**
     * Piglin/etc. MobForms draw through a private Immediate. Builtin held items
     * (trident) must still use the world entity Immediate with the Sodium-safe
     * tint wrapper — same path as {@link ModelFormRenderer} player-form items.
     * Do not {@code draw()} the world side.
     */
    public static VertexConsumerProvider deferBuiltinItemLayers(VertexConsumerProvider body)
    {
        VertexConsumerProvider world = FormUtilsClient.tintWorldEntityConsumers(
            BBSRendering.getColorConsumer(Color.white())
        );

        return (layer) ->
        {
            if (FormUtilsClient.isDeferredBuiltinItemLayer(layer))
            {
                return world.getBuffer(layer);
            }

            return body.getBuffer(layer);
        };
    }

    /**
     * Swap MobForm held-item consumers onto {@link #tintWorldEntityConsumers}
     * for builtin meshes (trident/shield). Call {@link #clearBuiltinItemTint}
     * after the item draw.
     */
    public static VertexConsumerProvider routeMobFormBuiltinItemConsumers(ItemStack stack, ModelTransformationMode mode, VertexConsumerProvider fallback)
    {
        if (fallback == null || !BBSRendering.isRenderingWorld())
        {
            return fallback;
        }

        if (!(FormUtilsClient.getCurrentForm() instanceof MobForm))
        {
            return fallback;
        }

        if (!FormUtilsClient.usesBuiltinItemRenderer(stack, mode))
        {
            return fallback;
        }

        return FormUtilsClient.tintWorldEntityConsumers(BBSRendering.getColorConsumer(Color.white()));
    }

    public static void clearBuiltinItemTint()
    {
        RecolorVertexConsumer.newColor = null;
        RecolorVertexConsumer.newPaintColor = null;
    }

    /**
     * Trident/shield {@code ModelPart} layers. Iris/Sodium often wrap the layer
     * so {@code toString()} is no longer exactly {@code entity_solid}.
     */
    public static boolean isDeferredBuiltinItemLayer(RenderLayer layer)
    {
        if (layer == null)
        {
            return false;
        }

        if (layer == RenderLayer.getEntitySolid(TridentEntityModel.TEXTURE))
        {
            return true;
        }

        String name = layer.toString();

        if (name == null || name.isEmpty())
        {
            return false;
        }

        String lower = name.toLowerCase();

        if (lower.contains("entity_solid"))
        {
            return true;
        }

        /* Keep armor glint on the mob Immediate; only item glint is deferred. */
        if (lower.contains("armor"))
        {
            return false;
        }

        return lower.contains("entity_glint")
            || lower.contains("glint_direct")
            || lower.contains("glint_translucent")
            || "glint".equals(lower);
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
                VertexConsumerProvider.immediate(new BufferAllocator(512 * 1024))
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

    public static interface IFormRendererFactory <T extends Form>
    {
        public FormRenderer<T> create(T form);
    }
}
