package mchorse.bbs_mod.film;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.actions.types.MobDeathActionClip;
import mchorse.bbs_mod.actions.types.item.ItemDropActionClip;
import mchorse.bbs_mod.film.MobCemItemCapture;
import mchorse.bbs_mod.film.MobCemPoseCapture;
import mchorse.bbs_mod.film.replays.MountLink;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Captures world mobs into new film replays while {@link Recorder} is active.
 */
public final class RecorderMobCapture
{
    private static final int DEATH_ANIMATION_TICKS = 20;
    private static final double DROP_SCAN_RADIUS = 2D;

    public static final class Session
    {
        public final int entityId;
        public final int replayIndex;
        public final boolean livingEntity;

        public int deathTickIndex = 0;
        public boolean deathHandled = false;
        public boolean recordingDeath = false;

        public double lastX;
        public double lastY;
        public double lastZ;
        public float lastYaw;
        public float lastPitch;
        public float lastHeadYaw;
        public float lastBodyYaw;

        public double deathX;
        public double deathY;
        public double deathZ;
        public float deathYaw;
        public float deathPitch;
        public float deathHeadYaw;
        public float deathBodyYaw;

        public boolean tracksSnowTrail = false;
        public final Map<Long, BlockState> snowTrailSnapshots = new HashMap<>();

        public Boolean lastFire;
        public Boolean lastParticles;

        public Session(int entityId, int replayIndex, boolean livingEntity)
        {
            this.entityId = entityId;
            this.replayIndex = replayIndex;
            this.livingEntity = livingEntity;
        }
    }

    private final List<Session> sessions = new ArrayList<>();
    private final Set<Integer> capturedEntityIds = new HashSet<>();
    private final Map<Integer, Integer> entityReplayIndices = new HashMap<>();
    private final Set<Integer> vanillaPlaybackEntityIds = new HashSet<>();

    public void applyRecordingSetup(MobCaptureRecordingSetup setup)
    {
        this.vanillaPlaybackEntityIds.clear();

        if (setup != null)
        {
            this.vanillaPlaybackEntityIds.addAll(setup.vanillaPlaybackEntityIds);
        }
    }

    private void applyVanillaMobPlayback(Replay replay, boolean enabled)
    {
        if (replay.form.get() instanceof MobForm)
        {
            replay.vanillaMobPlayback.set(enabled);
            replay.vanillaMobPlaybackSerialized = true;
        }
    }

    public List<Session> getSessions()
    {
        return this.sessions;
    }

    public boolean isEmpty()
    {
        return this.sessions.isEmpty();
    }

    public void clear()
    {
        this.sessions.clear();
        this.capturedEntityIds.clear();
        this.entityReplayIndices.clear();
        this.vanillaPlaybackEntityIds.clear();
    }

    public Set<Integer> getCapturedEntityIds()
    {
        return this.capturedEntityIds;
    }

    public int getReplayIndexForEntity(int entityId)
    {
        Integer index = this.entityReplayIndices.get(entityId);

        return index == null ? -1 : index;
    }

    public static boolean canCapture()
    {
        if (!BBSSettings.recordingAutoCaptureMobs.get())
        {
            return false;
        }

        Recorder recorder = BBSModClient.getFilms().getRecorder();

        return recorder != null && !recorder.hasNotStarted();
    }

    public static void onEntityInteraction(Entity target)
    {
        if (!canCapture())
        {
            return;
        }

        Recorder recorder = BBSModClient.getFilms().getRecorder();

        if (recorder != null)
        {
            recorder.getMobCapture().tryCapture(recorder, target);
        }
    }

    public static void recordMountKeyframes(List<Replay> replays, int riderIndex, ReplayKeyframes riderKeyframes, IEntity entity, int tick)
    {
        int mountIndex = -1;
        Entity vehicle = MorphMountSync.resolveVehicleEntity(entity);

        if (vehicle != null)
        {
            mountIndex = RecorderMobCapture.resolveReplayIndexForEntity(vehicle.getId());
        }

        riderKeyframes.riding.insert(tick, mountIndex >= 0 ? 1D : 0D);

        if (mountIndex >= 0 && replays != null && mountIndex < replays.size())
        {
            Replay mountReplay = replays.get(mountIndex);
            MountLink ridden = new MountLink(true, riderIndex);

            mountReplay.keyframes.ridden.insert(tick, ridden);
        }
    }

