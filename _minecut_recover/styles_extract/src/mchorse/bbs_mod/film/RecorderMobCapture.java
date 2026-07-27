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
import net.minecraft.class_1297;
import net.minecraft.class_1299;
import net.minecraft.class_1304;
import net.minecraft.class_1309;
import net.minecraft.class_1542;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_2487;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_638;
import net.minecraft.class_746;
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
        public final Map<Long, class_2680> snowTrailSnapshots = new HashMap<>();

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

    public static void onEntityInteraction(class_1297 target)
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
        class_1297 vehicle = MorphMountSync.resolveVehicleEntity(entity);

        if (vehicle != null)
        {
            mountIndex = RecorderMobCapture.resolveReplayIndexForEntity(vehicle.method_5628());
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

    public boolean tryCapture(Recorder recorder, class_1297 target)
    {
        return this.tryCapture(recorder, target, "");
    }

    public boolean tryCapture(Recorder recorder, class_1297 target, String groupPath)
    {
        if (target == null || target instanceof class_1657)
        {
            return false;
        }

        if (this.capturedEntityIds.contains(target.method_5628()))
        {
            return false;
        }

        class_310 mc = class_310.method_1551();
        class_746 player = mc.field_1724;

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
            this.applyVanillaMobPlayback(replay, this.vanillaPlaybackEntityIds.contains(target.method_5628()));

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
        class_310 mc = class_310.method_1551();
        class_746 player = mc.field_1724;

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

                for (class_1297 entity : bucket.entities)
                {
                    if (setup.selectedEntityIds.contains(entity.method_5628()))
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

                for (class_1297 entity : bucket.entities)
                {
                    if (!setup.selectedEntityIds.contains(entity.method_5628()))
                    {
                        continue;
                    }

                    if (this.capturedEntityIds.contains(entity.method_5628()))
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
                    this.applyVanillaMobPlayback(replay, setup.vanillaPlaybackEntityIds.contains(entity.method_5628()));
                    this.recordEntity(replay, entity, tick);

                    replays.add(replay);

                    Session session = new Session(entity.method_5628(), list.indexOf(replay), entity instanceof class_1309);

                    if (entity instanceof class_1309 living)
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
                    this.capturedEntityIds.add(entity.method_5628());
                    this.entityReplayIndices.put(entity.method_5628(), session.replayIndex);
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

        class_310 mc = class_310.method_1551();
        class_638 world = mc.field_1687;

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
            class_1297 entity = world.method_8469(session.entityId);

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

            if (entity instanceof class_1309 living)
            {
                boolean dying = !living.method_5805() || living.field_6213 > 0;

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
                        session.deathTickIndex = living.field_6213 > 0 ? living.field_6213 : 1;
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

    private boolean registerSession(Recorder recorder, class_1297 target, int replayIndex)
    {
        Session session = new Session(target.method_5628(), replayIndex, target instanceof class_1309);
        Form form = recorder.film.replays.getList().get(replayIndex).form.get();

        if (target instanceof class_1309 living)
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
        this.capturedEntityIds.add(target.method_5628());
        this.entityReplayIndices.put(target.method_5628(), replayIndex);
        this.refreshFilmUi(recorder);

        return true;
    }

    private void handleDeathForFilm(Film film, Replay replay, Session session, class_1309 living, int tick, class_638 world)
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
        class_310 mc = class_310.method_1551();
        class_746 player = mc.field_1724;

        if (player == null)
        {
            return;
        }

        class_1297 vehicle = player.method_5854();

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

        class_310 mc = class_310.method_1551();
        class_638 world = mc.field_1687;

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
            class_1297 entity = world.method_8469(session.entityId);

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

            if (entity instanceof class_1309 living)
            {
                boolean dying = !living.method_5805() || living.field_6213 > 0;

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
                        session.deathTickIndex = living.field_6213 > 0 ? living.field_6213 : 1;
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

    private void recordFireAndParticlesIfChanged(Replay replay, Session session, class_1309 living, int tick)
    {
        boolean fire = living.method_20802() > 0;
        boolean particles = living.method_5805();

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

    private boolean isSnowGolem(Form form, class_1309 living)
    {
        if (form instanceof MobForm mobForm && mobForm.mobID.get().equals("minecraft:snow_golem"))
        {
            return true;
        }

        return living.method_5864() == class_1299.field_6047;
    }

    private static final List<String> MOB_NBT_STRIP_KEYS = Arrays.asList("Pos", "Motion", "Rotation", "FallDistance", "Fire", "Air", "OnGround", "Invulnerable", "PortalCooldown", "UUID");

    private void recordEntity(Replay replay, class_1297 entity, int tick)
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

    private void syncMobFormNbt(Replay replay, class_1297 entity)
    {
        Form form = replay.form.get();

        if (!(form instanceof MobForm mobForm))
        {
            return;
        }

        class_2487 compound = entity.method_5647(new class_2487());

        for (String key : MOB_NBT_STRIP_KEYS)
        {
            compound.method_10551(key);
        }

        mobForm.mobNBT.set(compound.toString());
    }

    private void recordDeathEntity(Replay replay, Session session, int tick, int deathTime)
    {
        StubEntity wrapper = new StubEntity(class_310.method_1551().field_1687);

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

    private void updateSessionState(Session session, class_1297 entity)
    {
        session.lastX = entity.method_23317();
        session.lastY = entity.method_23318();
        session.lastZ = entity.method_23321();
        session.lastYaw = entity.method_36454();
        session.lastPitch = entity.method_36455();

        if (entity instanceof class_1309 living)
        {
            session.lastHeadYaw = living.method_5791();
            session.lastBodyYaw = living.field_6283;
        }
        else
        {
            session.lastHeadYaw = entity.method_36454();
            session.lastBodyYaw = entity.method_36454();
        }
    }

    private void captureDeathState(Session session, class_1309 living)
    {
        if (living.method_5805() || living.field_6213 <= 1)
        {
            session.deathX = living.method_23317();
            session.deathY = living.method_23318();
            session.deathZ = living.method_23321();
            session.deathYaw = living.method_36454();
            session.deathPitch = living.method_36455();
            session.deathHeadYaw = living.method_5791();
            session.deathBodyYaw = living.field_6283;
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

    private void handleDeath(Recorder recorder, Replay replay, Session session, class_1309 living, int tick, class_638 world)
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

    private boolean captureNearbyDrops(Replay replay, int tick, double x, double y, double z, class_638 world)
    {
        class_238 box = new class_238(
            x - DROP_SCAN_RADIUS, y - DROP_SCAN_RADIUS, z - DROP_SCAN_RADIUS,
            x + DROP_SCAN_RADIUS, y + DROP_SCAN_RADIUS, z + DROP_SCAN_RADIUS
        );
        List<class_1542> items = world.method_8390(class_1542.class, box, (item) -> item.field_6012 <= 2);
        boolean found = false;

        for (class_1542 item : items)
        {
            if (item.method_6983().method_7960())
            {
                continue;
            }

            this.addItemDropClip(replay, tick, item.method_19538(), item.method_18798(), item.method_6983());
            found = true;
        }

        return found;
    }

    private void captureEquipmentDrops(Replay replay, class_1309 living, int tick, double x, double y, double z)
    {
        for (class_1304 slot : class_1304.values())
        {
            class_1799 stack = living.method_6118(slot);

            if (stack.method_7960())
            {
                continue;
            }

            class_243 velocity = new class_243(
                (living.method_59922().method_43058() - 0.5D) * 0.2D,
                living.method_59922().method_43058() * 0.2D + 0.1D,
                (living.method_59922().method_43058() - 0.5D) * 0.2D
            );

            this.addItemDropClip(replay, tick, new class_243(x, y + 0.5D, z), velocity, stack);
        }
    }

    private void addItemDropClip(Replay replay, int tick, class_243 pos, class_243 velocity, class_1799 stack)
    {
        ItemDropActionClip clip = new ItemDropActionClip();

        clip.tick.set(tick);
        clip.duration.set(1);
        clip.posX.set(pos.field_1352);
        clip.posY.set(pos.field_1351);
        clip.posZ.set(pos.field_1350);
        clip.velocityX.set((float) velocity.field_1352);
        clip.velocityY.set((float) velocity.field_1351);
        clip.velocityZ.set((float) velocity.field_1350);
        clip.itemStack.set(stack.method_7972());
        replay.actions.addClip(clip);
    }

    private String getEntityLabel(class_1297 entity, Form form)
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

        return entity.method_5477().getString();
    }

    private void refreshFilmUi(Recorder recorder)
    {
        class_310.method_1551().execute(() ->
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
