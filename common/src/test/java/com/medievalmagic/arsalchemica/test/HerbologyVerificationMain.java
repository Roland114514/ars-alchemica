package com.medievalmagic.arsalchemica.test;

import com.medievalmagic.arsalchemica.api.aspect.AlchemyAspect;
import com.medievalmagic.arsalchemica.api.vector.AspectVector;
import com.medievalmagic.arsalchemica.herb.HerbRegistry;
import com.medievalmagic.arsalchemica.herb.IAlchemicalHerb.ProcessingForm;
import com.medievalmagic.arsalchemica.herb.apparatus.*;
import com.medievalmagic.arsalchemica.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Milestone 2 核心草药生态与物理加工器械完整自动化验证套件。
 *
 * @author Ars Alchemica Architecture Team
 */
public class HerbologyVerificationMain {

    public static void main(String[] args) {
        System.out.println("=========================================================");
        System.out.println("   Ars Alchemica Milestone 2 Herbology & Apparatus Test   ");
        System.out.println("=========================================================");

        // 初始化 ModBlockEntities 模拟类型
        BlockEntityType<MortarBlockEntity> mortarType = new BlockEntityType<>();
        BlockEntityType<HerbPressBlockEntity> pressType = new BlockEntityType<>();
        BlockEntityType<DryingRackBlockEntity> rackType = new BlockEntityType<>();
        ModBlockEntities.bind(() -> mortarType, () -> pressType, () -> rackType);

        testSevenHerbsAndMatrices();
        testProcessingFormTransformations();
        testMortarStateMachine();
        testHerbPressMechanism();
        testDryingRackThermodynamics();
        testTooltipsAndPurityStars();

        System.out.println("\n>>> ALL 6 HERBOLOGY & APPARATUS SUITES PASSED! <<<");
    }

    private static void testSevenHerbsAndMatrices() {
        System.out.print("[1/6] Testing 7 Canon Herbs & 12D Aspect Matrices... ");

        assert HerbRegistry.values().length == 7 : "Must have exactly 7 canon herbs";

        // 1. Cinder Thistle: Ignis & Sulfur dominant
        AspectVector cinderVec = HerbRegistry.CINDER_THISTLE.getAspectVector(ProcessingForm.RAW);
        assert cinderVec.getAmount(AlchemyAspect.IGNIS) == 8.5 : "Cinder thistle must have 8.5 Ignis";
        assert cinderVec.getAmount(AlchemyAspect.SULFUR) == 4.0 : "Cinder thistle must have 4.0 Sulfur";
        assert cinderVec.getTotalAspectMass() == 17.0 : "Cinder thistle total mass must be 17.0";

        // 2. Abyssal Lumina Mint: Aqua & Ordo dominant
        AspectVector mintVec = HerbRegistry.ABYSSAL_LUMINA_MINT.getAspectVector(ProcessingForm.RAW);
        assert mintVec.getAmount(AlchemyAspect.AQUA) == 7.5;
        assert mintVec.getAmount(AlchemyAspect.ORDO) == 6.5;

        // 3. Screaming Mandragora: Mortis dominant, high toxicity
        AspectVector mandragoraVec = HerbRegistry.SCREAMING_MANDRAGORA.getAspectVector(ProcessingForm.RAW);
        assert mandragoraVec.getAmount(AlchemyAspect.MORTIS) == 9.0;
        assert HerbRegistry.SCREAMING_MANDRAGORA.getBaseToxicity(ProcessingForm.RAW) == 65.0;

        // 4. Frostvein Orchid: Aqua & Sal dominant, 0 Ignis
        AspectVector orchidVec = HerbRegistry.FROSTVEIN_ORCHID.getAspectVector(ProcessingForm.RAW);
        assert orchidVec.getAmount(AlchemyAspect.IGNIS) == 0.0 : "Frostvein Orchid must have 0 Ignis";
        assert orchidVec.getAmount(AlchemyAspect.SAL) == 6.5;

        // 5. Moonveil Belladonna: Aether & Aer dominant, high toxicity
        AspectVector bellaVec = HerbRegistry.MOONVEIL_BELLADONNA.getAspectVector(ProcessingForm.RAW);
        assert bellaVec.getAmount(AlchemyAspect.AETHER) == 9.0;
        assert HerbRegistry.MOONVEIL_BELLADONNA.getBaseToxicity(ProcessingForm.RAW) == 80.0;

        // 6. Carrion Bogmoss: Sal & Terra dominant, low toxicity
        AspectVector mossVec = HerbRegistry.CARRION_BOGMOSS.getAspectVector(ProcessingForm.RAW);
        assert mossVec.getAmount(AlchemyAspect.SAL) == 8.5;
        assert mossVec.getAmount(AlchemyAspect.TERRA) == 7.0;

        // 7. Storm Marigold: Aer & Ignis dominant
        AspectVector stormVec = HerbRegistry.STORM_MARIGOLD.getAspectVector(ProcessingForm.RAW);
        assert stormVec.getAmount(AlchemyAspect.AER) == 9.5;
        assert stormVec.getAmount(AlchemyAspect.IGNIS) == 5.5;

        System.out.println("PASSED!");
    }

