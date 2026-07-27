package mchorse.bbs_mod.selectors;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.forms.Form;
import net.minecraft.class_1309;
import net.minecraft.class_1937;
import net.minecraft.class_2487;
import net.minecraft.class_2520;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class SelectorOwner
{
    public IEntity entity;

    private Form form;
    private long check;
    private int nbtCheck;
    private class_2487 lastNbt;

    private class_1309 mcEntity;

    public SelectorOwner(class_1309 mcEntity)
    {
        this.mcEntity = mcEntity;
        this.entity = new MCEntity(mcEntity);
    }

    public Form getForm()
    {
        return form;
    }

    public void update()
    {
        class_1937 world = this.entity.getWorld();

        if (!world.field_9236)
        {
            return;
        }

        this.check();
        this.entity.update();

        if (this.form != null)
        {
            this.form.update(this.entity);
        }
    }

    public void check()
    {
        EntitySelectors selectors = BBSModClient.getSelectors();

        if (this.nbtCheck <= 0)
        {
            this.nbtCheck = 10;

            Set<String> keys = createWhitelist();
            class_2487 compound = this.mcEntity.method_5647(new class_2487());
            class_2487 newCompound = new class_2487();

            for (String key : keys)
            {
                class_2520 element = compound.method_10580(key);

                if (element != null)
                {
                    newCompound.method_10566(key, element);
                }
            }

            if (!Objects.equals(newCompound, this.lastNbt))
            {
                this.check = 0;
            }

            this.lastNbt = newCompound;
        }

        if (this.check < selectors.getLastUpdate())
        {
            this.check = selectors.getLastUpdate();

            EntitySelector selectorFor = selectors.getSelectorFor(this.mcEntity);

            if (selectorFor != null)
            {
                this.form = FormUtils.copy(selectorFor.form);

                if (this.form != null)
                {
                    this.form.playMain();
                }
            }
            else
            {
                this.form = null;
            }
        }

        this.nbtCheck -= 1;
    }

    private Set<String> createWhitelist()
    {
        HashSet<String> strings = new HashSet<>();
        String s = BBSSettings.entitySelectorsPropertyWhitelist.get();
        String[] split = s.split(",");

        for (String string : split)
        {
            strings.add(string.trim());
        }

        return strings;
    }
}