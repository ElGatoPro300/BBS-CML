package mchorse.bbs_mod.client.renderer.item;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.renderer.LightTexture;
import mchorse.bbs_mod.client.renderer.item.GunItemRenderer;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.items.GunProperties;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.model_blocks.UIModelBlockEditorMenu;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.pose.Transform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;

import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

public class GunItemRenderer implements SpecialModelRenderer<ItemStack>
{
    private Map<ItemStack, Item> map = new HashMap<>();

    public void update()
    {
        Iterator<Item> it = this.map.values().iterator();

        while (it.hasNext())
        {
            Item item = it.next();

            if (item.expiration <= 0)
            {
                it.remove();
            }

            item.expiration -= 1;
            item.properties.update(item.formEntity);
            item.formEntity.update();
        }
    }

    @Override
    public ItemStack extractArgument(ItemStack stack)
    {
        return stack;
    }

    @Override
    public void getExtents(Consumer<Vector3fc> consumer)
    {
        float minX = -0.5F;
        float maxX = 1.5F;
        float minY = -0.5F;
        float maxY = 1.5F;
        float minZ = -0.5F;
        float maxZ = 1.5F;

        consumer.accept(new Vector3f(minX, minY, minZ));
        consumer.accept(new Vector3f(maxX, minY, minZ));
        consumer.accept(new Vector3f(minX, maxY, minZ));
        consumer.accept(new Vector3f(maxX, maxY, minZ));
        consumer.accept(new Vector3f(minX, minY, maxZ));
        consumer.accept(new Vector3f(maxX, minY, maxZ));
        consumer.accept(new Vector3f(minX, maxY, maxZ));
        consumer.accept(new Vector3f(maxX, maxY, maxZ));
    }

    @Override
    public void submit(ItemStack data, PoseStack matrices, SubmitNodeCollector queue, int light, int overlay, boolean hasGlint, int outlineColor)
    {
        Item item = this.get(data);

        if (item != null)
        {
            ItemDisplayContext mode = ItemDisplayContextTracker.resolve();
            GunProperties properties = item.properties;
            Form form = properties.getForm(mode);
            Transform transform = properties.getTransform(mode);
            boolean zoom = mode.firstPerson() && BBSModClient.getGunZoom() != null && properties.getZoomForm() != null;

            if (zoom)
            {
                form = properties.getZoomForm();
                transform = properties.zoomTransform;
            }

            /* Preview zoom form */
            if (UIScreen.getCurrentMenu() instanceof UIModelBlockEditorMenu editorMenu && editorMenu.currentSection == editorMenu.sectionZoom)
            {
                form = editorMenu.getGunProperties().getZoomForm();
                transform = editorMenu.getGunProperties().zoomTransform;
            }

            if (form != null)
            {
                item.expiration = 20;

                matrices.pushPose();
                matrices.translate(0.5F, 0F, 0.5F);
                MatrixStackUtils.applyTransform(matrices, transform);

                BBSRendering.enableDepthTest();

                try
                {
                    int renderLight = light;

                    FormUtilsClient.render(form, new FormRenderingContext()
                        .set(FormRenderType.fromModelMode(mode), item.formEntity, matrices, renderLight, overlay, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false))
                        .camera(Minecraft.getInstance().gameRenderer.getMainCamera()));
                }
                finally
                {
                    BBSRendering.setShaderColor(1F, 1F, 1F, 1F);
                    BBSRendering.disableDepthTest();
                }
                matrices.popPose();
            }
        }
    }

    public Item get(ItemStack stack)
    {
        if (stack == null || stack.getItem() != BBSMod.GUN_ITEM)
        {
            return null;
        }

        if (this.map.containsKey(stack))
        {
            return this.map.get(stack);
        }

        Item item = new Item(GunProperties.get(stack));

        this.map.put(stack, item);

        return item;
    }

    public static class Unbaked implements SpecialModelRenderer.Unbaked<ItemStack>
    {
        public static final MapCodec<GunItemRenderer.Unbaked> CODEC = MapCodec.unit(new GunItemRenderer.Unbaked());

        @Override
        public MapCodec<GunItemRenderer.Unbaked> type()
        {
            return CODEC;
        }

        @Override
        public SpecialModelRenderer<ItemStack> bake(SpecialModelRenderer.BakingContext context)
        {
            return BBSModClient.getGunItemRenderer();
        }
    }

    public static class Item
    {
        public GunProperties properties;
        public IEntity formEntity;
        public int expiration = 20;

        public Item(GunProperties properties)
        {
            this.properties = properties;
            this.formEntity = new StubEntity(Minecraft.getInstance().level);
        }
    }
}
