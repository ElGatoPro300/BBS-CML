package mchorse.bbs_mod.ui.forms.editors.utils;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.forms.StructureLikeManager;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.list.UILikeableStructureList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UILikedStructureList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIVanillaStructureList;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIConfirmOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIPromptOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIStringOverlayPanel;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.WorldSavePath;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;

/**
 * Overlay panel for picking structures with three tabs: World, Vanilla, and Favorites.
 */
public class UIStructureOverlayPanel extends UIStringOverlayPanel
{
    private final StructureLikeManager likeManager;
    private final UIIcon worldButton;
    private final UIIcon vanillaButton;
    private final UIIcon likeButton;

    private UISearchList<String> worldStructures;
    private UILikeableStructureList worldStructureList;
    private UISearchList<String> vanillaStructures;
    private UIVanillaStructureList vanillaStructureList;
    private UISearchList<StructureLikeManager.LikedStructure> likedStructures;
    private UILikedStructureList likedStructureList;

    private ViewMode currentMode = null;
    private final UIContext context;
    private final Consumer<Link> originalCallback;
    private String selectedStructure;

    public UIStructureOverlayPanel(IKey title, Consumer<Link> callback)
    {
        this(callback, null);
    }

    public UIStructureOverlayPanel(Consumer<Link> callback)
    {
        this(callback, null);
    }

    public UIStructureOverlayPanel(Consumer<Link> callback, UIContext context)
    {
        super(UIKeys.GENERAL_SEARCH, getAllStructureFiles(), null);

        this.resizable();
        this.minSize(280, 200);

        this.context = context;
        this.originalCallback = callback;
        this.likeManager = new StructureLikeManager();

        /* Replace the default list with the like-aware list */
        this.content.remove(this.strings);

        UILikeableStructureList likeableList = new UILikeableStructureList((list) ->
        {
            if (!list.isEmpty())
            {
                this.pickStructure(list.get(0));
            }
        }, this.likeManager);

        likeableList.scroll.scrollSpeed *= 2;
        
        likeableList.setSaveCallback(this::saveStructure);
        
        likeableList.setEditCallback((structureName) ->
        {
            UIContext ctx = this.context != null ? this.context : this.getContext();

            if (ctx == null)
            {
                return;
            }
            
            UIPromptOverlayPanel renamePanel = new UIPromptOverlayPanel(
                UIKeys.GENERAL_RENAME,
                UIKeys.GENERAL_RENAME,
                (newName) -> this.renameStructure(structureName, newName)
            );
            
            renamePanel.text.setText(structureName);
            renamePanel.text.filename();
            
            UIOverlay.addOverlay(ctx, renamePanel);
        });
        
        likeableList.setRemoveCallback((structureName) ->
        {
            UIContext ctx = this.context != null ? this.context : this.getContext();

            if (ctx == null)
            {
                return;
            }
            
            UIConfirmOverlayPanel confirmPanel = new UIConfirmOverlayPanel(
                UIKeys.GENERAL_REMOVE,
                UIKeys.GENERAL_REMOVE,
                (confirmed) ->
                {
                    if (confirmed)
                    {
                        this.deleteStructure(structureName);
                    }
                }
            );
            
            UIOverlay.addOverlay(ctx, confirmPanel);
        });

        likeableList.setRefreshCallback(this::refreshLikedList);

        this.strings = new UISearchList<>(likeableList);
        this.strings.label(UIKeys.GENERAL_SEARCH).full(this.content).x(6).w(1F, -12);
        this.content.add(this.strings);
        this.worldStructures = this.strings;
        this.worldStructureList = likeableList;

        /* Create vanilla structure list */
        this.vanillaStructureList = new UIVanillaStructureList((list) ->
        {
            if (!list.isEmpty())
            {
                this.pickStructure(list.get(0));
            }
        }, this.likeManager);
        
        this.vanillaStructureList.setLikeToggleCallback(() ->
        {
            this.refreshVanillaStructureList();
            this.refreshLikedList();
        });

        this.vanillaStructures = new UISearchList<>(this.vanillaStructureList);
        this.vanillaStructures.label(UIKeys.GENERAL_SEARCH).full(this.content).x(6).w(1F, -12);
        this.vanillaStructures.setVisible(false);
        this.content.add(this.vanillaStructures);

        /* Create liked structures list */
        this.likedStructureList = new UILikedStructureList((list) ->
        {
            if (!list.isEmpty())
            {
                this.pickStructure(list.get(0).getPath());
            }
        });
        
        this.likedStructureList.setUnlikeCallback((structure) ->
        {
            this.likeManager.setStructureLiked(structure.getPath(), structure.getDisplayName(), false);
            this.refreshLikedList();
        });
        
        this.likedStructures = new UISearchList<>(this.likedStructureList);
        this.likedStructures.label(UIKeys.GENERAL_SEARCH).full(this.content).x(6).w(1F, -12);
        this.likedStructures.setVisible(false);
        this.content.add(this.likedStructures);

        /* Setup button icons */
        this.worldButton = new UIIcon(Icons.FOLDER, (b) -> this.switchToMode(ViewMode.WORLD));
        this.worldButton.tooltip(UIKeys.GENERAL_SEARCH);

        this.vanillaButton = new UIIcon(Icons.ADD, (b) -> this.switchToMode(ViewMode.VANILLA));
        this.vanillaButton.tooltip(UIKeys.GENERAL_ADD);

        this.likeButton = new UIIcon(Icons.HEART_ALT, (b) -> this.switchToMode(ViewMode.LIKE));
        this.likeButton.tooltip(UIKeys.GENERAL_PRESETS);

        this.icons.add(this.worldButton, this.vanillaButton, this.likeButton);

        this.callback(this::pickStructure);

        this.refreshWorldStructureList();
        this.refreshVanillaStructureList();
        this.refreshLikedList();
        this.switchToMode(ViewMode.WORLD);
    }

