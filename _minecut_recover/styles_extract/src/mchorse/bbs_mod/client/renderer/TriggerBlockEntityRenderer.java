package mchorse.bbs_mod.client.renderer;

import mchorse.bbs_mod.blocks.entities.TriggerBlockEntity;
import mchorse.bbs_mod.graphics.Draw;
import net.minecraft.class_238;
import net.minecraft.class_310;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_5614;
import net.minecraft.class_827;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.HashSet;
import java.util.Set;

public class TriggerBlockEntityRenderer implements class_827<TriggerBlockEntity>
{
    public static final Set<TriggerBlockEntity> capturedTriggerBlocks = new HashSet<>();

    public TriggerBlockEntityRenderer(class_5614.class_5615 ctx)
    {}

    @Override
    public void render(TriggerBlockEntity entity, float tickDelta, class_4587 matrices, class_4597 vertexConsumers, int light, int overlay)
    {
        capturedTriggerBlocks.add(entity);

        class_310 mc = class_310.method_1551();
        
        if (mc.method_53526().method_53536())
        {
            matrices.method_22903();
            matrices.method_22904(0.5D, 0, 0.5D);
            /* Render green debug box for triggers */
            Draw.renderBox(matrices, -0.5D, 0, -0.5D, 1, 1, 1, 0, 1F, 0.5F, 0.5F);
            matrices.method_22909();

            if (entity.region.get())
            {
                class_238 box = entity.getRegionBoxRelative();

                /* Render white debug box for region triggers */
                RenderSystem.disableDepthTest();
                Draw.renderBox(matrices, box.field_1323, box.field_1322, box.field_1321, box.field_1320 - box.field_1323, box.field_1325 - box.field_1322, box.field_1324 - box.field_1321, 1F, 1F, 1F, 0.5F);
                RenderSystem.enableDepthTest();
            }
        }
    }
}