    private static void testProcessingFormTransformations() {
        System.out.print("[2/6] Testing 5 Processing Forms & Purity/Volatility/Toxicity... ");

        HerbRegistry herb = HerbRegistry.CINDER_THISTLE;

        AspectVector raw = herb.getAspectVector(ProcessingForm.RAW);
        AspectVector coarse = herb.getAspectVector(ProcessingForm.MILLED_COARSE);
        AspectVector fine = herb.getAspectVector(ProcessingForm.MILLED_FINE);
        AspectVector oil = herb.getAspectVector(ProcessingForm.PRESSED_OIL);
        AspectVector dried = herb.getAspectVector(ProcessingForm.DRIED);

        // Vector scaling checks
        assert Math.abs(coarse.getTotalAspectMass() - raw.getTotalAspectMass() * 1.15) < 1e-4;
        assert Math.abs(fine.getTotalAspectMass() - raw.getTotalAspectMass() * 1.40) < 1e-4;
        assert Math.abs(oil.getTotalAspectMass() - raw.getTotalAspectMass() * 1.80) < 1e-4;
        assert Math.abs(dried.getTotalAspectMass() - raw.getTotalAspectMass() * 1.10) < 1e-4;

        // Volatility modifier checks
        assert herb.getVolatilityModifier(ProcessingForm.DRIED) < herb.getVolatilityModifier(ProcessingForm.RAW) : "Dried herb must have lower volatility";
        assert herb.getVolatilityModifier(ProcessingForm.PRESSED_OIL) > herb.getVolatilityModifier(ProcessingForm.RAW) : "Oil must have higher volatility";

        // Toxicity attenuation checks
        HerbRegistry toxicHerb = HerbRegistry.SCREAMING_MANDRAGORA;
        double rawTox = toxicHerb.getBaseToxicity(ProcessingForm.RAW);
        double driedTox = toxicHerb.getBaseToxicity(ProcessingForm.DRIED);
        double oilTox = toxicHerb.getBaseToxicity(ProcessingForm.PRESSED_OIL);

        assert driedTox == rawTox * 0.45 : "Drying must reduce toxicity by 55%";
        assert oilTox == rawTox * 1.35 : "Cold pressing oil concentrates toxicity by 35%";

        System.out.println("PASSED!");
    }

