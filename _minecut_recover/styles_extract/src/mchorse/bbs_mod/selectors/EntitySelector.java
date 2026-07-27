package mchorse.bbs_mod.selectors;

import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.utils.StringUtils;
import net.minecraft.class_1309;
import net.minecraft.class_2487;
import net.minecraft.class_2520;
import net.minecraft.class_2522;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_7923;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.Objects;

public class EntitySelector implements IMapSerializable
{
    public boolean enabled = true;
    public Form form;
    public class_2960 entity;
    public String name = "";
    public class_2487 nbt;

    public boolean matches(class_1309 mcEntity)
    {
        if (!this.enabled)
        {
            return false;
        }

        class_2960 id = class_7923.field_41177.method_10221(mcEntity.method_5864());

        if (!id.equals(this.entity))
        {
            return false;
        }

        class_2561 displayName = mcEntity.method_5476();

        if (this.nbt != null)
        {
            class_2487 entityCompound = mcEntity.method_5647(new class_2487());

            if (!this.compare(this.nbt, entityCompound))
            {
                return false;
            }
        }

        if (displayName != null && !this.name.isEmpty())
        {
            String a = StringUtils.plainText(displayName.method_30937());

            return Objects.equals(a, this.name);
        }

        return true;
    }

    private boolean compare(class_2487 source, class_2487 base)
    {
        for (String key : source.method_10541())
        {
            class_2520 a = source.method_10580(key);
            class_2520 b = base.method_10580(key);

            if (a instanceof class_2487 aCompound && b instanceof class_2487 bCompound)
            {
                return this.compare(aCompound, bCompound);
            }
            else if (!Objects.equals(a, b))
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public void fromData(MapType data)
    {
        this.nbt = null;

        if (data.has("enabled")) this.enabled = data.getBool("enabled");
        if (data.has("form")) this.form = FormUtils.fromData(data.getMap("form"));
        if (data.has("entity")) this.entity = class_2960.method_60654(data.getString("entity"));
        if (data.has("name")) this.name = data.getString("name");
        if (data.has("nbt"))
        {
            try
            {
                this.nbt = (new class_2522(new StringReader(data.getString("nbt")))).method_10727();
            }
            catch (CommandSyntaxException e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void toData(MapType data)
    {
        data.putBool("enabled", this.enabled);
        if (this.form != null) data.put("form", FormUtils.toData(this.form));
        if (this.entity != null) data.putString("entity", this.entity.toString());
        if (!this.name.isEmpty()) data.putString("name", this.name);
        if (this.nbt != null) data.putString("nbt", this.nbt.toString());
    }
}