package mchorse.bbs_mod.actions;

import com.mojang.authlib.GameProfile;

import com.google.common.collect.MapMaker;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.UUID;
import net.minecraft.class_1263;
import net.minecraft.class_1282;
import net.minecraft.class_1297;
import net.minecraft.class_1496;
import net.minecraft.class_2281;
import net.minecraft.class_2338;
import net.minecraft.class_2625;
import net.minecraft.class_268;
import net.minecraft.class_2680;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import net.minecraft.class_3417;
import net.minecraft.class_3419;
import net.minecraft.class_3445;
import net.minecraft.class_3908;
import net.minecraft.class_8791;
import org.jetbrains.annotations.Nullable;

public class SuperFakePlayer extends class_3222
{
    private static final GameProfile PROFILE = new GameProfile(UUID.fromString("12345678-9ABC-DEF1-2345-6789ABCDEF69"), "[BBS Player]");
    private static final Map<SuperFakePlayer.FakePlayerKey, SuperFakePlayer> FAKE_PLAYER_MAP = new MapMaker().weakValues().makeMap();
    private final Map<String, class_2338> replayChestPositions = new HashMap<>();

    public static SuperFakePlayer get(class_3218 world)
    {
        Objects.requireNonNull(world, "World may not be null.");

        return FAKE_PLAYER_MAP.computeIfAbsent(new SuperFakePlayer.FakePlayerKey(world, PROFILE), key -> new SuperFakePlayer(key.world, key.profile));
    }

    protected SuperFakePlayer(class_3218 world, GameProfile profile)
    {
        super(world.method_8503(), world, profile, class_8791.method_53821());

        this.field_13987 = new SuperFakePlayerNetworkHandler(this);
    }

    @Override
    protected int method_5691()
    {
        return 2;
    }

    @Override
    public boolean method_9201()
    {
        return false;
    }

    @Override
    public boolean method_9200()
    {
        return false;
    }

    @Override
    public void method_5773()
    {}

    @Override
    public void method_14213(class_8791 settings)
    {}

    @Override
    public void method_7342(class_3445<?> stat, int amount)
    {}

    @Override
    public void method_7266(class_3445<?> stat)
    {}

    @Override
    public boolean method_5679(class_1282 damageSource)
    {
        return true;
    }

    @Nullable
    @Override
    public class_268 method_5781()
    {
        return null;
    }

    @Override
    public void method_18403(class_2338 pos)
    {}

    @Override
    public boolean method_5873(class_1297 entity, boolean force)
    {
        return false;
    }

    @Override
    public void method_7311(class_2625 sign, boolean front)
    {}

    @Override
    public OptionalInt method_17355(@Nullable class_3908 factory)
    {
        return super.method_17355(factory);
    }

    @Override
    public void method_7291(class_1496 horse, class_1263 inventory)
    {}

    public void openReplayChest(String replayId, class_2338 pos)
    {
        if (replayId == null || replayId.isBlank() || pos == null)
        {
            return;
        }

        this.closeReplayChest(replayId);

        class_2680 state = this.method_37908().method_8320(pos);

        if (state.method_26204() instanceof class_2281)
        {
            this.method_37908().method_8427(pos, state.method_26204(), 1, 1);
            this.method_37908().method_8396(null, pos, class_3417.field_14982, class_3419.field_15245, 0.5F, this.method_37908().method_8409().method_43057() * 0.1F + 0.9F);
            this.replayChestPositions.put(replayId, pos.method_10062());
        }
    }

    public void closeReplayChest(String replayId)
    {
        if (replayId == null || replayId.isBlank())
        {
            return;
        }

        class_2338 replayChestPos = this.replayChestPositions.remove(replayId);

        if (replayChestPos == null)
        {
            return;
        }

        class_2680 state = this.method_37908().method_8320(replayChestPos);

        if (state.method_26204() instanceof class_2281)
        {
            this.method_37908().method_8427(replayChestPos, state.method_26204(), 1, 0);
            this.method_37908().method_8396(null, replayChestPos, class_3417.field_14823, class_3419.field_15245, 0.5F, this.method_37908().method_8409().method_43057() * 0.1F + 0.9F);
        }
    }

    private record FakePlayerKey(class_3218 world, GameProfile profile)
    {}
}
