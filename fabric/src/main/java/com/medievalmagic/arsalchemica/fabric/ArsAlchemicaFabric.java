package com.medievalmagic.arsalchemica.fabric;

import com.medievalmagic.arsalchemica.ArsAlchemica;
import com.medievalmagic.arsalchemica.cauldron.AlchemicalCauldronBlock;
import com.medievalmagic.arsalchemica.cauldron.AlchemicalCauldronBlockEntity;
import com.medievalmagic.arsalchemica.cauldron.AlchemicalStirringRodItem;
import com.medievalmagic.arsalchemica.cauldron.item.AlchemicalSlagItem;
import com.medievalmagic.arsalchemica.cauldron.item.AlchemicalSludgeItem;
import com.medievalmagic.arsalchemica.grimoire.AlchemicalJournalItem;
import com.medievalmagic.arsalchemica.herb.AlchemicalHerbBlock;
import com.medievalmagic.arsalchemica.herb.HerbRegistry;
import com.medievalmagic.arsalchemica.herb.IAlchemicalHerb;
import com.medievalmagic.arsalchemica.herb.apparatus.DryingRackBlock;
import com.medievalmagic.arsalchemica.herb.apparatus.DryingRackBlockEntity;
import com.medievalmagic.arsalchemica.herb.apparatus.HerbPressBlock;
import com.medievalmagic.arsalchemica.herb.apparatus.HerbPressBlockEntity;
import com.medievalmagic.arsalchemica.herb.apparatus.MortarBlock;
import com.medievalmagic.arsalchemica.herb.apparatus.MortarBlockEntity;
import com.medievalmagic.arsalchemica.herb.apparatus.ProcessedHerbItem;
import com.medievalmagic.arsalchemica.herb.item.AlchemicalHerbItem;
import com.medievalmagic.arsalchemica.potion.AlchemicalCorrosionEffect;
import com.medievalmagic.arsalchemica.potion.AlchemicalPotionItem;
import com.medievalmagic.arsalchemica.potion.BladeOilItem;
import com.medievalmagic.arsalchemica.recipe.item.AncientParchmentItem;
import com.medievalmagic.arsalchemica.recipe.item.RecipeScrollItem;
import com.medievalmagic.arsalchemica.recipe.item.ScholiaFragmentItem;
import com.medievalmagic.arsalchemica.registry.ModBlockEntities;
import com.medievalmagic.arsalchemica.registry.ModBlocks;
import com.medievalmagic.arsalchemica.registry.ModEffects;
import com.medievalmagic.arsalchemica.registry.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * 《Ars Alchemica》Fabric 1.20.1 主模组入口类。
 * <p>
 * 实现 {@link ModInitializer} 接口，在通用平台注册表完成初始化并绑定 Common 抽象层单例。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 */
public class ArsAlchemicaFabric implements ModInitializer {

