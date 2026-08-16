package mchorse.bbs_mod.utils.iris;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;

import net.irisshaders.iris.helpers.EntityState;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.uniforms.CapturedRenderingState;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.trim.ArmorTrim;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import it.unimi.dsi.fastutil.objects.Object2IntFunction;

/**
 * Mirrors Iris {@code MixinHumanoidArmorLayer}: bake entity/item IDs into armor vertices
 * via {@link CapturedRenderingState} while ModelForm / ModelBlock armor tessellates.
 * <p>
 * Do <b>not</b> wrap buffers with {@code EntityRenderStateShard} here. That calls
 * {@code GbufferPrograms.beginEntities()} during isolated {@code Immediate.draw()},
 * outside {@code EntityRenderDispatcher}'s MatrixStack pairing, and leaves WorldRenderer
 * with a non-empty pose stack (crash: "Pose stack not empty").
 */
public final class IrisEntityArmorContext
{
    private IrisEntityArmorContext()
    {}

    public static boolean isActive()
    {
        return IrisUtils.isShaderPackEnabled()
            && BBSRendering.isRenderingWorld()
            && !BBSRendering.isIrisShadowPass();
    }

    /** Identity — phase wrapping is unsafe outside the entity dispatcher (see class javadoc). */
    public static VertexConsumerProvider wrapEntityBuffers(VertexConsumerProvider consumers)
    {
        return consumers;
    }

    public static int resolveEntityId(IEntity entity)
    {
        Object2IntFunction<NamespacedId> entityIds = WorldRenderingSettings.INSTANCE.getEntityIds();

        if (entityIds == null)
        {
            return 0;
        }

        if (entity instanceof MCEntity mc)
        {
            Entity mcEntity = mc.getMcEntity();

            if (mcEntity != null)
            {
                Identifier id = Registries.ENTITY_TYPE.getId(mcEntity.getType());

                return entityIds.applyAsInt(new NamespacedId(id.getNamespace(), id.getPath()));
            }
        }

        /* StubEntity / unknown — player materials match Complementary armor IPBR best. */
        return entityIds.applyAsInt(new NamespacedId("minecraft", "player"));
    }

    public static int resolveItemId(Item item)
    {
        Object2IntFunction<NamespacedId> itemIds = WorldRenderingSettings.INSTANCE.getItemIds();

        if (itemIds == null || item == null)
        {
            return 0;
        }

        Identifier id = Registries.ITEM.getId(item);

        return itemIds.applyAsInt(new NamespacedId(id.getNamespace(), id.getPath()));
    }

    public static int resolveTrimItemId(ArmorTrim trim)
    {
        Object2IntFunction<NamespacedId> itemIds = WorldRenderingSettings.INSTANCE.getItemIds();

        if (itemIds == null || trim == null)
        {
            return 0;
        }

        String asset = trim.getMaterial().value().assetName();

        return itemIds.applyAsInt(new NamespacedId("minecraft", "trim_" + asset));
    }

    public static Scope beginArmorPiece(IEntity entity, Item item)
    {
        if (!isActive())
        {
            return Scope.INACTIVE;
        }

        CapturedRenderingState state = CapturedRenderingState.INSTANCE;
        int prevEntity = state.getCurrentRenderedEntity();
        int prevItem = state.getCurrentRenderedItem();
        boolean setEntity = prevEntity <= 0;

        if (setEntity)
        {
            state.setCurrentEntity(resolveEntityId(entity));
        }

        state.setCurrentRenderedItem(resolveItemId(item));

        return new Scope(prevEntity, prevItem, setEntity, false);
    }

    public static void beginTrim(ArmorTrim trim)
    {
        if (!isActive() || trim == null)
        {
            return;
        }

        EntityState.interposeItemId(resolveTrimItemId(trim));
    }

    public static void endTrim()
    {
        if (!isActive())
        {
            return;
        }

        EntityState.restoreItemId();
    }

    public static final class Scope implements AutoCloseable
    {
        private static final Scope INACTIVE = new Scope(0, 0, false, true);

        private final int prevEntity;
        private final int prevItem;
        private final boolean restoreEntity;
        private final boolean inactive;

        private Scope(int prevEntity, int prevItem, boolean restoreEntity, boolean inactive)
        {
            this.prevEntity = prevEntity;
            this.prevItem = prevItem;
            this.restoreEntity = restoreEntity;
            this.inactive = inactive;
        }

        @Override
        public void close()
        {
            if (this.inactive)
            {
                return;
            }

            CapturedRenderingState state = CapturedRenderingState.INSTANCE;

            state.setCurrentRenderedItem(this.prevItem);

            if (this.restoreEntity)
            {
                state.setCurrentEntity(this.prevEntity);
            }

            EntityState.restoreItemId();
        }
    }
}
