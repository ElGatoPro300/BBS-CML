package mchorse.bbs_mod.ui.utils.keys;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.UIKeybinds;

import java.util.ArrayList;
import java.util.List;

/**
 * Keybind manager
 */
public class KeybindManager
{
    public List<Keybind> keybinds = new ArrayList<>();
    public boolean focus = true;

    public Keybind register(KeyCombo combo, Runnable callback)
    {
        Keybind keybind = new Keybind(combo, callback);

        this.keybinds.add(keybind);

        return keybind;
    }

    public KeybindManager ignoreFocus()
    {
        this.focus = false;

        return this;
    }

    public void add(UIContext context, boolean inside)
    {
        if (this.focus && context.isFocused())
        {
            return;
        }

        UIKeybinds keybinds = context.keybinds;

        if (!keybinds.hasParent())
        {
            return;
        }

        for (Keybind keybind : this.keybinds)
        {
            if (keybind.isActive() && (!keybind.inside || inside))
            {
                keybinds.addKeybind(keybind);
            }
        }
    }

    public boolean check(UIContext context, boolean inside)
    {
        if (context.getKeyAction() == KeyAction.RELEASED || (this.focus && context.isFocused()))
        {
            return false;
        }

        return this.checkKeybinds(context, inside, false);
    }

    public boolean checkMouse(UIContext context, boolean inside)
    {
        if (this.focus && context.isFocused())
        {
            return false;
        }

        return this.checkKeybinds(context, inside, true);
    }

    private boolean checkKeybinds(UIContext context, boolean inside, boolean mouse)
    {
        int keyCode = context.getKeyCode();
        int size = this.keybinds.size();
        int index = -1;
        int score = 0;

        for (int i = 0; i < size; i++)
        {
            Keybind keybind = this.keybinds.get(i);

            if (keybind.callback != null && keybind.isActive())
            {
                boolean condition = mouse
                    ? keybind.checkMouse(context.mouseButton, inside)
                    : keybind.check(keyCode, context.getKeyAction(), inside);

                if (condition)
                {
                    int keybindScore = keybind.getScore();

                    if (index == -1 || keybindScore > score)
                    {
                        index = i;
                        score = keybindScore;
                    }
                }
            }
        }

        if (index >= 0)
        {
            this.keybinds.get(index).callback.run();

            return true;
        }

        return false;
    }
}