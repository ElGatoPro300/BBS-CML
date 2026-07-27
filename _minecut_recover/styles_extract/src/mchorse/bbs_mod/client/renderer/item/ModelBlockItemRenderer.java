package mchorse.bbs_mod.client.renderer.item;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.blocks.entities.ModelProperties;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.pose.Transform;

import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.class_1799;
import net.minecraft.class_2338;
import net.minecraft.class_2487;
import net.minecraft.class_308;
import net.minecraft.class_310;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_811;
import net.minecraft.class_9279;
import net.minecraft.class_9334;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ModelBlockItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer
{
    private Map<class_1799, Item> map = new HashMap<>();

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
            item.entity.getProperties().update(item.formEntity);
            item.formEntity.update();
        }
    }

    @Override
    public void render(class_1799 stack, class_811 mode, class_4587 matrices, class_4597 vertexConsumers, int light, int overlay)
    {
        Item item = this.get(stack);

        if (item != null)
        {
            ModelProperties properties = item.entity.getProperties();
            Form form = properties.getForm(mode);

            if (form != null)
            {
                item.expiration = 20;

                Transform transform = properties.getTransform(mode);

                matrices.method_22903();
                matrices.method_46416(0.5F, 0F, 0.5F);
                MatrixStackUtils.applyTransform(matrices, transform);

                RenderSystem.enableDepthTest();

                if (mode == class_811.field_4317)
                {
                    Vector3f a = new Vector3f(0.85F, 0.85F, -1.0F).normalize();
                    Vector3f b = new Vector3f(-0.85F, 0.85F, 1.0F).normalize();
                    RenderSystem.setupGui3DDiffuseLighting(a, b);
                }

                FormUtilsClient.render(form, new FormRenderingContext()
                    .set(FormRenderType.fromModelMode(mode), item.formEntity, matrices, light, overlay, class_310.method_1551().method_60646().method_60637(false))
                    .camera(class_310.method_1551().field_1773.method_19418()));

                if (mode == class_811.field_4317)
                {
                    class_308.method_24210();
                }

                RenderSystem.disableDepthTest();

                matrices.method_22909();
            }
        }
    }

    public Item get(class_1799 stack)
    {
        if (stack == null || stack.method_7909() != BBSMod.MODEL_BLOCK_ITEM)
        {
            return null;
        }

        if (this.map.containsKey(stack))
        {
            return this.map.get(stack);
        }

        ModelBlockEntity entity = new ModelBlockEntity(class_2338.field_10980, BBSMod.MODEL_BLOCK.method_9564());
        Item item = new Item(entity);

        this.map.put(stack, item);

        class_9279 nbtComponent = stack.method_57824(class_9334.field_49611);
        if (nbtComponent == null)
        {
            return item;
        }

        class_2487 nbt = nbtComponent.method_57463();
        var world = class_310.method_1551().field_1687;
        if (world != null)
        {
            entity.method_11014(nbt, world.method_30349());
        }

        return item;
    }

    public static class Item
    {
        public ModelBlockEntity entity;
        public IEntity formEntity;
        public int expiration = 20;

        public Item(ModelBlockEntity entity)
        {
            this.entity = entity;
            this.formEntity = new StubEntity(class_310.method_1551().field_1687);
        }
    }
}
