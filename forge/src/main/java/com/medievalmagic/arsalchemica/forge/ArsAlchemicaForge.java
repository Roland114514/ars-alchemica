package com.medievalmagic.arsalchemica.forge;

import com.medievalmagic.arsalchemica.ArsAlchemica;
import com.medievalmagic.arsalchemica.cauldron.AlchemicalCauldronBlock;
import com.medievalmagic.arsalchemica.cauldron.AlchemicalCauldronBlockEntity;
import com.medievalmagic.arsalchemica.cauldron.AlchemicalStirringRodItem;
import com.medievalmagic.arsalchemica.cauldron.item.AlchemicalSlagItem;
import com.medievalmagic.arsalchemica.cauldron.item.AlchemicalSludgeItem;
import com.medievalmagic.arsalchemica.grimoire.AlchemicalJournalItem;
import com.medievalmagic.arsalchemica.herb.apparatus.DryingRackBlock;
import com.medievalmagic.arsalchemica.herb.apparatus.DryingRackBlockEntity;
import com.medievalmagic.arsalchemica.herb.apparatus.HerbPressBlock;
import com.medievalmagic.arsalchemica.herb.apparatus.HerbPressBlockEntity;
import com.medievalmagic.arsalchemica.herb.apparatus.MortarBlock;
import com.medievalmagic.arsalchemica.herb.apparatus.MortarBlockEntity;
import com.medievalmagic.arsalchemica.potion.AlchemicalCorrosionEffect;
import com.medievalmagic.arsalchemica.potion.AlchemicalPotionItem;
import com.medievalmagic.arsalchemica.potion.BladeOilItem;
import com.medievalmagic.arsalchemica.potion.PlayerToxicityManager;
import com.medievalmagic.arsalchemica.recipe.item.AncientParchmentItem;
import com.medievalmagic.arsalchemica.recipe.item.RecipeScrollItem;
import com.medievalmagic.arsalchemica.recipe.item.ScholiaFragmentItem;
import com.medievalmagic.arsalchemica.registry.ModBlockEntities;
import com.medievalmagic.arsalchemica.registry.ModBlocks;
import com.medievalmagic.arsalchemica.registry.ModEffects;
import com.medievalmagic.arsalchemica.registry.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 《Ars Alchemica》Forge & NeoForge 1.20.1 主模组入口类。
 * <p>
 * 负责平台延迟注册器（DeferredRegister）初始化与 Common 抽象层单例绑定。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 */
