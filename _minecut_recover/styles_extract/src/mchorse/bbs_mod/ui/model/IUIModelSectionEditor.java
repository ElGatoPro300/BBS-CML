package mchorse.bbs_mod.ui.model;

import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.utils.pose.UIPoseEditor;

import java.util.List;

public interface IUIModelSectionEditor
{
    void dirty();

    void forceSave();

    UIPoseEditor getPoseEditor();

    String getSelectedBone();

    void setSelectedBone(String bone);

    List<UIModelSection> getSections();

    void setRight(UIElement element);

    UIDashboard getDashboard();

    UIModelPanel getModelPanel();
}