    private static void testMortarStateMachine() {
        System.out.print("[3/6] Testing Mortar Grinding State Machine & Anti-Overgrind... ");

        BlockPos pos = new BlockPos(10, 64, 10);
        BlockState state = new BlockState(null);
        MortarBlockEntity mortar = new MortarBlockEntity(pos, state);

        ServerLevel level = new ServerLevel();
        level.setGameTime(100L);
        mortar.setLevel(level);

        Item dummyItem = new Item("herb_item");
        ProcessedHerbItem herbItem = new ProcessedHerbItem(new Item.Properties(), HerbRegistry.CINDER_THISTLE, ProcessingForm.RAW);
        ItemStack rawHerb = ProcessedHerbItem.create(herbItem, HerbRegistry.CINDER_THISTLE, ProcessingForm.RAW, 1.0);

        assert mortar.canAccept(rawHerb) : "Mortar must accept raw herb";
        mortar.insertItem(rawHerb);
        assert !mortar.isEmpty();
        assert mortar.getGrindCount() == 0;

        Player player = new Player();
        ItemStack pestle = new ItemStack(new Item("stone_pestle"));

        // Step 1: Grinds 1 ~ 3 -> Milled Coarse
        level.setGameTime(105L);
        assert mortar.performGrind(player, pestle);
        assert mortar.getGrindCount() == 1;
        assert herbItem.getForm(mortar.getContent()) == ProcessingForm.MILLED_COARSE;

        level.setGameTime(110L);
        mortar.performGrind(player, pestle);
        level.setGameTime(115L);
        mortar.performGrind(player, pestle);
        assert mortar.getGrindCount() == 3;
        assert herbItem.getForm(mortar.getContent()) == ProcessingForm.MILLED_COARSE;

        // Step 2: Grinds 4 ~ 6 -> Milled Fine
        level.setGameTime(120L);
        mortar.performGrind(player, pestle);
        assert mortar.getGrindCount() == 4;
        assert herbItem.getForm(mortar.getContent()) == ProcessingForm.MILLED_FINE;

        level.setGameTime(125L);
        mortar.performGrind(player, pestle);
        level.setGameTime(130L);
        mortar.performGrind(player, pestle);
        assert mortar.getGrindCount() == 6;
        assert herbItem.getForm(mortar.getContent()) == ProcessingForm.MILLED_FINE;
        double fineQuality = herbItem.getQuality(mortar.getContent());
        assert fineQuality > 1.0 : "Peak fine grinding must yield quality > 1.0";

        // Step 3: Grinds 7 ~ 8 -> Overheat smoke & quality penalty
        level.setGameTime(135L);
        mortar.performGrind(player, pestle);
        level.setGameTime(140L);
        mortar.performGrind(player, pestle);
        assert mortar.getGrindCount() == 8;
        assert herbItem.getQuality(mortar.getContent()) < fineQuality : "Overgrind must degrade quality";

        // Step 4: Grind > 8 -> Charred Slag
        level.setGameTime(145L);
        mortar.performGrind(player, pestle);
        assert mortar.getGrindCount() == 9;
        assert herbItem.isSlag(mortar.getContent()) : "Grind > 8 must burn into charred slag";

        // Extraction
        ItemStack extracted = mortar.extractItem();
        assert mortar.isEmpty() : "Mortar must be empty after extraction";
        assert herbItem.isSlag(extracted);

        // Cooldown throttle test
        mortar.insertItem(rawHerb);
        level.setGameTime(200L);
        assert mortar.performGrind(player, pestle);
        // Instant second grind within 2 ticks must be rejected by cooldown
        level.setGameTime(202L);
        assert !mortar.performGrind(player, pestle) : "Fast-clicking macro must be throttled";

        System.out.println("PASSED!");
    }

    private static void testHerbPressMechanism() {
        System.out.print("[4/6] Testing Herb Press Mechanism & Oil Extraction... ");

        BlockPos pos = new BlockPos(20, 64, 20);
        BlockState state = new BlockState(null);
        HerbPressBlockEntity press = new HerbPressBlockEntity(pos, state);

        ServerLevel level = new ServerLevel();
        press.setLevel(level);

        ProcessedHerbItem herbItem = new ProcessedHerbItem(new Item.Properties(), HerbRegistry.ABYSSAL_LUMINA_MINT, ProcessingForm.RAW);
        ItemStack mintStack = ProcessedHerbItem.create(herbItem, HerbRegistry.ABYSSAL_LUMINA_MINT, ProcessingForm.RAW, 1.25);
        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);

        assert press.canInsertHerb(mintStack);
        press.insertHerb(mintStack);
        assert !press.canActivatePress() : "Cannot press without bottle";

        assert press.canInsertVial();
        press.insertVial(bottle);
        assert press.canActivatePress() : "Press is ready with herb + bottle";

        // Pull lever stage 1
        press.activateLeverPress(state);
        assert press.getPressStage() == 1;
        assert !press.hasOutputOil();

