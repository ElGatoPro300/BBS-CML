package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.selectors.ISelectorOwnerProvider;
import mchorse.bbs_mod.selectors.SelectorOwner;
import net.minecraft.class_1297;
import net.minecraft.class_1299;
import net.minecraft.class_1309;
import net.minecraft.class_1937;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(class_1309.class)
public abstract class LivingEntityMixin extends class_1297 implements ISelectorOwnerProvider
{
    public SelectorOwner selector = new SelectorOwner((class_1309) (Object) this);

    protected LivingEntityMixin(class_1299<?> type, class_1937 world)
    {
        super(type, world);
    }

    @Override
    public SelectorOwner getOwner()
    {
        return this.selector;
    }
}