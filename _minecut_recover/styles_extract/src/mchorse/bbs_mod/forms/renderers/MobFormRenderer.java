package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.BBSModClient;
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
import net.minecraft.class_1268;
import net.minecraft.class_1297;
import net.minecraft.class_1304;
import net.minecraft.class_1308;
import net.minecraft.class_1309;
import net.minecraft.class_1799;
import net.minecraft.class_2487;
import net.minecraft.class_2522;
import net.minecraft.class_2960;
import net.minecraft.class_308;
import net.minecraft.class_310;
import net.minecraft.class_4050;
import net.minecraft.class_4587;
import net.minecraft.class_583;
import net.minecraft.class_630;
import net.minecraft.class_638;
import net.minecraft.class_745;
import net.minecraft.class_765;
import net.minecraft.class_7833;
import net.minecraft.class_7923;
import net.minecraft.class_922;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.StringReader;

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
    private static final Map<Class, Map<String, class_630>> parts = new HashMap<>();
    private static final Map<class_630, Transform> cache = new HashMap<>();
    private static Pose currentPose;
    private static Pose currentPoseOverlay;

    public static final GameProfile WIDE = new GameProfile(UUID.fromString("b99a2400-28a8-4288-92dc-924beafbf756"), "McHorseYT");
    public static final GameProfile SLIM = new GameProfile(UUID.fromString("5477bd28-e672-4f87-a209-c03cf75f3606"), "osmiq");

    private class_1297 entity;

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

    public static Map<Class, Map<String, class_630>> getParts()
    {
        return parts;
    }

    public static Map<String, class_630> resolveModelParts(class_583<?> model, Class<?> entityClass)
    {
        if (model == null)
        {
            return Collections.emptyMap();
        }

        Map<String, class_630> resolved = new HashMap<>();

        MobFormRenderer.collectPartsFromModel(model, resolved);

        if (!resolved.isEmpty())
        {
            parts.put(entityClass, resolved);
        }

        return resolved;
    }

    private static void collectPartsFromModel(class_583<?> model, Map<String, class_630> output)
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
            if (!declaredField.getType().equals(class_630.class))
            {
                continue;
            }

            try
            {
                declaredField.setAccessible(true);

                class_630 part = (class_630) declaredField.get(model);

                if (part != null)
                {
                    MobFormRenderer.collectModelPartTree(declaredField.getName(), part, output);
                }
            }
            catch (Exception ignored)
            {}
        }
    }

    private static void collectModelPartTree(String name, class_630 part, Map<String, class_630> output)
    {
        if (part == null)
        {
            return;
        }

        output.put(name, part);
        MobFormRenderer.collectModelPartChildren(part, name, output);
    }

    @SuppressWarnings("unchecked")
    private static void collectModelPartChildren(class_630 part, String prefix, Map<String, class_630> output)
    {
        try
        {
            Field childrenField = class_630.class.getDeclaredField("children");

            childrenField.setAccessible(true);

            Map<String, class_630> children = (Map<String, class_630>) childrenField.get(part);

            for (Map.Entry<String, class_630> entry : children.entrySet())
            {
                String childName = prefix + "/" + entry.getKey();

                MobFormRenderer.collectModelPartTree(childName, entry.getValue(), output);
            }
        }
        catch (Exception ignored)
        {}
    }

    public static Map<class_630, Transform> getCache()
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
            Map<String, class_630> stringModelPartMap = parts.get(this.entity.getClass());

            if (stringModelPartMap == null)
            {
                stringModelPartMap = new HashMap<>();

                if (class_310.method_1551().method_1561().method_3953(this.entity) instanceof class_922 renderer)
                {
                    MobFormRenderer.collectPartsFromModel(renderer.method_4038(), stringModelPartMap);
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

        class_638 world = class_310.method_1551().field_1687;

        if (world == null)
        {
            return;
        }

        class_2487 compound = new class_2487();

        try
        {
            compound = (new class_2522(new StringReader(nbt))).method_10727();
        }
        catch (Exception e)
        {}

        this.entity = class_7923.field_41177.method_10223(class_2960.method_60654(id)).method_5883(world);

        if (this.entity == null && this.form.isPlayer())
        {
            this.entity = new class_745(world, slim ? SLIM : WIDE);
            this.entity.method_5841().method_12778(PlayerUtils.ProtectedAccess.getModelParts(), (byte) 0b1111111);
        }

        if (this.entity != null)
        {
            compound.method_10582("id", id);
            this.entity.method_5651(compound);
            this.entity.field_5960 = true;
        }
    }

    public MobItemStats sampleItemStats(IEntity source, float transition)
    {
        MobItemStats stats = new MobItemStats();

        this.ensureEntity();

        if (!(this.entity instanceof class_1309 living))
        {
            return stats;
        }

        if (source != null)
        {
            this.applyMorphRotation(living, source);
            this.applyLivingAnimationState(living, source);
        }

        stats.usingItem = living.method_6115();
        stats.activeHand = living.method_6058();
        stats.mainHand = living.method_6118(class_1304.field_6173).method_7972();
        stats.offHand = living.method_6118(class_1304.field_6171).method_7972();

        if (stats.usingItem)
        {
            class_1304 slot = stats.activeHand == class_1268.field_5810 ? class_1304.field_6171 : class_1304.field_6173;
            class_1799 stack = stats.activeHand == class_1268.field_5810 ? stats.offHand : stats.mainHand;

            if (!stack.method_7960())
            {
                stats.itemUseElapsed = Math.max(0, stack.method_7935(living) - living.method_6014());
            }
        }

        return stats;
    }

    public void ensureRenderEntity()
    {
        this.ensureEntity();
    }

    public class_1297 getRenderEntity()
    {
        this.ensureEntity();

        return this.entity;
    }

    @SuppressWarnings("unchecked")
    public static void setLivingAngles(class_583<?> model, class_1309 living, float animPos, float animSpeed, float transition, float headYaw, float pitch)
    {
        ((class_583<class_1309>) model).method_2819(living, animPos, animSpeed, transition, headYaw, pitch);
    }

    /**
     * Updates the hidden mob's model angles and returns vanilla {@link class_630}s
     * after {@code setAngles} (used by mob CEM pose capture).
     */
    public Map<String, class_630> sampleVanillaParts(IEntity source, float transition)
    {
        this.ensureEntity();

        if (!(this.entity instanceof class_1309 living))
        {
            return Collections.emptyMap();
        }

        if (source != null)
        {
            living.method_18380(this.getMorphPose(source));
            living.field_6012 = source.getAge();
            living.method_5660(source.isSneaking());
            living.method_5728(source.getMountTarget() == null && source.isSprinting());
            this.applyMorphRotation(living, source);
            this.applyLivingAnimationState(living, source);
            living.field_6213 = source.getDeathTime();
            living.field_6235 = source.getHurtTimer();
            living.field_6254 = source.getHurtTimer() > 0 ? Math.max(source.getHurtTimer(), living.field_6254) : 0;
            living.method_5673(class_1304.field_6173, source.getEquipmentStack(class_1304.field_6173));
            living.method_5673(class_1304.field_6171, source.getEquipmentStack(class_1304.field_6171));

            float handSwingProgress = source.getHandSwingProgress(transition);

            if (handSwingProgress > 0F && this.prevHandSwing == 0F)
            {
                living.method_6104(class_1268.field_5808);
            }

            this.prevHandSwing = handSwingProgress;

            if (living.field_42108 instanceof LimbAnimatorAccessor morphLimb && source.getMountTarget() == null && source.getLimbAnimator() instanceof LimbAnimatorAccessor sourceLimb)
            {
                morphLimb.setPos(sourceLimb.getPos());
                morphLimb.setSpeed(sourceLimb.getSpeed());
            }
        }

        if (!(class_310.method_1551().method_1561().method_3953(this.entity) instanceof class_922<?, ?> livingRenderer))
        {
            return Collections.emptyMap();
        }

        class_583<?> model = livingRenderer.method_4038();
        float animPos = living.field_42108.method_48572(transition);
        float animSpeed = living.field_42108.method_48570(transition);
        float headYaw = living.field_6241;
        float pitch = living.method_36455();

        MobFormRenderer.setLivingAngles(model, living, animPos, animSpeed, transition, headYaw, pitch);

        return MobFormRenderer.resolveModelParts(model, this.entity.getClass());
    }

    private class_4050 getMorphPose(IEntity source)
    {
        class_4050 pose = source.getEntityPose();

        if ((source.getMountTarget() != null || source.isSitting()) && pose == class_4050.field_18076)
        {
            return class_4050.field_40118;
        }

        if (source.isSneaking() && pose == class_4050.field_18076)
        {
            return class_4050.field_18081;
        }

        return pose;
    }

    @Override
    protected void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        this.ensureEntity();

        if (this.entity != null)
        {
            class_4587 stack = context.batcher.getContext().method_51448();

            stack.method_22903();

            Matrix4f uiMatrix = ModelFormRenderer.getUIMatrix(context, x1, y1, x2, y2);
            CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
            float scale = this.form.uiScale.get();
            float width = this.entity.method_17681();
            float height = this.entity.method_17682();

            scale = scale * Math.min(1.8F / Math.max(width, height), 1F);

            this.applyTransforms(uiMatrix, context.getTransition());
            MatrixStackUtils.multiply(stack, uiMatrix);
            stack.method_22905(scale, scale, scale);

            if (!this.form.mobID.get().equals("minecraft:ender_dragon"))
            {
                stack.method_22907(class_7833.field_40716.rotation(MathUtils.PI));
            }

            MatrixStackUtils.invertUiNormalY(stack);

            if (!FormUtilsClient.isUIPreviewAnimating() && this.entity instanceof class_1309 living)
            {
                living.field_6012 = 0;
                living.field_6235 = 0;
                living.field_42108.method_48567(0F);

                if (living.field_42108 instanceof LimbAnimatorAccessor limb)
                {
                    limb.setPos(0F);
                    limb.setSpeed(0F);
                    limb.setPrevSpeed(0F);
                }
            }
            else if (FormUtilsClient.isUIPreviewAnimating() && this.entity instanceof class_1309 living)
            {
                /* Drive idle/bob from world time so selected previews animate smoothly. */
                class_310 client = class_310.method_1551();
                int age = client.field_1687 != null ? (int) (client.field_1687.method_8510() % 72000L) : living.field_6012 + 1;

                living.field_6012 = age;
                living.field_6235 = 0;
                living.field_42108.method_48567(0F);

                if (living.field_42108 instanceof LimbAnimatorAccessor limb)
                {
                    limb.setPos(age * 0.1F);
                    limb.setSpeed(0F);
                    limb.setPrevSpeed(0F);
                }
            }

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
            RenderSystem.setupLevelDiffuseLighting(light0, light1);

            consumers.setUI(true);
            MobTextureOverride.begin(this.form.texture.get());
            try
            {
                class_310.method_1551().method_1561().method_3954(this.entity, 0D, 0D, 0D, 0F, context.getTransition(), stack, consumers, class_765.field_32769);
            }
            finally
            {
                MobTextureOverride.end();
            }
            consumers.draw();
            consumers.setUI(false);

            CustomVertexConsumerProvider.clearRunnables();

            class_308.method_24210();

            stack.method_22909();

            RenderSystem.depthFunc(GL11.GL_ALWAYS);
        }
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
        this.ensureEntity();

        if (this.entity != null)
        {
            CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
            int light = context.light;
            BooleanHolder first = new BooleanHolder();

            if (context.isPicking())
            {
                CustomVertexConsumerProvider.hijackVertexFormat((layer) ->
                {
                    if (!first.bool)
                    {
                        this.bindTexture();
                        this.setupTarget(context, BBSShaders.getPickerModelsProgram());
                        RenderSystem.setShader(BBSShaders::getPickerModelsProgram);

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

            context.stack.method_22903();

            if (this.form.mobID.get().equals("minecraft:ender_dragon"))
            {
                context.stack.method_22907(class_7833.field_40716.rotation(MathUtils.PI));
            }

            boolean detachedRiding = false;

            if (this.entity instanceof class_1309 livingMorph)
            {
                class_1309 player = class_310.method_1551().field_1724;
                class_1309 sourceLiving = context.entity instanceof class_1309 living ? living : null;

                if (context.entity != null)
                {
                    detachedRiding = this.prepareMorphRenderState(livingMorph, context.entity);
                    livingMorph.field_6213 = context.entity.getDeathTime();
                    ItemUseRenderState.syncEquipment(livingMorph, context.entity);
                    this.applyLivingAnimationState(livingMorph, context.entity);

                    int hurtTimer = context.entity.getHurtTimer();

                    if (player != null && sourceLiving == player && player.field_6235 > 0)
                    {
                        hurtTimer = player.field_6235;
                    }

                    livingMorph.field_6235 = hurtTimer;
                    livingMorph.field_6254 = hurtTimer > 0 ? Math.max(hurtTimer, livingMorph.field_6254) : 0;
                }

                if (livingMorph.field_42108 instanceof LimbAnimatorAccessor morphLimb && context.entity != null && context.entity.getMountTarget() == null && context.entity.getLimbAnimator() instanceof LimbAnimatorAccessor sourceLimb)
                {
                    morphLimb.setPos(sourceLimb.getPos());
                    morphLimb.setSpeed(sourceLimb.getSpeed());
                }
                else if (sourceLiving != null && livingMorph.field_42108 instanceof LimbAnimatorAccessor morphLimb && sourceLiving.field_42108 instanceof LimbAnimatorAccessor sourceLimb && context.entity != null && context.entity.getMountTarget() == null)
                {
                    morphLimb.setPos(sourceLimb.getPos());
                    morphLimb.setSpeed(sourceLimb.getSpeed());
                }
                else if (context.entity != null && context.entity.getMountTarget() != null && livingMorph.field_42108 instanceof LimbAnimatorAccessor morphLimb)
                {
                    morphLimb.setPrevSpeed(0F);
                    morphLimb.setSpeed(0F);
                }
            }

            currentPose = this.form.pose.get();
            currentPoseOverlay = this.form.poseOverlay.get();

            int savedFireTicks = 0;

            if (this.entity instanceof class_1309 livingMorphForFire)
            {
                savedFireTicks = livingMorphForFire.method_20802();
                livingMorphForFire.method_20803(0);
            }

            MobTextureOverride.begin(this.form.texture.get());
            try
            {
                class_310.method_1551().method_1561().method_3954(this.entity, 0D, 0D, 0D, 0F, context.getTransition(), context.stack, consumers, light);
            }
            finally
            {
                MobTextureOverride.end();
            }

            if (detachedRiding && context.entity != null)
            {
                MorphMountSync.applyRiding(this.entity, context.entity);
            }

            if (this.entity instanceof class_1309 livingMorphForFire)
            {
                livingMorphForFire.method_20803(savedFireTicks);
            }

            currentPose = currentPoseOverlay = null;

            consumers.draw();
            CustomVertexConsumerProvider.clearRunnables();

            context.stack.method_22909();

            RenderSystem.enableDepthTest();
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

                if (this.entity instanceof class_1309 livingEntity)
                {
                    livingEntity.field_6213 = entity.getDeathTime();
                    this.applyMorphRotation(livingEntity, entity);

                    /* Limb swing is so ugly */
                    if (mounted && livingEntity.field_42108 instanceof LimbAnimatorAccessor mountedLimb)
                    {
                        mountedLimb.setPrevSpeed(0F);
                        mountedLimb.setSpeed(0F);
                    }
                    else if (livingEntity.field_42108 instanceof LimbAnimatorAccessor a && entity.getLimbAnimator() instanceof LimbAnimatorAccessor b)
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
                        livingEntity.method_6104(class_1268.field_5808);
                    }

                    this.prevHandSwing = handSwingProgress;
                }

                this.entity.field_6014 = entity.getPrevX();
                this.entity.field_6036 = entity.getPrevY();
                this.entity.field_5969 = entity.getPrevZ();
                this.entity.method_23327(entity.getX(), entity.getY(), entity.getZ());
                this.entity.method_24830(entity.isOnGround());
                this.entity.method_5660(entity.isSneaking());
                this.entity.method_5728(mounted ? false : entity.isSprinting());
                this.entity.method_18380(this.getMorphPose(entity));

                MorphMountSync.applyRiding(this.entity, entity);

                if (this.entity instanceof class_1309 living)
                {
                    living.method_5673(class_1304.field_6173, entity.getEquipmentStack(class_1304.field_6173));
                    living.method_5673(class_1304.field_6171, entity.getEquipmentStack(class_1304.field_6171));
                    living.method_5673(class_1304.field_6169, entity.getEquipmentStack(class_1304.field_6169));
                    living.method_5673(class_1304.field_6174, entity.getEquipmentStack(class_1304.field_6174));
                    living.method_5673(class_1304.field_6172, entity.getEquipmentStack(class_1304.field_6172));
                    living.method_5673(class_1304.field_6166, entity.getEquipmentStack(class_1304.field_6166));
                    this.applyLivingAnimationState(living, entity);

                    if (this.entity instanceof class_745 && class_310.method_1551().method_1562() != null)
                    {
                        this.entity.method_5773();
                    }
                    else
                    {
                        if (living instanceof class_1308 mob)
                        {
                            mob.method_5977(true);
                        }

                        this.entity.method_5773();
                    }
                }
                else
                {
                    this.entity.method_5773();
                }

                if (particlesEnabled)
                {
                    MorphMobParticles.afterTick(this.entity, entity, true);
                }

                this.entity.method_23327(savedX, savedY, savedZ);
                this.entity.method_24830(savedOnGround);
                this.entity.method_5660(savedSneaking);
                this.entity.method_5728(mounted ? false : savedSprinting);
                this.entity.method_18380(this.getMorphPose(entity));

                if (this.entity instanceof class_1309 livingAfterTick)
                {
                    if (mounted && livingAfterTick.field_42108 instanceof LimbAnimatorAccessor mountedLimb)
                    {
                        mountedLimb.setPrevSpeed(0F);
                        mountedLimb.setSpeed(0F);
                    }

                    this.applyMorphRotation(livingAfterTick, entity);
                    this.applyLivingAnimationState(livingAfterTick, entity);
                }

                this.entity.field_6012 = entity.getAge();
                this.entity.field_5960 = true;

                this.prevYawHead = entity.getPrevHeadYaw() - entity.getPrevBodyYaw();
                this.prevPitch = entity.getPrevPitch();
            }
            finally
            {
                MorphMobParticles.endTick();
            }
        }
    }

    private void applyMorphRotation(class_1309 livingMorph, IEntity source)
    {
        float relativeHeadYaw = source.getHeadYaw() - source.getBodyYaw();
        float relativePrevHeadYaw = source.getPrevHeadYaw() - source.getPrevBodyYaw();

        livingMorph.method_36456(0F);
        livingMorph.method_5636(0F);
        livingMorph.method_5847(relativeHeadYaw);
        livingMorph.method_36457(source.getPitch());
        livingMorph.field_5982 = 0F;
        livingMorph.field_6220 = 0F;
        livingMorph.field_6259 = relativePrevHeadYaw;
        livingMorph.field_6004 = source.getPrevPitch();
    }

    /**
     * Vanilla passenger rendering repositions and frustum-culls from the vehicle AABB.
     * Film morphs are already placed by the form matrix, so detach riding for the draw call.
     */
    private boolean prepareMorphRenderState(class_1309 livingMorph, IEntity source)
    {
        boolean mounted = source.getMountTarget() != null || source.isSitting();

        livingMorph.method_5660(source.isSneaking());
        livingMorph.method_5728(mounted ? false : source.isSprinting());
        livingMorph.method_18380(this.getMorphPose(source));
        this.applyMorphRotation(livingMorph, source);

        if (!livingMorph.method_5765())
        {
            return false;
        }

        livingMorph.method_5848();

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
            class_2487 compound = (new class_2522(new StringReader(nbt))).method_10727();

            compound.method_10582("id", this.form.mobID.get());
            this.entity.method_5651(compound);
            this.appliedMobNbt = nbt;
            this.entity.field_5960 = true;
        }
        catch (Exception ignored)
        {}
    }

    private void applyLivingAnimationState(class_1309 living, IEntity entity)
    {
        living.method_20803(entity.getFireTicks());

        class_1268 hand = entity.getActiveHand();
        class_1304 slot = hand == class_1268.field_5810 ? class_1304.field_6171 : class_1304.field_6173;
        class_1799 stack = entity.getEquipmentStack(slot);

        ItemUseRenderState.syncItemUse(living, entity, hand, stack);
    }

    private static class BooleanHolder
    {
        public boolean bool;
    }

    public static int getStencilPickOffset(class_630 part, int light)
    {
        return light;
    }
}
