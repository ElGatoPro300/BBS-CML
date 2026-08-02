package mchorse.bbs_mod.ui.framework.elements.input.list;

import mchorse.bbs_mod.forms.StructureLikeManager;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Structure list with like/dislike buttons
 */
public class UILikeableStructureList extends UIStringList
{
    private StructureLikeManager likeManager;
    private boolean showOnlyLiked = false;
    private UIIcon likeButton;
    private UIIcon editButton;
    private UIIcon removeButton;
    private UIIcon saveButton;
    private Runnable refreshCallback;
    private Consumer<String> editCallback;
    private Consumer<String> removeCallback;
    private Consumer<String> saveCallback;
    private boolean showEditRemoveButtons = false;
    
    public UILikeableStructureList(Consumer<List<String>> callback, StructureLikeManager likeManager)
    {
        super(callback);
        this.likeManager = likeManager;
        this.likeButton = new UIIcon(Icons.LIKE, null);
        this.editButton = new UIIcon(Icons.EDIT, null);
        this.removeButton = new UIIcon(Icons.REMOVE, null);
        this.saveButton = new UIIcon(Icons.DOWNLOAD, null);
    }
    
    public void setShowOnlyLiked(boolean showOnlyLiked)
    {
        this.showOnlyLiked = showOnlyLiked;
    }
    
    private String getDisplayText(String element)
    {
        String display = this.likeManager.getDisplayName(element);

        return display != null ? display : element;
    }
    
    public void setSaveCallback(Consumer<String> saveCallback)
    {
        this.saveCallback = saveCallback;
    }

    public void setEditCallback(Consumer<String> editCallback)
    {
        this.editCallback = editCallback;
    }

    public void setRemoveCallback(Consumer<String> removeCallback)
    {
        this.removeCallback = removeCallback;
    }

    public void setRefreshCallback(Runnable refreshCallback)
    {
        this.refreshCallback = refreshCallback;
    }
    
    @Override
    protected void renderElementPart(UIContext context, String element, int i, int x, int y, boolean hover, boolean selected)
    {
        /* In show-only-liked mode, 'i' is the visible index, map it to the actual element */
        if (this.showOnlyLiked)
        {
            element = this.getVisibleElement(i);

            if (element == null)
            {
                return;
            }
        }

        boolean isNoneOption = element.equals(UIKeys.GENERAL_NONE.get());
        String displayText = this.getDisplayText(element);
        
        int textWidth = context.batcher.getFont().getWidth(displayText);
        
        boolean isSaved = element.startsWith("saved:");
        boolean isWorld = element.startsWith("world:");
        
        int buttonSpace = 0;

        if (!isNoneOption)
        {
            /* Calculate space based on available buttons */
            if (isSaved)
            {
                buttonSpace = 60; /* edit + remove + like */
            }
            else if (isWorld)
            {
                buttonSpace = 40; /* save + like */
            }
            else
            {
                buttonSpace = 20; /* like only */
            }
        }

        int maxWidth = this.area.w - 8 - buttonSpace;

        if (textWidth > maxWidth)
        {
            displayText = context.batcher.getFont().limitToWidth(displayText, maxWidth);
        }

        context.batcher.textShadow(displayText, x + 4, y + (this.scroll.scrollItemSize - context.batcher.getFont().getHeight()) / 2, hover ? Colors.HIGHLIGHT : Colors.WHITE);

        if (isNoneOption)
        {
            return;
        }

        int currentIconX = this.area.x + this.area.w - 20;
        int iconY = y + (this.scroll.scrollItemSize - 16) / 2;

        /* Render Like Button (Always present) */
        boolean isLiked = this.likeManager.isStructureLiked(element);
        boolean isHoverOnLike = this.area.isInside(context)
            && context.mouseX >= currentIconX
            && context.mouseX < currentIconX + 16
            && context.mouseY >= iconY
            && context.mouseY < iconY + 16;

        this.likeButton.both(isLiked ? Icons.DISLIKE : Icons.LIKE);
        this.likeButton.iconColor(isHoverOnLike || isLiked ? Colors.WHITE : Colors.GRAY);
        this.likeButton.area.set(currentIconX, iconY, 16, 16);
        this.likeButton.render(context);

        /* Render other buttons */
        if (isSaved)
        {
            /* Remove Button */
            currentIconX -= 20;
            boolean isHoverOnRemove = this.area.isInside(context)
                && context.mouseX >= currentIconX
                && context.mouseX < currentIconX + 16
                && context.mouseY >= iconY
                && context.mouseY < iconY + 16;

            this.removeButton.iconColor(isHoverOnRemove ? Colors.WHITE : Colors.GRAY);
            this.removeButton.area.set(currentIconX, iconY, 16, 16);
            this.removeButton.render(context);

            /* Edit Button */
            currentIconX -= 20;
            boolean isHoverOnEdit = this.area.isInside(context)
                && context.mouseX >= currentIconX
                && context.mouseX < currentIconX + 16
                && context.mouseY >= iconY
                && context.mouseY < iconY + 16;

            this.editButton.iconColor(isHoverOnEdit ? Colors.WHITE : Colors.GRAY);
            this.editButton.area.set(currentIconX, iconY, 16, 16);
            this.editButton.render(context);
        }
        else if (isWorld)
        {
            /* Save Button */
            currentIconX -= 20;
            boolean isHoverOnSave = this.area.isInside(context)
                && context.mouseX >= currentIconX
                && context.mouseX < currentIconX + 16
                && context.mouseY >= iconY
                && context.mouseY < iconY + 16;

            this.saveButton.iconColor(isHoverOnSave ? Colors.WHITE : Colors.GRAY);
            this.saveButton.area.set(currentIconX, iconY, 16, 16);
            this.saveButton.render(context);
        }
    }
    
    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (!this.area.isInside(context) || context.mouseButton != 0)
        {
            return super.subMouseClicked(context);
        }

