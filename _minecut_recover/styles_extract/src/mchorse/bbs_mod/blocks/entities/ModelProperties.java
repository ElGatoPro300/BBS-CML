package mchorse.bbs_mod.blocks.entities;

import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.class_1304;
import net.minecraft.class_1799;
import net.minecraft.class_811;

public class ModelProperties implements IMapSerializable
{
    private String name = "";

    private Form form;
    private Form formThirdPerson;
    private Form formInventory;
    private Form formFirstPerson;

    private final Transform transform = new Transform();
    private final Transform transformThirdPerson = new Transform();
    private final Transform transformInventory = new Transform();
    private final Transform transformFirstPerson = new Transform();
    private class_1799 itemMainHand = class_1799.field_8037;
    private class_1799 itemOffHand = class_1799.field_8037;
    private class_1799 armorHead = class_1799.field_8037;
    private class_1799 armorChest = class_1799.field_8037;
    private class_1799 armorLegs = class_1799.field_8037;
    private class_1799 armorFeet = class_1799.field_8037;

    private boolean enabled = true;
    private boolean global;
    private boolean shadow;
    private boolean hitbox;
    private boolean lookAt;
    /* When chroma sky hides terrain, this block still renders if true (or if the global setting is on). */
    private boolean chromaSky;
    private int lightLevel = 0;
    private float hardness;

    public Form getForm()
    {
        return this.form;
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name == null ? "" : name.trim();
    }

    protected Form processForm(Form form)
    {
        if (form != null)
        {
            form.playMain();
        }

        return form;
    }

    public void setForm(Form form)
    {
        this.form = this.processForm(form);
    }

    public Form getFormThirdPerson()
    {
        return this.formThirdPerson;
    }

    public void setFormThirdPerson(Form form)
    {
        this.formThirdPerson = this.processForm(form);
    }

    public Form getFormInventory()
    {
        return this.formInventory;
    }

    public void setFormInventory(Form form)
    {
        this.formInventory = this.processForm(form);
    }

    public Form getFormFirstPerson()
    {
        return this.formFirstPerson;
    }

    public void setFormFirstPerson(Form form)
    {
        this.formFirstPerson = this.processForm(form);
    }

    public Transform getTransform()
    {
        return this.transform;
    }

    public Transform getTransformThirdPerson()
    {
        return this.transformThirdPerson;
    }

    public Transform getTransformInventory()
    {
        return this.transformInventory;
    }

    public Transform getTransformFirstPerson()
    {
        return this.transformFirstPerson;
    }

    public class_1799 getItemMainHand()
    {
        return this.itemMainHand;
    }

    public void setItemMainHand(class_1799 itemMainHand)
    {
        this.itemMainHand = itemMainHand == null ? class_1799.field_8037 : itemMainHand.method_7972();
    }

    public class_1799 getItemOffHand()
    {
        return this.itemOffHand;
    }

    public void setItemOffHand(class_1799 itemOffHand)
    {
        this.itemOffHand = itemOffHand == null ? class_1799.field_8037 : itemOffHand.method_7972();
    }

    public class_1799 getArmorHead()
    {
        return this.armorHead;
    }

    public void setArmorHead(class_1799 armorHead)
    {
        this.armorHead = armorHead == null ? class_1799.field_8037 : armorHead.method_7972();
    }

    public class_1799 getArmorChest()
    {
        return this.armorChest;
    }

    public void setArmorChest(class_1799 armorChest)
    {
        this.armorChest = armorChest == null ? class_1799.field_8037 : armorChest.method_7972();
    }

    public class_1799 getArmorLegs()
    {
        return this.armorLegs;
    }

    public void setArmorLegs(class_1799 armorLegs)
    {
        this.armorLegs = armorLegs == null ? class_1799.field_8037 : armorLegs.method_7972();
    }

    public class_1799 getArmorFeet()
    {
        return this.armorFeet;
    }

