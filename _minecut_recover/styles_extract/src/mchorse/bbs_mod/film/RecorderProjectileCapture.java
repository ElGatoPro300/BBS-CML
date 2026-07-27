package mchorse.bbs_mod.film;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.mixin.client.FireworkRocketEntityAccessor;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIScreen;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_1671;
import net.minecraft.class_1676;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_638;
import net.minecraft.class_746;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Captures projectiles thrown during recording via vanilla {@code summon} commands.
 */
public final class RecorderProjectileCapture
{
    private static final double SCAN_RADIUS = 64D;
    private static final int MAX_PROJECTILE_AGE = 5;
    private static final int IMPACT_EFFECT_SCAN_TICKS = 2;
    private static final int IMPACT_EFFECT_RADIUS = 3;

    public static final class Session
    {
        public final int entityId;
        public final int ownerReplayIndex;
        public final int spawnTick;

        public int impactScanTicks = 0;
        public boolean pendingImpact = false;

        public double lastX;
        public double lastY;
        public double lastZ;

        public final Set<Long> capturedEffectPositions = new HashSet<>();

        public Session(int entityId, int ownerReplayIndex, int spawnTick)
        {
            this.entityId = entityId;
            this.ownerReplayIndex = ownerReplayIndex;
            this.spawnTick = spawnTick;
        }
    }

    private final List<Session> sessions = new ArrayList<>();
    private final Set<Integer> capturedProjectileIds = new HashSet<>();

    public boolean isEmpty()
    {
        return this.sessions.isEmpty();
    }

    public void clear()
    {
        this.sessions.clear();
        this.capturedProjectileIds.clear();
    }

    public static boolean canCapture(Recorder recorder)
    {
        if (!BBSSettings.recordingAutoCaptureProjectiles.get())
        {
            return false;
        }

        return recorder != null && !recorder.hasNotStarted();
    }

    public void recordTick(Recorder recorder, RecorderMobCapture mobCapture)
    {
        if (!RecorderProjectileCapture.canCapture(recorder))
        {
            return;
        }

        class_310 mc = class_310.method_1551();
        class_638 world = mc.field_1687;
        class_746 player = mc.field_1724;

        if (world == null || player == null)
        {
            return;
        }

        Set<Integer> capturedMobIds = mobCapture == null ? Set.of() : mobCapture.getCapturedEntityIds();
        int tick = recorder.getTick();
        Film film = recorder.film;
        int playerReplayIndex = recorder.exception;

        this.scanForNewProjectiles(recorder, mobCapture, world, player, capturedMobIds, tick, film, playerReplayIndex);
        this.trackActiveSessions(recorder, world, tick, film);
    }

    public void recordEditorTick(Film film, int ownerReplayIndex, int tick, RecorderMobCapture mobCapture, Map<String, Integer> actors)
    {
        if (!BBSSettings.recordingAutoCaptureProjectiles.get() || film == null || ownerReplayIndex < 0)
        {
            return;
        }

        class_310 mc = class_310.method_1551();
        class_638 world = mc.field_1687;
        class_746 player = mc.field_1724;

        if (world == null || player == null)
        {
            return;
        }

        Set<Integer> capturedMobIds = mobCapture == null ? Set.of() : mobCapture.getCapturedEntityIds();

        this.scanForNewProjectiles(null, mobCapture, world, player, capturedMobIds, tick, film, ownerReplayIndex, actors);
        this.trackActiveSessions(null, world, tick, film);
    }

    public void simplify(Film film)
    {
    }

    private void scanForNewProjectiles(Recorder recorder, RecorderMobCapture mobCapture, class_638 world, class_746 player, Set<Integer> capturedMobIds, int tick, Film film, int playerReplayIndex)
    {
        Map<String, Integer> actors = recorder == null ? null : recorder.getActors();

        this.scanForNewProjectiles(recorder, mobCapture, world, player, capturedMobIds, tick, film, playerReplayIndex, actors);
    }

    private void scanForNewProjectiles(Recorder recorder, RecorderMobCapture mobCapture, class_638 world, class_746 player, Set<Integer> capturedMobIds, int tick, Film film, int playerReplayIndex, Map<String, Integer> actors)
    {
        class_238 box = player.method_5829().method_1014(SCAN_RADIUS);
        List<class_1297> projectiles = world.method_8333(player, box, this::isFreshProjectile);

        for (class_1297 projectile : projectiles)
        {
            class_1297 owner = RecorderProjectileCapture.resolveOwner(projectile);

            if (owner == null)
            {
                continue;
            }

            int ownerReplayIndex = this.resolveOwnerReplayIndex(mobCapture, owner, capturedMobIds, actors, film, player, playerReplayIndex);

            if (ownerReplayIndex >= 0)
            {
                this.tryCapture(recorder, projectile, tick, film, ownerReplayIndex);
            }
        }
    }

    private boolean isFreshProjectile(class_1297 entity)
    {
        if (entity == null || !entity.method_5805() || entity.field_6012 > MAX_PROJECTILE_AGE)
        {
            return false;
        }

        if (this.capturedProjectileIds.contains(entity.method_5628()))
        {
            return false;
        }

        if (entity instanceof class_1309)
        {
            return false;
        }

        return entity instanceof class_1676 || entity instanceof class_1671;
    }