    private static Set<String> getAllStructureFiles()
    {
        Set<String> locations = new HashSet<>();
        MinecraftClient mc = MinecraftClient.getInstance();

        /* Scan world/generated/minecraft/structures/ (if in world) */
        if (mc.world != null && mc.isInSingleplayer() && mc.getServer() != null)
        {
            try
            {
                File generatedFolder = mc.getServer().getSavePath(WorldSavePath.GENERATED).toFile();
                File worldStructures = new File(new File(generatedFolder, "minecraft"), "structures");
                
                if (worldStructures.exists() && worldStructures.isDirectory())
                {
                    scanStructureFolder(worldStructures, worldStructures, locations, "world:");
                }
                
                // Also scan the standard world structures folder (generated/structures/...)
                // Sometimes structures are saved directly in generated/structures without 'minecraft' namespace folder
                File genericStructures = new File(generatedFolder, "structures");
                if (genericStructures.exists() && genericStructures.isDirectory())
                {
                    scanStructureFolder(genericStructures, genericStructures, locations, "world:");
                }
            }
            catch (Exception e)
            {
                System.err.println("Failed to scan world structures: " + e.getMessage());
            }
        }

        return locations;
    }

    private static File getSavedStructureFolder()
    {
        return new File(BBSMod.getAssetsFolder(), "structures");
    }

    private static Set<String> getSavedStructureFiles()
    {
        Set<String> locations = new HashSet<>();
        File savedFolder = getSavedStructureFolder();

        if (savedFolder.exists() && savedFolder.isDirectory())
        {
            scanStructureFolder(savedFolder, savedFolder, locations, "saved:");
        }

        return locations;
    }