    public void setArmorFeet(class_1799 armorFeet)
    {
        this.armorFeet = armorFeet == null ? class_1799.field_8037 : armorFeet.method_7972();
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public boolean isGlobal()
    {
        return this.global;
    }

    public void setGlobal(boolean global)
    {
        this.global = global;
    }

    public boolean isShadow()
    {
        return this.shadow;
    }

    public void setShadow(boolean shadow)
    {
        this.shadow = shadow;
    }

    public boolean isHitbox()
    {
        return this.hitbox;
    }

    public void setHitbox(boolean hitbox)
    {
        this.hitbox = hitbox;
    }

    public boolean isLookAt()
    {
        return this.lookAt;
    }

    public void setLookAt(boolean lookAt)
    {
        this.lookAt = lookAt;
    }

    public boolean isChromaSky()
    {
        return this.chromaSky;
    }

    public void setChromaSky(boolean chromaSky)
    {
        this.chromaSky = chromaSky;
    }

    public int getLightLevel()
    {
        return this.lightLevel;
    }

    public void setLightLevel(int level)
    {
        this.lightLevel = Math.max(0, Math.min(15, level));
    }

    public float getHardness()
    {
        return this.hardness;
    }

    public void setHardness(float hardness)
    {
        if (hardness < 0F)
        {
            hardness = 0F;
        }
        else if (hardness > 50F)
        {
            hardness = 50F;
        }

        this.hardness = hardness;
    }

    public Form getForm(class_811 mode)
    {
        Form form = this.form;

        if (mode == class_811.field_4317 && this.formInventory != null)
        {
            form = this.formInventory;
        }
        else if ((mode == class_811.field_4323 || mode == class_811.field_4320) && this.formThirdPerson != null)
        {
            form = this.formThirdPerson;
        }
        else if ((mode == class_811.field_4321 || mode == class_811.field_4322) && this.formFirstPerson != null)
        {
            form = this.formFirstPerson;
        }

        return form;
    }

    public Transform getTransform(class_811 mode)
    {
        Transform transform = this.transformThirdPerson;

        if (mode == class_811.field_4317)
        {
            transform = this.transformInventory;
        }
        else if (mode == class_811.field_4321 || mode == class_811.field_4322)
        {
            transform = this.transformFirstPerson;
        }
        else if (mode == class_811.field_4318)
        {
            transform = this.transform;
        }

        return transform;
    }

    @Override
    public void fromData(MapType data)
    {
        this.name = data.getString("name", "").trim();
        this.form = this.processForm(FormUtils.fromData(data.getMap("form")));
        this.formThirdPerson = this.processForm(FormUtils.fromData(data.getMap("formThirdPerson")));
        this.formInventory = this.processForm(FormUtils.fromData(data.getMap("formInventory")));
        this.formFirstPerson = this.processForm(FormUtils.fromData(data.getMap("formFirstPerson")));

        this.transform.fromData(data.getMap("transform"));
        this.transformThirdPerson.fromData(data.getMap("transformThirdPerson"));
        this.transformInventory.fromData(data.getMap("transformInventory"));
        this.transformFirstPerson.fromData(data.getMap("transformFirstPerson"));
        this.setItemMainHand(data.has("item_main_hand") ? KeyframeFactories.ITEM_STACK.fromData(data.get("item_main_hand")) : class_1799.field_8037);
        this.setItemOffHand(data.has("item_off_hand") ? KeyframeFactories.ITEM_STACK.fromData(data.get("item_off_hand")) : class_1799.field_8037);
        this.setArmorHead(data.has("item_head") ? KeyframeFactories.ITEM_STACK.fromData(data.get("item_head")) : class_1799.field_8037);
        this.setArmorChest(data.has("item_chest") ? KeyframeFactories.ITEM_STACK.fromData(data.get("item_chest")) : class_1799.field_8037);
        this.setArmorLegs(data.has("item_legs") ? KeyframeFactories.ITEM_STACK.fromData(data.get("item_legs")) : class_1799.field_8037);
        this.setArmorFeet(data.has("item_feet") ? KeyframeFactories.ITEM_STACK.fromData(data.get("item_feet")) : class_1799.field_8037);

        if (data.has("enabled")) this.enabled = data.getBool("enabled");
        this.shadow = data.getBool("shadow");
        this.global = data.getBool("global");
        this.lookAt = data.getBool("look_at");
        if (data.has("hitbox")) this.hitbox = data.getBool("hitbox");
        if (data.has("chroma_sky")) this.chromaSky = data.getBool("chroma_sky");
        if (data.has("light_level")) this.lightLevel = data.getInt("light_level");
        this.setHardness(data.getFloat("hardness", 0F));
    }

    @Override
    public void toData(MapType data)
    {
        data.putString("name", this.name);
        data.put("form", FormUtils.toData(this.form));
        data.put("formThirdPerson", FormUtils.toData(this.formThirdPerson));
        data.put("formInventory", FormUtils.toData(this.formInventory));
        data.put("formFirstPerson", FormUtils.toData(this.formFirstPerson));

        data.put("transform", this.transform.toData());
        data.put("transformThirdPerson", this.transformThirdPerson.toData());
        data.put("transformInventory", this.transformInventory.toData());
        data.put("transformFirstPerson", this.transformFirstPerson.toData());
        data.put("item_main_hand", KeyframeFactories.ITEM_STACK.toData(this.itemMainHand));
        data.put("item_off_hand", KeyframeFactories.ITEM_STACK.toData(this.itemOffHand));
        data.put("item_head", KeyframeFactories.ITEM_STACK.toData(this.armorHead));
        data.put("item_chest", KeyframeFactories.ITEM_STACK.toData(this.armorChest));
        data.put("item_legs", KeyframeFactories.ITEM_STACK.toData(this.armorLegs));
        data.put("item_feet", KeyframeFactories.ITEM_STACK.toData(this.armorFeet));

        data.putBool("enabled", this.enabled);
        data.putBool("shadow", this.shadow);
        data.putBool("global", this.global);
        data.putBool("hitbox", this.hitbox);
        data.putBool("look_at", this.lookAt);
        data.putBool("chroma_sky", this.chromaSky);
        data.putInt("light_level", this.lightLevel);
        data.putFloat("hardness", this.hardness);
    }

    public void update(IEntity entity)
    {
        entity.setEquipmentStack(class_1304.field_6173, this.itemMainHand.method_7972());
        entity.setEquipmentStack(class_1304.field_6171, this.itemOffHand.method_7972());
        entity.setEquipmentStack(class_1304.field_6169, this.armorHead.method_7972());
        entity.setEquipmentStack(class_1304.field_6174, this.armorChest.method_7972());
        entity.setEquipmentStack(class_1304.field_6172, this.armorLegs.method_7972());
        entity.setEquipmentStack(class_1304.field_6166, this.armorFeet.method_7972());

        if (this.form != null)
        {
            this.form.update(entity);
        }

        if (this.formThirdPerson != null)
        {
            this.formThirdPerson.update(entity);
        }

        if (this.formInventory != null)
        {
            this.formInventory.update(entity);
        }

        if (this.formFirstPerson != null)
        {
            this.formFirstPerson.update(entity);
        }
    }
}