@Mod(ArsAlchemica.MOD_ID)
public class ArsAlchemicaForge {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, ArsAlchemica.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ArsAlchemica.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ArsAlchemica.MOD_ID);
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ArsAlchemica.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ArsAlchemica.MOD_ID);

    // --- Blocks ---
    public static final RegistryObject<AlchemicalCauldronBlock> CAULDRON_BLOCK =
            BLOCKS.register("alchemical_cauldron", () -> new AlchemicalCauldronBlock(Block.Properties.of()));
    public static final RegistryObject<MortarBlock> MORTAR_BLOCK =
            BLOCKS.register("mortar", () -> new MortarBlock(Block.Properties.of()));
    public static final RegistryObject<HerbPressBlock> HERB_PRESS_BLOCK =
            BLOCKS.register("herb_press", () -> new HerbPressBlock(Block.Properties.of()));
    public static final RegistryObject<DryingRackBlock> DRYING_RACK_BLOCK =
            BLOCKS.register("drying_rack", () -> new DryingRackBlock(Block.Properties.of()));

    // --- Block Items ---
    public static final RegistryObject<Item> CAULDRON_ITEM =
            ITEMS.register("alchemical_cauldron", () -> new BlockItem(CAULDRON_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> MORTAR_ITEM =
            ITEMS.register("mortar", () -> new BlockItem(MORTAR_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> HERB_PRESS_ITEM =
            ITEMS.register("herb_press", () -> new BlockItem(HERB_PRESS_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> DRYING_RACK_ITEM =
            ITEMS.register("drying_rack", () -> new BlockItem(DRYING_RACK_BLOCK.get(), new Item.Properties()));

    // --- Apparatus & Grimoire Items ---
    public static final RegistryObject<AlchemicalStirringRodItem> STIRRING_ROD =
            ITEMS.register("stirring_rod", () -> new AlchemicalStirringRodItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<AlchemicalJournalItem> JOURNAL =
            ITEMS.register("alchemical_journal", () -> new AlchemicalJournalItem(new Item.Properties().stacksTo(1)));

    // --- Exploration & Relic Items ---
    public static final RegistryObject<RecipeScrollItem> RECIPE_SCROLL =
            ITEMS.register("recipe_scroll", () -> new RecipeScrollItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<AncientParchmentItem> ANCIENT_PARCHMENT =
            ITEMS.register("ancient_parchment", () -> new AncientParchmentItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<ScholiaFragmentItem> SCHOLIA_FRAGMENT =
            ITEMS.register("scholia_fragment", () -> new ScholiaFragmentItem(new Item.Properties().stacksTo(16)));

    // --- Mishap & Derivative Items ---
    public static final RegistryObject<AlchemicalSlagItem> SLAG =
            ITEMS.register("alchemical_slag", () -> new AlchemicalSlagItem(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<AlchemicalSludgeItem> SLUDGE =
            ITEMS.register("alchemical_sludge", () -> new AlchemicalSludgeItem(new Item.Properties().stacksTo(16)));

    // --- Potions & Blade Oil ---
    public static final RegistryObject<AlchemicalPotionItem> ELIXIR =
            ITEMS.register("elixir", () -> new AlchemicalPotionItem(new Item.Properties().stacksTo(16), AlchemicalPotionItem.DeliveryType.ELIXIR));
    public static final RegistryObject<AlchemicalPotionItem> INCENSE =
            ITEMS.register("incense", () -> new AlchemicalPotionItem(new Item.Properties().stacksTo(16), AlchemicalPotionItem.DeliveryType.INCENSE));
    public static final RegistryObject<BladeOilItem> BLADE_OIL =
            ITEMS.register("blade_oil", () -> new BladeOilItem(new Item.Properties().stacksTo(1)));

    // --- Block Entities ---
    public static final RegistryObject<BlockEntityType<AlchemicalCauldronBlockEntity>> CAULDRON_BE =
            BLOCK_ENTITY_TYPES.register("alchemical_cauldron", () ->
                    BlockEntityType.Builder.of(AlchemicalCauldronBlockEntity::new, CAULDRON_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<MortarBlockEntity>> MORTAR_BE =
            BLOCK_ENTITY_TYPES.register("mortar", () ->
                    BlockEntityType.Builder.of(MortarBlockEntity::new, MORTAR_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<HerbPressBlockEntity>> HERB_PRESS_BE =
            BLOCK_ENTITY_TYPES.register("herb_press", () ->
                    BlockEntityType.Builder.of(HerbPressBlockEntity::new, HERB_PRESS_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<DryingRackBlockEntity>> DRYING_RACK_BE =
            BLOCK_ENTITY_TYPES.register("drying_rack", () ->
                    BlockEntityType.Builder.of(DryingRackBlockEntity::new, DRYING_RACK_BLOCK.get()).build(null));

    // --- Effects ---
    public static final RegistryObject<AlchemicalCorrosionEffect> CORROSION_EFFECT =
            MOB_EFFECTS.register("alchemical_corrosion", AlchemicalCorrosionEffect::new);

    // --- Creative Mode Tab ---
    public static final RegistryObject<CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register(
            "main",
            () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, -1)
                    .title(Component.translatable("itemGroup.ars_alchemica.main"))
                    .icon(() -> new ItemStack(CAULDRON_ITEM.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(CAULDRON_ITEM.get());
                        output.accept(MORTAR_ITEM.get());
                        output.accept(HERB_PRESS_ITEM.get());
                        output.accept(DRYING_RACK_ITEM.get());
                        output.accept(STIRRING_ROD.get());
                        output.accept(JOURNAL.get());
                        output.accept(RECIPE_SCROLL.get());
                        output.accept(ANCIENT_PARCHMENT.get());
                        output.accept(SCHOLIA_FRAGMENT.get());
                        output.accept(SLAG.get());
                        output.accept(SLUDGE.get());
                        output.accept(ELIXIR.get());
                        output.accept(INCENSE.get());
                        output.accept(BLADE_OIL.get());
                    })
                    .build());

    public ArsAlchemicaForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册到 Forge/NeoForge 延迟注册器
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        MOB_EFFECTS.register(modBus);
        CREATIVE_MODE_TABS.register(modBus);

        // 绑定 Common 跨平台中心
        ModBlocks.bindCauldron(CAULDRON_BLOCK::get);
        ModBlocks.bindApparatus(MORTAR_BLOCK::get, HERB_PRESS_BLOCK::get, DRYING_RACK_BLOCK::get);
        ModBlockEntities.bindCauldron(CAULDRON_BE::get);
        ModBlockEntities.bind(MORTAR_BE::get, HERB_PRESS_BE::get, DRYING_RACK_BE::get);
        ModItems.bindApparatusItems(STIRRING_ROD::get, JOURNAL::get);
        ModItems.bindExplorationItems(RECIPE_SCROLL::get, ANCIENT_PARCHMENT::get, SCHOLIA_FRAGMENT::get);
        ModItems.bindMishapItems(SLAG::get, SLUDGE::get);
        ModItems.bindPotions(ELIXIR::get, INCENSE::get, BLADE_OIL::get);
        ModEffects.bindCorrosion(CORROSION_EFFECT::get);

        // 注册全局玩家事件监听
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player != null) {
            PlayerToxicityManager.tickPlayer(event.player);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.player != null) {
            PlayerToxicityManager.onPlayerDeathOrRespawn(event.player);
        }
    }
}