        // Pull lever stage 2 -> finishes pressing
        press.activateLeverPress(state);
        assert press.getPressStage() == 0;
        assert press.hasOutputOil();

        ItemStack oilStack = press.extractVialSlot();
        assert herbItem.getForm(oilStack) == ProcessingForm.PRESSED_OIL;
        assert Math.abs(herbItem.getQuality(oilStack) - 1.25) < 1e-4 : "Extracted oil must preserve raw herb quality";
        assert press.getVialSlot().isEmpty();

        System.out.println("PASSED!");
    }

    private static void testDryingRackThermodynamics() {
        System.out.print("[5/6] Testing Drying Rack Staggered Ticking & Weather... ");

        BlockPos pos = new BlockPos(30, 64, 30);
        BlockState state = new BlockState(null);
        DryingRackBlockEntity rack = new DryingRackBlockEntity(pos, state);

        ServerLevel level = new ServerLevel();
        rack.setLevel(level);

        ProcessedHerbItem herbItem = new ProcessedHerbItem(new Item.Properties(), HerbRegistry.FROSTVEIN_ORCHID, ProcessingForm.RAW);
        ItemStack rawHerb = ProcessedHerbItem.create(herbItem, HerbRegistry.FROSTVEIN_ORCHID, ProcessingForm.RAW, 1.0);

        assert rack.canHangHerb(rawHerb);
        assert rack.hangHerb(rawHerb);
        assert rack.getItems().get(0).getCount() == 1;

        // Non-aligned tick should do nothing
        level.setGameTime(1L);
        rack.tick(level, pos, state);
        assert rack.getDryingProgress()[0] == 0;

        // Aligned tick (gameTime + hash % 20 == 0)
        long alignedTick = (20 - (pos.hashCode() % 20) + 20) % 20;
        level.setGameTime(alignedTick);
        rack.tick(level, pos, state);
        assert rack.getDryingProgress()[0] == 20 : "Standard 1s tick should add 20 progress";

        // Fast forward 29 ticks to reach 600
        for (int i = 0; i < 29; i++) {
            alignedTick += 20;
            level.setGameTime(alignedTick);
            rack.tick(level, pos, state);
        }

        assert rack.getDryingProgress()[0] >= DryingRackBlockEntity.REQUIRED_DRYING_TICKS;
        ItemStack finished = rack.getItems().get(0);
        assert herbItem.getForm(finished) == ProcessingForm.DRIED : "Herb must transform into DRIED form";
        assert Math.abs(herbItem.getQuality(finished) - 1.15) < 1e-4 : "Drying rack must grant 1.15x quality bonus";

        // Take dried herb
        ItemStack taken = rack.takeHerb();
        assert herbItem.getForm(taken) == ProcessingForm.DRIED;
        assert rack.getItems().get(0).isEmpty();

        System.out.println("PASSED!");
    }

    private static void testTooltipsAndPurityStars() {
        System.out.print("[6/6] Testing Tooltips, Stars & Dew Quality Bonus... ");

        ProcessedHerbItem herbItem = new ProcessedHerbItem(new Item.Properties(), HerbRegistry.MOONVEIL_BELLADONNA, ProcessingForm.RAW);
        ItemStack dewStack = ProcessedHerbItem.create(herbItem, HerbRegistry.MOONVEIL_BELLADONNA, ProcessingForm.RAW, 1.5);
        ProcessedHerbItem.setDewBonus(dewStack, true);

        assert dewStack.getHoverName().getString().contains("herb.ars_alchemica.moonveil_belladonna");

        List<Component> tooltip = new ArrayList<>();
        herbItem.appendHoverText(dewStack, null, tooltip, TooltipFlag.Default.NORMAL);

        assert !tooltip.isEmpty() : "Tooltip must not be empty";
        boolean hasDewKey = false;
        boolean hasStarKey = false;
        for (Component c : tooltip) {
            String str = c.getString();
            if (str.contains("dew_inherited")) hasDewKey = true;
            if (str.contains("★") || str.contains("✦ 纯度星级")) hasStarKey = true;
        }
        assert hasDewKey : "Dew bonus tooltip must be rendered";
        assert hasStarKey : "Purity stars must be rendered";

        System.out.println("PASSED!");
    }
}