    private static class_1297 resolveOwner(class_1297 projectile)
    {
        if (projectile instanceof class_1676 projectileEntity)
        {
            class_1297 owner = projectileEntity.method_24921();

            if (owner != null)
            {
                return owner;
            }
        }

        if (projectile instanceof class_1671 firework)
        {
            class_1309 shooter = ((FireworkRocketEntityAccessor) firework).bbs$getShooter();

            if (shooter != null)
            {
                return shooter;
            }
        }

        return null;
    }

    private int resolveOwnerReplayIndex(RecorderMobCapture mobCapture, class_1297 owner, Set<Integer> capturedMobIds, Map<String, Integer> actors, Film film, class_746 player, int playerReplayIndex)
    {
        if (player != null && owner instanceof class_1657 && owner.method_5667().equals(player.method_5667()) && playerReplayIndex >= 0 && playerReplayIndex < film.replays.getList().size())
        {
            return playerReplayIndex;
        }

        if (mobCapture != null && capturedMobIds.contains(owner.method_5628()))
        {
            return mobCapture.getReplayIndexForEntity(owner.method_5628());
        }

        if (actors != null)
        {
            List<Replay> replays = film.replays.getList();

            for (int i = 0; i < replays.size(); i++)
            {
                Replay replay = replays.get(i);
                Integer entityId = actors.get(replay.getId());

                if (entityId != null && entityId == owner.method_5628())
                {
                    return i;
                }
            }
        }

        return -1;
    }

    private void trackActiveSessions(Recorder recorder, class_638 world, int tick, Film film)
    {
        Iterator<Session> iterator = this.sessions.iterator();

        while (iterator.hasNext())
        {
            Session session = iterator.next();

            if (session.ownerReplayIndex < 0 || session.ownerReplayIndex >= film.replays.getList().size())
            {
                iterator.remove();
                continue;
            }

            Replay ownerReplay = film.replays.getList().get(session.ownerReplayIndex);
            class_1297 entity = world.method_8469(session.entityId);

            if (entity != null && entity.method_5805())
            {
                session.lastX = entity.method_23317();
                session.lastY = entity.method_23318();
                session.lastZ = entity.method_23321();
            }
            else if (!session.pendingImpact)
            {
                session.pendingImpact = true;
                session.impactScanTicks = 0;
                this.captureImpactEffects(ownerReplay, session, tick, world);
                this.refreshFilmUi(film);
            }
            else
            {
                this.captureImpactEffects(ownerReplay, session, tick, world);

                if (session.impactScanTicks >= IMPACT_EFFECT_SCAN_TICKS)
                {
                    iterator.remove();
                }
                else
                {
                    session.impactScanTicks += 1;
                }
            }
        }
    }

    private boolean tryCapture(Recorder recorder, class_1297 projectile, int tick, Film film, int ownerReplayIndex)
    {
        if (this.capturedProjectileIds.contains(projectile.method_5628()))
        {
            return false;
        }

        if (ownerReplayIndex < 0 || ownerReplayIndex >= film.replays.getList().size())
        {
            return false;
        }

        Replay ownerReplay = film.replays.getList().get(ownerReplayIndex);

        RecorderWorldEffectCapture.addSummonCommand(ownerReplay, tick, projectile);

        Session session = new Session(projectile.method_5628(), ownerReplayIndex, tick);

        session.lastX = projectile.method_23317();
        session.lastY = projectile.method_23318();
        session.lastZ = projectile.method_23321();
        this.sessions.add(session);
        this.capturedProjectileIds.add(projectile.method_5628());
        this.refreshFilmUi(film);

        return true;
    }

    private void captureImpactEffects(Replay replay, Session session, int tick, class_638 world)
    {
        if (world == null)
        {
            return;
        }

        class_2338 center = class_2338.method_49637(session.lastX, session.lastY, session.lastZ);
        int radius = IMPACT_EFFECT_RADIUS;

        for (int dx = -radius; dx <= radius; dx++)
        {
            for (int dy = -radius; dy <= radius; dy++)
            {
                for (int dz = -radius; dz <= radius; dz++)
                {
                    class_2338 pos = center.method_10069(dx, dy, dz);
                    long key = pos.method_10063();

                    if (session.capturedEffectPositions.contains(key))
                    {
                        continue;
                    }

                    class_2680 state = world.method_8320(pos);

                    if (!this.isProjectileEffectBlock(state))
                    {
                        continue;
                    }

                    session.capturedEffectPositions.add(key);
                    RecorderWorldEffectCapture.addSetblockCommand(replay, tick, pos, state);
                }
            }
        }
    }

    private boolean isProjectileEffectBlock(class_2680 state)
    {
        class_2248 block = state.method_26204();

        return block == class_2246.field_10036
            || block == class_2246.field_22089
            || block == class_2246.field_17350
            || block == class_2246.field_23860;
    }

    private void refreshFilmUi(Film film)
    {
        if (film == null)
        {
            return;
        }

        class_310.method_1551().execute(() ->
        {
            UIDashboard dashboard = BBSModClient.getDashboard();

            if (dashboard == null)
            {
                return;
            }

            UIFilmPanel panel = dashboard.getPanel(UIFilmPanel.class);

            if (panel == null || panel.getData() != film || !(UIScreen.getCurrentMenu() instanceof UIDashboard))
            {
                return;
            }

            panel.replayEditor.replays.replays.buildVisualList();
            panel.replayEditor.updateChannelsList();
            panel.getController().createEntities();
        });
    }
}
