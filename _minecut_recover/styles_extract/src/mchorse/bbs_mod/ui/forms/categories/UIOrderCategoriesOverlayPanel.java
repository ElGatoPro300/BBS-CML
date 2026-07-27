package mchorse.bbs_mod.ui.forms.categories;

import mchorse.bbs_mod.forms.categories.UserFormCategory;
import mchorse.bbs_mod.forms.sections.UserFormSection;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.utils.icons.Icons;

import java.util.List;
import java.util.function.Consumer;

public class UIOrderCategoriesOverlayPanel extends UIOverlayPanel
{
    public UIList<UserFormCategory> categories;
    public UIIcon up;
    public UIIcon down;
    
    private UserFormSection section;
    private Runnable callback;

    public UIOrderCategoriesOverlayPanel(UserFormSection section, Runnable callback)
    {
        super(UIKeys.FORMS_CATEGORIES_ORDER);
        
        this.w(240);
        this.section = section;
        this.callback = callback;
        
        this.categories = new UIList<UserFormCategory>((l) -> {})
        {
            @Override
            protected String elementToString(UIContext context, int i, UserFormCategory element)
            {
                return element.getProcessedTitle();
            }

            @Override
            protected void handleSwap(int from, int to)
            {
                super.handleSwap(from, to);

                section.writeUserCategories();

                if (UIOrderCategoriesOverlayPanel.this.callback != null)
                {
                    UIOrderCategoriesOverlayPanel.this.callback.run();
                }
            }
        };
        
        this.categories.background().sorting();
        this.categories.setList(section.categories);
        
        this.up = new UIIcon(Icons.MOVE_UP, (b) -> this.move(-1));
        this.down = new UIIcon(Icons.MOVE_DOWN, (b) -> this.move(1));
        
        this.categories.relative(this.content).w(1F, -20).h(1F);
        this.up.relative(this.content).x(1F, -20).w(20).h(20);
        this.down.relative(this.content).x(1F, -20).y(20).w(20).h(20);
        
        this.content.add(this.categories, this.up, this.down);
    }
    
    private void move(int direction)
    {
        if (this.categories.isDeselected())
        {
            return;
        }
        
        int index = this.categories.getIndex();
        int newIndex = index + direction;
        
        if (newIndex >= 0 && newIndex < this.section.categories.size())
        {
            UserFormCategory category = this.section.categories.remove(index);
            this.section.categories.add(newIndex, category);
            
            this.section.writeUserCategories();
            this.categories.update();
            this.categories.setIndex(newIndex);
            
            if (this.callback != null)
            {
                this.callback.run();
            }
        }
    }
}