    public void ensurePlayerVehicleCaptured(Recorder recorder)
    {
        this.capturePlayerVehicle(recorder);
    }

    public static int resolveReplayIndexForEntity(int entityId)
    {
        Films films = BBSModClient.getFilms();
        Recorder recorder = films.getRecorder();

        if (recorder != null)
        {
            int index = recorder.getMobCapture().getReplayIndexForEntity(entityId);

            if (index >= 0)
            {
                return index;
            }
        }

        return films.getEditorMobCapture().getReplayIndexForEntity(entityId);
    }

    public boolean tryCapture(Recorder recorder, Entity target)
    {
        return this.tryCapture(recorder, target, "");
    }

    public boolean tryCapture(Recorder recorder, Entity target, String groupPath)
    {
        if (target == null || target instanceof PlayerEntity)
        {
            return false;
        }

        if (this.capturedEntityIds.contains(target.getId()))
        {
            return false;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;

        if (player == null)
        {
            return false;
        }

        Form captured = Morph.captureFormFromEntity(player, target);

        if (captured == null)
        {
            return false;
        }

        Form form = FormUtils.copy(captured);
        int tick = recorder.getTick();
        int[] replayIndex = new int[] {-1};

        BaseValue.edit(recorder.film.replays, (replays) ->
        {
            Replay replay = replays.addReplay();

            replay.form.set(form);
            replay.label.set(this.getEntityLabel(target, form));
            this.applyVanillaMobPlayback(replay, this.vanillaPlaybackEntityIds.contains(target.getId()));

            if (groupPath != null && !groupPath.isEmpty())
            {
                replay.group.set(groupPath);
            }

            this.recordEntity(replay, target, tick);

            replayIndex[0] = replays.getList().indexOf(replay);
        });

        if (replayIndex[0] < 0)
        {
            return false;
        }

        return this.registerSession(recorder, target, replayIndex[0]);
    }

    public void bulkCapture(Film film, int tick, MobCaptureRecordingSetup setup, UIFilmPanel panel)
    {
        if (setup == null)
        {
            return;
        }

        this.applyRecordingSetup(setup);

        if (!setup.shouldCapture())
        {
            return;
        }

        Map<String, MobCaptureAreaScanner.TypeBucket> buckets = MobCaptureAreaScanner.scan(setup.areaSize);
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;

        if (player == null || buckets.isEmpty())
        {
            return;
        }

        BaseValue.edit(film.replays, (replays) ->
        {
            List<Replay> list = replays.getList();

            for (Map.Entry<String, MobCaptureAreaScanner.TypeBucket> entry : buckets.entrySet())
            {
                MobCaptureAreaScanner.TypeBucket bucket = entry.getValue();

                if (bucket.entities.isEmpty())
                {
                    continue;
                }

                boolean hasSelectedEntity = false;

                for (Entity entity : bucket.entities)
                {
                    if (setup.selectedEntityIds.contains(entity.getId()))
                    {
                        hasSelectedEntity = true;

                        break;
                    }
                }

                if (!hasSelectedEntity)
                {
                    continue;
                }

                Replay group = new Replay("replay");

                group.uuid.set(UUID.randomUUID().toString());
                group.isGroup.set(true);
                group.label.set(bucket.label);

                int insertAt = list.size();
                String groupPath = group.uuid.get();

                replays.add(insertAt, group);

                for (Entity entity : bucket.entities)
                {
                    if (!setup.selectedEntityIds.contains(entity.getId()))
                    {
                        continue;
                    }

                    if (this.capturedEntityIds.contains(entity.getId()))
                    {
                        continue;
                    }

                    Form captured = Morph.captureFormFromEntity(player, entity);

                    if (captured == null)
                    {
                        continue;
                    }

                    Form form = FormUtils.copy(captured);
                    Replay replay = new Replay("replay");

                    replay.form.set(form);
                    replay.label.set(this.getEntityLabel(entity, form));
                    replay.group.set(groupPath);
                    this.applyVanillaMobPlayback(replay, setup.vanillaPlaybackEntityIds.contains(entity.getId()));
                    this.recordEntity(replay, entity, tick);

                    replays.add(replay);

                    Session session = new Session(entity.getId(), list.indexOf(replay), entity instanceof LivingEntity);

                    if (entity instanceof LivingEntity living)
                    {
                        session.tracksSnowTrail = this.isSnowGolem(form, living);
                        this.updateSessionState(session, living);
                        this.recordFireAndParticlesIfChanged(replay, session, living, tick);
                    }
                    else
                    {
                        this.updateSessionState(session, entity);
                    }

                    this.sessions.add(session);
                    this.capturedEntityIds.add(entity.getId());
                    this.entityReplayIndices.put(entity.getId(), session.replayIndex);
                }
            }

            replays.sync();
        });

        if (panel != null)
        {
            panel.replayEditor.replays.replays.buildVisualList();
            panel.replayEditor.updateChannelsList();
            panel.getController().createEntities();
        }
    }

    public void recordTickForFilm(Film film, int tick)
    {
        if (this.sessions.isEmpty())
        {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld world = mc.world;

        if (world == null)
        {
            return;
        }

        Iterator<Session> iterator = this.sessions.iterator();

        while (iterator.hasNext())
        {
            Session session = iterator.next();

            if (session.replayIndex < 0 || session.replayIndex >= film.replays.getList().size())
            {
                iterator.remove();
                continue;
            }

            Replay replay = film.replays.getList().get(session.replayIndex);
            Entity entity = world.getEntityById(session.entityId);

            if (session.recordingDeath)
            {
                session.deathTickIndex += 1;
                this.recordDeathEntity(replay, session, tick, Math.min(session.deathTickIndex, DEATH_ANIMATION_TICKS));

                if (session.deathTickIndex >= DEATH_ANIMATION_TICKS)
                {
                    this.applyDeathVisibilityKeyframes(replay, tick);
                    iterator.remove();
                }

                continue;
            }

            if (entity == null)
            {
                if (!session.livingEntity)
                {
                    this.applyDeathVisibilityKeyframes(replay, tick);
                    iterator.remove();
                }
                else if (session.deathHandled)
                {
                    session.recordingDeath = true;
                    session.deathTickIndex = 1;
                    this.recordDeathEntity(replay, session, tick, 1);
                }
                else
                {
                    iterator.remove();
                }

                continue;
            }

            if (!session.livingEntity)
            {
                this.updateSessionState(session, entity);
                this.recordEntity(replay, entity, tick);
                this.syncMobFormNbt(replay, entity);
                continue;
            }

            if (entity instanceof LivingEntity living)
            {
                boolean dying = !living.isAlive() || living.deathTime > 0;

                if (!dying)
                {
                    this.updateSessionState(session, living);
                    this.recordEntity(replay, entity, tick);
                    this.recordFireAndParticlesIfChanged(replay, session, living, tick);

                    if (session.tracksSnowTrail)
                    {
                        RecorderWorldEffectCapture.captureSnowTrail(replay, session.snowTrailSnapshots, living, tick, world);
                    }
                }
                else
                {
                    if (!session.deathHandled)
                    {
                        this.captureDeathState(session, living);
                        this.handleDeathForFilm(film, replay, session, living, tick, world);
                        session.deathHandled = true;
                        session.recordingDeath = true;
                        session.deathTickIndex = living.deathTime > 0 ? living.deathTime : 1;
                    }

                    this.recordDeathEntity(replay, session, tick, Math.min(session.deathTickIndex, DEATH_ANIMATION_TICKS));

                    if (session.deathTickIndex >= DEATH_ANIMATION_TICKS)
                    {
                        this.applyDeathVisibilityKeyframes(replay, tick);
                        iterator.remove();
                    }
                    else
                    {
                        session.recordingDeath = true;
                    }
                }
            }
            else if (session.deathHandled)
            {
                session.recordingDeath = true;
                session.deathTickIndex = 1;
                this.recordDeathEntity(replay, session, tick, 1);
            }
            else if (session.livingEntity)
            {
                iterator.remove();
            }
        }
    }

    private boolean registerSession(Recorder recorder, Entity target, int replayIndex)
    {
        Session session = new Session(target.getId(), replayIndex, target instanceof LivingEntity);
        Form form = recorder.film.replays.getList().get(replayIndex).form.get();

        if (target instanceof LivingEntity living)
        {
            session.tracksSnowTrail = this.isSnowGolem(form, living);
            this.updateSessionState(session, living);
            this.recordFireAndParticlesIfChanged(recorder.film.replays.getList().get(replayIndex), session, living, recorder.getTick());
        }
        else
        {
            this.updateSessionState(session, target);
        }

        this.sessions.add(session);
        this.capturedEntityIds.add(target.getId());
        this.entityReplayIndices.put(target.getId(), replayIndex);
        this.refreshFilmUi(recorder);

        return true;
    }

    private void handleDeathForFilm(Film film, Replay replay, Session session, LivingEntity living, int tick, ClientWorld world)
    {
        this.applyDeathEffectKeyframes(replay, session, tick);

        BaseValue.edit(replay.actions, (actions) ->
        {
            MobDeathActionClip deathClip = new MobDeathActionClip();

            deathClip.tick.set(tick);
            deathClip.duration.set(1);
            actions.addClip(deathClip);

            if (!this.captureNearbyDrops(replay, tick, session.deathX, session.deathY, session.deathZ, world))
            {
                this.captureEquipmentDrops(replay, living, tick, session.deathX, session.deathY, session.deathZ);
            }
        });
    }

    private void capturePlayerVehicle(Recorder recorder)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;

        if (player == null)
        {
            return;
        }

        Entity vehicle = player.getVehicle();

        if (vehicle != null)
        {
            this.tryCapture(recorder, vehicle);
        }
    }

    public void recordTick(Recorder recorder)
    {
        this.capturePlayerVehicle(recorder);

        if (this.sessions.isEmpty())
        {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld world = mc.world;

        if (world == null)
        {
            return;
        }

        int tick = recorder.getTick();
        Film film = recorder.film;
        Iterator<Session> iterator = this.sessions.iterator();

        while (iterator.hasNext())
        {
            Session session = iterator.next();

            if (session.replayIndex < 0 || session.replayIndex >= film.replays.getList().size())
            {
                iterator.remove();
                continue;
            }

            Replay replay = film.replays.getList().get(session.replayIndex);
            Entity entity = world.getEntityById(session.entityId);

            if (session.recordingDeath)
            {
                session.deathTickIndex += 1;
                this.recordDeathEntity(replay, session, tick, Math.min(session.deathTickIndex, DEATH_ANIMATION_TICKS));

                if (session.deathTickIndex >= DEATH_ANIMATION_TICKS)
                {
                    this.finishDeathRecording(recorder, replay, tick, iterator);
                }

                continue;
            }

            if (entity == null)
            {
                if (!session.livingEntity)
                {
                    this.applyDeathVisibilityKeyframes(replay, tick);
                    iterator.remove();
                }
                else if (session.deathHandled)
                {
                    session.recordingDeath = true;
                    session.deathTickIndex = 1;
                    this.recordDeathEntity(replay, session, tick, 1);
                }
                else
                {
                    iterator.remove();
                }

                continue;
            }

            if (!session.livingEntity)
            {
                this.updateSessionState(session, entity);
                this.recordEntity(replay, entity, tick);
                this.syncMobFormNbt(replay, entity);
                continue;
            }

            if (entity instanceof LivingEntity living)
            {
                boolean dying = !living.isAlive() || living.deathTime > 0;

                if (!dying)
                {
                    this.updateSessionState(session, living);
                    this.recordEntity(replay, entity, tick);
                    this.recordFireAndParticlesIfChanged(replay, session, living, tick);

                    if (session.tracksSnowTrail)
                    {
                        RecorderWorldEffectCapture.captureSnowTrail(replay, session.snowTrailSnapshots, living, tick, world);
                    }
                }
                else
                {
                    if (!session.deathHandled)
                    {
                        this.captureDeathState(session, living);
                        this.handleDeath(recorder, replay, session, living, tick, world);
                        session.deathHandled = true;
                        session.recordingDeath = true;
                        session.deathTickIndex = living.deathTime > 0 ? living.deathTime : 1;
                    }

                    this.recordDeathEntity(replay, session, tick, Math.min(session.deathTickIndex, DEATH_ANIMATION_TICKS));

                    if (session.deathTickIndex >= DEATH_ANIMATION_TICKS)
                    {
                        this.finishDeathRecording(recorder, replay, tick, iterator);
                    }
                    else
                    {
                        session.recordingDeath = true;
                    }
                }
            }
            else if (session.deathHandled)
            {
                session.recordingDeath = true;
                session.deathTickIndex = 1;
                this.recordDeathEntity(replay, session, tick, 1);
            }
            else if (session.livingEntity)
            {
                iterator.remove();
            }
        }
    }

    public void simplify(Film film)
    {
        for (Session session : this.sessions)
        {
            if (session.replayIndex >= 0 && session.replayIndex < film.replays.getList().size())
            {
                Replay replay = film.replays.getList().get(session.replayIndex);

                for (KeyframeChannel<?> channel : replay.keyframes.getChannels())
                {
                    channel.simplify();
                }

                BaseValue poseValue = replay.properties.get("pose");

                if (poseValue instanceof KeyframeChannel<?> poseChannel)
                {
                    poseChannel.simplify();
                }
            }
        }
    }

    private void finishDeathRecording(Recorder recorder, Replay replay, int disappearTick, Iterator<Session> iterator)
    {
        this.applyDeathVisibilityKeyframes(replay, disappearTick);
        iterator.remove();
        this.refreshFilmUi(recorder);
    }

    private void applyDeathVisibilityKeyframes(Replay replay, int disappearTick)
    {
        Form form = replay.form.get();

        if (form == null || disappearTick < 0)
        {
            return;
        }

        int visibleTick = disappearTick - 1;

        BaseValue.edit(replay.properties, (properties) ->
        {
            KeyframeChannel channel = properties.getOrCreate(form, "render");

            if (channel == null)
            {
                return;
            }

            if (visibleTick >= 0)
            {
                channel.insert(visibleTick, Boolean.TRUE);
            }

            channel.insert(disappearTick, Boolean.FALSE);
        });
    }

    private void applyDeathEffectKeyframes(Replay replay, Session session, int deathTick)
    {
        if (deathTick < 0)
        {
            return;
        }

        replay.keyframes.fire.insert(deathTick, 0D);
        replay.keyframes.particles.insert(deathTick, 0D);
        session.lastFire = Boolean.FALSE;
        session.lastParticles = Boolean.FALSE;
    }

    private void recordFireAndParticlesIfChanged(Replay replay, Session session, LivingEntity living, int tick)
    {
        boolean fire = living.getFireTicks() > 0;
        boolean particles = living.isAlive();

        if (session.lastFire == null || session.lastFire.booleanValue() != fire)
        {
            replay.keyframes.fire.insert(tick, fire ? 1D : 0D);
            session.lastFire = fire;
        }

        if (session.lastParticles == null || session.lastParticles.booleanValue() != particles)
        {
            replay.keyframes.particles.insert(tick, particles ? 1D : 0D);
            session.lastParticles = particles;
        }
    }

    private boolean isSnowGolem(Form form, LivingEntity living)
    {
        if (form instanceof MobForm mobForm && mobForm.mobID.get().equals("minecraft:snow_golem"))
        {
            return true;
        }

        return living.getType() == EntityType.SNOW_GOLEM;
    }

    private static final List<String> MOB_NBT_STRIP_KEYS = Arrays.asList("Pos", "Motion", "Rotation", "FallDistance", "Fire", "Air", "OnGround", "Invulnerable", "PortalCooldown", "UUID");

    private void recordEntity(Replay replay, Entity entity, int tick)
    {
        MCEntity wrapper = new MCEntity(entity);

        wrapper.update();
        replay.keyframes.record(tick, wrapper, null);
        this.syncMobFormNbt(replay, entity);

        Form form = replay.form.get();

        if (MobCemPoseCapture.isActive(replay))
        {
            MobCemPoseCapture.recordPoseKeyframe(replay, form, wrapper, tick, 0F);
        }

        if (form instanceof MobForm mobForm)
        {
            MobCemItemCapture.recordItemStats(replay, mobForm, wrapper, tick, 0F);
        }
    }

    private void syncMobFormNbt(Replay replay, Entity entity)
    {
        Form form = replay.form.get();

        if (!(form instanceof MobForm mobForm))
        {
            return;
        }

        NbtCompound compound = entity.writeNbt(new NbtCompound());

        for (String key : MOB_NBT_STRIP_KEYS)
        {
            compound.remove(key);
        }

        mobForm.mobNBT.set(compound.toString());
    }

    private void recordDeathEntity(Replay replay, Session session, int tick, int deathTime)
    {
        StubEntity wrapper = new StubEntity(MinecraftClient.getInstance().world);

        wrapper.setPosition(session.deathX, session.deathY, session.deathZ);
        wrapper.setPrevX(session.deathX);
        wrapper.setPrevY(session.deathY);
        wrapper.setPrevZ(session.deathZ);
        wrapper.setYaw(session.deathYaw);
        wrapper.setPitch(session.deathPitch);
        wrapper.setHeadYaw(session.deathHeadYaw);
        wrapper.setBodyYaw(session.deathBodyYaw);
        wrapper.setPrevYaw(session.deathYaw);
        wrapper.setPrevPitch(session.deathPitch);
        wrapper.setPrevHeadYaw(session.deathHeadYaw);
        wrapper.setPrevBodyYaw(session.deathBodyYaw);
        wrapper.setDeathTime(deathTime);
        wrapper.setHurtTimer(0);
        wrapper.setSneaking(false);
        wrapper.setSprinting(false);
        wrapper.setOnGround(true);
        wrapper.setVelocity(0F, 0F, 0F);

        replay.keyframes.record(tick, wrapper, null);
    }

    private void updateSessionState(Session session, Entity entity)
    {
        session.lastX = entity.getX();
        session.lastY = entity.getY();
        session.lastZ = entity.getZ();
        session.lastYaw = entity.getYaw();
        session.lastPitch = entity.getPitch();

        if (entity instanceof LivingEntity living)
        {
            session.lastHeadYaw = living.getHeadYaw();
            session.lastBodyYaw = living.bodyYaw;
        }
        else
        {
            session.lastHeadYaw = entity.getYaw();
            session.lastBodyYaw = entity.getYaw();
        }
    }

    private void captureDeathState(Session session, LivingEntity living)
    {
        if (living.isAlive() || living.deathTime <= 1)
        {
            session.deathX = living.getX();
            session.deathY = living.getY();
            session.deathZ = living.getZ();
            session.deathYaw = living.getYaw();
            session.deathPitch = living.getPitch();
            session.deathHeadYaw = living.getHeadYaw();
            session.deathBodyYaw = living.bodyYaw;
        }
        else
        {
            session.deathX = session.lastX;
            session.deathY = session.lastY;
            session.deathZ = session.lastZ;
            session.deathYaw = session.lastYaw;
            session.deathPitch = session.lastPitch;
            session.deathHeadYaw = session.lastHeadYaw;
            session.deathBodyYaw = session.lastBodyYaw;
        }
    }

    private void handleDeath(Recorder recorder, Replay replay, Session session, LivingEntity living, int tick, ClientWorld world)
    {
        this.applyDeathEffectKeyframes(replay, session, tick);

        BaseValue.edit(replay.actions, (actions) ->
        {
            MobDeathActionClip deathClip = new MobDeathActionClip();

            deathClip.tick.set(tick);
            deathClip.duration.set(1);
            actions.addClip(deathClip);

            if (!this.captureNearbyDrops(replay, tick, session.deathX, session.deathY, session.deathZ, world))
            {
                this.captureEquipmentDrops(replay, living, tick, session.deathX, session.deathY, session.deathZ);
            }
        });

        this.refreshFilmUi(recorder);
    }

    private boolean captureNearbyDrops(Replay replay, int tick, double x, double y, double z, ClientWorld world)
    {
        Box box = new Box(
            x - DROP_SCAN_RADIUS, y - DROP_SCAN_RADIUS, z - DROP_SCAN_RADIUS,
            x + DROP_SCAN_RADIUS, y + DROP_SCAN_RADIUS, z + DROP_SCAN_RADIUS
        );
        List<ItemEntity> items = world.getEntitiesByClass(ItemEntity.class, box, (item) -> item.age <= 2);
        boolean found = false;

        for (ItemEntity item : items)
        {
            if (item.getStack().isEmpty())
            {
                continue;
            }

            this.addItemDropClip(replay, tick, item.getPos(), item.getVelocity(), item.getStack());
            found = true;
        }

        return found;
    }

    private void captureEquipmentDrops(Replay replay, LivingEntity living, int tick, double x, double y, double z)
    {
        for (EquipmentSlot slot : EquipmentSlot.values())
        {
            ItemStack stack = living.getEquippedStack(slot);

            if (stack.isEmpty())
            {
                continue;
            }

            Vec3d velocity = new Vec3d(
                (living.getRandom().nextDouble() - 0.5D) * 0.2D,
                living.getRandom().nextDouble() * 0.2D + 0.1D,
                (living.getRandom().nextDouble() - 0.5D) * 0.2D
            );

            this.addItemDropClip(replay, tick, new Vec3d(x, y + 0.5D, z), velocity, stack);
        }
    }

    private void addItemDropClip(Replay replay, int tick, Vec3d pos, Vec3d velocity, ItemStack stack)
    {
        ItemDropActionClip clip = new ItemDropActionClip();

        clip.tick.set(tick);
        clip.duration.set(1);
        clip.posX.set(pos.x);
        clip.posY.set(pos.y);
        clip.posZ.set(pos.z);
        clip.velocityX.set((float) velocity.x);
        clip.velocityY.set((float) velocity.y);
        clip.velocityZ.set((float) velocity.z);
        clip.itemStack.set(stack.copy());
        replay.actions.addClip(clip);
    }

    private String getEntityLabel(Entity entity, Form form)
    {
        if (form instanceof MobForm mobForm && !mobForm.mobID.get().isEmpty())
        {
            String id = mobForm.mobID.get();
            int colon = id.indexOf(':');

            if (colon >= 0 && colon < id.length() - 1)
            {
                return id.substring(colon + 1);
            }

            return id;
        }

        return entity.getName().getString();
    }

    private void refreshFilmUi(Recorder recorder)
    {
        MinecraftClient.getInstance().execute(() ->
        {
            UIDashboard dashboard = BBSModClient.getDashboard();

            if (dashboard == null)
            {
                return;
            }

            UIFilmPanel panel = dashboard.getPanel(UIFilmPanel.class);

            if (panel == null || panel.getData() != recorder.film || !(UIScreen.getCurrentMenu() instanceof UIDashboard))
            {
                return;
            }

            panel.replayEditor.replays.replays.buildVisualList();
            panel.replayEditor.updateChannelsList();
            panel.getController().createEntities();
        });
    }
}