    private static void scanStructureFolder(File root, File folder, Set<String> results, String prefix)
    {
        if (!folder.exists() || !folder.isDirectory())
        {
            return;
        }

        try (Stream<Path> paths = Files.walk(folder.toPath()))
        {
            paths.filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".nbt"))
                .forEach(path ->
                {
                    try
                    {
                        String relativePath = root.toPath().relativize(path).toString().replace("\\", "/");
                        results.add(prefix + relativePath);
                    }
                    catch (Exception e)
                    {
                        /* Skip invalid structures */
                    }
                });
        }
        catch (Exception e)
        {
            System.err.println("Failed to scan folder: " + folder + " - " + e.getMessage());
        }
    }

    private void switchToMode(ViewMode mode)
    {
        if (this.currentMode == mode)
        {
            return;
        }

        this.currentMode = mode;

        this.updateButtonStates();

        switch (mode)
        {
            case WORLD:
                this.showWorldMode();
                break;

            case VANILLA:
                this.showVanillaMode();
                break;

            case LIKE:
                this.showLikeMode();
                break;
        }

        this.updateListSelections();
    }

    private void showWorldMode()
    {
        this.strings.setVisible(true);
        this.vanillaStructures.setVisible(false);

        if (this.likedStructures != null)
        {
            this.likedStructures.setVisible(false);
        }

        UILikeableStructureList list = (UILikeableStructureList) this.strings.list;

        list.setShowOnlyLiked(false);
        list.setShowEditRemoveButtons(true);

        this.refreshWorldStructureList();
        list.update();
    }

    private void showVanillaMode()
    {
        this.strings.setVisible(false);
        this.vanillaStructures.setVisible(true);

        if (this.likedStructures != null)
        {
            this.likedStructures.setVisible(false);
        }

        this.vanillaStructures.resize();
        this.content.resize();

        this.refreshVanillaStructureList();
    }

    private void showLikeMode()
    {
        this.strings.setVisible(false);
        this.vanillaStructures.setVisible(false);

        if (this.likedStructures != null)
        {
            this.likedStructures.setVisible(true);
        }

        UILikeableStructureList list = (UILikeableStructureList) this.strings.list;

        list.setShowOnlyLiked(true);
        list.setShowEditRemoveButtons(false);

        this.refreshLikedList();
        list.update();
    }

    private void updateButtonStates()
    {
        this.worldButton.active(this.currentMode == ViewMode.WORLD);
        this.vanillaButton.active(this.currentMode == ViewMode.VANILLA);
        this.likeButton.active(this.currentMode == ViewMode.LIKE);
    }

    private void refreshVanillaStructureList()
    {
        if (this.vanillaStructureList == null)
        {
            return;
        }

        this.vanillaStructureList.refresh();

        if (this.vanillaStructures != null)
        {
            String filter = this.vanillaStructures.search.getText();

            this.vanillaStructures.filter(filter, true);
            this.vanillaStructures.resize();
        }
    }

    private void refreshLikedList()
    {
        if (this.likedStructureList == null)
        {
            return;
        }

        this.likedStructureList.setStructures(this.likeManager.getLikedStructures());

        if (this.likedStructures != null)
        {
            String filter = this.likedStructures.search.getText();

            this.likedStructures.filter(filter, true);
            this.likedStructures.resize();
        }
    }

    public void refreshWorldStructureList()
    {
        Set<String> structureFiles = getAllStructureFiles();
        Set<String> savedFiles = getSavedStructureFiles();
        
        structureFiles.addAll(savedFiles);

        UILikeableStructureList list = (UILikeableStructureList) this.strings.list;
        list.getList().clear();

        List<String> sorted = new ArrayList<>(structureFiles);

        sorted.sort(null);
        list.getList().addAll(sorted);
        list.getList().add(0, UIKeys.GENERAL_NONE.get());
        list.update();
        list.sort();

        String filter = this.strings.search.getText();

        this.strings.filter(filter, true);
        this.strings.resize();
        this.refreshLikedList();
    }
    
    private void saveStructure(String worldPath)
    {
        if (!worldPath.startsWith("world:")) return;
        
        try
        {
            File worldFile = resolveWorldFile(worldPath);
            if (worldFile == null || !worldFile.exists()) return;
            
            String relative = worldPath.substring(6);
            File destFile = new File(getSavedStructureFolder(), relative);
            
            // Ensure parent directories exist
            destFile.getParentFile().mkdirs();
            
            Files.copy(worldFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            this.refreshWorldStructureList();
            this.refreshLikedList();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private File resolveWorldFile(String worldPath)
    {
        if (!worldPath.startsWith("world:")) return null;
        
        String relative = worldPath.substring(6);
        MinecraftClient mc = MinecraftClient.getInstance();
        
        if (mc.getServer() == null) return null;
        
        File generatedFolder = mc.getServer().getSavePath(WorldSavePath.GENERATED).toFile();
        
        // Check minecraft/structures
        File f1 = new File(new File(generatedFolder, "minecraft"), "structures/" + relative);
        if (f1.exists()) return f1;
        
        // Check generated/structures
        File f2 = new File(generatedFolder, "structures/" + relative);
        if (f2.exists()) return f2;
        
        return null;
    }

    private void pickStructure(String structure)
    {
        this.selectedStructure = structure;

        if (structure == null || structure.isEmpty() || structure.equals(UIKeys.GENERAL_NONE.get()))
        {
            if (this.originalCallback != null)
            {
                this.originalCallback.accept(null);
            }

            return;
        }

        this.updateListSelections();

        if (this.originalCallback != null)
        {
            if (structure.startsWith("saved:"))
            {
                String path = structure.substring(6);
                this.originalCallback.accept(Link.create("structures/" + path));
            }
            else
            {
                this.originalCallback.accept(Link.create(structure));
            }
        }
    }

    private void updateListSelections()
    {
        if (this.selectedStructure == null)
        {
            return;
        }

        if (this.strings.list instanceof UILikeableStructureList list)
        {
            int index = list.getList().indexOf(this.selectedStructure);

            if (index >= 0)
            {
                list.setIndex(index);
            }
        }

        if (this.vanillaStructureList != null)
        {
            int index = this.vanillaStructureList.getList().indexOf(this.selectedStructure);

            if (index >= 0)
            {
                this.vanillaStructureList.setIndex(index);
            }
        }

        if (this.likedStructureList != null)
        {
            List<StructureLikeManager.LikedStructure> liked = this.likedStructureList.getStructures();

            for (int i = 0; i < liked.size(); i++)
            {
                if (liked.get(i).getPath().equals(this.selectedStructure))
                {
                    this.likedStructureList.setIndex(i);

                    break;
                }
            }
        }
    }

    @Override
    public UIStringOverlayPanel set(String string)
    {
        if (string != null)
        {
            if (string.startsWith("bbs:structures/"))
            {
                string = "saved:" + string.substring(15);
            }
            else if (string.startsWith("assets:structures/"))
            {
                string = "saved:" + string.substring(18);
            }
            else if (string.startsWith("structures/"))
            {
                string = "saved:" + string.substring(11);
            }
        }
        
        this.selectedStructure = string;
        this.updateListSelections();
        
        return super.set(string);
    }

    @Override
    public void render(UIContext context)
    {
        super.render(context);
    }

    @Override
    protected void renderBackground(UIContext context)
    {
        super.renderBackground(context);

        if (this.worldButton.isActive())
        {
            this.worldButton.area.render(context.batcher, BBSSettings.primaryColor(Colors.A100));
        }

        if (this.vanillaButton.isActive())
        {
            this.vanillaButton.area.render(context.batcher, BBSSettings.primaryColor(Colors.A100));
        }

        if (this.likeButton.isActive())
        {
            this.likeButton.area.render(context.batcher, BBSSettings.primaryColor(Colors.A100));
        }
    }

    private void renameStructure(String oldName, String newName)
    {
        if (oldName == null || newName == null || oldName.equals(newName))
        {
            return;
        }
        
        if (oldName.startsWith("saved:"))
        {
            String oldRel = oldName.substring(6);
            String newRel = newName;
            
            // Remove prefix if user typed it
            if (newRel.startsWith("saved:")) newRel = newRel.substring(6);
            
            // Keep extension
            if (oldRel.endsWith(".nbt") && !newRel.endsWith(".nbt")) newRel += ".nbt";
            
            File oldFile = new File(getSavedStructureFolder(), oldRel);
            File newFile = new File(getSavedStructureFolder(), newRel);
            
            if (oldFile.exists() && !newFile.exists())
            {
                oldFile.renameTo(newFile);
                this.refreshWorldStructureList();
                this.refreshLikedList();
            }
            return;
        }

        /* Only rename world: structures */
        if (!oldName.startsWith("world:"))
        {
            return;
        }

        String oldFileName = oldName.replace("world:", "").replace(".nbt", "");
        String newFileName = newName.replace("world:", "").replace(".nbt", "");

        /* TODO: Implement file renaming in world structures folder */
        
        this.refreshWorldStructureList();
        this.refreshVanillaStructureList();
        this.refreshLikedList();
    }

    private void deleteStructure(String structureName)
    {
        if (structureName == null)
        {
            return;
        }
        
        if (structureName.startsWith("saved:"))
        {
            String rel = structureName.substring(6);
            File file = new File(getSavedStructureFolder(), rel);
            
            if (file.exists())
            {
                file.delete();
                this.refreshWorldStructureList();
                this.refreshLikedList();
            }
            return;
        }

        /* Only delete world: structures */
        if (!structureName.startsWith("world:"))
        {
            return;
        }

        /* TODO: Implement file deletion in world structures folder */
        
        this.likeManager.removeStructure(structureName);

        this.refreshWorldStructureList();
        this.refreshVanillaStructureList();
        this.refreshLikedList();
    }

    @Override
    public void onClose()
    {
        super.onClose();
    }

    private enum ViewMode
    {
        WORLD, VANILLA, LIKE;
    }
}
