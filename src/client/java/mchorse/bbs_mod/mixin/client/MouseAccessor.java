package mchorse.bbs_mod.mixin.client;

import net.minecraft.client.MouseHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHandler.class)
public interface MouseAccessor
{
    @Accessor("xpos")
    public void bbs$setX(double x);

    @Accessor("ypos")
    public void bbs$setY(double y);

    @Accessor("accumulatedDX")
    public void bbs$setCursorDeltaX(double cursorDeltaX);

    @Accessor("accumulatedDY")
    public void bbs$setCursorDeltaY(double cursorDeltaY);
}
