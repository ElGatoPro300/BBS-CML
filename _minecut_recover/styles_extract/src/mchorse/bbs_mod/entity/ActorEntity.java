package mchorse.bbs_mod.entity;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.network.ServerNetwork;
import net.minecraft.class_1282;
import net.minecraft.class_1297;
import net.minecraft.class_1299;
import net.minecraft.class_1304;
import net.minecraft.class_1306;
import net.minecraft.class_1309;
import net.minecraft.class_1542;
import net.minecraft.class_1799;
import net.minecraft.class_1937;
import net.minecraft.class_238;
import net.minecraft.class_2487;
import net.minecraft.class_2509;
import net.minecraft.class_2520;
import net.minecraft.class_2775;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import net.minecraft.class_4048;
import net.minecraft.class_4050;
import net.minecraft.class_5132;
import net.minecraft.class_5134;
import net.minecraft.class_6903;
import net.minecraft.class_7225;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ActorEntity extends class_1309 implements IEntityFormProvider
{
    public static class_5132.class_5133 createActorAttributes()
    {
        return class_1309.method_26827()
            .method_26868(class_5134.field_23721, 1D)
            .method_26868(class_5134.field_23719, 0.1D)
            .method_26867(class_5134.field_23723)
            .method_26867(class_5134.field_23726);
    }

    private boolean despawn;
    private MCEntity entity = new MCEntity(this);
    private Form form;

    private Map<class_1304, class_1799> equipment = new HashMap<>();

    private boolean lastHitboxEnabled;
    private float lastHitboxWidth = Float.NaN;
    private float lastHitboxHeight = Float.NaN;
    private float lastHitboxSneakMultiplier = Float.NaN;
    private boolean lastSneaking;

    /* Film and replay data for item drops */
    private Film film;
    private Replay replay;
    private int currentTick;
    private boolean replayItemsDropped;
    
    /* Runtime inventory for replay actors (initial inventory + picked up items) */
    private final List<class_1799> runtimeInventory = new ArrayList<>();
    private boolean runtimeInventoryInitialized;
    private final Set<UUID> pickedUpEntityIds = new HashSet<>();

    public ActorEntity(class_1299<? extends class_1309> entityType, class_1937 world)
    {
        super(entityType, world);
    }

    /**
     * Set the film and replay associated with this actor for item dropping on death
     */
    public void setReplayData(Film film, Replay replay, int tick)
    {
        this.film = film;
        this.replay = replay;
        this.currentTick = tick;
        this.initializeRuntimeInventory();
    }
    
    /**
     * Update the current tick for accurate item retrieval
     */
    public void updateTick(int tick)
    {
        this.currentTick = tick;
    }

    private void initializeRuntimeInventory()
    {
        this.runtimeInventory.clear();
        this.pickedUpEntityIds.clear();

        if (this.replay != null && this.replay.inventory != null)
        {
            for (class_1799 stack : this.replay.inventory.getStacks())
            {
                this.runtimeInventory.add(stack == null ? class_1799.field_8037 : stack.method_7972());
            }
        }

        this.runtimeInventoryInitialized = true;
    }

    public MCEntity getEntity()
    {
        return this.entity;
    }

    @Override
    public int getEntityId()
    {
        return this.method_5628();
    }

    @Override
    public Form getForm()
    {
        return this.form;
    }

    @Override
    public void setForm(Form form)
    {
        Form lastForm = this.form;

        this.form = form;

        if (!this.method_37908().method_8608())
        {
            if (lastForm != null) lastForm.onDemorph(this);
            if (form != null) form.onMorph(this);
        }
        
        this.updateHitboxDimensions();
    }

    @Override
    public boolean method_30948()
    {
        return this.form != null && this.form.hitbox.get();
    }

    @Override
    public boolean method_5810()
    {
        return this.form == null || !this.form.hitbox.get();
    }

    @Override
    public void method_5697(class_1297 entity)
    {
        if (this.form == null || !this.form.hitbox.get())
        {
            super.method_5697(entity);
        }
    }

    @Override
    public void method_6087(class_1297 entity)
    {
        if (this.form == null || !this.form.hitbox.get())
        {
            super.method_6087(entity);
        }
    }

    @Override
    public boolean method_5640(double distance)
    {
        double d = this.method_5829().method_995();

        if (Double.isNaN(d))
        {
            d = 1D;
        }

        return distance < (d * 256D) * (d * 256D);
    }

    @Override
    public Iterable<class_1799> method_5877()
    {
        return List.of(this.method_6118(class_1304.field_6173), this.method_6118(class_1304.field_6171));
    }

    @Override
    public Iterable<class_1799> method_5661()
    {
        return List.of(this.method_6118(class_1304.field_6166), this.method_6118(class_1304.field_6172), this.method_6118(class_1304.field_6174), this.method_6118(class_1304.field_6169));
    }

    @Override
    public class_1799 method_6118(class_1304 slot)
    {
        return this.equipment.getOrDefault(slot, class_1799.field_8037);
    }

    @Override
    public void method_5673(class_1304 slot, class_1799 stack)
    {
        this.equipment.put(slot, stack == null ? class_1799.field_8037 : stack);
    }

    @Override
    public class_1306 method_6068()
    {
        return class_1306.field_6183;
    }

    @Override
    public void method_5773()
    {
        super.method_5773();

        this.method_6119();
        this.updateHitboxDimensions();

        if (this.form != null)
        {
            this.form.update(this.entity);
        }

        if (this.method_37908().field_9236)
        {
            return;
        }

        /* Don't pickup items when dead */
        if (this.method_29504())
        {
            return;
        }

        /* Pickup items */
        class_238 box = this.method_5829().method_1009(1D, 0.5D, 1D);
        List<class_1297> list = this.method_37908().method_8335(this, box);

        for (class_1297 entity : list)
        {
            if (entity instanceof class_1542 itemEntity)
            {
                UUID entityId = itemEntity.method_5667();
                class_1799 itemStack = itemEntity.method_6983();
                int i = itemStack.method_7947();

                if (!entity.method_31481() && !itemEntity.method_6977() && !this.pickedUpEntityIds.contains(entityId))
                {
                    this.pickedUpEntityIds.add(entityId);
                    this.addToRuntimeInventory(itemStack.method_7972());
                    
                    ((class_3218) this.method_37908()).method_14178().method_18754(entity, new class_2775(entity.method_5628(), this.method_5628(), i));
                    entity.method_31472();
                }
            }
        }
    }

    private void addToRuntimeInventory(class_1799 stack)
    {
        if (stack == null || stack.method_7960())
        {
            return;
        }

        if (!this.runtimeInventoryInitialized)
        {
            this.initializeRuntimeInventory();
        }

        int remaining = stack.method_7947();

        for (int i = 0; i < this.runtimeInventory.size(); i++)
        {
            class_1799 existing = this.runtimeInventory.get(i);

            if (existing.method_7960())
            {
                int move = Math.min(remaining, stack.method_7914());
                class_1799 copy = stack.method_7972();
                copy.method_7939(move);
                this.runtimeInventory.set(i, copy);
                remaining -= move;

                if (remaining <= 0)
                {
                    return;
                }
            }
            else if (class_1799.method_31577(existing, stack) && existing.method_7947() < existing.method_7914())
            {
                int space = existing.method_7914() - existing.method_7947();
                int move = Math.min(space, remaining);
                existing.method_7933(move);
                remaining -= move;

                if (remaining <= 0)
                {
                    return;
                }
            }
        }

        if (remaining > 0)
        {
            class_1799 copy = stack.method_7972();
            copy.method_7939(remaining);
            this.runtimeInventory.add(copy);
        }
    }

    @Override
    public void method_5660(boolean sneaking)
    {
        super.method_5660(sneaking);

        if (this.form != null && this.form.hitbox.get())
        {
            this.updateHitboxDimensions();
        }
    }

    private void updateHitboxDimensions()
    {
        if (this.form == null)
        {
            return;
        }

        boolean enabled = this.form.hitbox.get();
        boolean sneaking = this.method_5715();
        float width = this.form.hitboxWidth.get();
        float height = this.form.hitboxHeight.get();
        float sneakMultiplier = this.form.hitboxSneakMultiplier.get();

        if (enabled != this.lastHitboxEnabled
            || sneaking != this.lastSneaking
            || width != this.lastHitboxWidth
            || height != this.lastHitboxHeight
            || sneakMultiplier != this.lastHitboxSneakMultiplier)
        {
            this.lastHitboxEnabled = enabled;
            this.lastSneaking = sneaking;
            this.lastHitboxWidth = width;
            this.lastHitboxHeight = height;
            this.lastHitboxSneakMultiplier = sneakMultiplier;

            this.method_18382();
        }
    }

    @Override
    public class_4048 method_55694(class_4050 pose)
    {
        class_4048 dimensions = super.method_55694(pose);
        Form currentForm = this.form;

        if (currentForm != null && currentForm.hitbox.get())
        {
            float height = currentForm.hitboxHeight.get() * (this.method_5715() ? currentForm.hitboxSneakMultiplier.get() : 1F);

            return dimensions.comp_2189()
                ? class_4048.method_18385(currentForm.hitboxWidth.get(), height)
                : class_4048.method_18384(currentForm.hitboxWidth.get(), height);
        }

        return dimensions;
    }



        @Override
    public void method_6078(class_1282 damageSource)
    {
        super.method_6078(damageSource);
        
        if (!this.method_37908().method_8608() && !this.replayItemsDropped && this.replay != null && this.film != null && this.replay.dropItemsOnDeath.get())
        {
            this.dropReplayItems();
            this.replayItemsDropped = true;
        }
    }
    
    /**
     * Drop items from the replay's inventory and equipment when it dies
     * Mimics vanilla Minecraft item drop behavior
     */
    private void dropReplayItems()
    {
        List<class_1799> inventoryStacks = this.runtimeInventoryInitialized
            ? this.runtimeInventory
            : (this.replay.inventory == null ? Collections.emptyList() : this.replay.inventory.getStacks());
        boolean hasInventoryData = !inventoryStacks.isEmpty();
        boolean inventoryHasItems = false;

        if (hasInventoryData)
        {
            for (class_1799 stack : inventoryStacks)
            {
                if (stack != null && !stack.method_7960())
                {
                    inventoryHasItems = true;
                    break;
                }
            }
        }

        boolean inventoryLikelyIncludesEquipment = inventoryStacks.size() >= 40;
        boolean dropEquipment = !hasInventoryData || !inventoryHasItems || !inventoryLikelyIncludesEquipment;

        // Drop equipped items from keyframes at current tick
        if (dropEquipment && this.replay.keyframes != null)
        {
            float tick = (float) this.currentTick;
            
            // Drop main hand item
            class_1799 mainHand = this.replay.keyframes.mainHand.interpolate(tick, class_1799.field_8037);
            if (!mainHand.method_7960())
            {
                this.dropItemStack(mainHand.method_7972());
            }
            
            // Drop off hand item
            class_1799 offHand = this.replay.keyframes.offHand.interpolate(tick, class_1799.field_8037);
            if (!offHand.method_7960())
            {
                this.dropItemStack(offHand.method_7972());
            }
            
            // Drop armor pieces
            class_1799 armorHead = this.replay.keyframes.armorHead.interpolate(tick, class_1799.field_8037);
            if (!armorHead.method_7960())
            {
                this.dropItemStack(armorHead.method_7972());
            }
            
            class_1799 armorChest = this.replay.keyframes.armorChest.interpolate(tick, class_1799.field_8037);
            if (!armorChest.method_7960())
            {
                this.dropItemStack(armorChest.method_7972());
            }
            
            class_1799 armorLegs = this.replay.keyframes.armorLegs.interpolate(tick, class_1799.field_8037);
            if (!armorLegs.method_7960())
            {
                this.dropItemStack(armorLegs.method_7972());
            }
            
            class_1799 armorFeet = this.replay.keyframes.armorFeet.interpolate(tick, class_1799.field_8037);
            if (!armorFeet.method_7960())
            {
                this.dropItemStack(armorFeet.method_7972());
            }
        }
        
        // Drop items from replay inventory if available
        if (hasInventoryData && inventoryHasItems)
        {
            for (class_1799 stack : inventoryStacks)
            {
                if (stack != null && !stack.method_7960())
                {
                    this.dropItemStack(stack.method_7972());
                }
            }
        }
    }
    
    /**
     * Drop a single item stack with configurable physics from replay settings
     */
    private void dropItemStack(class_1799 stack)
    {
        if (stack.method_7960() || this.replay == null)
        {
            return;
        }
        
        // Create item entity at actor's position
        class_1542 itemEntity = new class_1542(
            this.method_37908(),
            this.method_23317(),
            this.method_23318() + 0.5,
            this.method_23321(),
            stack
        );
        
        // Apply random velocity using replay's configured values
        float minX = this.replay.dropVelocityMinX.get();
        float maxX = this.replay.dropVelocityMaxX.get();
        float minY = this.replay.dropVelocityMinY.get();
        float maxY = this.replay.dropVelocityMaxY.get();
        float minZ = this.replay.dropVelocityMinZ.get();
        float maxZ = this.replay.dropVelocityMaxZ.get();
        
        // Debug: Print velocity values to console
        System.out.println("[BBS Debug] Drop velocities - X: [" + minX + ", " + maxX + "], Y: [" + minY + ", " + maxY + "], Z: [" + minZ + ", " + maxZ + "]");
        
        double velocityX = minX + this.field_5974.method_43058() * (maxX - minX);
        double velocityY = minY + this.field_5974.method_43058() * (maxY - minY);
        double velocityZ = minZ + this.field_5974.method_43058() * (maxZ - minZ);
        
        itemEntity.method_18800(velocityX, velocityY, velocityZ);
        itemEntity.method_6988();
        
        this.method_37908().method_8649(itemEntity);
    }


    @Override
    public void method_5982()
    {
        super.method_5982();

        if (this.despawn)
        {
            this.method_31472();
        }
    }

    @Override
    public void method_5837(class_3222 player)
    {
        super.method_5837(player);

        ServerNetwork.sendEntityForm(player, this);
    }

    @Override
    public void method_5749(class_2487 nbt)
    {
        super.method_5749(nbt);

        this.despawn = nbt.method_10577("despawn");

        if (nbt.method_10573("Equipment", 10))
        {
            class_2487 equipmentNbt = nbt.method_10562("Equipment");
            class_7225.class_7874 registries = this.method_37908() != null ? this.method_37908().method_30349() : BBSMod.getRegistryManager();

            for (class_1304 slot : class_1304.values())
            {
                if (equipmentNbt.method_10573(slot.method_5923(), 10))
                {
                    class_2487 itemNbt = equipmentNbt.method_10562(slot.method_5923());
                    class_1799 stack = registries != null
                        ? class_1799.field_24671.parse(class_6903.method_46632(class_2509.field_11560, registries), itemNbt).result().orElse(class_1799.field_8037)
                        : class_1799.method_57359(null, itemNbt);

                    this.equipment.put(slot, stack);
                }
            }
        }
    }

    @Override
    public void method_5652(class_2487 nbt)
    {
        super.method_5652(nbt);

        nbt.method_10556("despawn", true);

        class_2487 equipmentNbt = new class_2487();
        class_7225.class_7874 registries = this.method_37908() != null ? this.method_37908().method_30349() : BBSMod.getRegistryManager();

        for (Map.Entry<class_1304, class_1799> entry : this.equipment.entrySet())
        {
            if (!entry.getValue().method_7960())
            {
                class_1799 stack = entry.getValue();
                class_2520 itemNbt = registries != null
                    ? class_1799.field_24671.encodeStart(class_6903.method_46632(class_2509.field_11560, registries), stack).result().orElse(null)
                    : class_1799.field_24671.encodeStart(class_2509.field_11560, stack).result().orElse(null);

                if (itemNbt instanceof class_2487 compound)
                {
                    equipmentNbt.method_10566(entry.getKey().method_5923(), compound);
                }
            }
        }

        nbt.method_10566("Equipment", equipmentNbt);
    }

    @Override
    protected int method_5691()
    {
        return 4;
    }
}
