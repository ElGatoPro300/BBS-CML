package mchorse.bbs_mod.events.register;

import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;

import java.util.function.BiConsumer;

public class RegisterPropTransformEvent
{
    public void register(BiConsumer<UIPropTransform, ContextMenuManager> consumer)
    {
        UIPropTransform.contextMenuExtensions.add(consumer);
    }
}
