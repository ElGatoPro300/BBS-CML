package mchorse.bbs_mod.ui.framework.elements;

public interface IUITreeEventListener
{
    public void onAddedToTree(UIElement element);

    public void onRemovedFromTree(UIElement element);
}