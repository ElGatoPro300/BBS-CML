package mchorse.bbs_mod.selectors;

import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.utils.StringUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.TagValueOutput;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.Objects;

public class EntitySelector implements IMapSerializable
{
    public boolean enabled = true;
    public Form form;
    public Identifier entity;
    public String name = "";
    public CompoundTag nbt;

    public boolean matches(LivingEntity mcEntity)
    {
        if (!this.enabled)
        {
            return false;
        }

        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(mcEntity.getType());

        if (!id.equals(this.entity))
        {
            return false;
        }

        Component displayName = mcEntity.getDisplayName();

        if (this.nbt != null)
        {
            TagValueOutput view = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, mcEntity.level().registryAccess());
            mcEntity.saveWithoutId(view);
            CompoundTag entityCompound = view.buildResult();

            if (!this.compare(this.nbt, entityCompound))
            {
                return false;
            }
        }

        if (displayName != null && !this.name.isEmpty())
        {
            String a = StringUtils.plainText(displayName.getVisualOrderText());

            return Objects.equals(a, this.name);
        }

        return true;
    }

    private boolean compare(CompoundTag source, CompoundTag base)
    {
        for (String key : source.keySet())
        {
            Tag a = source.get(key);
            Tag b = base.get(key);

            if (a instanceof CompoundTag aCompound && b instanceof CompoundTag bCompound)
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
        if (data.has("entity")) this.entity = Identifier.parse(data.getString("entity"));
        if (data.has("name")) this.name = data.getString("name");
        if (data.has("nbt"))
        {
            try
            {
                this.nbt = TagParser.parseCompoundFully(data.getString("nbt"));
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
