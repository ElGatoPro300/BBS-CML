package mchorse.bbs_mod.cubic.render.vanilla;

import mchorse.bbs_mod.cubic.model.ArmorType;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.renderers.utils.RecolorVertexConsumer;
import mchorse.bbs_mod.utils.colors.Color;
import net.minecraft.class_1058;
import net.minecraft.class_1059;
import net.minecraft.class_1092;
import net.minecraft.class_1304;
import net.minecraft.class_1738;
import net.minecraft.class_1741;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1921;
import net.minecraft.class_2960;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import net.minecraft.class_4608;
import net.minecraft.class_4722;
import net.minecraft.class_572;
import net.minecraft.class_630;
import net.minecraft.class_6880;
import net.minecraft.class_8053;
import net.minecraft.class_9282;
import net.minecraft.class_9334;
import com.google.common.collect.Maps;

import java.util.Map;

public class ArmorRenderer
{
    private static final Map<String, class_2960> ARMOR_TEXTURE_CACHE = Maps.newHashMap();
    private final class_572 innerModel;
    private final class_572 outerModel;
    private final class_1059 armorTrimsAtlas;

    public ArmorRenderer(class_572 innerModel, class_572 outerModel, class_1092 bakery)
    {
        this.innerModel = innerModel;
        this.outerModel = outerModel;
        this.armorTrimsAtlas = bakery.method_24153(class_4722.field_42071);
    }

    public void renderArmorSlot(class_4587 matrices, class_4597 vertexConsumers, IEntity entity, class_1304 armorSlot, ArmorType type, int light)
    {
        class_1799 itemStack = entity.getEquipmentStack(armorSlot);
        class_1792 item = itemStack.method_7909();

        if (item instanceof class_1738 armorItem)
        {
            if (armorItem.method_7685() == armorSlot)
            {
                boolean innerModel = this.usesInnerModel(armorSlot);
                class_572 bipedModel = this.getModel(armorSlot);
                class_630 part = this.getPart(bipedModel, type);

                bipedModel.method_2805(true);

                part.field_3657 = part.field_3656 = part.field_3655 = 0F;
                part.field_3654 = part.field_3675 = part.field_3674 = 0F;
                part.field_37938 = part.field_37939 = part.field_37940 = 1F;

                class_9282 dyed = itemStack.method_57824(class_9334.field_49644);
                if (dyed != null)
                {
                    int color = dyed.comp_2384();
                    float r = (float)(color >> 16 & 255) / 255.0F;
                    float g = (float)(color >> 8 & 255) / 255.0F;
                    float b = (float)(color & 255) / 255.0F;

                    this.renderArmorParts(part, matrices, vertexConsumers, light, armorItem, innerModel, r, g, b, null);
                    this.renderArmorParts(part, matrices, vertexConsumers, light, armorItem, innerModel, 1F, 1F, 1F, "overlay");
                }
                else
                {
                    this.renderArmorParts(part, matrices, vertexConsumers, light, armorItem, innerModel, 1F, 1F, 1F, null);
                }

                class_8053 trim = itemStack.method_57824(class_9334.field_49607);
                if (trim != null)
                {
                    this.renderTrim(part, armorItem.method_7686(), matrices, vertexConsumers, light, trim, innerModel);
                }

                if (itemStack.method_7958())
                {
                    this.renderGlint(part, matrices, vertexConsumers, light);
                }
            }
        }
    }

    private class_630 getPart(class_572 bipedModel, ArmorType type)
    {
        switch (type)
        {
            case HELMET -> {
                return bipedModel.field_3398;
            }
            case CHEST, LEGGINGS -> {
                return bipedModel.field_3391;
            }
            case LEFT_ARM -> {
                return bipedModel.field_27433;
            }
            case RIGHT_ARM -> {
                return bipedModel.field_3401;
            }
            case LEFT_LEG, LEFT_BOOT -> {
                return bipedModel.field_3397;
            }
            case RIGHT_LEG, RIGHT_BOOT -> {
                return bipedModel.field_3392;
            }
        }

        return bipedModel.field_3398;
    }

    private void renderArmorParts(class_630 part, class_4587 matrices, class_4597 vertexConsumers, int light, class_1738 item, boolean secondTextureLayer, float red, float green, float blue, String overlay)
    {
        class_4588 base = vertexConsumers.getBuffer(class_1921.method_25448(this.getArmorTexture(item, secondTextureLayer, overlay)));
        class_4588 vertexConsumer = new RecolorVertexConsumer(base, new Color(red, green, blue, 1F));

        part.method_22698(matrices, vertexConsumer, light, class_4608.field_21444);
    }

    private void renderTrim(class_630 part, class_6880<class_1741> material, class_4587 matrices, class_4597 vertexConsumers, int light, class_8053 trim, boolean leggings)
    {
        class_1058 sprite = this.armorTrimsAtlas.method_4608(leggings ? trim.method_48434(material) : trim.method_48436(material));
        class_4588 vertexConsumer = sprite.method_24108(vertexConsumers.getBuffer(class_4722.method_48480(trim.method_48424().comp_349().comp_1905())));

        part.method_22698(matrices, vertexConsumer, light, class_4608.field_21444);
    }

    private void renderGlint(class_630 part, class_4587 matrices, class_4597 vertexConsumers, int light)
    {
        part.method_22698(matrices, vertexConsumers.getBuffer(class_1921.method_27949()), light, class_4608.field_21444);
    }

    private class_572 getModel(class_1304 slot)
    {
        return this.usesInnerModel(slot) ? this.innerModel : this.outerModel;
    }

    private boolean usesInnerModel(class_1304 slot)
    {
        return slot == class_1304.field_6172;
    }

    private class_2960 getArmorTexture(class_1738 item, boolean secondLayer, String overlay)
    {
        String materialName = item.method_7686().method_40230().map(k -> k.method_29177().method_12832()).orElse("unknown");
        String id = "textures/models/armor/" + materialName + "_layer_" + (secondLayer ? 2 : 1) + (overlay == null ? "" : "_" + overlay) + ".png";

        class_2960 found = ARMOR_TEXTURE_CACHE.get(id);
        if (found == null)
        {
            found = class_2960.method_60655("minecraft", id);
            ARMOR_TEXTURE_CACHE.put(id, found);
        }

        return found;
    }
}