    @Override
    @SuppressWarnings("unchecked")
    public void onInitialize() {
        // --- 1. Blocks 注册 ---
        AlchemicalCauldronBlock cauldronBlock = Registry.register(
                BuiltInRegistries.BLOCK,
                ArsAlchemica.id("alchemical_cauldron"),
                new AlchemicalCauldronBlock(Block.Properties.of())
        );
        MortarBlock mortarBlock = Registry.register(
                BuiltInRegistries.BLOCK,
                ArsAlchemica.id("mortar"),
                new MortarBlock(Block.Properties.of())
        );
        HerbPressBlock herbPressBlock = Registry.register(
                BuiltInRegistries.BLOCK,
                ArsAlchemica.id("herb_press"),
                new HerbPressBlock(Block.Properties.of())
        );
        DryingRackBlock dryingRackBlock = Registry.register(
                BuiltInRegistries.BLOCK,
                ArsAlchemica.id("drying_rack"),
                new DryingRackBlock(Block.Properties.of())
        );

        // --- 7 大草药方块 ---
        AlchemicalHerbBlock.CinderThistleBlock cinderThistleBlock = Registry.register(
                BuiltInRegistries.BLOCK,
                ArsAlchemica.id("cinder_thistle"),
                new AlchemicalHerbBlock.CinderThistleBlock(Block.Properties.of().instabreak().sound(SoundType.GRASS).noOcclusion())
        );
        AlchemicalHerbBlock.FrostveinOrchidBlock frostveinOrchidBlock = Registry.register(
                BuiltInRegistries.BLOCK,
                ArsAlchemica.id("frostvein_orchid"),
                new AlchemicalHerbBlock.FrostveinOrchidBlock(Block.Properties.of().instabreak().sound(SoundType.GRASS).noOcclusion())
        );
        AlchemicalHerbBlock.StormMarigoldBlock stormMarigoldBlock = Registry.register(
                BuiltInRegistries.BLOCK,
                ArsAlchemica.id("storm_marigold"),
                new AlchemicalHerbBlock.StormMarigoldBlock(Block.Properties.of().instabreak().sound(SoundType.GRASS).noOcclusion())
        );
        AlchemicalHerbBlock.AbyssalLuminaMintBlock abyssalMintBlock = Registry.register(
                BuiltInRegistries.BLOCK,
                ArsAlchemica.id("abyssal_lumina_mint"),
                new AlchemicalHerbBlock.AbyssalLuminaMintBlock(Block.Properties.of().instabreak().sound(SoundType.GRASS).noOcclusion())
        );
        AlchemicalHerbBlock.ScreamingMandragoraBlock mandragoraBlock = Registry.register(
                BuiltInRegistries.BLOCK,
                ArsAlchemica.id("screaming_mandragora"),
                new AlchemicalHerbBlock.ScreamingMandragoraBlock(Block.Properties.of().instabreak().sound(SoundType.GRASS).noOcclusion())
        );
        AlchemicalHerbBlock.MoonveilBelladonnaBlock belladonnaBlock = Registry.register(
                BuiltInRegistries.BLOCK,
                ArsAlchemica.id("moonveil_belladonna"),
                new AlchemicalHerbBlock.MoonveilBelladonnaBlock(Block.Properties.of().instabreak().sound(SoundType.GRASS).noOcclusion())
        );
        AlchemicalHerbBlock.CarrionBogmossBlock bogmossBlock = Registry.register(
                BuiltInRegistries.BLOCK,
                ArsAlchemica.id("carrion_bogmoss"),
                new AlchemicalHerbBlock.CarrionBogmossBlock(Block.Properties.of().instabreak().sound(SoundType.GRASS).noOcclusion())
        );

        // --- 2. Block Items 注册 ---
        BlockItem cauldronItem = Registry.register(BuiltInRegistries.ITEM, ArsAlchemica.id("alchemical_cauldron"),
                new BlockItem(cauldronBlock, new Item.Properties()));
        BlockItem mortarItem = Registry.register(BuiltInRegistries.ITEM, ArsAlchemica.id("mortar"),
                new BlockItem(mortarBlock, new Item.Properties()));
        BlockItem herbPressItem = Registry.register(BuiltInRegistries.ITEM, ArsAlchemica.id("herb_press"),
                new BlockItem(herbPressBlock, new Item.Properties()));
        BlockItem dryingRackItem = Registry.register(BuiltInRegistries.ITEM, ArsAlchemica.id("drying_rack"),
                new BlockItem(dryingRackBlock, new Item.Properties()));

        // --- 7 大草药植株物品 (BlockItem) ---
        AlchemicalHerbItem cinderThistleItem = Registry.register(BuiltInRegistries.ITEM, ArsAlchemica.id("cinder_thistle"),
                new AlchemicalHerbItem(cinderThistleBlock, new Item.Properties(), HerbRegistry.CINDER_THISTLE));
        AlchemicalHerbItem frostveinOrchidItem = Registry.register(BuiltInRegistries.ITEM, ArsAlchemica.id("frostvein_orchid"),
                new AlchemicalHerbItem(frostveinOrchidBlock, new Item.Properties(), HerbRegistry.FROSTVEIN_ORCHID));
        AlchemicalHerbItem stormMarigoldItem = Registry.register(BuiltInRegistries.ITEM, ArsAlchemica.id("storm_marigold"),
                new AlchemicalHerbItem(stormMarigoldBlock, new Item.Properties(), HerbRegistry.STORM_MARIGOLD));
        AlchemicalHerbItem abyssalMintItem = Registry.register(BuiltInRegistries.ITEM, ArsAlchemica.id("abyssal_lumina_mint"),
                new AlchemicalHerbItem(abyssalMintBlock, new Item.Properties(), HerbRegistry.ABYSSAL_LUMINA_MINT));
        AlchemicalHerbItem mandragoraItem = Registry.register(BuiltInRegistries.ITEM, ArsAlchemica.id("screaming_mandragora"),
                new AlchemicalHerbItem(mandragoraBlock, new Item.Properties(), HerbRegistry.SCREAMING_MANDRAGORA));
        AlchemicalHerbItem belladonnaItem = Registry.register(BuiltInRegistries.ITEM, ArsAlchemica.id("moonveil_belladonna"),
                new AlchemicalHerbItem(belladonnaBlock, new Item.Properties(), HerbRegistry.MOONVEIL_BELLADONNA));
        AlchemicalHerbItem bogmossItem = Registry.register(BuiltInRegistries.ITEM, ArsAlchemica.id("carrion_bogmoss"),
                new AlchemicalHerbItem(bogmossBlock, new Item.Properties(), HerbRegistry.CARRION_BOGMOSS));

        // --- 统一草药加工产物 ---
        ProcessedHerbItem processedHerbItem = Registry.register(BuiltInRegistries.ITEM, ArsAlchemica.id("processed_herb"),
                new ProcessedHerbItem(new Item.Properties().stacksTo(64), HerbRegistry.CINDER_THISTLE, IAlchemicalHerb.ProcessingForm.RAW));

        // --- 3. Items 注册 ---
        AlchemicalStirringRodItem stirringRod = Registry.register(
                BuiltInRegistries.ITEM,
                ArsAlchemica.id("stirring_rod"),
                new AlchemicalStirringRodItem(new Item.Properties().stacksTo(1))
        );
        AlchemicalJournalItem journal = Registry.register(
                BuiltInRegistries.ITEM,
                ArsAlchemica.id("alchemical_journal"),
                new AlchemicalJournalItem(new Item.Properties().stacksTo(1))
        );
        RecipeScrollItem recipeScroll = Registry.register(
                BuiltInRegistries.ITEM,
                ArsAlchemica.id("recipe_scroll"),
                new RecipeScrollItem(new Item.Properties().stacksTo(16))
        );
        AncientParchmentItem ancientParchment = Registry.register(
                BuiltInRegistries.ITEM,
                ArsAlchemica.id("ancient_parchment"),
                new AncientParchmentItem(new Item.Properties().stacksTo(1))
        );
        ScholiaFragmentItem scholiaFragment = Registry.register(
                BuiltInRegistries.ITEM,
                ArsAlchemica.id("scholia_fragment"),
                new ScholiaFragmentItem(new Item.Properties().stacksTo(16))
        );
        AlchemicalSlagItem slag = Registry.register(
                BuiltInRegistries.ITEM,
                ArsAlchemica.id("alchemical_slag"),
                new AlchemicalSlagItem(new Item.Properties().stacksTo(64))
        );
        AlchemicalSludgeItem sludge = Registry.register(
                BuiltInRegistries.ITEM,
                ArsAlchemica.id("alchemical_sludge"),
                new AlchemicalSludgeItem(new Item.Properties().stacksTo(16))
        );
        AlchemicalPotionItem elixir = Registry.register(
                BuiltInRegistries.ITEM,
                ArsAlchemica.id("elixir"),
                new AlchemicalPotionItem(new Item.Properties().stacksTo(16), AlchemicalPotionItem.DeliveryType.ELIXIR)
        );
        AlchemicalPotionItem incense = Registry.register(
                BuiltInRegistries.ITEM,
                ArsAlchemica.id("incense"),
                new AlchemicalPotionItem(new Item.Properties().stacksTo(16), AlchemicalPotionItem.DeliveryType.INCENSE)
        );
        BladeOilItem bladeOil = Registry.register(
                BuiltInRegistries.ITEM,
                ArsAlchemica.id("blade_oil"),
                new BladeOilItem(new Item.Properties().stacksTo(1))
        );

        // --- 4. Block Entities 注册 ---
        BlockEntityType<AlchemicalCauldronBlockEntity> cauldronBe = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ArsAlchemica.id("alchemical_cauldron"),
                FabricBlockEntityTypeBuilder.create(AlchemicalCauldronBlockEntity::new, cauldronBlock).build()
        );
        BlockEntityType<MortarBlockEntity> mortarBe = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ArsAlchemica.id("mortar"),
                FabricBlockEntityTypeBuilder.create(MortarBlockEntity::new, mortarBlock).build()
        );
        BlockEntityType<HerbPressBlockEntity> herbPressBe = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ArsAlchemica.id("herb_press"),
                FabricBlockEntityTypeBuilder.create(HerbPressBlockEntity::new, herbPressBlock).build()
        );
        BlockEntityType<DryingRackBlockEntity> dryingRackBe = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ArsAlchemica.id("drying_rack"),
                FabricBlockEntityTypeBuilder.create(DryingRackBlockEntity::new, dryingRackBlock).build()
        );

        // --- 5. MobEffects 注册 ---
        AlchemicalCorrosionEffect corrosionEffect = Registry.register(
                BuiltInRegistries.MOB_EFFECT,
                ArsAlchemica.id("alchemical_corrosion"),
                new AlchemicalCorrosionEffect()
        );

        // --- 6. 绑定到 Common 抽象中心 ---
        ModBlocks.bindCauldron(() -> cauldronBlock);
        ModBlocks.bindApparatus(() -> mortarBlock, () -> herbPressBlock, () -> dryingRackBlock);
        ModBlocks.bindHerbs(
                () -> cinderThistleBlock,
                () -> frostveinOrchidBlock,
                () -> stormMarigoldBlock,
                () -> abyssalMintBlock,
                () -> mandragoraBlock,
                () -> belladonnaBlock,
                () -> bogmossBlock
        );
        ModBlockEntities.bindCauldron(() -> cauldronBe);
        ModBlockEntities.bind(() -> mortarBe, () -> herbPressBe, () -> dryingRackBe);
        ModItems.bindApparatusItems(() -> stirringRod, () -> journal);
        ModItems.bindExplorationItems(() -> recipeScroll, () -> ancientParchment, () -> scholiaFragment);
        ModItems.bindMishapItems(() -> slag, () -> sludge);
        ModItems.bindPotions(() -> elixir, () -> incense, () -> bladeOil);
        ModItems.bindHerbs(
                () -> cinderThistleItem,
                () -> frostveinOrchidItem,
                () -> stormMarigoldItem,
                () -> abyssalMintItem,
                () -> mandragoraItem,
                () -> belladonnaItem,
                () -> bogmossItem,
                () -> processedHerbItem
        );
        ModEffects.bindCorrosion(() -> corrosionEffect);

        // --- 7. CreativeModeTab 注册 ---
        CreativeModeTab mainTab = FabricItemGroup.builder()
                .title(Component.translatable("itemGroup.ars_alchemica.main"))
                .icon(() -> new ItemStack(cauldronItem))
                .displayItems((parameters, output) -> {
                    output.accept(cauldronItem);
                    output.accept(mortarItem);
                    output.accept(herbPressItem);
                    output.accept(dryingRackItem);
                    output.accept(cinderThistleItem);
                    output.accept(frostveinOrchidItem);
                    output.accept(stormMarigoldItem);
                    output.accept(abyssalMintItem);
                    output.accept(mandragoraItem);
                    output.accept(belladonnaItem);
                    output.accept(bogmossItem);
                    output.accept(processedHerbItem);
                    output.accept(stirringRod);
                    output.accept(journal);
                    output.accept(recipeScroll);
                    output.accept(ancientParchment);
                    output.accept(scholiaFragment);
                    output.accept(slag);
                    output.accept(sludge);
                    output.accept(elixir);
                    output.accept(incense);
                    output.accept(bladeOil);
                })
                .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ArsAlchemica.id("main"), mainTab);

        // --- 8. 世界生成挂载 ---
        registerWorldgen();
    }

    private void registerWorldgen() {
        String[] herbs = {
                "cinder_thistle",
                "frostvein_orchid",
                "storm_marigold",
                "abyssal_lumina_mint",
                "screaming_mandragora",
                "moonveil_belladonna",
                "carrion_bogmoss"
        };
        for (String herb : herbs) {
            TagKey<Biome> biomeTag = TagKey.create(Registries.BIOME, ArsAlchemica.id("has_herb/" + herb));
            ResourceKey<PlacedFeature> placedKey = ResourceKey.create(Registries.PLACED_FEATURE, ArsAlchemica.id("patch_herb_" + herb));

            BiomeModifications.addFeature(
                    BiomeSelectors.tag(biomeTag),
                    GenerationStep.Decoration.VEGETAL_DECORATION,
                    placedKey
            );
        }
    }
}
