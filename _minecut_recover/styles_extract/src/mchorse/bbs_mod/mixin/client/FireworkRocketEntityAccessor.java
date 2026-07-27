package mchorse.bbs_mod.mixin.client;

import net.minecraft.class_1309;
import net.minecraft.class_1671;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(class_1671.class)
public interface FireworkRocketEntityAccessor
{
    @Accessor("shooter")
    class_1309 bbs$getShooter();
}
