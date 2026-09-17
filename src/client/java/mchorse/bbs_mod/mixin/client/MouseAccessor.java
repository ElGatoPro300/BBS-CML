package mchorse.bbs_mod.mixin.client;

import net.minecraft.client.Mouse;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Mouse.class)
public interface MouseAccessor
{
    @Accessor("x")
    public void bbs$setX(double x);

    @Accessor("y")
    public void bbs$setY(double y);

    @Accessor("cursorDeltaX")
    public void bbs$setCursorDeltaX(double cursorDeltaX);

    @Accessor("cursorDeltaY")
    public void bbs$setCursorDeltaY(double cursorDeltaY);
}
