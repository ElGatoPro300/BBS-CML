package mchorse.bbs_mod.ui.forms.editors.panels.widgets;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class UIBlockStateEditor extends UIElement
{
    private static List<String> blockIDs = new ArrayList<>();

    public UISearchList<String> blockList;
    public UIElement properties;

    private Consumer<BlockState> callback;
    private BlockState blockState;

    static
    {
        for (RegistryKey<Block> key : Registries.BLOCK.getKeys())
        {
            blockIDs.add(key.getValue().toString());
        }

        blockIDs.sort(String::compareToIgnoreCase);
    }

    public UIBlockStateEditor(Consumer<BlockState> callback)
    {
        this.callback = callback;

        this.blockList = new UISearchList<>(new UIStringList((l) -> this.setBlock(l.get(0))));
        this.blockList.label(UIKeys.GENERAL_SEARCH).list.background();
        this.blockList.h(20 + 96);
        this.properties = UI.column();

        this.column().vertical().stretch();

        this.add(this.blockList);
        this.add(this.properties);

        this.blockList.list.clear();
        this.blockList.list.add(blockIDs);
    }

    public void setBlockState(BlockState blockState)
    {
        this.blockState = blockState;

        this.fillPropertiesEditor(blockState);
        this.blockList.list.setCurrentScroll(Registries.BLOCK.getId(blockState.getBlock()).toString());
    }

    private void setBlock(String blockID)
    {
        Identifier id = new Identifier(blockID);
        BlockState blockState = Registries.BLOCK.get(id).getDefaultState();

        this.acceptBlockState(blockState);
        this.fillPropertiesEditor(blockState);
    }

    private void acceptBlockState(BlockState blockState)
    {
        this.blockState = blockState;

        if (this.callback != null)
        {
            this.callback.accept(blockState);
        }
    }

    private void fillPropertiesEditor(BlockState state)
    {
        this.properties.removeAll();

        for (Property p : state.getProperties())
        {
            UIButton button = new UIButton(IKey.constant(state.get(p).toString()), (b) ->
            {
                this.getContext().replaceContextMenu((menu) ->
                {
                    for (Object v : p.getValues())
                    {
                        IKey raw = IKey.constant(v.toString());

                        menu.action(Icons.BLOCK, raw, () ->
                        {
                            this.acceptBlockState(this.blockState.with(p, (Comparable) v));

                            b.label = raw;
                        });
                    }
                });
            });

            button.tooltip(IKey.constant(p.getName()));

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