        int scrollIndex = this.scroll.getIndex(context.mouseX, context.mouseY);

        if (!this.exists(scrollIndex))
        {
            return super.subMouseClicked(context);
        }

        String element = this.showOnlyLiked ? this.getVisibleElement(scrollIndex) : this.list.get(scrollIndex);

        if (element == null || element.equals(UIKeys.GENERAL_NONE.get()))
        {
            return super.subMouseClicked(context);
        }

        int y = this.area.y + scrollIndex * this.scroll.scrollItemSize - (int) this.scroll.getScroll();
        int iconY = y + (this.scroll.scrollItemSize - 16) / 2;
        int currentIconX = this.area.x + this.area.w - 20;

        boolean isSaved = element.startsWith("saved:");
        boolean isWorld = element.startsWith("world:");

        /* Check Like Button */
        if (context.mouseX >= currentIconX && context.mouseX < currentIconX + 16 &&
            context.mouseY >= iconY && context.mouseY < iconY + 16)
        {
            this.likeManager.toggleStructureLiked(element);

            if (this.refreshCallback != null)
            {
                this.refreshCallback.run();
            }

            return true;
        }

        if (isSaved)
        {
            /* Check Remove Button */
            currentIconX -= 20;
            if (context.mouseX >= currentIconX && context.mouseX < currentIconX + 16 &&
                context.mouseY >= iconY && context.mouseY < iconY + 16)
            {
                if (this.removeCallback != null) this.removeCallback.accept(element);
                return true;
            }

            /* Check Edit Button */
            currentIconX -= 20;
            if (context.mouseX >= currentIconX && context.mouseX < currentIconX + 16 &&
                context.mouseY >= iconY && context.mouseY < iconY + 16)
            {
                if (this.editCallback != null) this.editCallback.accept(element);
                return true;
            }
        }
        else if (isWorld)
        {
            /* Check Save Button */
            currentIconX -= 20;
            if (context.mouseX >= currentIconX && context.mouseX < currentIconX + 16 &&
                context.mouseY >= iconY && context.mouseY < iconY + 16)
            {
                if (this.saveCallback != null) this.saveCallback.accept(element);
                return true;
            }
        }

        return super.subMouseClicked(context);
    }
    
    @Override
    public int renderElement(UIContext context, String element, int i, int index, boolean postDraw)
    {
        boolean isNoneOption = element.equals(UIKeys.GENERAL_NONE.get());
        
        if (this.showOnlyLiked && !this.likeManager.isStructureLiked(element) && !isNoneOption)
        {
            return i;
        }

        return super.renderElement(context, element, i, index, postDraw);
    }

    @Override
    public void renderList(UIContext context)
    {
        if (!this.showOnlyLiked)
        {
            super.renderList(context);

            return;
        }

        int visibleIndex = 0;

        for (int actualIndex = 0; actualIndex < this.list.size(); actualIndex++)
        {
            String element = this.list.get(actualIndex);
            boolean isNoneOption = element.equals(UIKeys.GENERAL_NONE.get());

            if (this.likeManager.isStructureLiked(element) || isNoneOption)
            {
                int nextVisibleIndex = this.renderElement(context, element, visibleIndex, actualIndex, false);

                if (nextVisibleIndex == -1)
                {
                    break;
                }
                
                visibleIndex = nextVisibleIndex;
            }
        }
    }
    
    public int getVisibleElementCount()
    {
        if (!this.showOnlyLiked)
        {
            return this.list.size();
        }

        int count = 0;

        for (String element : this.list)
        {
            boolean isNoneOption = element.equals(UIKeys.GENERAL_NONE.get());

            if (this.likeManager.isStructureLiked(element) || isNoneOption)
            {
                count += 1;
            }
        }

        return count;
    }
    
    public String getVisibleElement(int visibleIndex)
    {
        if (!this.showOnlyLiked)
        {
            return this.list.get(visibleIndex);
        }
        
        int currentIndex = 0;

        for (String element : this.list)
        {
            boolean isNoneOption = element.equals(UIKeys.GENERAL_NONE.get());
            
            if (this.showOnlyLiked && !this.likeManager.isStructureLiked(element) && !isNoneOption)
            {
                continue;
            }
            
            if (currentIndex == visibleIndex)
            {
                return element;
            }

            currentIndex += 1;
        }
        
        return null;
    }
    
    public void setShowEditRemoveButtons(boolean show)
    {
        this.showEditRemoveButtons = show;
    }
    
    @Override
    public void update()
    {
        int size = this.showOnlyLiked ? this.getVisibleElementCount() : this.list.size();
        
        this.scroll.setSize(size);
        this.scroll.clamp();
    }
}
