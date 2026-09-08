package mchorse.bbs_mod.client.renderer.item;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.blocks.entities.ModelProperties;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.renderer.item.ModelBlockItemRenderer;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.pose.Transform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;

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

public class ModelBlockItemRenderer implements SpecialModelRenderer<ItemStack>
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
            item.entity.getProperties().update(item.formEntity);
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
        float minY = 0F;
        float maxY = 2.5F;
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
    public void render(ItemStack stack, ItemDisplayContext mode, PoseStack matrices, SubmitNodeCollector queue, int light, int overlay, boolean hasGlint, int outlineColor)
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

                matrices.pushPose();
                matrices.translate(0.5F, 0F, 0.5F);
                MatrixStackUtils.applyTransform(matrices, transform);

                BBSRendering.enableDepthTest();

                try
                {
                    if (mode == ItemDisplayContext.GUI)
                    {
                        BBSRendering.depthMask(true);
                        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
                        Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
                    }

                    int renderLight = mode == ItemDisplayContext.GUI ? LightTexture.FULL_BRIGHT : light;

                    FormUtilsClient.render(form, new FormRenderingContext()
                        .set(FormRenderType.fromModelMode(mode), item.formEntity, matrices, renderLight, overlay, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false))
                        .camera(Minecraft.getInstance().gameRenderer.getMainCamera()));
                }
                finally
                {
                    if (mode == ItemDisplayContext.GUI)
                    {
                        Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_FLAT);
                        BBSRendering.restoreAfterGuiItemForm();
                        BBSRendering.depthMask(true);
                        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
                    }
                    else
                    {
                        BBSRendering.setShaderColor(1F, 1F, 1F, 1F);
                    }

                    BBSRendering.disableDepthTest();
                }
                matrices.popPose();
            }
        }
    }

    public Item get(ItemStack stack)
    {
        if (stack == null || stack.getItem() != BBSMod.MODEL_BLOCK_ITEM)
        {
            return null;
        }

        if (this.map.containsKey(stack))
        {
            return this.map.get(stack);
        }

        ModelBlockEntity entity = new ModelBlockEntity(BlockPos.ZERO, BBSMod.MODEL_BLOCK.getDefaultState());
        Item item = new Item(entity);

        this.map.put(stack, item);

        var nbtComponent = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (nbtComponent == null)
        {
            return item;
        }

        CompoundTag nbt = nbtComponent.copyTagWithoutId();
        var world = Minecraft.getInstance().level;
        if (world != null)
        {
            entity.read(TagValueInput.create(ProblemReporter.DISCARDING, world.registryAccess(), nbt));
        }

        return item;
    }

    public static class Unbaked implements SpecialModelRenderer.Unbaked
    {
        public static final MapCodec<ModelBlockItemRenderer.Unbaked> CODEC = MapCodec.unit(new ModelBlockItemRenderer.Unbaked());

        @Override
        public MapCodec<ModelBlockItemRenderer.Unbaked> type()
        {
            return CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context)
        {
            return BBSModClient.getModelBlockItemRenderer();
        }
    }

    public static class Item
    {
        public ModelBlockEntity entity;
        public IEntity formEntity;
        public int expiration = 20;

        public Item(ModelBlockEntity entity)
        {
            this.entity = entity;
            this.formEntity = new StubEntity(Minecraft.getInstance().level);
        }
    }
}
