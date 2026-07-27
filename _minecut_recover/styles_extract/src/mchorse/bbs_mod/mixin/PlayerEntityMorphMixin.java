package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.morphing.IMorphProvider;
import mchorse.bbs_mod.morphing.Morph;
import net.minecraft.class_1299;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_1937;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(class_1657.class)
public abstract class PlayerEntityMorphMixin extends class_1309 implements IMorphProvider
{
    public Morph morph = new Morph(this);

    protected PlayerEntityMorphMixin(class_1299<? extends class_1309> entityType, class_1937 world)
    {
        super(entityType, world);
    }

    @Override
    public Morph getMorph()
    {
        return this.morph;
    }

    @Override
    public void method_5670()
    {
        this.morph.update();

        super.method_5670();
    }
}