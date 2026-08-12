package mchorse.bbs_mod;

import mchorse.bbs_mod.actions.ActionHandler;
import mchorse.bbs_mod.actions.ActionManager;
import mchorse.bbs_mod.actions.types.AttackActionClip;
import mchorse.bbs_mod.actions.types.DamageActionClip;
import mchorse.bbs_mod.actions.types.MobDeathActionClip;
import mchorse.bbs_mod.actions.types.ProjectileAttackActionClip;
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
import mchorse.bbs_mod.events.register.RegisterActionConfigsEvent;
import mchorse.bbs_mod.events.register.RegisterCameraClipsEvent;
import mchorse.bbs_mod.events.register.RegisterEntityCaptureHandlersEvent;
import mchorse.bbs_mod.events.register.RegisterFormsEvent;
import mchorse.bbs_mod.events.register.RegisterKeyframeFactoriesEvent;
import mchorse.bbs_mod.events.register.RegisterMolangFunctionsEvent;
import mchorse.bbs_mod.events.register.RegisterParticleSimulationsEvent;
import mchorse.bbs_mod.events.register.RegisterSettingsEvent;
import mchorse.bbs_mod.events.register.RegisterSourcePacksEvent;
import mchorse.bbs_mod.film.FilmManager;
import mchorse.bbs_mod.forms.FormArchitect;
import mchorse.bbs_mod.forms.forms.AnchorForm;
import mchorse.bbs_mod.forms.forms.BillboardForm;
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
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class BBSMod implements ModInitializer
{
    public static final String MOD_ID = "bbs";
    public static final String VERSION = "2.1-beta-1";
    public static final boolean IS_CML = true;

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

    private static final ThreadLocal<HolderLookup.Provider> registryManager = new ThreadLocal<>();
    /**
     * Client-side fallback for registry-backed codecs (ItemStack enchantments, etc.).
     * {@link ThreadLocal} alone is not enough if a client helper runs off the JOIN thread.
     */
    private static volatile HolderLookup.Provider clientRegistryManager;

    public static HolderLookup.Provider getRegistryManager()
    {
        HolderLookup.Provider local = registryManager.get();

        if (local != null)
        {
            return local;
        }

        return clientRegistryManager;
    }

    public static void setRegistryManager(HolderLookup.Provider registryManager)
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

    public static void setClientRegistryManager(HolderLookup.Provider registries)
    {
        clientRegistryManager = registries;
    }

    private static MapFactory<Clip, ClipFactoryData> factoryCameraClips;
    private static MapFactory<Clip, ClipFactoryData> factoryActionClips;
    private static MapFactory<Clip, ClipFactoryData> factoryScreenClips;

    public static final EntityType<ActorEntity> ACTOR_ENTITY = Registry.register(
        BuiltInRegistries.ENTITY_TYPE,
        Identifier.fromNamespaceAndPath(MOD_ID, "actor"),
        EntityType.Builder.of(ActorEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.8F)
            .clientTrackingRange(16)
            .updateInterval(1)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "actor"))));

    public static final EntityType<GunProjectileEntity> GUN_PROJECTILE_ENTITY = Registry.register(
        BuiltInRegistries.ENTITY_TYPE,
        Identifier.fromNamespaceAndPath(MOD_ID, "gun_projectile"),
        EntityType.Builder.of(GunProjectileEntity::new, MobCategory.MISC)
            .sized(0.25F, 0.25F)
            .clientTrackingRange(24)
            .updateInterval(1)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "gun_projectile"))));

    public static final Block MODEL_BLOCK = new ModelBlock(BlockBehaviour.Properties.of()
        .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "model")))
        .noTerrainParticles()
        .noLootTable()
        .noOcclusion()
        .forceSolidOff()
        .strength(0F)
        .lightLevel((state) -> state.getValue(ModelBlock.LIGHT_LEVEL)));
        
    public static final Block TRIGGER_BLOCK = new TriggerBlock(BlockBehaviour.Properties.of()
        .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "trigger")))
        .noTerrainParticles()
        .noLootTable()
        .noOcclusion()
        .forceSolidOff()
        .strength(-1F, 3600000F));

    public static final Block CHROMA_RED_BLOCK = createChromaBlock("chroma_red");
    public static final Block CHROMA_GREEN_BLOCK = createChromaBlock("chroma_green");
    public static final Block CHROMA_BLUE_BLOCK = createChromaBlock("chroma_blue");
    public static final Block CHROMA_CYAN_BLOCK = createChromaBlock("chroma_cyan");
    public static final Block CHROMA_MAGENTA_BLOCK = createChromaBlock("chroma_magenta");
    public static final Block CHROMA_YELLOW_BLOCK = createChromaBlock("chroma_yellow");
    public static final Block CHROMA_BLACK_BLOCK = createChromaBlock("chroma_black");
    public static final Block CHROMA_WHITE_BLOCK = createChromaBlock("chroma_white");

    public static final BlockItem MODEL_BLOCK_ITEM = new BlockItem(MODEL_BLOCK, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "model"))));
    public static final BlockItem TRIGGER_BLOCK_ITEM = new BlockItem(TRIGGER_BLOCK, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "trigger"))));
    public static final GunItem GUN_ITEM = new GunItem(new Item.Properties().stacksTo(1).setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "gun"))));
    public static final MobKillerItem MOB_KILLER_ITEM = new MobKillerItem(new Item.Properties().stacksTo(1).setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "mob_killer"))));
    public static final BlockPickerItem BLOCK_PICKER_ITEM = new BlockPickerItem(new Item.Properties().stacksTo(1).setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "block_picker"))));
    public static final StructurePickerItem STRUCTURE_PICKER_ITEM = new StructurePickerItem(new Item.Properties().stacksTo(1).setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "structure_picker"))));
    public static final BlockItem CHROMA_RED_BLOCK_ITEM = new BlockItem(CHROMA_RED_BLOCK, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "chroma_red"))));
    public static final BlockItem CHROMA_GREEN_BLOCK_ITEM = new BlockItem(CHROMA_GREEN_BLOCK, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "chroma_green"))));
    public static final BlockItem CHROMA_BLUE_BLOCK_ITEM = new BlockItem(CHROMA_BLUE_BLOCK, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "chroma_blue"))));
    public static final BlockItem CHROMA_CYAN_BLOCK_ITEM = new BlockItem(CHROMA_CYAN_BLOCK, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "chroma_cyan"))));
    public static final BlockItem CHROMA_MAGENTA_BLOCK_ITEM = new BlockItem(CHROMA_MAGENTA_BLOCK, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "chroma_magenta"))));
    public static final BlockItem CHROMA_YELLOW_BLOCK_ITEM = new BlockItem(CHROMA_YELLOW_BLOCK, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "chroma_yellow"))));
    public static final BlockItem CHROMA_BLACK_BLOCK_ITEM = new BlockItem(CHROMA_BLACK_BLOCK, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "chroma_black"))));
    public static final BlockItem CHROMA_WHITE_BLOCK_ITEM = new BlockItem(CHROMA_WHITE_BLOCK, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "chroma_white"))));

    public static final GameRule<Boolean> BBS_EDITING_RULE = GameRuleBuilder.forBoolean(true)
        .category(GameRuleCategory.MISC)
        .buildAndRegister(Identifier.fromNamespaceAndPath(MOD_ID, "bbs_editing"));

    public static final BlockEntityType<ModelBlockEntity> MODEL_BLOCK_ENTITY = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE,
        Identifier.fromNamespaceAndPath(MOD_ID, "model_block_entity"),
        FabricBlockEntityTypeBuilder.create(ModelBlockEntity::new, MODEL_BLOCK).build()
    );

    public static final BlockEntityType<TriggerBlockEntity> TRIGGER_BLOCK_ENTITY = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE,
        Identifier.fromNamespaceAndPath(MOD_ID, "trigger_block"),
        FabricBlockEntityTypeBuilder.create(TriggerBlockEntity::new, TRIGGER_BLOCK).build()
    );

    public static final CreativeModeTab ITEM_GROUP = FabricItemGroup.builder()
        .icon(() -> createModelBlockStack(Link.assets("textures/icon.png")))
        .title(Component.translatable("itemGroup.bbs.main"))
        .displayItems((context, entries) ->
        {
            entries.accept(createModelBlockStack(Link.assets("textures/model_block.png")));
            entries.accept(new ItemStack(TRIGGER_BLOCK_ITEM));
            entries.accept(CHROMA_RED_BLOCK_ITEM);
            entries.accept(CHROMA_GREEN_BLOCK_ITEM);
            entries.accept(CHROMA_BLUE_BLOCK_ITEM);
            entries.accept(CHROMA_CYAN_BLOCK_ITEM);
            entries.accept(CHROMA_MAGENTA_BLOCK_ITEM);
            entries.accept(CHROMA_YELLOW_BLOCK_ITEM);
            entries.accept(CHROMA_BLACK_BLOCK_ITEM);
            entries.accept(CHROMA_WHITE_BLOCK_ITEM);
            entries.accept(new ItemStack(GUN_ITEM));
            entries.accept(new ItemStack(MOB_KILLER_ITEM));
            entries.accept(new ItemStack(BLOCK_PICKER_ITEM));
            entries.accept(new ItemStack(STRUCTURE_PICKER_ITEM));
        })
        .build();

    public static final SoundEvent CLICK = registerSound("click");

    private static SoundEvent registerSound(String path)
    {
        Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, path);

        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    private static File worldFolder;

    private static Block createChromaBlock(String name)
    {
        return new Block(BlockBehaviour.Properties.of()
            .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, name)))
            .noTerrainParticles()
            .noLootTable()
            .requiresCorrectToolForDrops()
            .strength(-1F, 3600000F));
    }

    private static ItemStack createModelBlockStack(Link texture)
    {
        ItemStack stack = new ItemStack(MODEL_BLOCK_ITEM);
        ModelBlockEntity entity = new ModelBlockEntity(BlockPos.ZERO, MODEL_BLOCK.defaultBlockState());
        BillboardForm form = new BillboardForm();
        ModelProperties properties = entity.getProperties();

        form.transform.get().translate.set(0F, 0.5F, 0F);
        form.texture.set(texture);
        properties.setForm(form);
        properties.getTransformFirstPerson().translate.set(0F, 0F, -0.25F);

        CompoundTag compound = new CompoundTag();

        DataStorageUtils.writeToNbtCompound(compound, "Properties", properties.toData());

        stack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(MODEL_BLOCK_ENTITY, compound));
        stack.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(Map.of("light_level", String.valueOf(properties.getLightLevel()))));

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

        settings = new SettingsManager();
        forms = new FormArchitect();
        forms
            .register(Link.bbs("billboard"), BillboardForm.class, null)
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
        events.post(new RegisterActionConfigsEvent());
        events.post(new RegisterParticleSimulationsEvent());

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
            .register(Link.bbs("projectile_attack"), ProjectileAttackActionClip.class, new ClipFactoryData(Icons.ARROW_DOWN, Colors.RED))
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
        Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "model"), MODEL_BLOCK);
        Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "trigger"), TRIGGER_BLOCK);
        Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "chroma_red"), CHROMA_RED_BLOCK);
        Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "chroma_green"), CHROMA_GREEN_BLOCK);
        Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "chroma_blue"), CHROMA_BLUE_BLOCK);
        Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "chroma_cyan"), CHROMA_CYAN_BLOCK);
        Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "chroma_magenta"), CHROMA_MAGENTA_BLOCK);
        Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "chroma_yellow"), CHROMA_YELLOW_BLOCK);
        Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "chroma_black"), CHROMA_BLACK_BLOCK);
        Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "chroma_white"), CHROMA_WHITE_BLOCK);

        Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "model"), MODEL_BLOCK_ITEM);
        Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "trigger"), TRIGGER_BLOCK_ITEM);
        Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "gun"), GUN_ITEM);
        Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "mob_killer"), MOB_KILLER_ITEM);
        Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "block_picker"), BLOCK_PICKER_ITEM);
        Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "structure_picker"), STRUCTURE_PICKER_ITEM);
        Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "chroma_red"), CHROMA_RED_BLOCK_ITEM);
        Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "chroma_green"), CHROMA_GREEN_BLOCK_ITEM);
        Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "chroma_blue"), CHROMA_BLUE_BLOCK_ITEM);
        Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "chroma_cyan"), CHROMA_CYAN_BLOCK_ITEM);
        Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "chroma_magenta"), CHROMA_MAGENTA_BLOCK_ITEM);
        Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "chroma_yellow"), CHROMA_YELLOW_BLOCK_ITEM);
        Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "chroma_black"), CHROMA_BLACK_BLOCK_ITEM);
        Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "chroma_white"), CHROMA_WHITE_BLOCK_ITEM);

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(MOD_ID, "main"), ITEM_GROUP);
    }

    private void registerEvents()
    {
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) ->
        {
            BlockEntity be = world.getBlockEntity(pos);

            if (be instanceof TriggerBlockEntity trigger)
            {
                if (player.isCreative())
                {
                    return InteractionResult.PASS;
                }

                if (world.isClientSide())
                {
                    return InteractionResult.SUCCESS;
                }

                if (player instanceof ServerPlayer serverPlayer)
                {
                    trigger.trigger(serverPlayer, false);
                }

                return InteractionResult.SUCCESS;
            }

            if (player.getItemInHand(hand).getItem() == STRUCTURE_PICKER_ITEM)
            {
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        });

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) ->
        {
            if (entity instanceof ServerPlayer player)
            {
                Morph morph = Morph.getMorph(player);

                ServerNetwork.sendMorphToTracked(player, morph.getForm());
            }
        });

        ServerLifecycleEvents.SERVER_STARTED.register((event) -> {
            worldFolder = event.getWorldPath(LevelResource.ROOT).toFile();
            setRegistryManager(event.registryAccess());
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
                if (trackedEntity instanceof ServerPlayer playerTwo)
                {
                    Morph morph = Morph.getMorph(trackedEntity);

                    if (morph != null)
                    {
                        ServerNetwork.sendMorph(player, playerTwo.getId(), morph.getForm());
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
            BBSSettings.migrateOrbitSettingsAfterLoad(settings.file);
        }

        return settings;
    }
}
