package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.client.ItemUseRenderState;
import mchorse.bbs_mod.client.MobTextureOverride;
import mchorse.bbs_mod.client.render.EntityRenderHelper;
import mchorse.bbs_mod.client.renderer.MorphMobParticles;
import mchorse.bbs_mod.entity.ActorEntity;
import mchorse.bbs_mod.film.MobItemStats;
import mchorse.bbs_mod.film.MorphMountSync;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.ITickable;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.mixin.LimbAnimatorAccessor;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.PlayerUtils;
import mchorse.bbs_mod.utils.joml.Vectors;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.Transform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.brigadier.StringReader;
import com.mojang.math.Axis;

import org.lwjgl.opengl.GL11;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MobFormRenderer extends FormRenderer<MobForm> implements ITickable
{
    private static final Map<Class, Map<String, ModelPart>> parts = new HashMap<>();
    private static final Map<ModelPart, Transform> cache = new HashMap<>();
    private static Pose currentPose;
    private static Pose currentPoseOverlay;
    /**
     * While true, {@link #getStencilPickOffset} forces lightmap U to 0 so every ModelPart
     * (body, eyes, clothing, armor, …) writes the same pick id. Eyes/glow layers hardcode
     * fullbright light and would otherwise only highlight the hit layer under Alt-hover.
     */
    private static boolean forceZeroPickLight;

    public static final GameProfile WIDE = new GameProfile(UUID.fromString("b99a2400-28a8-4288-92dc-924beafbf756"), "McHorseYT");
    public static final GameProfile SLIM = new GameProfile(UUID.fromString("5477bd28-e672-4f87-a209-c03cf75f3606"), "osmiq");

    private Entity entity;

    private String lastId = "";
    private String lastNBT = "";
    private boolean lastSlim;

    public float prevHandSwing;
    private float prevYawHead;
    private float prevPitch;
    private String appliedMobNbt = "";

    public static Pose getCurrentPose()
    {
        return currentPose;
    }

    public static Pose getCurrentPoseOverlay()
    {
        return currentPoseOverlay;
    }

    public static Map<Class, Map<String, ModelPart>> getParts()
    {
        return parts;
    }

    public static Map<String, ModelPart> resolveModelParts(EntityModel<?> model, Class<?> entityClass)
    {
        if (model == null)
        {
            return Collections.emptyMap();
        }

        Map<String, ModelPart> resolved = new HashMap<>();

        MobFormRenderer.collectPartsFromModel(model, resolved);

        if (!resolved.isEmpty())
        {
            parts.put(entityClass, resolved);
        }

        return resolved;
    }

    private static void collectPartsFromModel(EntityModel<?> model, Map<String, ModelPart> output)
    {
        Set<Field> fields = new HashSet<>();
        Class<?> modelClass = model.getClass();

        while (modelClass != Object.class)
        {
            for (Field field : modelClass.getDeclaredFields())
            {
                fields.add(field);
            }

            modelClass = modelClass.getSuperclass();
        }

        for (Field declaredField : fields)
        {
            if (!declaredField.getType().equals(ModelPart.class))
            {
                continue;
            }

            try
            {
                declaredField.setAccessible(true);

                ModelPart part = (ModelPart) declaredField.get(model);

                if (part != null)
                {
                    MobFormRenderer.collectModelPartTree(declaredField.getName(), part, output);
                }
            }
            catch (Exception ignored)
            {}
        }
    }

    private static void collectModelPartTree(String name, ModelPart part, Map<String, ModelPart> output)
    {
        if (part == null)
        {
            return;
        }

        output.put(name, part);
        MobFormRenderer.collectModelPartChildren(part, name, output);
    }

    @SuppressWarnings("unchecked")
    private static void collectModelPartChildren(ModelPart part, String prefix, Map<String, ModelPart> output)
    {
        try
        {
            Field childrenField = ModelPart.class.getDeclaredField("children");

            childrenField.setAccessible(true);

            Map<String, ModelPart> children = (Map<String, ModelPart>) childrenField.get(part);

            for (Map.Entry<String, ModelPart> entry : children.entrySet())
            {
                String childName = prefix + "/" + entry.getKey();

                MobFormRenderer.collectModelPartTree(childName, entry.getValue(), output);
            }
        }
        catch (Exception ignored)
        {}
    }

    public static Map<ModelPart, Transform> getCache()
    {
        return cache;
    }

    public MobFormRenderer(MobForm form)
    {
        super(form);
    }

    @Override
    public List<String> getBones()
    {
        this.ensureEntity();

        if (this.entity != null)
        {
            Map<String, ModelPart> stringModelPartMap = parts.get(this.entity.getClass());

            if (stringModelPartMap == null)
            {
                stringModelPartMap = new HashMap<>();

                if (Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(this.entity) instanceof LivingEntityRenderer renderer)
                {
                    MobFormRenderer.collectPartsFromModel(renderer.getModel(), stringModelPartMap);
                }

                parts.put(this.entity.getClass(), stringModelPartMap);
            }

            return new ArrayList<>(stringModelPartMap.keySet());
        }

        return super.getBones();
    }

    private void bindTexture()
    {
        Link link = this.form.texture.get();

        if (link != null)
        {
            BBSModClient.getTextures().bindTexture(link);
        }
    }

    private void ensureEntity()
    {
        String id = this.form.mobID.get();
        String nbt = this.form.mobNBT.get();
        boolean slim = this.form.slim.get();

        if (this.entity == null || !this.lastId.equals(id) || !this.lastNBT.equals(nbt) || slim != this.lastSlim)
        {
            MorphMobParticles.clear(this.entity);

            this.lastId = id;
            this.lastNBT = nbt;
            this.lastSlim = slim;
            this.entity = null;
        }

        if (this.entity != null)
        {
            return;
        }

        ClientLevel world = Minecraft.getInstance().level;

        if (world == null)
        {
            return;
        }

        CompoundTag compound = new CompoundTag();

        try
        {
            compound = TagParser.parseCompoundFully(nbt);
        }
        catch (Exception e)
        {}

        this.entity = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(id)).create(Minecraft.getInstance().level, EntitySpawnReason.MOB_SUMMONED);

        if (this.entity == null && this.form.isPlayer())
        {
            this.entity = new RemotePlayer(world, slim ? SLIM : WIDE);
            this.entity.getEntityData().set(PlayerUtils.ProtectedAccess.getModelParts(), (byte) 0b1111111);
        }

        if (this.entity != null)
        {
            compound.putString("id", id);
            ValueInput view = TagValueInput.create(ProblemReporter.DISCARDING, this.entity.registryAccess(), compound);
            this.entity.load(view);
            this.entity.noPhysics = true;
        }
    }

    public MobItemStats sampleItemStats(IEntity source, float transition)
    {
        MobItemStats stats = new MobItemStats();

        this.ensureEntity();

        if (!(this.entity instanceof LivingEntity living))
        {
            return stats;
        }

        if (source != null)
        {
            this.applyMorphRotation(living, source);
            this.applyLivingAnimationState(living, source);
        }

        stats.usingItem = living.isUsingItem();
        stats.activeHand = living.getUsedItemHand();
        stats.mainHand = living.getItemBySlot(EquipmentSlot.MAINHAND).copy();
        stats.offHand = living.getItemBySlot(EquipmentSlot.OFFHAND).copy();

        if (stats.usingItem)
        {
            EquipmentSlot slot = stats.activeHand == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
            ItemStack stack = stats.activeHand == InteractionHand.OFF_HAND ? stats.offHand : stats.mainHand;

            if (!stack.isEmpty())
            {
                stats.itemUseElapsed = Math.max(0, stack.getUseDuration(living) - living.getUseItemRemainingTicks());
            }
        }

        return stats;
    }

    public void ensureRenderEntity()
    {
        this.ensureEntity();
    }

    public Entity getRenderEntity()
    {
        this.ensureEntity();

        return this.entity;
    }

    @SuppressWarnings("unchecked")
    public static void setLivingAngles(LivingEntityRenderer<?, ?, ?> livingRenderer, EntityModel<?> model, LivingEntity living, float transition)
    {
        LivingEntityRenderState state = (LivingEntityRenderState) livingRenderer.createRenderState();
        ((LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>) livingRenderer).extractRenderState(living, state, transition);
        ((EntityModel<LivingEntityRenderState>) model).setupAnim(state);
    }

    /**
     * Updates the hidden mob's model angles and returns vanilla {@link ModelPart}s
     * after {@code setAngles} (used by mob CEM pose capture).
     */
    public Map<String, ModelPart> sampleVanillaParts(IEntity source, float transition)
    {
        this.ensureEntity();

        if (!(this.entity instanceof LivingEntity living))
        {
            return Collections.emptyMap();
        }

        if (source != null)
        {
            living.setPose(this.getMorphPose(source));
            living.tickCount = source.getAge();
            living.setShiftKeyDown(source.isSneaking());
            living.setSprinting(source.getMountTarget() == null && source.isSprinting());
            this.applyMorphRotation(living, source);
            this.applyLivingAnimationState(living, source);
            living.deathTime = this.resolveDeathTimeForRender(source);
            living.hurtTime = source.getHurtTimer();
            living.hurtDuration = source.getHurtTimer() > 0 ? Math.max(source.getHurtTimer(), living.hurtDuration) : 0;
            living.setItemSlot(EquipmentSlot.MAINHAND, source.getEquipmentStack(EquipmentSlot.MAINHAND));
            living.setItemSlot(EquipmentSlot.OFFHAND, source.getEquipmentStack(EquipmentSlot.OFFHAND));

            float handSwingProgress = source.getHandSwingProgress(transition);

            if (handSwingProgress > 0F && this.prevHandSwing == 0F)
            {
                living.swing(InteractionHand.MAIN_HAND);
            }

            this.prevHandSwing = handSwingProgress;

            if (living.walkAnimation instanceof LimbAnimatorAccessor morphLimb && source.getMountTarget() == null && source.getLimbAnimator() instanceof LimbAnimatorAccessor sourceLimb)
            {
                morphLimb.setPos(sourceLimb.getPos());
                morphLimb.setSpeed(sourceLimb.getSpeed());
            }
        }

        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(this.entity) instanceof LivingEntityRenderer<?, ?, ?> livingRenderer))
        {
            return Collections.emptyMap();
        }

        EntityModel<?> model = livingRenderer.getModel();

        MobFormRenderer.setLivingAngles(livingRenderer, model, living, transition);

        return MobFormRenderer.resolveModelParts(model, this.entity.getClass());
    }

    private Pose getMorphPose(IEntity source)
    {
        Pose pose = source.getEntityPose();

        if ((source.getMountTarget() != null || source.isSitting()) && pose == Pose.STANDING)
        {
            return Pose.SITTING;
        }

        if (source.isSneaking() && pose == Pose.STANDING)
        {
            return Pose.CROUCHING;
        }

        return pose;
    }

    @Override
    protected void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        this.ensureEntity();

        if (this.entity != null)
        {
            PoseStack stack = new PoseStack();

            stack.pushPose();

            Matrix4f uiMatrix = ModelFormRenderer.getUIMatrix(context, x1, y1, x2, y2);
            CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
            float scale = this.form.uiScale.get();
            float width = this.entity.getBbWidth();
            float height = this.entity.getBbHeight();

            scale = scale * Math.min(1.8F / Math.max(width, height), 1F);

            this.applyTransforms(uiMatrix, context.getTransition());
            MatrixStackUtils.multiply(stack, uiMatrix);
            stack.scale(scale, scale, scale);

            if (!this.form.mobID.get().equals("minecraft:ender_dragon"))
            {
                stack.mulPose(Axis.YP.rotation(MathUtils.PI));
            }

            MatrixStackUtils.invertUiNormalY(stack);

            BooleanHolder first = new BooleanHolder();

            CustomVertexConsumerProvider.hijackVertexFormat((layer) ->
            {
                if (!first.bool)
                {
                    this.bindTexture();

                    first.bool = true;
                }
            });

            Vector3f light0 = new Vector3f(0.85F, 0.85F, -1F).normalize();
            Vector3f light1 = new Vector3f(-0.85F, 0.85F, 1F).normalize();
            // RenderSystem.setupLevelDiffuseLighting(light0, light1);

            consumers.setUI(true);
            MobTextureOverride.begin(this.form.texture.get());
            try
            {
                var state = Minecraft.getInstance().getEntityRenderDispatcher().extractEntity(this.entity, 0F);

                EntityRenderHelper.renderEntityState(state, stack, true);
            }
            finally
            {
                MobTextureOverride.end();
            }
            consumers.draw();
            consumers.setUI(false);

            CustomVertexConsumerProvider.clearRunnables();

            // DiffuseLighting.disableGuiDepthLighting();

            stack.popPose();

            GlStateManager._depthFunc(GL11.GL_ALWAYS);
        }
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
        this.ensureEntity();

        if (this.entity != null)
        {
            /* Private Immediate so villager clothing layers are not mixed with world leftovers. */
            CustomVertexConsumerProvider consumers = FormUtilsClient.getMobMorphProvider();
            int light = context.light;
            BooleanHolder first = new BooleanHolder();
            boolean prepareLighting = BBSRendering.isRenderingWorld()
                && !context.isPicking()
                && !context.isShadowPass;

            if (context.isPicking())
            {
                forceZeroPickLight = true;
                /* Re-apply picker shader after every RenderLayer.startDrawing (TAIL mixin),
                 * same as ItemFormRenderer — otherwise eyes/clothing keep their own shader
                 * or a different lightmap and Alt-hover only highlights one layer. */
                CustomVertexConsumerProvider.hijackVertexFormat((layer) ->
                {
                    if (!first.bool)
                    {
                        this.bindTexture();
                        first.bool = true;
                    }
                });

                light = 0;
            }
            else
            {
                CustomVertexConsumerProvider.hijackVertexFormat((layer) ->
                {
                    if (!first.bool)
                    {
                        this.bindTexture();

                        first.bool = true;
                    }
                });
            }

            try
            {
            context.stack.pushPose();

            if (this.form.mobID.get().equals("minecraft:ender_dragon"))
            {
                context.stack.mulPose(Axis.YP.rotation(MathUtils.PI));
            }

            boolean detachedRiding = false;

            if (this.entity instanceof LivingEntity livingMorph)
            {
                LivingEntity player = Minecraft.getInstance().player;
                LivingEntity sourceLiving = context.entity instanceof LivingEntity living ? living : null;

                if (context.entity != null)
                {
                    detachedRiding = this.prepareMorphRenderState(livingMorph, context.entity);
                    /* Tip comes from LivingEntityRenderer via morph.deathTime. Sample
                     * keyframed death_time for ActorEntity+MobForm here only — never
                     * write it onto ActorEntity (that stuck the red overlay on scrub). */
                    livingMorph.deathTime = this.resolveDeathTimeForRender(context.entity);
                    ItemUseRenderState.syncEquipment(livingMorph, context.entity);
                    this.applyLivingAnimationState(livingMorph, context.entity);

                    int hurtTimer = context.entity.getHurtTimer();

                    if (player != null && sourceLiving == player && player.hurtTime > 0)
                    {
                        hurtTimer = player.hurtTime;
                    }

                    livingMorph.hurtTime = hurtTimer;
                    livingMorph.hurtDuration = hurtTimer > 0 ? Math.max(hurtTimer, livingMorph.hurtDuration) : 0;
                }

                if (livingMorph.walkAnimation instanceof LimbAnimatorAccessor morphLimb && context.entity != null && context.entity.getMountTarget() == null && context.entity.getLimbAnimator() instanceof LimbAnimatorAccessor sourceLimb)
                {
                    morphLimb.setPos(sourceLimb.getPos());
                    morphLimb.setSpeed(sourceLimb.getSpeed());
                }
                else if (sourceLiving != null && livingMorph.walkAnimation instanceof LimbAnimatorAccessor morphLimb && sourceLiving.walkAnimation instanceof LimbAnimatorAccessor sourceLimb && context.entity != null && context.entity.getMountTarget() == null)
                {
                    morphLimb.setPos(sourceLimb.getPos());
                    morphLimb.setSpeed(sourceLimb.getSpeed());
                }
                else if (context.entity != null && context.entity.getMountTarget() != null && livingMorph.walkAnimation instanceof LimbAnimatorAccessor morphLimb)
                {
                    morphLimb.setPrevSpeed(0F);
                    morphLimb.setSpeed(0F);
                }
            }

            currentPose = this.form.pose.get();
            currentPoseOverlay = this.form.poseOverlay.get();

            int savedFireTicks = 0;

            if (this.entity instanceof LivingEntity livingMorphForFire)
            {
                savedFireTicks = livingMorphForFire.getRemainingFireTicks();
                livingMorphForFire.setRemainingFireTicks(0);
            }

            MobTextureOverride.begin(this.form.texture.get());

            try
            {
                if (prepareLighting)
                {
                    BBSRendering.prepareVanillaEntityLighting();
                }

                var state = Minecraft.getInstance().getEntityRenderDispatcher().extractEntity(this.entity, context.getTransition());

                /* World/film path: queue is flushed by the frame pipeline — do not flush here. */
                EntityRenderHelper.renderEntityState(state, context.stack, false);
            }
            finally
            {
                MobTextureOverride.end();
            }

            if (detachedRiding && context.entity != null)
            {
                MorphMountSync.applyRiding(this.entity, context.entity);
            }

            if (this.entity instanceof LivingEntity livingMorphForFire)
            {
                livingMorphForFire.setRemainingFireTicks(savedFireTicks);
            }

            currentPose = currentPoseOverlay = null;

            if (prepareLighting)
            {
                BBSRendering.prepareVanillaEntityLighting();
            }

            consumers.draw();
            CustomVertexConsumerProvider.clearRunnables();

            if (prepareLighting)
            {
                BBSRendering.restoreWorldRenderState();
            }

            context.stack.popPose();

            GlStateManager._enableDepthTest();
            }
            finally
            {
                forceZeroPickLight = false;
            }
        }
    }

    @Override
    public void tick(IEntity entity)
    {
        this.ensureEntity();
        this.applyMobNbt();

        if (this.entity != null)
        {
            boolean particlesEnabled = entity.isParticlesEnabled();

            MorphMobParticles.beginTick(particlesEnabled);

            try
            {
                boolean mounted = entity.getMountTarget() != null || entity.isSitting();
                double savedX = entity.getX();
                double savedY = entity.getY();
                double savedZ = entity.getZ();
                boolean savedOnGround = entity.isOnGround();
                boolean savedSneaking = entity.isSneaking();
                boolean savedSprinting = entity.isSprinting();

                if (this.entity instanceof LivingEntity livingEntity)
                {
                    livingEntity.deathTime = this.resolveDeathTimeForRender(entity);
                    this.applyMorphRotation(livingEntity, entity);

                    /* Limb swing is so ugly */
                    if (mounted && livingEntity.walkAnimation instanceof LimbAnimatorAccessor mountedLimb)
                    {
                        mountedLimb.setPrevSpeed(0F);
                        mountedLimb.setSpeed(0F);
                    }
                    else if (livingEntity.walkAnimation instanceof LimbAnimatorAccessor a && entity.getLimbAnimator() instanceof LimbAnimatorAccessor b)
                    {
                        a.setPrevSpeed(b.getPrevSpeed());
                        a.setSpeed(b.getSpeed());
                        a.setPos(b.getPos());
                    }

                    /* Arm swing */
                    float handSwingProgress = entity.getHandSwingProgress(0F);

                    if (handSwingProgress < this.prevHandSwing)
                    {
                        this.prevHandSwing = 0;
                    }

                    if (handSwingProgress > 0 && this.prevHandSwing == 0)
                    {
                        livingEntity.swing(InteractionHand.MAIN_HAND);
                    }

                    this.prevHandSwing = handSwingProgress;
                }

                this.entity.xo = entity.getPrevX();
                this.entity.yo = entity.getPrevY();
                this.entity.zo = entity.getPrevZ();
                this.entity.setPosRaw(entity.getX(), entity.getY(), entity.getZ());
                this.entity.setOnGround(entity.isOnGround());
                this.entity.setShiftKeyDown(entity.isSneaking());
                this.entity.setSprinting(mounted ? false : entity.isSprinting());
                this.entity.setPose(this.getMorphPose(entity));

                MorphMountSync.applyRiding(this.entity, entity);

                if (this.entity instanceof LivingEntity living)
                {
                    living.setItemSlot(EquipmentSlot.MAINHAND, entity.getEquipmentStack(EquipmentSlot.MAINHAND));
                    living.setItemSlot(EquipmentSlot.OFFHAND, entity.getEquipmentStack(EquipmentSlot.OFFHAND));
                    living.setItemSlot(EquipmentSlot.HEAD, entity.getEquipmentStack(EquipmentSlot.HEAD));
                    living.setItemSlot(EquipmentSlot.CHEST, entity.getEquipmentStack(EquipmentSlot.CHEST));
                    living.setItemSlot(EquipmentSlot.LEGS, entity.getEquipmentStack(EquipmentSlot.LEGS));
                    living.setItemSlot(EquipmentSlot.FEET, entity.getEquipmentStack(EquipmentSlot.FEET));
                    this.applyLivingAnimationState(living, entity);

                    if (this.entity instanceof RemotePlayer && Minecraft.getInstance().getConnection() != null)
                    {
                        this.entity.tick();
                    }
                    else
                    {
                        if (living instanceof Mob mob)
                        {
                            mob.setNoAi(true);
                        }

                        this.entity.tick();
                    }
                }
                else
                {
                    this.entity.tick();
                }

                if (particlesEnabled)
                {
                    MorphMobParticles.afterTick(this.entity, entity, true);
                }

                this.entity.setPosRaw(savedX, savedY, savedZ);
                this.entity.setOnGround(savedOnGround);
                this.entity.setShiftKeyDown(savedSneaking);
                this.entity.setSprinting(mounted ? false : savedSprinting);
                this.entity.setPose(this.getMorphPose(entity));

                if (this.entity instanceof LivingEntity livingAfterTick)
                {
                    if (mounted && livingAfterTick.walkAnimation instanceof LimbAnimatorAccessor mountedLimb)
                    {
                        mountedLimb.setPrevSpeed(0F);
                        mountedLimb.setSpeed(0F);
                    }

                    this.applyMorphRotation(livingAfterTick, entity);
                    this.applyLivingAnimationState(livingAfterTick, entity);
                }

                this.entity.tickCount = entity.getAge();
                this.entity.noPhysics = true;

                this.prevYawHead = entity.getPrevHeadYaw() - entity.getPrevBodyYaw();
                this.prevPitch = entity.getPrevPitch();
            }
            finally
            {
                MorphMobParticles.endTick();
            }
        }
    }

    private void applyMorphRotation(LivingEntity livingMorph, IEntity source)
    {
        float relativeHeadYaw = source.getHeadYaw() - source.getBodyYaw();
        float relativePrevHeadYaw = source.getPrevHeadYaw() - source.getPrevBodyYaw();

        livingMorph.setYRot(0F);
        livingMorph.setYBodyRot(0F);
        livingMorph.setYHeadRot(relativeHeadYaw);
        livingMorph.setXRot(source.getPitch());
        livingMorph.yRotO = 0F;
        livingMorph.yBodyRotO = 0F;
        livingMorph.yHeadRotO = relativePrevHeadYaw;
        livingMorph.xRotO = source.getPrevPitch();
    }

    /**
     * Death tip for mob morphs is driven by {@code livingMorph.deathTime} inside
     * vanilla {@code LivingEntityRenderer}. For film actors, also honor keyframed
     * {@code death_time} without mutating {@link ActorEntity#deathTime} (writing
     * that field stuck the damage-red overlay across timeline scrubs).
     */
    private int resolveDeathTimeForRender(IEntity source)
    {
        int deathTime = source == null ? 0 : source.getDeathTime();

        if (!(source instanceof MCEntity mcEntity) || !(mcEntity.getMcEntity() instanceof ActorEntity actor))
        {
            return deathTime;
        }

        Replay replay = actor.getReplay();

        if (replay != null && replay.keyframes != null)
        {
            int keyDeath = replay.keyframes.deathTime.interpolate((float) actor.getCurrentTick()).intValue();

            if (keyDeath > 0)
            {
                deathTime = Math.max(deathTime, keyDeath);
            }
        }

        if (deathTime <= 0 && (actor.isDead() || actor.getHealth() <= 0F))
        {
            deathTime = Math.max(1, actor.deathTime);
        }

        return deathTime;
    }

    /**
     * Vanilla passenger rendering repositions and frustum-culls from the vehicle AABB.
     * Film morphs are already placed by the form matrix, so detach riding for the draw call.
     */
    private boolean prepareMorphRenderState(LivingEntity livingMorph, IEntity source)
    {
        boolean mounted = source.getMountTarget() != null || source.isSitting();

        livingMorph.setShiftKeyDown(source.isSneaking());
        livingMorph.setSprinting(mounted ? false : source.isSprinting());
        livingMorph.setPose(this.getMorphPose(source));
        this.applyMorphRotation(livingMorph, source);

        if (!livingMorph.isPassenger())
        {
            return false;
        }

        livingMorph.stopRiding();

        return true;
    }

    private void applyMobNbt()
    {
        String nbt = this.form.mobNBT.get();

        if (this.entity == null || nbt.isEmpty() || nbt.equals(this.appliedMobNbt))
        {
            return;
        }

        try
        {
            CompoundTag compound = TagParser.parseCompoundFully(nbt);

            compound.putString("id", this.form.mobID.get());
            ValueInput view = TagValueInput.create(ProblemReporter.DISCARDING, this.entity.registryAccess(), compound);
            this.entity.load(view);
            this.appliedMobNbt = nbt;
            this.entity.noPhysics = true;
        }
        catch (Exception ignored)
        {}
    }

    private void applyLivingAnimationState(LivingEntity living, IEntity entity)
    {
        living.setRemainingFireTicks(entity.getFireTicks());

        InteractionHand hand = entity.getActiveHand();
        EquipmentSlot slot = hand == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
        ItemStack stack = entity.getEquipmentStack(slot);

        ItemUseRenderState.syncItemUse(living, entity, hand, stack);
    }

    private static class BooleanHolder
    {
        public boolean bool;
    }

    public static int getStencilPickOffset(ModelPart part, int light)
    {
        /* Eyes / glowing feature layers pass fullbright light into ModelPart.render;
         * picker_models encodes Target + lightmap.u, so non-zero light splits the form
         * into multiple pick ids. Zero them while stencil-picking MobForms. */
        return forceZeroPickLight ? 0 : light;
    }
}
