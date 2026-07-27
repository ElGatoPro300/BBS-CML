package mchorse.bbs_mod.blocks;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.network.ServerNetwork;
import net.minecraft.class_1264;
import net.minecraft.class_1269;
import net.minecraft.class_1657;
import net.minecraft.class_1750;
import net.minecraft.class_1799;
import net.minecraft.class_1922;
import net.minecraft.class_1937;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2343;
import net.minecraft.class_2371;
import net.minecraft.class_2464;
import net.minecraft.class_2586;
import net.minecraft.class_259;
import net.minecraft.class_2591;
import net.minecraft.class_265;
import net.minecraft.class_2680;
import net.minecraft.class_2689;
import net.minecraft.class_2741;
import net.minecraft.class_2758;
import net.minecraft.class_3222;
import net.minecraft.class_3610;
import net.minecraft.class_3612;
import net.minecraft.class_3726;
import net.minecraft.class_3737;
import net.minecraft.class_3965;
import net.minecraft.class_4538;
import net.minecraft.class_5558;
import net.minecraft.class_9275;
import net.minecraft.class_9279;
import net.minecraft.class_9334;
import org.joml.Vector3f;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

public class ModelBlock extends class_2248 implements class_2343, class_3737
{
    public static final class_2758 LIGHT_LEVEL = class_2758.method_11867("light_level", 0, 15);

    public static <E extends class_2586, A extends class_2586> class_5558<A> validateTicker(class_2591<A> givenType, class_2591<E> expectedType, class_5558<? super E> ticker)
    {
        return expectedType == givenType ? (class_5558<A>) ticker : null;
    }

    public ModelBlock(class_2251 settings)
    {
        super(settings);

        this.method_9590(method_9564()
            .method_11657(class_2741.field_12508, false)
            .method_11657(LIGHT_LEVEL, 0));
    }

    @Override
    protected void method_9515(class_2689.class_2690<class_2248, class_2680> builder)
    {
        builder.method_11667(class_2741.field_12508, LIGHT_LEVEL);
    }

    @Nullable
    @Override
    public class_2680 method_9605(class_1750 ctx)
    {
        return this.method_9564()
            .method_11657(class_2741.field_12508, ctx.method_8045().method_8316(ctx.method_8037()).method_39360(class_3612.field_15910));
    }

    @Override
    public class_1799 method_9574(class_4538 world, class_2338 pos, class_2680 state)
    {
        class_2586 entity = world.method_8321(pos);

        if (entity instanceof ModelBlockEntity modelBlock)
        {
            class_1799 stack = new class_1799(this);
            stack.method_57379(class_9334.field_49611, class_9279.method_57456(modelBlock.method_38243(world.method_30349())));
            
            stack.method_57379(class_9334.field_49623, new class_9275(Map.of("light_level", String.valueOf(modelBlock.getProperties().getLightLevel()))));

            return stack;
        }

        return super.method_9574(world, pos, state);
    }

    @Override
    public class_2464 method_9604(class_2680 state)
    {
        return class_2464.field_11455;
    }

    @Override
    public boolean method_9579(class_2680 state, class_1922 world, class_2338 pos)
    {
        return true;
    }

    @Nullable
    @Override
    public <T extends class_2586> class_5558<T> method_31645(class_1937 world, class_2680 state, class_2591<T> type)
    {
        return validateTicker(type, BBSMod.MODEL_BLOCK_ENTITY, (w, p, s, e) -> ModelBlockEntity.tick(w, p, s, e));
    }

    @Nullable
    @Override
    public class_2586 method_10123(class_2338 pos, class_2680 state)
    {
        return new ModelBlockEntity(pos, state);
    }

    @Override
    public class_265 method_9530(class_2680 state, class_1922 world, class_2338 pos, class_3726 context)
    {
        /* Keep the 1×1 cell clickable even when collision is empty (e.g. structure solid hitbox). */
        return class_259.method_1077();
    }

    @Override
    public class_265 method_9549(class_2680 state, class_1922 world, class_2338 pos, class_3726 context)
    {
        try
        {
            if (world instanceof class_1937 w)
            {
                class_2586 be = w.method_8321(pos);

                if (be instanceof ModelBlockEntity model)
                {
                    /* Solid structure/model hitbox uses injected multi-block shapes — avoid a wrong 1×1 cube. */
                    if (mchorse.bbs_mod.forms.structure.ModelBlockSolidCollisions.hasSolidFormHitbox(model))
                    {
                        return class_259.method_1073();
                    }

                    if (model.getProperties().isHitbox())
                    {
                        Form form = model.getProperties().getForm();

                        if (form != null && form.hitbox.get())
                        {
                            float width = form.hitboxWidth.get();
                            float height = form.hitboxHeight.get();

                            if (width > 0F && height > 0F)
                            {
                                float halfWidth = width / 2F;

                                double minX = 0.5D - halfWidth;
                                double maxX = 0.5D + halfWidth;
                                double minZ = 0.5D - halfWidth;
                                double maxZ = 0.5D + halfWidth;
                                double minY = 0D;
                                double maxY = height;

                                minX = Math.max(0D, minX);
                                minZ = Math.max(0D, minZ);
                                maxX = Math.min(1D, maxX);
                                maxZ = Math.min(1D, maxZ);
                                maxY = Math.min(1D, maxY);

                                if (minX < maxX && minZ < maxZ && maxY > minY)
                                {
                                    return class_259.method_1081(minX, minY, minZ, maxX, maxY, maxZ);
                                }
                            }
                        }

                        return class_259.method_1077();
                    }
                }
            }
        }
        catch (Exception e)
        {

        }

        return class_259.method_1073();
    }

    @Override
    public class_1269 method_55766(class_2680 state, class_1937 world, class_2338 pos, class_1657 player, class_3965 hit)
    {
        if (player instanceof class_3222 serverPlayer)
        {
            ServerNetwork.sendClickedModelBlock(serverPlayer, pos);
        }

        return class_1269.field_5812;
    }

    /* Waterloggable implementation */

    @Override
    public class_3610 method_9545(class_2680 state)
    {
        return state.method_11654(class_2741.field_12508) ? class_3612.field_15910.method_15729(false) : super.method_9545(state);
    }

    @Override
    public void method_9556(class_1937 world, class_1657 player, class_2338 pos, class_2680 state, class_2586 be, class_1799 tool)
    {
        if (!world.field_9236 && !player.method_31549().field_7477)
        {
            if (be instanceof ModelBlockEntity model)
            {
                class_1799 stack = new class_1799(this);
                stack.method_57379(class_9334.field_49611, class_9279.method_57456(model.method_38243(world.method_30349())));
                
                stack.method_57379(class_9334.field_49623, new class_9275(Map.of("light_level", String.valueOf(model.getProperties().getLightLevel()))));

                class_1264.method_17349(world, pos, class_2371.method_10213(1, stack));
            }
        }

        super.method_9556(world, player, pos, state, be, tool);
    }
}
