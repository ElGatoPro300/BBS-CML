package mchorse.bbs_mod.film;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.mixin.client.FireworkRocketEntityAccessor;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIScreen;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

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

        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld world = mc.world;
        ClientPlayerEntity player = mc.player;

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

        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld world = mc.world;
        ClientPlayerEntity player = mc.player;

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

    private void scanForNewProjectiles(Recorder recorder, RecorderMobCapture mobCapture, ClientWorld world, ClientPlayerEntity player, Set<Integer> capturedMobIds, int tick, Film film, int playerReplayIndex)
    {
        Map<String, Integer> actors = recorder == null ? null : recorder.getActors();

        this.scanForNewProjectiles(recorder, mobCapture, world, player, capturedMobIds, tick, film, playerReplayIndex, actors);
    }

    private void scanForNewProjectiles(Recorder recorder, RecorderMobCapture mobCapture, ClientWorld world, ClientPlayerEntity player, Set<Integer> capturedMobIds, int tick, Film film, int playerReplayIndex, Map<String, Integer> actors)
    {
        Box box = player.getBoundingBox().expand(SCAN_RADIUS);
        List<Entity> projectiles = world.getOtherEntities(player, box, this::isFreshProjectile);

        for (Entity projectile : projectiles)
        {
            Entity owner = RecorderProjectileCapture.resolveOwner(projectile);

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

    private boolean isFreshProjectile(Entity entity)
    {
        if (entity == null || !entity.isAlive() || entity.age > MAX_PROJECTILE_AGE)
        {
            return false;
        }

        if (this.capturedProjectileIds.contains(entity.getId()))
        {
            return false;
        }

        if (entity instanceof LivingEntity)
        {
            return false;
        }

        return entity instanceof ProjectileEntity || entity instanceof FireworkRocketEntity;
    }

    private static Entity resolveOwner(Entity projectile)
    {
        if (projectile instanceof ProjectileEntity projectileEntity)
        {
            Entity owner = projectileEntity.getOwner();

            if (owner != null)
            {
                return owner;
            }
        }

        if (projectile instanceof FireworkRocketEntity firework)
        {
            LivingEntity shooter = ((FireworkRocketEntityAccessor) firework).bbs$getShooter();

            if (shooter != null)
            {
                return shooter;
            }
        }

        return null;
    }

    private int resolveOwnerReplayIndex(RecorderMobCapture mobCapture, Entity owner, Set<Integer> capturedMobIds, Map<String, Integer> actors, Film film, ClientPlayerEntity player, int playerReplayIndex)
    {
        if (player != null && owner instanceof PlayerEntity && owner.getUuid().equals(player.getUuid()) && playerReplayIndex >= 0 && playerReplayIndex < film.replays.getList().size())
        {
            return playerReplayIndex;
        }

        if (mobCapture != null && capturedMobIds.contains(owner.getId()))
        {
            return mobCapture.getReplayIndexForEntity(owner.getId());
        }

        if (actors != null)
        {
            List<Replay> replays = film.replays.getList();

            for (int i = 0; i < replays.size(); i++)
            {
                Replay replay = replays.get(i);
                Integer entityId = actors.get(replay.getId());

                if (entityId != null && entityId == owner.getId())
                {
                    return i;
                }
            }
        }

        return -1;
    }

    private void trackActiveSessions(Recorder recorder, ClientWorld world, int tick, Film film)
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
            Entity entity = world.getEntityById(session.entityId);

            if (entity != null && entity.isAlive())
            {
                session.lastX = entity.getX();
                session.lastY = entity.getY();
                session.lastZ = entity.getZ();
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

    private boolean tryCapture(Recorder recorder, Entity projectile, int tick, Film film, int ownerReplayIndex)
    {
        if (this.capturedProjectileIds.contains(projectile.getId()))
        {
            return false;
        }

        if (ownerReplayIndex < 0 || ownerReplayIndex >= film.replays.getList().size())
        {
            return false;
        }

        Replay ownerReplay = film.replays.getList().get(ownerReplayIndex);

        RecorderWorldEffectCapture.addSummonCommand(ownerReplay, tick, projectile);

        Session session = new Session(projectile.getId(), ownerReplayIndex, tick);

        session.lastX = projectile.getX();
        session.lastY = projectile.getY();
        session.lastZ = projectile.getZ();
        this.sessions.add(session);
        this.capturedProjectileIds.add(projectile.getId());
        this.refreshFilmUi(film);

        return true;
    }

    private void captureImpactEffects(Replay replay, Session session, int tick, ClientWorld world)
    {
        if (world == null)
        {
            return;
        }

        BlockPos center = BlockPos.ofFloored(session.lastX, session.lastY, session.lastZ);
        int radius = IMPACT_EFFECT_RADIUS;

        for (int dx = -radius; dx <= radius; dx++)
        {
            for (int dy = -radius; dy <= radius; dy++)
            {
                for (int dz = -radius; dz <= radius; dz++)
                {
                    BlockPos pos = center.add(dx, dy, dz);
                    long key = pos.asLong();

                    if (session.capturedEffectPositions.contains(key))
                    {
                        continue;
                    }

                    BlockState state = world.getBlockState(pos);

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

    private boolean isProjectileEffectBlock(BlockState state)
    {
        Block block = state.getBlock();

        return block == Blocks.FIRE
            || block == Blocks.SOUL_FIRE
            || block == Blocks.CAMPFIRE
            || block == Blocks.SOUL_CAMPFIRE;
    }

    private void refreshFilmUi(Film film)
    {
        if (film == null)
        {
            return;
        }

        MinecraftClient.getInstance().execute(() ->
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
