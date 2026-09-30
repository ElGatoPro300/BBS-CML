package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.forms.structure.ModelBlockSolidCollisions;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.CollisionView;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NeoForge solid meshes via {@link CollisionView#getBlockCollisions} (proven to remap under
 * Connector). Do NOT inject {@code getEntityCollisions} here — Connector reports it as a failing
 * mixin on {@code Level}/{@code CollisionGetter} and aborts launch.
 * <p>
 * Space-check helpers are suppressed so {@code pushOutOfBlocks}/{@code canCollide} stay vanilla.
 */
@Mixin(CollisionView.class)
public interface CollisionViewMixin
{
    @Inject(method = "canCollide", at = @At("HEAD"), require = 0)
    private void bbs$enterVanillaOnlyCanCollide(@Nullable Entity entity, Box box, CallbackInfoReturnable<Boolean> cir)
    {
        ModelBlockSolidCollisions.enterVanillaBlockCollisionOnly();
    }

    @Inject(method = "canCollide", at = @At("RETURN"), require = 0)
    private void bbs$exitVanillaOnlyCanCollide(@Nullable Entity entity, Box box, CallbackInfoReturnable<Boolean> cir)
    {
        ModelBlockSolidCollisions.exitVanillaBlockCollisionOnly();
    }

    @Inject(method = "isBlockSpaceEmpty", at = @At("HEAD"), require = 0)
    private void bbs$enterVanillaOnlyBlockSpaceEmpty(@Nullable Entity entity, Box box, CallbackInfoReturnable<Boolean> cir)
    {
        ModelBlockSolidCollisions.enterVanillaBlockCollisionOnly();
    }

    @Inject(method = "isBlockSpaceEmpty", at = @At("RETURN"), require = 0)
    private void bbs$exitVanillaOnlyBlockSpaceEmpty(@Nullable Entity entity, Box box, CallbackInfoReturnable<Boolean> cir)
    {
        ModelBlockSolidCollisions.exitVanillaBlockCollisionOnly();
    }

    @Inject(method = "isSpaceEmpty(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Box;)Z", at = @At("HEAD"), require = 0)
    private void bbs$enterVanillaOnlySpaceEmptyEntityBox(@Nullable Entity entity, Box box, CallbackInfoReturnable<Boolean> cir)
    {
        ModelBlockSolidCollisions.enterVanillaBlockCollisionOnly();
    }

    @Inject(method = "isSpaceEmpty(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Box;)Z", at = @At("RETURN"), require = 0)
    private void bbs$exitVanillaOnlySpaceEmptyEntityBox(@Nullable Entity entity, Box box, CallbackInfoReturnable<Boolean> cir)
    {
        ModelBlockSolidCollisions.exitVanillaBlockCollisionOnly();
    }

    @Inject(method = "isSpaceEmpty(Lnet/minecraft/util/math/Box;)Z", at = @At("HEAD"), require = 0)
    private void bbs$enterVanillaOnlySpaceEmptyBox(Box box, CallbackInfoReturnable<Boolean> cir)
    {
        ModelBlockSolidCollisions.enterVanillaBlockCollisionOnly();
    }

    @Inject(method = "isSpaceEmpty(Lnet/minecraft/util/math/Box;)Z", at = @At("RETURN"), require = 0)
    private void bbs$exitVanillaOnlySpaceEmptyBox(Box box, CallbackInfoReturnable<Boolean> cir)
    {
        ModelBlockSolidCollisions.exitVanillaBlockCollisionOnly();
    }

    @Inject(method = "isSpaceEmpty(Lnet/minecraft/entity/Entity;)Z", at = @At("HEAD"), require = 0)
    private void bbs$enterVanillaOnlySpaceEmptyEntity(@Nullable Entity entity, CallbackInfoReturnable<Boolean> cir)
    {
        ModelBlockSolidCollisions.enterVanillaBlockCollisionOnly();
    }

    @Inject(method = "isSpaceEmpty(Lnet/minecraft/entity/Entity;)Z", at = @At("RETURN"), require = 0)
    private void bbs$exitVanillaOnlySpaceEmptyEntity(@Nullable Entity entity, CallbackInfoReturnable<Boolean> cir)
    {
        ModelBlockSolidCollisions.exitVanillaBlockCollisionOnly();
    }

    @Inject(method = "getBlockCollisions", at = @At("RETURN"), cancellable = true, require = 0)
    private void bbs$appendSolidHitboxes(
        @Nullable Entity entity,
        Box box,
        CallbackInfoReturnable<Iterable<VoxelShape>> cir)
    {
        if (box == null
            || !ModelBlockSolidCollisions.shouldAppendSolidToBlockCollisions()
            || !((Object) this instanceof World world))
        {
            return;
        }

        Iterable<VoxelShape> merged = ModelBlockSolidCollisions.appendToBlockCollisions(
            entity, box, world, cir.getReturnValue());

        if (merged != cir.getReturnValue())
        {
            cir.setReturnValue(merged);
        }
    }
}
