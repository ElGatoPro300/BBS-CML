package mchorse.bbs_mod;

import mchorse.bbs_mod.actions.ActionHandler;
import mchorse.bbs_mod.actions.ActionManager;
import mchorse.bbs_mod.actions.types.AttackActionClip;
import mchorse.bbs_mod.actions.types.DamageActionClip;
import mchorse.bbs_mod.actions.types.MobDeathActionClip;
import mchorse.bbs_mod.actions.types.SwipeActionClip;
import mchorse.bbs_mod.actions.types.blocks.BreakBlockActionClip;
import mchorse.bbs_mod.actions.types.blocks.CloseContainerActionClip;
import mchorse.bbs_mod.actions.types.blocks.InteractBlockActionClip;
import mchorse.bbs_mod.actions.types.blocks.PlaceBlockActionClip;
import mchorse.bbs_mod.actions.types.chat.ChatActionClip;
import mchorse.bbs_mod.actions.types.chat.CommandActionClip;
import mchorse.bbs_mod.actions.types.item.ItemDropActionClip;
import mchorse.bbs_mod.actions.types.item.UseBlockItemActionClip;
import mchorse.bbs_mod.actions.types.item.UseItemActionClip;
import mchorse.bbs_mod.blocks.ModelBlock;
import mchorse.bbs_mod.blocks.TriggerBlock;
import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.blocks.entities.ModelProperties;
import mchorse.bbs_mod.blocks.entities.TriggerBlockEntity;
import mchorse.bbs_mod.camera.clips.ClipFactoryData;
import mchorse.bbs_mod.camera.clips.converters.DollyToKeyframeConverter;
import mchorse.bbs_mod.camera.clips.converters.DollyToPathConverter;
import mchorse.bbs_mod.camera.clips.converters.IdleConverter;
import mchorse.bbs_mod.camera.clips.converters.IdleToDollyConverter;
import mchorse.bbs_mod.camera.clips.converters.IdleToKeyframeConverter;
import mchorse.bbs_mod.camera.clips.converters.IdleToPathConverter;
import mchorse.bbs_mod.camera.clips.converters.PathToDollyConverter;
import mchorse.bbs_mod.camera.clips.converters.PathToKeyframeConverter;
import mchorse.bbs_mod.camera.clips.misc.AudioClip;
import mchorse.bbs_mod.camera.clips.misc.BossBarClip;
import mchorse.bbs_mod.camera.clips.misc.CurveClip;
import mchorse.bbs_mod.camera.clips.misc.HotbarClip;
import mchorse.bbs_mod.camera.clips.misc.ImageClip;
import mchorse.bbs_mod.camera.clips.misc.SubtitleClip;
import mchorse.bbs_mod.camera.clips.misc.VideoClip;
import mchorse.bbs_mod.camera.clips.modifiers.AngleClip;
import mchorse.bbs_mod.camera.clips.modifiers.DollyZoomClip;
import mchorse.bbs_mod.camera.clips.modifiers.DragClip;
import mchorse.bbs_mod.camera.clips.modifiers.LookClip;
import mchorse.bbs_mod.camera.clips.modifiers.MathClip;
import mchorse.bbs_mod.camera.clips.modifiers.OrbitClip;
import mchorse.bbs_mod.camera.clips.modifiers.RemapperClip;
import mchorse.bbs_mod.camera.clips.modifiers.ShakeClip;
import mchorse.bbs_mod.camera.clips.modifiers.TrackerClip;
import mchorse.bbs_mod.camera.clips.modifiers.TranslateClip;
import mchorse.bbs_mod.camera.clips.overwrite.DollyClip;
import mchorse.bbs_mod.camera.clips.overwrite.IdleClip;
import mchorse.bbs_mod.camera.clips.overwrite.KeyframeClip;
import mchorse.bbs_mod.camera.clips.overwrite.PathClip;
import mchorse.bbs_mod.camera.clips.screen.CinematicClip;
import mchorse.bbs_mod.camera.clips.screen.ColorClip;
import mchorse.bbs_mod.camera.clips.screen.EyeClip;
import mchorse.bbs_mod.camera.clips.screen.GrainClip;
import mchorse.bbs_mod.camera.clips.screen.LetterboxClip;
import mchorse.bbs_mod.camera.clips.screen.ScreenNodeClip;
import mchorse.bbs_mod.camera.clips.screen.VignetteClip;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.entity.ActorEntity;
import mchorse.bbs_mod.entity.GunProjectileEntity;
import mchorse.bbs_mod.events.BBSAddonMod;
import mchorse.bbs_mod.events.EventBus;
import mchorse.bbs_mod.events.register.RegisterActionClipsEvent;
import mchorse.bbs_mod.events.register.RegisterCameraClipsEvent;
import mchorse.bbs_mod.events.register.RegisterEntityCaptureHandlersEvent;
import mchorse.bbs_mod.events.register.RegisterFormsEvent;
import mchorse.bbs_mod.events.register.RegisterKeyframeFactoriesEvent;
import mchorse.bbs_mod.events.register.RegisterMolangFunctionsEvent;
import mchorse.bbs_mod.events.register.RegisterSettingsEvent;
import mchorse.bbs_mod.events.register.RegisterSourcePacksEvent;
import mchorse.bbs_mod.film.FilmManager;
import mchorse.bbs_mod.forms.FormArchitect;
import mchorse.bbs_mod.forms.forms.AnchorForm;
import mchorse.bbs_mod.forms.forms.BillboardForm;
import mchorse.bbs_mod.forms.forms.VideoForm;
import mchorse.bbs_mod.forms.forms.BlockForm;
import mchorse.bbs_mod.forms.forms.ExtrudedForm;
import mchorse.bbs_mod.forms.forms.FluidForm;
import mchorse.bbs_mod.forms.forms.FramebufferForm;
import mchorse.bbs_mod.forms.forms.ItemForm;
import mchorse.bbs_mod.forms.forms.LabelForm;
import mchorse.bbs_mod.forms.forms.LightForm;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.forms.ParticleForm;
import mchorse.bbs_mod.forms.forms.ShapeForm;
import mchorse.bbs_mod.forms.forms.StructureForm;
import mchorse.bbs_mod.forms.forms.TrailForm;
import mchorse.bbs_mod.forms.forms.VanillaParticleForm;
import mchorse.bbs_mod.items.BlockPickerItem;
import mchorse.bbs_mod.items.GunItem;
import mchorse.bbs_mod.items.MobKillerItem;
import mchorse.bbs_mod.items.StructurePickerItem;
import mchorse.bbs_mod.math.molang.MolangParser;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.network.ServerNetwork;
import mchorse.bbs_mod.resources.AssetProvider;
import mchorse.bbs_mod.resources.ISourcePack;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.resources.packs.DynamicSourcePack;
import mchorse.bbs_mod.resources.packs.ExternalAssetsSourcePack;
import mchorse.bbs_mod.resources.packs.InternalAssetsSourcePack;
import mchorse.bbs_mod.resources.packs.WorldStructuresSourcePack;
import mchorse.bbs_mod.settings.Settings;
import mchorse.bbs_mod.settings.SettingsBuilder;
import mchorse.bbs_mod.settings.SettingsManager;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.factory.MapFactory;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_1269;
import net.minecraft.class_1299;
import net.minecraft.class_1311;
import net.minecraft.class_1747;
import net.minecraft.class_1761;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1928;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2378;
import net.minecraft.class_2487;
import net.minecraft.class_2561;
import net.minecraft.class_2586;
import net.minecraft.class_2591;
import net.minecraft.class_2960;
import net.minecraft.class_3222;
import net.minecraft.class_3414;
import net.minecraft.class_4048;
import net.minecraft.class_5218;
import net.minecraft.class_7225;
import net.minecraft.class_7923;
import net.minecraft.class_9275;
import net.minecraft.class_9279;
import net.minecraft.class_9334;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class BBSMod implements ModInitializer
{
    public static final String MOD_ID = "bbs";
    public static final String VERSION = "2.1-beta-1";

    public static final EventBus events = new EventBus();

    private static ActionManager actions;

    /* Important folders */
    private static File gameFolder;
    private static File assetsFolder;
    private static File settingsFolder;

    /* Core services */
    private static AssetProvider provider;
    private static DynamicSourcePack dynamicSourcePack;
    private static ExternalAssetsSourcePack originalSourcePack;

    /* Foundation services */
    private static SettingsManager settings;
    private static FormArchitect forms;

    /* Data */
    private static FilmManager films;

    private static List<Runnable> runnables = new ArrayList<>();

    private static final ThreadLocal<class_7225.class_7874> registryManager = new ThreadLocal<>();

    public static class_7225.class_7874 getRegistryManager()
    {
        return registryManager.get();
    }

    public static void setRegistryManager(class_7225.class_7874 registryManager)
    {
        if (registryManager == null)
        {
            BBSMod.registryManager.remove();
        }
        else
        {
            BBSMod.registryManager.set(registryManager);
        }
    }

    private static MapFactory<Clip, ClipFactoryData> factoryCameraClips;
    private static MapFactory<Clip, ClipFactoryData> factoryActionClips;
    private static MapFactory<Clip, ClipFactoryData> factoryScreenClips;

    public static final class_1299<ActorEntity> ACTOR_ENTITY = class_2378.method_10230(
        class_7923.field_41177,
        class_2960.method_60655(MOD_ID, "actor"),
        FabricEntityTypeBuilder.create(class_1311.field_6294, ActorEntity::new)
            .dimensions(class_4048.method_18385(0.6F, 1.8F))
            .trackRangeBlocks(256)
            .trackedUpdateRate(1)
            .build());

    public static final class_1299<GunProjectileEntity> GUN_PROJECTILE_ENTITY = class_2378.method_10230(
        class_7923.field_41177,
        class_2960.method_60655(MOD_ID, "gun_projectile"),
        FabricEntityTypeBuilder.create(class_1311.field_6294, GunProjectileEntity::new)
            .dimensions(class_4048.method_18385(0.25F, 0.25F))
            .trackRangeChunks(24)
            .trackedUpdateRate(1)
            .build());

    public static final class_2248 MODEL_BLOCK = new ModelBlock(FabricBlockSettings.method_9637()
        .method_45477()
        .method_42327()
        .method_22488()
        .method_51370()
        .method_9632(0F)
        .method_9631((state) -> state.method_11654(ModelBlock.LIGHT_LEVEL)));

    public static final class_2248 TRIGGER_BLOCK = new TriggerBlock(FabricBlockSettings.method_9637()
        .method_45477()
        .method_42327()
        .method_22488()
        .method_51370()
        .method_9629(-1F, 3600000F));

    public static final class_2248 CHROMA_RED_BLOCK = createChromaBlock();
    public static final class_2248 CHROMA_GREEN_BLOCK = createChromaBlock();
    public static final class_2248 CHROMA_BLUE_BLOCK = createChromaBlock();
    public static final class_2248 CHROMA_CYAN_BLOCK = createChromaBlock();
    public static final class_2248 CHROMA_MAGENTA_BLOCK = createChromaBlock();
    public static final class_2248 CHROMA_YELLOW_BLOCK = createChromaBlock();
    public static final class_2248 CHROMA_BLACK_BLOCK = createChromaBlock();
    public static final class_2248 CHROMA_WHITE_BLOCK = createChromaBlock();

    public static final class_1747 MODEL_BLOCK_ITEM = new class_1747(MODEL_BLOCK, new class_1792.class_1793());
    public static final class_1747 TRIGGER_BLOCK_ITEM = new class_1747(TRIGGER_BLOCK, new class_1792.class_1793());
    public static final GunItem GUN_ITEM = new GunItem(new class_1792.class_1793().method_7889(1));
    public static final MobKillerItem MOB_KILLER_ITEM = new MobKillerItem(new class_1792.class_1793().method_7889(1));
    public static final BlockPickerItem BLOCK_PICKER_ITEM = new BlockPickerItem(new class_1792.class_1793().method_7889(1));
    public static final StructurePickerItem STRUCTURE_PICKER_ITEM = new StructurePickerItem(new class_1792.class_1793().method_7889(1));
    public static final class_1747 CHROMA_RED_BLOCK_ITEM = new class_1747(CHROMA_RED_BLOCK, new class_1792.class_1793());
    public static final class_1747 CHROMA_GREEN_BLOCK_ITEM = new class_1747(CHROMA_GREEN_BLOCK, new class_1792.class_1793());
    public static final class_1747 CHROMA_BLUE_BLOCK_ITEM = new class_1747(CHROMA_BLUE_BLOCK, new class_1792.class_1793());
    public static final class_1747 CHROMA_CYAN_BLOCK_ITEM = new class_1747(CHROMA_CYAN_BLOCK, new class_1792.class_1793());
    public static final class_1747 CHROMA_MAGENTA_BLOCK_ITEM = new class_1747(CHROMA_MAGENTA_BLOCK, new class_1792.class_1793());
    public static final class_1747 CHROMA_YELLOW_BLOCK_ITEM = new class_1747(CHROMA_YELLOW_BLOCK, new class_1792.class_1793());
    public static final class_1747 CHROMA_BLACK_BLOCK_ITEM = new class_1747(CHROMA_BLACK_BLOCK, new class_1792.class_1793());
    public static final class_1747 CHROMA_WHITE_BLOCK_ITEM = new class_1747(CHROMA_WHITE_BLOCK, new class_1792.class_1793());

    public static final class_1928.class_4313<class_1928.class_4310> BBS_EDITING_RULE = GameRuleRegistry.register("bbsEditing", class_1928.class_5198.field_24100, GameRuleFactory.createBooleanRule(true));

    public static final class_2591<ModelBlockEntity> MODEL_BLOCK_ENTITY = class_2378.method_10230(
        class_7923.field_41181,
        class_2960.method_60655(MOD_ID, "model_block_entity"),
        FabricBlockEntityTypeBuilder.create(ModelBlockEntity::new, MODEL_BLOCK).build()
    );

    public static final class_2591<TriggerBlockEntity> TRIGGER_BLOCK_ENTITY = class_2378.method_10230(
        class_7923.field_41181,
        class_2960.method_60655(MOD_ID, "trigger_block"),
        FabricBlockEntityTypeBuilder.create(TriggerBlockEntity::new, TRIGGER_BLOCK).build()
    );

    public static final class_1761 ITEM_GROUP = FabricItemGroup.builder()
        .method_47320(() -> createModelBlockStack(Link.assets("textures/icon.png")))
        .method_47321(class_2561.method_43471("itemGroup.bbs.main"))
        .method_47317((context, entries) ->
        {
            entries.method_45420(createModelBlockStack(Link.assets("textures/model_block.png")));
            entries.method_45420(new class_1799(TRIGGER_BLOCK_ITEM));
            entries.method_45421(CHROMA_RED_BLOCK_ITEM);
            entries.method_45421(CHROMA_GREEN_BLOCK_ITEM);
            entries.method_45421(CHROMA_BLUE_BLOCK_ITEM);
            entries.method_45421(CHROMA_CYAN_BLOCK_ITEM);
            entries.method_45421(CHROMA_MAGENTA_BLOCK_ITEM);
            entries.method_45421(CHROMA_YELLOW_BLOCK_ITEM);
            entries.method_45421(CHROMA_BLACK_BLOCK_ITEM);
            entries.method_45421(CHROMA_WHITE_BLOCK_ITEM);
            entries.method_45420(new class_1799(GUN_ITEM));
            entries.method_45420(new class_1799(MOB_KILLER_ITEM));
            entries.method_45420(new class_1799(BLOCK_PICKER_ITEM));
            entries.method_45420(new class_1799(STRUCTURE_PICKER_ITEM));
        })
        .method_47324();

    public static final class_3414 CLICK = registerSound("click");

    private static class_3414 registerSound(String path)
    {
        class_2960 id = class_2960.method_60655(MOD_ID, path);

        return class_2378.method_10230(class_7923.field_41172, id, class_3414.method_47908(id));
    }

    private static File worldFolder;

    private static class_2248 createChromaBlock()
    {
        return new class_2248(FabricBlockSettings.method_9637()
            .method_45477()
            .method_42327()
            .method_29292()
            .method_9629(-1F, 3600000F));
    }

    private static class_1799 createModelBlockStack(Link texture)
    {
        class_1799 stack = new class_1799(MODEL_BLOCK_ITEM);
        ModelBlockEntity entity = new ModelBlockEntity(class_2338.field_10980, MODEL_BLOCK.method_9564());
        BillboardForm form = new BillboardForm();
        ModelProperties properties = entity.getProperties();

        form.transform.get().translate.set(0F, 0.5F, 0F);
        form.texture.set(texture);
        properties.setForm(form);
        properties.getTransformFirstPerson().translate.set(0F, 0F, -0.25F);

        class_2487 compound = new class_2487();
        compound.method_10582("id", class_2591.method_11033(MODEL_BLOCK_ENTITY).toString());
        DataStorageUtils.writeToNbtCompound(compound, "Properties", properties.toData());

        stack.method_57379(class_9334.field_49611, class_9279.method_57456(compound));
        stack.method_57379(class_9334.field_49623, new class_9275(Map.of("light_level", String.valueOf(properties.getLightLevel()))));

        return stack;
    }

    /**
     * Main folder, where all the other folders are located.
     */
    public static File getGameFolder()
    {
        return gameFolder;
    }

    public static File getGamePath(String path)
    {
        return new File(gameFolder, path);
    }

    /**
     * Assets folder within game's folder. It's used to store any assets that can
     * be loaded by {@link #provider}.
     */
    public static File getAssetsFolder()
    {
        ISourcePack sourcePack = getDynamicSourcePack().getSourcePack();

        if (sourcePack instanceof ExternalAssetsSourcePack pack)
        {
            return pack.getFolder();
        }

        return assetsFolder;
    }

    /**
     * Copy a bundled internal asset into {@code config/bbs/assets} when missing so
     * Emoticons (and anything else that reads {@code actions.bobj}) always resolve it
     * through the external pack as well as the jar.
     */
    private static void ensureBundledAsset(String path)
    {
        File out = new File(assetsFolder, path);

        if (out.exists() && out.length() > 0L)
        {
            return;
        }

        try
        {
            File parent = out.getParentFile();

            if (parent != null)
            {
                parent.mkdirs();
            }

            try (InputStream stream = provider.getAsset(Link.assets(path)))
            {
                if (stream == null)
                {
                    System.err.println("Bundled asset missing from classpath: " + path);

                    return;
                }

                java.nio.file.Files.copy(stream, out.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Extracted bundled asset to " + out.getAbsolutePath());
            }
        }
        catch (Exception e)
        {
            System.err.println("Failed to extract bundled asset: " + path);
            e.printStackTrace();
        }
    }

    public static File getAudioFolder()
    {
        return getAssetsPath("audio");
    }

    public static File getAssetsPath(String path)
    {
        return new File(getAssetsFolder(), path);
    }

    public static File getAudioCacheFolder()
    {
        return getSettingsPath("audio_cache");
    }

    /**
     * Config folder within game's folder. It's used to store any configuration
     * files.
     */
    public static File getSettingsFolder()
    {
        return settingsFolder;
    }

    public static File getSettingsPath(String path)
    {
        return new File(settingsFolder, path);
    }

    public static File getExportFolder()
    {
        return getGamePath("export");
    }

    public static ActionManager getActions()
    {
        return actions;
    }

    public static AssetProvider getProvider()
    {
        return provider;
    }

    public static DynamicSourcePack getDynamicSourcePack()
    {
        return dynamicSourcePack;
    }

    public static ExternalAssetsSourcePack getOriginalSourcePack()
    {
        return originalSourcePack;
    }

    public static SettingsManager getSettings()
    {
        return settings;
    }

    public static FormArchitect getForms()
    {
        return forms;
    }

    public static FilmManager getFilms()
    {
        return films;
    }

    public static File getWorldFolder()
    {
        return worldFolder;
    }

    public static MapFactory<Clip, ClipFactoryData> getFactoryCameraClips()
    {
        return factoryCameraClips;
    }

    public static MapFactory<Clip, ClipFactoryData> getFactoryActionClips()
    {
        return factoryActionClips;
    }

    public static MapFactory<Clip, ClipFactoryData> getFactoryScreenClips()
    {
        return factoryScreenClips;
    }

    @Override
    public void onInitialize()
    {
        /* Core */
        gameFolder = FabricLoader.getInstance().getGameDir().toFile();
        assetsFolder = new File(gameFolder, "config/bbs/assets");
        settingsFolder = new File(gameFolder, "config/bbs/settings");

        assetsFolder.mkdirs();
        new File(assetsFolder, "video").mkdirs();
        new File(assetsFolder, "structures").mkdirs();

        FabricLoader.getInstance()
            .getEntrypointContainers("bbs-addon", BBSAddonMod.class)
            .forEach((container) ->
            {
                events.register(container.getEntrypoint());
            });

        events.post(new RegisterMolangFunctionsEvent(MolangParser.CUSTOM_FUNCTIONS));

        actions = new ActionManager();

        originalSourcePack = new ExternalAssetsSourcePack(Link.ASSETS, assetsFolder).providesFiles();
        dynamicSourcePack = new DynamicSourcePack(originalSourcePack);
        provider = new AssetProvider();
        provider.register(dynamicSourcePack);
        provider.registerFirst(new WorldStructuresSourcePack());
        provider.register(new InternalAssetsSourcePack());

        events.post(new RegisterSourcePacksEvent(provider));
        ensureBundledAsset("actions.bobj");

        settings = new SettingsManager();
        forms = new FormArchitect();
        forms
            .register(Link.bbs("billboard"), BillboardForm.class, null)
            .register(Link.bbs("video"), VideoForm.class, null)
            .register(Link.bbs("fluid"), FluidForm.class, null)
            .register(Link.bbs("label"), LabelForm.class, null)
            .register(Link.bbs("model"), ModelForm.class, null)
            .register(Link.bbs("particle"), ParticleForm.class, null)
            .register(Link.bbs("extruded"), ExtrudedForm.class, null)
            .register(Link.bbs("block"), BlockForm.class, null)
            .register(Link.bbs("item"), ItemForm.class, null)
            .register(Link.bbs("anchor"), AnchorForm.class, null)
            .register(Link.bbs("mob"), MobForm.class, null)
            .register(Link.bbs("vanilla_particles"), VanillaParticleForm.class, null)
            .register(Link.bbs("trail"), TrailForm.class, null)
            .register(Link.bbs("framebuffer"), FramebufferForm.class, null)
            .register(Link.bbs("structure"), StructureForm.class, null)
            .register(Link.bbs("shape"), ShapeForm.class, null)
            .register(Link.bbs("light"), LightForm.class, null);

        events.post(new RegisterFormsEvent(forms));

        films = new FilmManager(() -> new File(worldFolder, "bbs/films"));

        /* Register camera clips */
        events.post(new RegisterKeyframeFactoriesEvent(KeyframeFactories.FACTORIES));
        events.post(new RegisterEntityCaptureHandlersEvent(Morph.HANDLERS));

        factoryCameraClips = new MapFactory<Clip, ClipFactoryData>()
            .register(Link.bbs("idle"), IdleClip.class, new ClipFactoryData(Icons.FRUSTUM, 0x159e64)
                .withConverter(Link.bbs("dolly"), new IdleToDollyConverter())
                .withConverter(Link.bbs("path"), new IdleToPathConverter())
                .withConverter(Link.bbs("keyframe"), new IdleToKeyframeConverter()))
            .register(Link.bbs("dolly"), DollyClip.class, new ClipFactoryData(Icons.CAMERA, 0xffa500)
                .withConverter(Link.bbs("idle"), IdleConverter.CONVERTER)
                .withConverter(Link.bbs("path"), new DollyToPathConverter())
                .withConverter(Link.bbs("keyframe"), new DollyToKeyframeConverter()))
            .register(Link.bbs("path"), PathClip.class, new ClipFactoryData(Icons.GALLERY, 0x6820ad)
                .withConverter(Link.bbs("idle"), IdleConverter.CONVERTER)
                .withConverter(Link.bbs("dolly"), new PathToDollyConverter())
                .withConverter(Link.bbs("keyframe"), new PathToKeyframeConverter()))
            .register(Link.bbs("keyframe"), KeyframeClip.class, new ClipFactoryData(Icons.CURVES, 0xde2e9f)
                .withConverter(Link.bbs("idle"), IdleConverter.CONVERTER))
            .register(Link.bbs("translate"), TranslateClip.class, new ClipFactoryData(Icons.UPLOAD, 0x4ba03e))
            .register(Link.bbs("angle"), AngleClip.class, new ClipFactoryData(Icons.ORBIT, 0xd77a0a))
            .register(Link.bbs("drag"), DragClip.class, new ClipFactoryData(Icons.FADING, 0x4baff7))
            .register(Link.bbs("shake"), ShakeClip.class, new ClipFactoryData(Icons.EXCHANGE, 0x159e64))
            .register(Link.bbs("math"), MathClip.class, new ClipFactoryData(Icons.GRAPH, 0x6820ad))
            .register(Link.bbs("look"), LookClip.class, new ClipFactoryData(Icons.VISIBLE, 0x197fff))
            .register(Link.bbs("orbit"), OrbitClip.class, new ClipFactoryData(Icons.GLOBE, 0xd82253))
            .register(Link.bbs("remapper"), RemapperClip.class, new ClipFactoryData(Icons.TIME, 0x222222))
            .register(Link.bbs("audio"), AudioClip.class, new ClipFactoryData(Icons.SOUND, 0xffc825))
            .register(Link.bbs("video"), VideoClip.class, new ClipFactoryData(Icons.IMAGE, 0x9933cc))
            .register(Link.bbs("subtitle"), SubtitleClip.class, new ClipFactoryData(Icons.FONT, 0x888899))
            .register(Link.bbs("image"), ImageClip.class, new ClipFactoryData(Icons.GALLERY, 0x44aa88))
            .register(Link.bbs("hotbar"), HotbarClip.class, new ClipFactoryData(Icons.BLOCK, 0x55aaff))
            .register(Link.bbs("curve"), CurveClip.class, new ClipFactoryData(Icons.ARC, 0xff775f))
            .register(Link.bbs("tracker"), TrackerClip.class, new ClipFactoryData(Icons.USER, 0xffffff))
            .register(Link.bbs("dolly_zoom"), DollyZoomClip.class, new ClipFactoryData(Icons.FILTER, 0x7d56c9))
            .register(Link.bbs("color"), ColorClip.class, new ClipFactoryData(Icons.FILTER, 0xff6633))
            .register(Link.bbs("cinematic"), CinematicClip.class, new ClipFactoryData(Icons.VIDEO_CAMERA, 0xffaa00))
            .register(Link.bbs("vignette"), VignetteClip.class, new ClipFactoryData(Icons.CIRCLE, 0x222244))
            .register(Link.bbs("letterbox"), LetterboxClip.class, new ClipFactoryData(Icons.FULLSCREEN, 0x111111))
            .register(Link.bbs("grain"), GrainClip.class, new ClipFactoryData(Icons.FIVE_STAR, 0x887766))
            .register(Link.bbs("screen_node"), ScreenNodeClip.class, new ClipFactoryData(Icons.GRAPH, 0x3355cc))
            .register(Link.bbs("boss_bar"), BossBarClip.class, new ClipFactoryData(Icons.SKULL, 0xaa00ff))
            .register(Link.bbs("eye"), EyeClip.class, new ClipFactoryData(Icons.VISIBLE, 0x111111));

        events.post(new RegisterCameraClipsEvent(factoryCameraClips));

        factoryActionClips = new MapFactory<Clip, ClipFactoryData>()
            .register(Link.bbs("chat"), ChatActionClip.class, new ClipFactoryData(Icons.BUBBLE, Colors.YELLOW))
            .register(Link.bbs("command"), CommandActionClip.class, new ClipFactoryData(Icons.PROPERTIES, Colors.ACTIVE))
            .register(Link.bbs("place_block"), PlaceBlockActionClip.class, new ClipFactoryData(Icons.BLOCK, Colors.INACTIVE))
            .register(Link.bbs("interact_block"), InteractBlockActionClip.class, new ClipFactoryData(Icons.FULLSCREEN, Colors.MAGENTA))
            .register(Link.bbs("close_container"), CloseContainerActionClip.class, new ClipFactoryData(Icons.FULLSCREEN, Colors.MAGENTA))
            .register(Link.bbs("break_block"), BreakBlockActionClip.class, new ClipFactoryData(Icons.BULLET, Colors.GREEN))
            .register(Link.bbs("use_item"), UseItemActionClip.class, new ClipFactoryData(Icons.POINTER, Colors.BLUE))
            .register(Link.bbs("use_block_item"), UseBlockItemActionClip.class, new ClipFactoryData(Icons.BUCKET, Colors.CYAN))
            .register(Link.bbs("drop_item"), ItemDropActionClip.class, new ClipFactoryData(Icons.ARROW_DOWN, Colors.DEEP_PINK))
            .register(Link.bbs("attack"), AttackActionClip.class, new ClipFactoryData(Icons.DROP, Colors.RED))
            .register(Link.bbs("damage"), DamageActionClip.class, new ClipFactoryData(Icons.SKULL, Colors.CURSOR))
            .register(Link.bbs("mob_death"), MobDeathActionClip.class, new ClipFactoryData(Icons.SKULL, Colors.RED))
            .register(Link.bbs("swipe"), SwipeActionClip.class, new ClipFactoryData(Icons.LIMB, Colors.ORANGE));

        events.post(new RegisterActionClipsEvent(factoryActionClips));

        factoryScreenClips = new MapFactory<Clip, ClipFactoryData>()
            .register(Link.bbs("color"), ColorClip.class, new ClipFactoryData(Icons.FILTER, 0xff6633))
            .register(Link.bbs("cinematic"), CinematicClip.class, new ClipFactoryData(Icons.VIDEO_CAMERA, 0xffaa00))
            .register(Link.bbs("vignette"), VignetteClip.class, new ClipFactoryData(Icons.CIRCLE, 0x222244))
            .register(Link.bbs("letterbox"), LetterboxClip.class, new ClipFactoryData(Icons.FULLSCREEN, 0x111111))
            .register(Link.bbs("grain"), GrainClip.class, new ClipFactoryData(Icons.FIVE_STAR, 0x887766))
            .register(Link.bbs("screen_node"), ScreenNodeClip.class, new ClipFactoryData(Icons.GRAPH, 0x3355cc))
            .register(Link.bbs("boss_bar"), BossBarClip.class, new ClipFactoryData(Icons.SKULL, 0xaa00ff))
            .register(Link.bbs("eye"), EyeClip.class, new ClipFactoryData(Icons.VISIBLE, 0x111111));

        setupConfig(Icons.PROCESSOR, "bbs", new File(settingsFolder, "bbs.json"), BBSSettings::register);

        events.post(new RegisterSettingsEvent());

        /* Networking */
        ServerNetwork.setup();

        /* Commands */
        CommandRegistrationCallback.EVENT.register(BBSCommands::register);

        /* Event listener */
        registerEvents();

        /* Entities */
        FabricDefaultAttributeRegistry.register(ACTOR_ENTITY, ActorEntity.createActorAttributes());

        /* Blocks */
        class_2378.method_10230(class_7923.field_41175, class_2960.method_60655(MOD_ID, "model"), MODEL_BLOCK);
        class_2378.method_10230(class_7923.field_41175, class_2960.method_60655(MOD_ID, "trigger"), TRIGGER_BLOCK);
        class_2378.method_10230(class_7923.field_41175, class_2960.method_60655(MOD_ID, "chroma_red"), CHROMA_RED_BLOCK);
        class_2378.method_10230(class_7923.field_41175, class_2960.method_60655(MOD_ID, "chroma_green"), CHROMA_GREEN_BLOCK);
        class_2378.method_10230(class_7923.field_41175, class_2960.method_60655(MOD_ID, "chroma_blue"), CHROMA_BLUE_BLOCK);
        class_2378.method_10230(class_7923.field_41175, class_2960.method_60655(MOD_ID, "chroma_cyan"), CHROMA_CYAN_BLOCK);
        class_2378.method_10230(class_7923.field_41175, class_2960.method_60655(MOD_ID, "chroma_magenta"), CHROMA_MAGENTA_BLOCK);
        class_2378.method_10230(class_7923.field_41175, class_2960.method_60655(MOD_ID, "chroma_yellow"), CHROMA_YELLOW_BLOCK);
        class_2378.method_10230(class_7923.field_41175, class_2960.method_60655(MOD_ID, "chroma_black"), CHROMA_BLACK_BLOCK);
        class_2378.method_10230(class_7923.field_41175, class_2960.method_60655(MOD_ID, "chroma_white"), CHROMA_WHITE_BLOCK);

        class_2378.method_10230(class_7923.field_41178, class_2960.method_60655(MOD_ID, "model"), MODEL_BLOCK_ITEM);
        class_2378.method_10230(class_7923.field_41178, class_2960.method_60655(MOD_ID, "trigger"), TRIGGER_BLOCK_ITEM);
        class_2378.method_10230(class_7923.field_41178, class_2960.method_60655(MOD_ID, "gun"), GUN_ITEM);
        class_2378.method_10230(class_7923.field_41178, class_2960.method_60655(MOD_ID, "mob_killer"), MOB_KILLER_ITEM);
        class_2378.method_10230(class_7923.field_41178, class_2960.method_60655(MOD_ID, "block_picker"), BLOCK_PICKER_ITEM);
        class_2378.method_10230(class_7923.field_41178, class_2960.method_60655(MOD_ID, "structure_picker"), STRUCTURE_PICKER_ITEM);
        class_2378.method_10230(class_7923.field_41178, class_2960.method_60655(MOD_ID, "chroma_red"), CHROMA_RED_BLOCK_ITEM);
        class_2378.method_10230(class_7923.field_41178, class_2960.method_60655(MOD_ID, "chroma_green"), CHROMA_GREEN_BLOCK_ITEM);
        class_2378.method_10230(class_7923.field_41178, class_2960.method_60655(MOD_ID, "chroma_blue"), CHROMA_BLUE_BLOCK_ITEM);
        class_2378.method_10230(class_7923.field_41178, class_2960.method_60655(MOD_ID, "chroma_cyan"), CHROMA_CYAN_BLOCK_ITEM);
        class_2378.method_10230(class_7923.field_41178, class_2960.method_60655(MOD_ID, "chroma_magenta"), CHROMA_MAGENTA_BLOCK_ITEM);
        class_2378.method_10230(class_7923.field_41178, class_2960.method_60655(MOD_ID, "chroma_yellow"), CHROMA_YELLOW_BLOCK_ITEM);
        class_2378.method_10230(class_7923.field_41178, class_2960.method_60655(MOD_ID, "chroma_black"), CHROMA_BLACK_BLOCK_ITEM);
        class_2378.method_10230(class_7923.field_41178, class_2960.method_60655(MOD_ID, "chroma_white"), CHROMA_WHITE_BLOCK_ITEM);

        class_2378.method_10230(class_7923.field_44687, class_2960.method_60655(MOD_ID, "main"), ITEM_GROUP);
    }

    private void registerEvents()
    {
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) ->
        {
            class_2586 be = world.method_8321(pos);

            if (be instanceof TriggerBlockEntity trigger)
            {
                if (player.method_7337())
                {
                    return class_1269.field_5811;
                }

                if (world.field_9236)
                {
                    return class_1269.field_5812;
                }

                if (player instanceof class_3222 serverPlayer)
                {
                    trigger.trigger(serverPlayer, false);
                }

                return class_1269.field_5812;
            }

            if (player.method_5998(hand).method_7909() == STRUCTURE_PICKER_ITEM)
            {
                return class_1269.field_5812;
            }

            return class_1269.field_5811;
        });

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) ->
        {
            if (entity instanceof class_3222 player)
            {
                Morph morph = Morph.getMorph(player);

                ServerNetwork.sendMorphToTracked(player, morph.getForm());
            }
        });

        ServerLifecycleEvents.SERVER_STARTED.register((event) -> {
            worldFolder = event.method_27050(class_5218.field_24188).toFile();
            setRegistryManager(event.method_30611());
        });
        ServerPlayConnectionEvents.JOIN.register((a, b, c) -> ServerNetwork.sendHandshake(c, b));

        ActionHandler.registerHandlers(actions);

        ServerTickEvents.START_SERVER_TICK.register((server) ->
        {
            actions.tick();
        });

        ServerTickEvents.END_SERVER_TICK.register((server) ->
        {
            for (Runnable runnable : runnables)
            {
                runnable.run();
            }

            runnables.clear();
        });

        ServerLifecycleEvents.SERVER_STOPPED.register((server) ->
        {
            actions.reset();
            ServerNetwork.reset();
            setRegistryManager(null);
        });

        EntityTrackingEvents.START_TRACKING.register((trackedEntity, player) ->
        {
            runnables.add(() ->
            {
                if (trackedEntity instanceof class_3222 playerTwo)
                {
                    Morph morph = Morph.getMorph(trackedEntity);

                    if (morph != null)
                    {
                        ServerNetwork.sendMorph(player, playerTwo.method_5628(), morph.getForm());
                    }
                }
            });
        });
    }

    public static Settings setupConfig(Icon icon, String id, File destination, Consumer<SettingsBuilder> registerer)
    {
        SettingsBuilder builder = new SettingsBuilder(icon, id, destination);
        Settings settings = builder.getConfig();

        registerer.accept(builder);

        BBSMod.settings.modules.put(settings.getId(), settings);
        BBSMod.settings.load(settings, settings.file);

        if (id.equals("bbs"))
        {
            BBSSettings.migrateShaderOpacityPatchesAfterLoad();

            File cmlFile = new File(destination.getParentFile(), "cml.json");
            
            if (cmlFile.exists())
            {
                try
                {
                    BaseType data = DataToString.read(cmlFile);
                    
                    if (data != null && data.isMap())
                    {
                        MapType map = data.asMap();
                        
                        for (String key : map.keys())
                        {
                            if (map.get(key).isMap())
                            {
                                MapType category = map.getMap(key);
                                
                                for (String valKey : category.keys())
                                {
                                    for (ValueGroup bbsCategory : settings.categories.values())
                                    {
                                        BaseValue value = bbsCategory.get(valKey);
                                        
                                        if (value != null)
                                        {
                                            value.fromData(category.get(valKey));
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        
                        settings.saveLater();
                        cmlFile.delete();
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }

            BBSSettings.migrateIrisOpacityFix();
        }

        return settings;
    }
}
