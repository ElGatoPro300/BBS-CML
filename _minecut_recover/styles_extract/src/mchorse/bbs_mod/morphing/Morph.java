package mchorse.bbs_mod.morphing;

import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.utils.RayTracing;
import net.minecraft.class_1297;
import net.minecraft.class_1299;
import net.minecraft.class_1657;
import net.minecraft.class_239;
import net.minecraft.class_2487;
import net.minecraft.class_2520;
import net.minecraft.class_3966;
import net.minecraft.class_5321;
import net.minecraft.class_7923;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Morph
{
    public static final List<IEntityCaptureHandler> HANDLERS = new ArrayList<>();

    private Form form;
    public final MCEntity entity;

    public static Form getMobForm(class_1657 player)
    {
        class_239 hitResult = RayTracing.rayTraceEntity(player, player.method_37908(), player.method_33571(), player.method_5720(), 64);

        if (hitResult.method_17783() == class_239.class_240.field_1331)
        {
            class_1297 target = ((class_3966) hitResult).method_17782();

            return captureFormFromEntity(player, target);
        }

        return null;
    }

    public static Form captureFormFromEntity(class_1657 player, class_1297 target)
    {
        if (target == null || target == player)
        {
            return null;
        }

        for (IEntityCaptureHandler handler : HANDLERS)
        {
            Form form = handler.capture(player, target);

            if (form != null)
            {
                return form;
            }
        }

        Optional<class_5321<class_1299<?>>> key = class_7923.field_41177.method_29113(target.method_5864());

        if (key.isPresent())
        {
            MobForm form = new MobForm();
            class_2487 compound = target.method_5647(new class_2487());

            for (String s : Arrays.asList("Pos", "Motion", "Rotation", "FallDistance", "Fire", "Air", "OnGround", "Invulnerable", "PortalCooldown", "UUID"))
            {
                compound.method_10551(s);
            }

            form.mobID.set(key.get().method_29177().toString());
            form.mobNBT.set(compound.toString());

            return form;
        }

        return null;
    }

    public static Morph getMorph(class_1297 entity)
    {
        if (entity instanceof IMorphProvider provider)
        {
            return provider.getMorph();
        }

        return null;
    }

    public Morph(class_1297 entity)
    {
        this.entity = new MCEntity(entity);
    }

    public Form getForm()
    {
        return this.form;
    }

    public void setForm(Form form)
    {
        if (form == null && this.form != null && this.entity.getMcEntity() instanceof class_1657 player)
        {
            this.form.onDemorph(player);
        }

        this.form = form;

        if (this.form != null && this.entity.getMcEntity() instanceof class_1657 player)
        {
            this.form.onMorph(player);
            this.form.playMain();
        }

        this.entity.getMcEntity().method_18382();
    }

    public void update()
    {
        this.entity.update();

        if (this.form != null)
        {
            this.form.update(this.entity);
        }
    }

    public class_2520 toNbt()
    {
        class_2487 compound = new class_2487();

        if (this.form != null)
        {
            compound.method_10566("Form", DataStorageUtils.toNbt(FormUtils.toData(this.form)));
        }

        return compound;
    }

    public void fromNbt(class_2487 compound)
    {
        if (compound.method_10545("Form"))
        {
            MapType map = (MapType) DataStorageUtils.fromNbt(compound.method_10562("Form"));

            this.form = FormUtils.fromData(map);
        }
    }
}