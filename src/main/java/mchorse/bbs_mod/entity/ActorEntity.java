package mchorse.bbs_mod.entity;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.ActorReplayStateSync;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.network.ServerNetwork;
import mchorse.bbs_mod.utils.StringUtils;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ActorEntity extends LivingEntity implements IEntityFormProvider
{
    public static DefaultAttributeContainer.Builder createActorAttributes()
    {
        return LivingEntity.createLivingAttributes()
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1D)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.1D)
            .add(EntityAttributes.GENERIC_ATTACK_SPEED)
            .add(EntityAttributes.GENERIC_LUCK);
    }

    private boolean despawn;
    private MCEntity entity = new MCEntity(this);
    private Form form;

    private Map<EquipmentSlot, ItemStack> equipment = new HashMap<>();

    private boolean lastHitboxEnabled;
    private float lastHitboxWidth = Float.NaN;
    private float lastHitboxHeight = Float.NaN;
    private float lastHitboxSneakMultiplier = Float.NaN;
    private boolean lastSneaking;

    /**
     * After unpause, ramp walk velocity back in over a few ticks so limb swing
     * accelerates naturally instead of snapping for one tick from settled → full walk.
     */
    private int playbackVelocityBlendTicks;

    /**
     * When true, {@link #spawnSprintingParticles()} is a no-op. Used while the film
     * editor has actor-control on this replay: the live player may be sprinting, and
     * {@code ActorReplayStateSync} copies that flag onto this entity, but the body can
     * still sit on the film pose — without this, vanilla dust sprays at the wrong place.
     */
    private boolean suppressSprintParticles;

    /**
     * When true, skip real-time {@link LivingEntity} limb/age ticks and form animator
     * updates so film timeline pause/scrub can drive them instead (see
     * {@code BBSSettings.editorActorPauseAnimations}).
     */
    private boolean pauseNaturalAnimations;

    /* Film and replay data for item drops */
    private Film film;
    private Replay replay;
    private int currentTick;
    private boolean replayItemsDropped;
    
    /* Runtime inventory for replay actors (initial inventory + picked up items) */
    private final List<ItemStack> runtimeInventory = new ArrayList<>();
    private boolean runtimeInventoryInitialized;
    private final Set<UUID> pickedUpEntityIds = new HashSet<>();

    public ActorEntity(EntityType<? extends LivingEntity> entityType, World world)
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
        this.syncNameTag(replay);
    }

    /**
     * Actor-mode bodies are drawn by {@code ActorEntityRenderer}, not the film
     * stub path that draws {@link Replay#nameTag}. Mirror that string onto the
     * living entity so vanilla label rendering shows it.
     */
    public void syncNameTag(Replay replay)
    {
        String nameTag = replay == null ? "" : replay.nameTag.get();

        if (nameTag == null || nameTag.isEmpty())
        {
            this.setCustomName(null);
            this.setCustomNameVisible(false);
        }
        else
        {
            this.setCustomName(Text.literal(StringUtils.processColoredText(nameTag)));
            this.setCustomNameVisible(true);
        }
    }
    
    /**
     * Update the current tick for accurate item retrieval
     */
    public void updateTick(int tick)
    {
        this.currentTick = tick;
    }

    /**
     * Film playback resumed after a hold-still pause. Softens the first walk
     * velocities so {@link net.minecraft.entity.LimbAnimator} does not jump.
     */
    public void markPlaybackResumed()
    {
        this.playbackVelocityBlendTicks = 4;
    }

    /**
     * Multiplier for keyframe horizontal velocity this tick (1 = normal playback).
     */
    public double consumePlaybackVelocityScale()
    {
        if (this.playbackVelocityBlendTicks <= 0)
        {
            return 1D;
        }

        int remaining = this.playbackVelocityBlendTicks;

        this.playbackVelocityBlendTicks -= 1;

        return 1D - (remaining / 4D);
    }

    public void setSuppressSprintParticles(boolean suppress)
    {
        this.suppressSprintParticles = suppress;
    }

    public void setPauseNaturalAnimations(boolean pause)
    {
        this.pauseNaturalAnimations = pause;
    }

    public boolean areNaturalAnimationsPaused()
    {
        return this.pauseNaturalAnimations;
    }

    /**
     * Advance emoticon/BOBJ form animators by {@code steps} ticks while natural
     * animations are timeline-driven.
     */
    public void advanceFormAnimationTicks(int steps)
    {
        if (steps <= 0 || this.form == null)
        {
            return;
        }

        for (int i = 0; i < steps; i++)
        {
            this.form.update(this.entity);
        }
    }

    /**
     * One natural walk/form step for timeline scrub: limb swing from horizontal
     * delta (same formula as {@link mchorse.bbs_mod.forms.entities.StubEntity#update})
     * plus one form animator tick.
     */
    public void advanceNaturalMotionStep(double fromX, double fromZ, double toX, double toZ)
    {
        ActorReplayStateSync.advanceLimbStep(this, fromX, fromZ, toX, toZ);
        this.age += 1;

        if (this.form != null)
        {
            this.form.update(this.entity);
        }
    }

    @Override
    protected void spawnSprintingParticles()
    {
        if (this.suppressSprintParticles)
        {
            return;
        }

        super.spawnSprintingParticles();
    }

    private void initializeRuntimeInventory()
    {
        this.runtimeInventory.clear();
        this.pickedUpEntityIds.clear();

        if (this.replay != null && this.replay.inventory != null)
        {
            for (ItemStack stack : this.replay.inventory.getStacks())
            {
                this.runtimeInventory.add(stack == null ? ItemStack.EMPTY : stack.copy());
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
        return this.getId();
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

        if (!this.getWorld().isClient())
        {
            if (lastForm != null) lastForm.onDemorph(this);
            if (form != null) form.onMorph(this);
        }
        
        this.updateHitboxDimensions();
    }

    @Override
    public boolean isCollidable()
    {
        return this.form != null && this.form.hitbox.get();
    }

    @Override
    public boolean isPushable()
    {
        return this.form == null || !this.form.hitbox.get();
    }

    @Override
    public void pushAwayFrom(Entity entity)
    {
        if (this.form == null || !this.form.hitbox.get())
        {
            super.pushAwayFrom(entity);
        }
    }

    @Override
    public void pushAway(Entity entity)
    {
        if (this.form == null || !this.form.hitbox.get())
        {
            super.pushAway(entity);
        }
    }

    @Override
    public boolean shouldRender(double distance)
    {
        double d = this.getBoundingBox().getAverageSideLength();

        if (Double.isNaN(d))
        {
            d = 1D;
        }

        return distance < (d * 256D) * (d * 256D);
    }

    @Override
    public Iterable<ItemStack> getHandItems()
    {
        return List.of(this.getEquippedStack(EquipmentSlot.MAINHAND), this.getEquippedStack(EquipmentSlot.OFFHAND));
    }

    @Override
    public Iterable<ItemStack> getArmorItems()
    {
        return List.of(this.getEquippedStack(EquipmentSlot.FEET), this.getEquippedStack(EquipmentSlot.LEGS), this.getEquippedStack(EquipmentSlot.CHEST), this.getEquippedStack(EquipmentSlot.HEAD));
    }

    @Override
    public ItemStack getEquippedStack(EquipmentSlot slot)
    {
        return this.equipment.getOrDefault(slot, ItemStack.EMPTY);
    }

    @Override
    public void equipStack(EquipmentSlot slot, ItemStack stack)
    {
        this.equipment.put(slot, stack == null ? ItemStack.EMPTY : stack);
    }

    @Override
    public Arm getMainArm()
    {
        return Arm.RIGHT;
    }

    @Override
    public void tick()
    {
        if (this.pauseNaturalAnimations)
        {
            /* Hold limbs / emoticon clocks; still allow swipe hand-swing progress. */
            this.tickHandSwing();
            this.updateHitboxDimensions();

            if (!this.getWorld().isClient())
            {
                this.tickItemPickup();
            }

            return;
        }

        super.tick();

        this.tickHandSwing();
        this.updateHitboxDimensions();

        if (this.form != null)
        {
            this.form.update(this.entity);
        }

        if (!this.getWorld().isClient())
        {
            this.tickItemPickup();
        }
    }

    private void tickItemPickup()
    {
        /* Don't pickup items when dead */
        if (this.isDead())
        {
            return;
        }

        /* Pickup items */
        Box box = this.getBoundingBox().expand(1D, 0.5D, 1D);
        List<Entity> list = this.getWorld().getOtherEntities(this, box);

        for (Entity entity : list)
        {
            if (entity instanceof ItemEntity itemEntity)
            {
                UUID entityId = itemEntity.getUuid();
                ItemStack itemStack = itemEntity.getStack();
                int i = itemStack.getCount();

                if (!entity.isRemoved() && !itemEntity.cannotPickup() && !this.pickedUpEntityIds.contains(entityId))
                {
                    this.pickedUpEntityIds.add(entityId);
                    this.addToRuntimeInventory(itemStack.copy());
                    
                    ((ServerWorld) this.getWorld()).getChunkManager().sendToOtherNearbyPlayers(entity, new ItemPickupAnimationS2CPacket(entity.getId(), this.getId(), i));
                    entity.discard();
                }
            }
        }
    }

    private void addToRuntimeInventory(ItemStack stack)
    {
        if (stack == null || stack.isEmpty())
        {
            return;
        }

        if (!this.runtimeInventoryInitialized)
        {
            this.initializeRuntimeInventory();
        }

        int remaining = stack.getCount();

        for (int i = 0; i < this.runtimeInventory.size(); i++)
        {
            ItemStack existing = this.runtimeInventory.get(i);

            if (existing.isEmpty())
            {
                int move = Math.min(remaining, stack.getMaxCount());
                ItemStack copy = stack.copy();
                copy.setCount(move);
                this.runtimeInventory.set(i, copy);
                remaining -= move;

                if (remaining <= 0)
                {
                    return;
                }
            }
            else if (ItemStack.areItemsAndComponentsEqual(existing, stack) && existing.getCount() < existing.getMaxCount())
            {
                int space = existing.getMaxCount() - existing.getCount();
                int move = Math.min(space, remaining);
                existing.increment(move);
                remaining -= move;

                if (remaining <= 0)
                {
                    return;
                }
            }
        }

        if (remaining > 0)
        {
            ItemStack copy = stack.copy();
            copy.setCount(remaining);
            this.runtimeInventory.add(copy);
        }
    }

    @Override
    public void setSneaking(boolean sneaking)
    {
        super.setSneaking(sneaking);

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
        boolean sneaking = this.isSneaking();
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

            this.calculateDimensions();
        }
    }

    @Override
    public EntityDimensions getBaseDimensions(EntityPose pose)
    {
        EntityDimensions dimensions = super.getBaseDimensions(pose);
        Form currentForm = this.form;

        if (currentForm != null && currentForm.hitbox.get())
        {
            float height = currentForm.hitboxHeight.get() * (this.isSneaking() ? currentForm.hitboxSneakMultiplier.get() : 1F);

            return dimensions.fixed()
                ? EntityDimensions.fixed(currentForm.hitboxWidth.get(), height)
                : EntityDimensions.changing(currentForm.hitboxWidth.get(), height);
        }

        return dimensions;
    }



    @Override
    public void onDeath(DamageSource damageSource)
    {
        super.onDeath(damageSource);
        
        if (!this.getWorld().isClient() && !this.replayItemsDropped && this.replay != null && this.film != null && this.replay.dropItemsOnDeath.get())
        {
            this.dropReplayItems();
            this.replayItemsDropped = true;
        }
    }

    /**
     * Actor-mode invulnerability from the nested {@code invulnerable} keyframe track.
     * Blocks live hits / action-clip damage while keyframed {@code damage} hurt flash
     * still applies via {@link ActorReplayStateSync}.
     */
    @Override
    public boolean isInvulnerableTo(DamageSource damageSource)
    {
        if (this.isKeyframeInvulnerable())
        {
            return true;
        }

        return super.isInvulnerableTo(damageSource);
    }

    private boolean isKeyframeInvulnerable()
    {
        if (this.replay == null || this.replay.keyframes == null)
        {
            return false;
        }

        return this.replay.keyframes.isInvulnerableAt((float) this.currentTick);
    }
    
    /**
     * Drop items from the replay's inventory and equipment when it dies
     * Mimics vanilla Minecraft item drop behavior
     */
    private void dropReplayItems()
    {
        List<ItemStack> inventoryStacks = this.runtimeInventoryInitialized
            ? this.runtimeInventory
            : (this.replay.inventory == null ? Collections.emptyList() : this.replay.inventory.getStacks());
        boolean hasInventoryData = !inventoryStacks.isEmpty();
        boolean inventoryHasItems = false;

        if (hasInventoryData)
        {
            for (ItemStack stack : inventoryStacks)
            {
                if (stack != null && !stack.isEmpty())
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
            ItemStack mainHand = this.replay.keyframes.mainHand.interpolate(tick, ItemStack.EMPTY);
            if (!mainHand.isEmpty())
            {
                this.dropItemStack(mainHand.copy());
            }
            
            // Drop off hand item
            ItemStack offHand = this.replay.keyframes.offHand.interpolate(tick, ItemStack.EMPTY);
            if (!offHand.isEmpty())
            {
                this.dropItemStack(offHand.copy());
            }
            
            // Drop armor pieces
            ItemStack armorHead = this.replay.keyframes.armorHead.interpolate(tick, ItemStack.EMPTY);
            if (!armorHead.isEmpty())
            {
                this.dropItemStack(armorHead.copy());
            }
            
            ItemStack armorChest = this.replay.keyframes.armorChest.interpolate(tick, ItemStack.EMPTY);
            if (!armorChest.isEmpty())
            {
                this.dropItemStack(armorChest.copy());
            }
            
            ItemStack armorLegs = this.replay.keyframes.armorLegs.interpolate(tick, ItemStack.EMPTY);
            if (!armorLegs.isEmpty())
            {
                this.dropItemStack(armorLegs.copy());
            }
            
            ItemStack armorFeet = this.replay.keyframes.armorFeet.interpolate(tick, ItemStack.EMPTY);
            if (!armorFeet.isEmpty())
            {
                this.dropItemStack(armorFeet.copy());
            }
        }
        
        // Drop items from replay inventory if available
        if (hasInventoryData && inventoryHasItems)
        {
            for (ItemStack stack : inventoryStacks)
            {
                if (stack != null && !stack.isEmpty())
                {
                    this.dropItemStack(stack.copy());
                }
            }
        }
    }
    
    /**
     * Drop a single item stack with configurable physics from replay settings
     */
    private void dropItemStack(ItemStack stack)
    {
        if (stack.isEmpty() || this.replay == null)
        {
            return;
        }
        
        // Create item entity at actor's position
        ItemEntity itemEntity = new ItemEntity(
            this.getWorld(),
            this.getX(),
            this.getY() + 0.5,
            this.getZ(),
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
        
        double velocityX = minX + this.random.nextDouble() * (maxX - minX);
        double velocityY = minY + this.random.nextDouble() * (maxY - minY);
        double velocityZ = minZ + this.random.nextDouble() * (maxZ - minZ);
        
        itemEntity.setVelocity(velocityX, velocityY, velocityZ);
        itemEntity.setToDefaultPickupDelay();
        
        this.getWorld().spawnEntity(itemEntity);
    }


    @Override
    public void checkDespawn()
    {
        super.checkDespawn();

        if (this.despawn)
        {
            this.discard();
        }
    }

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player)
    {
        super.onStartedTrackingBy(player);

        ServerNetwork.sendEntityForm(player, this);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt)
    {
        super.readCustomDataFromNbt(nbt);

        this.despawn = nbt.getBoolean("despawn");

        if (nbt.contains("Equipment", 10))
        {
            NbtCompound equipmentNbt = nbt.getCompound("Equipment");
            RegistryWrapper.WrapperLookup registries = this.getWorld() != null ? this.getWorld().getRegistryManager() : BBSMod.getRegistryManager();

            for (EquipmentSlot slot : EquipmentSlot.values())
            {
                if (equipmentNbt.contains(slot.getName(), 10))
                {
                    NbtCompound itemNbt = equipmentNbt.getCompound(slot.getName());
                    ItemStack stack = registries != null
                        ? ItemStack.CODEC.parse(RegistryOps.of(NbtOps.INSTANCE, registries), itemNbt).result().orElse(ItemStack.EMPTY)
                        : ItemStack.fromNbtOrEmpty(null, itemNbt);

                    this.equipment.put(slot, stack);
                }
            }
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt)
    {
        super.writeCustomDataToNbt(nbt);

        nbt.putBoolean("despawn", true);

        NbtCompound equipmentNbt = new NbtCompound();
        RegistryWrapper.WrapperLookup registries = this.getWorld() != null ? this.getWorld().getRegistryManager() : BBSMod.getRegistryManager();

        for (Map.Entry<EquipmentSlot, ItemStack> entry : this.equipment.entrySet())
        {
            if (!entry.getValue().isEmpty())
            {
                ItemStack stack = entry.getValue();
                NbtElement itemNbt = registries != null
                    ? ItemStack.CODEC.encodeStart(RegistryOps.of(NbtOps.INSTANCE, registries), stack).result().orElse(null)
                    : ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack).result().orElse(null);

                if (itemNbt instanceof NbtCompound compound)
                {
                    equipmentNbt.put(entry.getKey().getName(), compound);
                }
            }
        }

        nbt.put("Equipment", equipmentNbt);
    }

    @Override
    protected int getPermissionLevel()
    {
        return 4;
    }
}
