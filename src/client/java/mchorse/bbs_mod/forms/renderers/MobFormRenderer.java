package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.client.ItemUseRenderState;
import mchorse.bbs_mod.client.MobTextureOverride;
import mchorse.bbs_mod.client.renderer.MorphMobParticles;
import mchorse.bbs_mod.film.MobItemStats;
import mchorse.bbs_mod.film.MorphMountSync;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.ITickable;
import mchorse.bbs_mod.forms.entities.IEntity;
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
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.authlib.GameProfile;
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
    private String lastPlayerName = "";
    private String lastPlayerUuid = "";
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

    private void applyPBRTextureIntensity()
    {
        BBSRendering.setPBRTextureIntensity(this.form.pbrNormalIntensity.get(), this.form.pbrSpecularIntensity.get());
    }

    private void clearPBRTextureIntensity()
    {
        BBSRendering.clearPBRTextureIntensity();
    }

    private void ensureEntity()
    {
        String id = this.form.mobID.get();
        String nbt = this.form.mobNBT.get();
        String playerName = this.form.playerName.get();
        String playerUuid = this.form.playerUuid.get();
        boolean slim = this.form.slim.get();

        if (this.entity == null
            || !this.lastId.equals(id)
            || !this.lastNBT.equals(nbt)
            || !this.lastPlayerName.equals(playerName)
            || !this.lastPlayerUuid.equals(playerUuid)
            || slim != this.lastSlim)
        {
            MorphMobParticles.clear(this.entity);

            this.lastId = id;
            this.lastNBT = nbt;
            this.lastPlayerName = playerName;
            this.lastPlayerUuid = playerUuid;
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
            this.entity = new RemotePlayer(world, this.getPlayerProfile(slim));
            this.entity.getEntityData().set(PlayerUtils.ProtectedAccess.getModelParts(), (byte) 0b1111111);
        }

        if (this.entity != null)
        {
            compound.putString("id", id);
            HolderLookup.Provider lookup = this.entity.level() != null ? this.entity.level().registryAccess() : Minecraft.getInstance().level.registryAccess();
            ValueInput readView = TagValueInput.create(ProblemReporter.DISCARDING, lookup, compound);
            this.entity.load(readView);
            this.entity.noPhysics = true;
        }
    }

    private GameProfile getPlayerProfile(boolean slim)
    {
        String uuid = this.form.playerUuid.get();
        String name = this.form.playerName.get();

        if (!uuid.isEmpty())
        {
            try
            {
                return new GameProfile(UUID.fromString(uuid), name.isEmpty() ? null : name);
            }
            catch (Exception e)
            {}
        }

        return slim ? SLIM : WIDE;
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

    /**
     * Copy walk-cycle limb phase from a replay stub (or other source) onto the morph.
     * Must include {@code prevSpeed}: vanilla {@code getPos(tickDelta)} / {@code getSpeed(tickDelta)}
     * lerp with it, and omitting it after {@link Entity#tick()} made non-actor MobForms look stepped.
     */
    private static void copyLimbAnimator(LimbAnimatorAccessor target, LimbAnimatorAccessor source)
    {
        target.setPrevSpeed(source.getPrevSpeed());
        target.setSpeed(source.getSpeed());
        target.setPos(source.getPos());
    }

    private static void copyLimbAnimator(LivingEntity target, IEntity source)
    {
        if (target != null && target.walkAnimation instanceof LimbAnimatorAccessor morphLimb
            && source != null && source.getLimbAnimator() instanceof LimbAnimatorAccessor sourceLimb)
        {
            copyLimbAnimator(morphLimb, sourceLimb);
        }
    }

    private static void zeroLimbAnimator(LivingEntity target)
    {
        if (target != null && target.walkAnimation instanceof LimbAnimatorAccessor limb)
        {
            limb.setPrevSpeed(0F);
            limb.setSpeed(0F);
        }
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
            /* Tip is FormDeathTilt with float death_time. Keep morph.deathTime at 0 so
             * LivingEntityRenderer does not add tickDelta on a held mid value (shake)
             * or double-tip recorded deaths. */
            living.deathTime = 0;
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

            if (source.getMountTarget() == null)
            {
                copyLimbAnimator(living, source);
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

    private net.minecraft.world.entity.Pose getMorphPose(IEntity source)
    {
        net.minecraft.world.entity.Pose pose = source.getEntityPose();

        if ((source.getMountTarget() != null || source.isSitting()) && pose == net.minecraft.world.entity.Pose.STANDING)
        {
            return net.minecraft.world.entity.Pose.SITTING;
        }

        if (source.isSneaking() && pose == net.minecraft.world.entity.Pose.STANDING)
        {
            return net.minecraft.world.entity.Pose.CROUCHING;
        }

        return pose;
    }

    @Override
    protected void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        this.ensureEntity();

        if (this.entity != null)
        {
            context.batcher.flush();

            PoseStack stack = new PoseStack();

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

            BBSRendering.setupLevelLighting();

            consumers.setUI(true);
            MobTextureOverride.begin(this.form.texture.get());
            this.applyPBRTextureIntensity();
            try
            {
                EntityRenderDispatcher entityRenderManager = Minecraft.getInstance().getEntityRenderDispatcher();
                EntityRenderState renderState = entityRenderManager.extractEntity(this.entity, 0F);
                renderState.shadowRadius = 0F;
                if (renderState.shadowPieces != null)
                {
                    renderState.shadowPieces.clear();
                }

                FeatureRenderDispatcher dispatcher = Minecraft.getInstance().gameRenderer.getFeatureRenderDispatcher();
                CameraRenderState cameraRenderState = new CameraRenderState();
                entityRenderManager.submit(renderState, cameraRenderState, 0.0D, 0.0D, 0.0D, stack, dispatcher.getSubmitNodeStorage());
                dispatcher.renderAllFeatures();
            }
            finally
            {
                this.clearPBRTextureIntensity();
                MobTextureOverride.end();
            }
            consumers.draw();
            consumers.setUI(false);

            CustomVertexConsumerProvider.clearRunnables();

            BBSRendering.depthFunc(GL11.GL_ALWAYS);
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
                    this.bindTexture();
                    this.setupTarget(context, BBSShaders.getPickerModelsProgram());
                    BBSRendering.bindProgram(BBSShaders.getPickerModelsProgram());
                });

                light = 0;
            }
            else
            {
                CustomVertexConsumerProvider.hijackVertexFormat((layer) ->
                {
                    if (first.bool || FormUtilsClient.isMobFormEquipmentLayer(layer))
                    {
                        return;
                    }

                    this.bindTexture();
                    first.bool = true;
                });
            }

            PoseStack.Pose stackMarker = context.stack.last();

            context.stack.pushPose();

            try
            {
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
                    /* Tip is FormDeathTilt (float sample). Zero morph.deathTime to avoid
                     * LivingEntityRenderer(deathTime + tickDelta) wobble / double tip. */
                    livingMorph.deathTime = 0;
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

                if (context.entity != null && context.entity.getMountTarget() != null)
                {
                    zeroLimbAnimator(livingMorph);
                }
                else if (context.entity != null)
                {
                    copyLimbAnimator(livingMorph, context.entity);
                }
                else if (sourceLiving != null)
                {
                    if (livingMorph.walkAnimation instanceof LimbAnimatorAccessor morphLimb
                        && sourceLiving.walkAnimation instanceof LimbAnimatorAccessor sourceLimb)
                    {
                        copyLimbAnimator(morphLimb, sourceLimb);
                    }
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
            this.applyPBRTextureIntensity();

            EntityRenderDispatcher entityRenderManager = Minecraft.getInstance().getEntityRenderDispatcher();

            try
            {
                if (prepareLighting)
                {
                    BBSRendering.prepareVanillaEntityLighting();
                }

                EntityRenderState renderState = entityRenderManager.extractEntity(this.entity, context.getTransition());
                renderState.shadowRadius = 0F;
                if (renderState.shadowPieces != null)
                {
                    renderState.shadowPieces.clear();
                }

                FeatureRenderDispatcher dispatcher = Minecraft.getInstance().gameRenderer.getFeatureRenderDispatcher();
                CameraRenderState cameraRenderState = new CameraRenderState();
                entityRenderManager.submit(renderState, cameraRenderState, 0.0D, 0.0D, 0.0D, context.stack, dispatcher.getSubmitNodeStorage());
                dispatcher.renderAllFeatures();
            }
            finally
            {
                this.clearPBRTextureIntensity();
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
            }
            finally
            {
                currentPose = currentPoseOverlay = null;
                CustomVertexConsumerProvider.clearRunnables();
                forceZeroPickLight = false;

                if (prepareLighting)
                {
                    BBSRendering.prepareVanillaEntityLighting();
                }

                try
                {
                    consumers.draw();
                }
                catch (Exception ignored)
                {
                }

                if (prepareLighting)
                {
                    BBSRendering.restoreWorldRenderState();
                }

                MatrixStackUtils.popUntil(context.stack, stackMarker);
                BBSRendering.enableDepthTest();
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
                    livingEntity.deathTime = 0;
                    this.applyMorphRotation(livingEntity, entity);

                    /* Stub already ran updateLimbs; morph.tick() would advance again and
                     * leave prevSpeed out of sync — restore the stub phase for smooth walk. */
                    if (mounted)
                    {
                        zeroLimbAnimator(livingEntity);
                    }
                    else
                    {
                        copyLimbAnimator(livingEntity, entity);
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
                this.entity.xOld = entity.getPrevX();
                this.entity.yOld = entity.getPrevY();
                this.entity.zOld = entity.getPrevZ();
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
                    /* LivingEntity.tick() calls updateLimbs again; keep stub limb phase. */
                    if (mounted)
                    {
                        zeroLimbAnimator(livingAfterTick);
                    }
                    else
                    {
                        copyLimbAnimator(livingAfterTick, entity);
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
            HolderLookup.Provider lookup = this.entity.level() != null ? this.entity.level().registryAccess() : Minecraft.getInstance().level.registryAccess();
            ValueInput readView = TagValueInput.create(ProblemReporter.DISCARDING, lookup, compound);
            this.entity.load(readView);
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
