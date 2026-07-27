package mchorse.bbs_mod.settings.values.ui;

import java.util.List;

public interface IIconToolbarValue
{
    List<String> getOrder();

    List<String> getVisibleOrder();

    boolean isHidden(String id);

    boolean canHide(String id);

    void toggleHidden(String id);

    void moveButton(int from, int to);
}
