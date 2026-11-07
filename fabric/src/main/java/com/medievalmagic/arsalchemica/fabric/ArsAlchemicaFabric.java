package com.medievalmagic.arsalchemica.fabric;

import com.medievalmagic.arsalchemica.ArsAlchemica;
import com.medievalmagic.arsalchemica.cauldron.AlchemicalCauldronBlock;
import com.medievalmagic.arsalchemica.cauldron.AlchemicalCauldronBlockEntity;
import com.medievalmagic.arsalchemica.cauldron.AlchemicalStirringRodItem;
import com.medievalmagic.arsalchemica.cauldron.item.AlchemicalSlagItem;
import com.medievalmagic.arsalchemica.cauldron.item.AlchemicalSludgeItem;
import com.medievalmagic.arsalchemica.grimoire.AlchemicalJournalItem;
import com.medievalmagic.arsalchemica.herb.apparatus.DryingRackBlock;
import com.medievalmagic.arsalchemica.herb.apparatus.HerbPressBlock;
import com.medievalmagic.arsalchemica.herb.apparatus.MortarBlock;
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
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

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

        // --- 2. Block Items 注册 ---
        Registry.register(BuiltInRegistries.ITEM, ArsAlchemica.id("alchemical_cauldron"),
                new BlockItem(cauldronBlock, new Item.Properties()));
        Registry.register(BuiltInRegistries.ITEM, ArsAlchemica.id("mortar"),
                new BlockItem(mortarBlock, new Item.Properties()));
        Registry.register(BuiltInRegistries.ITEM, ArsAlchemica.id("herb_press"),
                new BlockItem(herbPressBlock, new Item.Properties()));
        Registry.register(BuiltInRegistries.ITEM, ArsAlchemica.id("drying_rack"),
                new BlockItem(dryingRackBlock, new Item.Properties()));

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

        // --- 5. MobEffects 注册 ---
        AlchemicalCorrosionEffect corrosionEffect = Registry.register(
                BuiltInRegistries.MOB_EFFECT,
                ArsAlchemica.id("alchemical_corrosion"),
                new AlchemicalCorrosionEffect()
        );

        // --- 6. 绑定到 Common 抽象中心 ---
        ModBlocks.bindCauldron(() -> cauldronBlock);
        ModBlocks.bindApparatus(() -> mortarBlock, () -> herbPressBlock, () -> dryingRackBlock);
        ModBlockEntities.bindCauldron(() -> cauldronBe);
        ModItems.bindApparatusItems(() -> stirringRod, () -> journal);
        ModItems.bindExplorationItems(() -> recipeScroll, () -> ancientParchment, () -> scholiaFragment);
        ModItems.bindMishapItems(() -> slag, () -> sludge);
        ModItems.bindPotions(() -> elixir, () -> incense, () -> bladeOil);
        ModEffects.bindCorrosion(() -> corrosionEffect);
    }
}
