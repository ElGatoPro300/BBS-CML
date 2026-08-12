package mchorse.bbs_mod.actions;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stat;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.scores.PlayerTeam;

import com.mojang.authlib.GameProfile;

import com.google.common.collect.MapMaker;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

public class SuperFakePlayer extends ServerPlayer
{
    private static final GameProfile PROFILE = new GameProfile(UUID.fromString("12345678-9ABC-DEF1-2345-6789ABCDEF69"), "[BBS Player]");
    private static final Map<SuperFakePlayer.FakePlayerKey, SuperFakePlayer> FAKE_PLAYER_MAP = new MapMaker().weakValues().makeMap();
    private final Map<String, BlockPos> replayChestPositions = new HashMap<>();

    public static SuperFakePlayer get(ServerLevel world)
    {
        Objects.requireNonNull(world, "World may not be null.");

        return FAKE_PLAYER_MAP.computeIfAbsent(new SuperFakePlayer.FakePlayerKey(world, PROFILE), key -> new SuperFakePlayer(key.world, key.profile));
    }

    protected SuperFakePlayer(ServerLevel world, GameProfile profile)
    {
        super(world.getServer(), world, profile, ClientInformation.createDefault());

        this.connection = new SuperFakePlayerNetworkHandler(this);
    }

    @Override
    public PermissionSet permissions()
    {
        return PermissionSet.ALL_PERMISSIONS;
    }

    public boolean shouldBroadcastConsoleToOps()
    {
        return false;
    }

    public boolean shouldReceiveFeedback()
    {
        return false;
    }

    @Override
    public void tick()
    {}

    @Override
    public void updateOptions(ClientInformation settings)
    {}

    @Override
    public void awardStat(Stat<?> stat, int amount)
    {}

    @Override
    public void resetStat(Stat<?> stat)
    {}

    public boolean isInvulnerableTo(DamageSource damageSource)
    {
        return true;
    }

    @Nullable
    @Override
    public PlayerTeam getTeam()
    {
        return null;
    }

    @Override
    public void startSleeping(BlockPos pos)
    {}

    @Override
    public boolean startRiding(Entity entity, boolean force, boolean shouldCancelInteract)
    {
        return false;
    }

    @Override
    public void openTextEdit(SignBlockEntity sign, boolean front)
    {}

    @Override
    public OptionalInt openMenu(@Nullable MenuProvider factory)
    {
        return super.openMenu(factory);
    }

    @Override
    public void openHorseInventory(AbstractHorse horse, Container inventory)
    {}

    public void openReplayChest(String replayId, BlockPos pos)
    {
        if (replayId == null || replayId.isBlank() || pos == null)
        {
            return;
        }

        this.closeReplayChest(replayId);

        BlockState state = this.level().getBlockState(pos);

        if (state.getBlock() instanceof ChestBlock)
        {
            this.level().blockEvent(pos, state.getBlock(), 1, 1);
            this.level().playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5F, this.level().getRandom().nextFloat() * 0.1F + 0.9F);
            this.replayChestPositions.put(replayId, pos.immutable());
        }
    }

    public void closeReplayChest(String replayId)
    {
        if (replayId == null || replayId.isBlank())
        {
            return;
        }

        BlockPos replayChestPos = this.replayChestPositions.remove(replayId);

        if (replayChestPos == null)
        {
            return;
        }

        BlockState state = this.level().getBlockState(replayChestPos);

        if (state.getBlock() instanceof ChestBlock)
        {
            this.level().blockEvent(replayChestPos, state.getBlock(), 1, 0);
            this.level().playSound(null, replayChestPos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5F, this.level().getRandom().nextFloat() * 0.1F + 0.9F);
        }
    }

    private record FakePlayerKey(ServerLevel world, GameProfile profile)
    {}
}
