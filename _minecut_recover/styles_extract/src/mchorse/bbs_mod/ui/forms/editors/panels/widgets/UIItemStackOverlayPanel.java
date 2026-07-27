package mchorse.bbs_mod.ui.forms.editors.panels.widgets;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextarea;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.utils.UI;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_2487;
import net.minecraft.class_2509;
import net.minecraft.class_2520;
import net.minecraft.class_2522;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_5321;
import net.minecraft.class_6903;
import net.minecraft.class_7225;
import net.minecraft.class_7923;
import net.minecraft.class_9334;
import com.mojang.brigadier.StringReader;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class UIItemStackOverlayPanel extends UIOverlayPanel
{
    private static final List<String> itemIDs = new ArrayList<>();

    public UISearchList<String> itemList;
    public UITextbox name;
    public UITrackpad count;
    public UITextarea nbt;

    private Consumer<class_1799> callback;
    private class_1799 stack;

    static
    {
        for (class_5321<class_1792> key : class_7923.field_41178.method_42021())
        {
            itemIDs.add(key.method_29177().toString());
        }

        itemIDs.sort(String::compareToIgnoreCase);
    }

    public UIItemStackOverlayPanel(Consumer<class_1799> callback, class_1799 stack)
    {
        super(UIKeys.ACTIONS_ITEM_STACK);

        this.callback = callback;
        this.stack = stack.method_7972();
        this.name = new UITextbox(1000, (v) ->
        {
            this.stack.method_57379(class_9334.field_49631, class_2561.method_43470(v));
            this.pickItemStack(this.stack);
            this.updateNbt();
        });
        this.name.setText(stack.method_7964().getString());
        this.count = new UITrackpad((v) ->
        {
            this.stack.method_7939(v.intValue());
            this.pickItemStack(this.stack);
            this.updateNbt();
        });
        this.count.limit(1.0, stack.method_7914(), true).setValue(stack.method_7947());
        this.nbt = new UITextarea<>((v) ->
        {
            try
            {
                class_2487 nbtCompound = new class_2522(new StringReader(v)).method_10727();
                class_7225.class_7874 registries = BBSMod.getRegistryManager();
                class_6903<class_2520> ops = registries != null ? class_6903.method_46632(class_2509.field_11560, registries) : null;

                class_1799 itemStack = registries != null
                    ? class_1799.field_24671.parse(ops, nbtCompound).result().orElse(class_1799.field_8037)
                    : class_1799.method_57359(null, nbtCompound);

                this.pickItemStack(itemStack);
                this.itemList.list.setCurrentScroll(class_7923.field_41178.method_10221(this.stack.method_7909()).toString());
            }
            catch (Exception e)
            {
                this.pickItemStack(class_1799.field_8037);
            }

        }).background();
        this.nbt.wrap();
        this.updateNbt();
        this.itemList = new UISearchList<>(new UIStringList((l) -> this.setItem(l.get(0))));
        this.itemList.label(UIKeys.GENERAL_SEARCH).list.background();
        this.itemList.list.clear();
        this.itemList.list.add(itemIDs);
        this.itemList.list.setCurrentScroll(class_7923.field_41178.method_10221(stack.method_7909()).toString());

        UIElement element = UI.column(5, 6, this.name, this.count);

        element.relative(this.content).y(1.0F).w(0.5F).anchorY(1.0F);
        this.nbt.relative(this.content).x(0.5F, 6).y(6).w(0.5F, -12).h(1.0F, -12);
        this.itemList.relative(this.content).xy(6, 6).w(0.5F, -12).hTo(element.area, 0.0F, 0);

        this.content.add(this.nbt, element, this.itemList);
    }

    private void updateNbt()
    {
        class_7225.class_7874 registries = BBSMod.getRegistryManager();
        class_6903<class_2520> ops = registries != null ? class_6903.method_46632(class_2509.field_11560, registries) : null;

        String nbtString = "{}";

        if (registries != null)
        {
            nbtString = class_1799.field_24671.encodeStart(ops, this.stack).result().map(class_2520::method_10714).orElse("{}");
        }
        else
        {
            nbtString = class_1799.field_24671.encodeStart(class_2509.field_11560, this.stack).result().map(class_2520::method_10714).orElse("{}");
        }

        this.nbt.setText(nbtString);
    }

    private void pickItemStack(class_1799 itemStack)
    {
        if (this.callback != null)
        {
            this.callback.accept(itemStack);
        }
    }

    private void setItem(String s)
    {
        this.stack = new class_1799(class_7923.field_41178.method_10223(class_2960.method_60654(s)));

        this.pickItemStack(this.stack);
        this.updateNbt();
    }
}