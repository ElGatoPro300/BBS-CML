package mchorse.bbs_mod.ui.forms.editors.panels.widgets;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import net.minecraft.class_1747;
import net.minecraft.class_1799;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2680;
import net.minecraft.class_2769;
import net.minecraft.class_2960;
import net.minecraft.class_5321;
import net.minecraft.class_7923;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class UIBlockStateEditor extends UIElement
{
    private static List<String> blockIDs = new ArrayList<>();

    public UISearchList<String> blockList;
    public UIButton inventory;
    public UIElement properties;

    private Consumer<class_2680> callback;
    private class_2680 blockState;

    static
    {
        for (class_5321<class_2248> key : class_7923.field_41175.method_42021())
        {
            blockIDs.add(key.method_29177().toString());
        }

        blockIDs.sort(String::compareToIgnoreCase);
    }

    public UIBlockStateEditor(Consumer<class_2680> callback)
    {
        this.callback = callback;

        this.blockList = new UISearchList<>(new UIStringList((l) -> this.setBlock(l.get(0))));
        this.blockList.label(UIKeys.GENERAL_SEARCH).list.background();
        this.blockList.h(20 + 96);
        this.inventory = new UIButton(UIKeys.ITEM_STACK_CONTEXT_INVENTORY, (b) -> this.openInventoryPanel());
        this.properties = UI.column();

        this.column().vertical().stretch();

        this.add(this.blockList);
        this.add(this.inventory);
        this.add(this.properties);

        this.blockList.list.clear();
        this.blockList.list.add(blockIDs);
    }

    public void setBlockState(class_2680 blockState)
    {
        this.blockState = blockState;

        this.fillPropertiesEditor(blockState);
        this.blockList.list.setCurrentScroll(class_7923.field_41175.method_10221(blockState.method_26204()).toString());
    }

    private void setBlock(String blockID)
    {
        class_2960 id = class_2960.method_60654(blockID);
        class_2680 blockState = class_7923.field_41175.method_10223(id).method_9564();

        this.acceptBlockState(blockState);
        this.fillPropertiesEditor(blockState);
    }

    private void openInventoryPanel()
    {
        UIPlayerInventoryPanel panel = new UIPlayerInventoryPanel((stack) ->
        {
            class_2680 state = this.toBlockState(stack);

            if (state == null)
            {
                return;
            }

            this.acceptBlockState(state);
            this.fillPropertiesEditor(state);
            this.blockList.list.setCurrentScroll(class_7923.field_41175.method_10221(state.method_26204()).toString());
        });

        UIOverlay.addOverlay(this.getContext(), panel, UIPlayerInventoryPanel.PANEL_WIDTH, UIPlayerInventoryPanel.PANEL_HEIGHT);
        UIUtils.playClick();
    }

    private class_2680 toBlockState(class_1799 stack)
    {
        if (stack == null || stack.method_7960())
        {
            return class_2246.field_10124.method_9564();
        }

        if (stack.method_7909() instanceof class_1747 blockItem)
        {
            return blockItem.method_7711().method_9564();
        }

        return null;
    }

    private void acceptBlockState(class_2680 blockState)
    {
        this.blockState = blockState;

        if (this.callback != null)
        {
            this.callback.accept(blockState);
        }
    }

    private void fillPropertiesEditor(class_2680 state)
    {
        this.properties.removeAll();

        for (class_2769 p : state.method_28501())
        {
            UIButton button = new UIButton(IKey.constant(state.method_11654(p).toString()), (b) ->
            {
                this.getContext().replaceContextMenu((menu) ->
                {
                    for (Object v : p.method_11898())
                    {
                        IKey raw = IKey.constant(v.toString());

                        menu.action(Icons.BLOCK, raw, () ->
                        {
                            this.acceptBlockState(this.blockState.method_11657(p, (Comparable) v));

                            b.label = raw;
                        });
                    }
                });
            });

            button.tooltip(IKey.constant(p.method_11899()));

            this.properties.add(button);
        }

        if (!this.properties.getChildren().isEmpty())
        {
            this.properties.prepend(UI.label(UIKeys.FORMS_EDITORS_BLOCK_PROPERTIES).marginTop(6));
        }

        UIBaseMenu.UIRootElement root = this.getRoot();

        if (root != null)
        {
            root.resize();
        }
    }
}