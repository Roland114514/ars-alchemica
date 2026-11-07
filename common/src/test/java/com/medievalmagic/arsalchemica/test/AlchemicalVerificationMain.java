package com.medievalmagic.arsalchemica.test;

import com.medievalmagic.arsalchemica.api.aspect.AlchemyAspect;
import com.medievalmagic.arsalchemica.api.vector.AspectVector;
import com.medievalmagic.arsalchemica.api.map.AlchemyMap;
import com.medievalmagic.arsalchemica.api.map.AlchemyMapNode;
import com.medievalmagic.arsalchemica.api.cauldron.CauldronReactionState;
import com.medievalmagic.arsalchemica.api.cauldron.CauldronReactionState.HeatLevel;
import com.medievalmagic.arsalchemica.api.cauldron.CauldronReactionState.MishapResult;

public class AlchemicalVerificationMain {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   Ars Alchemica Core Mathematical Engine Test   ");
        System.out.println("=================================================");

        testAspects();
        testVectorMath();
        testMapAndNodes();
        testCauldronPhysics();
        testHardeningAndEdgeCases();

        System.out.println("\n>>> ALL MATHEMATICAL, REACTION & HARDENING TESTS PASSED! <<<");
    }

    private static void testAspects() {
        System.out.print("[1/5] Testing AlchemyAspect System... ");
        assert AlchemyAspect.COUNT == 12 : "Must have exactly 12 aspects";
        assert AlchemyAspect.IGNIS.isOppositeTo(AlchemyAspect.AQUA) : "Ignis must oppose Aqua";
        assert AlchemyAspect.SULFUR.isOppositeTo(AlchemyAspect.SAL) : "Sulfur must oppose Sal";
        assert AlchemyAspect.TERRA.isOppositeTo(AlchemyAspect.AER) : "Terra must oppose Aer";
        assert AlchemyAspect.VITA.isOppositeTo(AlchemyAspect.MORTIS) : "Vita must oppose Mortis";
        assert AlchemyAspect.ORDO.isOppositeTo(AlchemyAspect.CHAOS) : "Ordo must oppose Chaos";

        // Mercurius & Aether should have no opposites (neutral/transcendent)
        assert AlchemyAspect.MERCURIUS.getOpposite().isEmpty() : "Mercurius has no opposite";
        assert AlchemyAspect.AETHER.getOpposite().isEmpty() : "Aether has no opposite";

        // Annihilation factor
        double conflict = AlchemyAspect.IGNIS.getAnnihilationFactor(AlchemyAspect.AQUA);
        assert conflict > 1.0 : "Opposite elements must have conflict > 1.0";
        double buffer = AlchemyAspect.MERCURIUS.getAnnihilationFactor(AlchemyAspect.IGNIS);
        assert buffer < 0.0 : "Mercurius buffer factor must be negative";

        System.out.println("PASSED!");
    }

    private static void testVectorMath() {
        System.out.print("[2/5] Testing AspectVector Algebra & Dynamics... ");

        // Builder & Projection
        AspectVector v1 = AspectVector.builder()
                .add(AlchemyAspect.IGNIS, 5.0)
                .add(AlchemyAspect.SULFUR, 3.0)
                .projectAstralCoordinates()
                .build();

        assert v1.getTotalAspectMass() == 8.0 : "Total mass should be 8.0";
        assert v1.getAmount(AlchemyAspect.IGNIS) == 5.0;
        assert v1.getAmount(AlchemyAspect.SULFUR) == 3.0;
        assert v1.getX() > 0 : "Ignis & Sulfur have positive X axis";

        // Pure single aspect purity should be 1.0
        AspectVector pureFire = AspectVector.builder()
                .add(AlchemyAspect.IGNIS, 10.0)
                .build();
        assert Math.abs(pureFire.calculatePurity() - 1.0) < 1e-5 : "Pure single element purity must be 1.0";

        // Mixed aspect purity should be less than 1.0
        assert v1.calculatePurity() < 1.0 : "Mixed aspects purity < 1.0";

        // Color blending test (Gamma 2.0 RMS)
        int color = pureFire.calculateBlendedColor();
        assert color == AlchemyAspect.IGNIS.getColor() : "Pure color must match aspect color";

        // Instability test: Conflict between Fire and Water
        AspectVector conflicting = AspectVector.builder()
                .add(AlchemyAspect.IGNIS, 5.0)
                .add(AlchemyAspect.AQUA, 5.0)
                .build();
        double instability = conflicting.calculateInstability();
        assert instability > 0.5 : "Fire + Water must create significant instability";

        // Buffer test: adding Mercurius & Aether should dampen instability
        AspectVector buffered = AspectVector.builder()
                .add(AlchemyAspect.IGNIS, 5.0)
                .add(AlchemyAspect.AQUA, 5.0)
                .add(AlchemyAspect.MERCURIUS, 8.0)
                .add(AlchemyAspect.AETHER, 4.0)
                .build();
        assert buffered.calculateInstability() < instability : "Mercurius and Aether must dampen instability";

        // Annihilation test
        AspectVector resolved = conflicting.resolveAnnihilation(1.0);
        assert resolved.getAmount(AlchemyAspect.IGNIS) < 1e-4 : "Ignis and Aqua should cancel out at 1:1";
        assert resolved.getAmount(AlchemyAspect.AQUA) < 1e-4;

        System.out.println("PASSED!");
    }

    private static void testMapAndNodes() {
        System.out.print("[3/5] Testing AlchemyMap Topology & Nodes... ");

        AlchemyMap map = AlchemyMap.getInstance();
        assert !map.getAllNodes().isEmpty() : "Map must have default nodes";

        // Test Regeneration node at (12.5, 14.0)
        var hitNode = map.findHitNode(12.5, 14.0);
        assert hitNode.isPresent() : "Must find hit node at center";
        assert hitNode.get().getNodeId().getPath().contains("regeneration");

        // Precision score: perfect bullseye = 1.0
        double perfectScore = hitNode.get().getPrecisionScore(12.5, 14.0);
        assert Math.abs(perfectScore - 1.0) < 1e-5 : "Center precision must be 1.0";

        // Precision score at outer rim
        double edgeScore = hitNode.get().getPrecisionScore(12.5 + hitNode.get().getRadius() - 0.1, 14.0);
        assert edgeScore > 0.0 && edgeScore < 0.2 : "Edge precision must be small";

        // Off-target
        assert hitNode.get().getPrecisionScore(100.0, 100.0) == 0.0;

        // Hazard test: Bone Graveyard at (-18.0, -18.0)
        double hazardIntensity = map.getHazardIntensity(-18.0, -18.0);
        assert hazardIntensity > 0.05 : "Hazard zone must have positive intensity at center";

        System.out.println("PASSED!");
    }

    private static void testCauldronPhysics() {
        System.out.print("[4/5] Testing CauldronReactionState Thermodynamic State Machine... ");

        CauldronReactionState cauldron = new CauldronReactionState();
        assert cauldron.getTemperature() == 20.0 : "Initial temp should be 20C";

        // Heat up with Roiling Boil (150C)
        for (int i = 0; i < 100; i++) {
            cauldron.tick(true, HeatLevel.ROILING_BOIL);
        }
        assert cauldron.getTemperature() > 80.0 : "Cauldron should heat up over 80C";

        // Add Fire Ingredient
        AspectVector fireMat = AspectVector.builder()
                .add(AlchemyAspect.IGNIS, 4.0)
                .projectAstralCoordinates()
                .build();
        cauldron.addIngredient(fireMat);
        assert cauldron.getDissolvingBuffer().getTotalAspectMass() > 0 : "Buffer should hold material";

        // Stirring test
        cauldron.stir(true, 0.3); // Clockwise
        assert cauldron.getStirVelocity() > 0 : "Clockwise stir velocity should be positive";

        // Run ticks for dissolution
        for (int i = 0; i < 50; i++) {
            cauldron.tick(true, HeatLevel.ROILING_BOIL);
        }
        assert cauldron.getCurrentVector().getTotalAspectMass() > 0 : "Material should dissolve into solution";
        assert cauldron.getCurrentVector().getX() > 0 : "Coordinates should advance along X axis";

        // Test mishap: Add huge water without buffer at 150C
        AspectVector waterBomb = AspectVector.builder()
                .add(AlchemyAspect.AQUA, 20.0)
                .build();
        cauldron.addIngredient(waterBomb);
        for (int i = 0; i < 200; i++) {
            cauldron.tick(true, HeatLevel.ASTRAL_INFERNO);
            MishapResult mishap = cauldron.checkMishap();
            if (mishap != MishapResult.NONE) {
                assert mishap == MishapResult.ARCANE_EXPLOSION || mishap == MishapResult.TOXIC_BOILOVER
                        : "High instability must trigger mishap";
                break;
            }
        }

        System.out.println("PASSED!");
    }

    private static void testHardeningAndEdgeCases() {
        System.out.print("[5/5] Testing Defensive Hardening & Edge Cases... ");

        // 1. Locale.ROOT safety in byId
        assert AlchemyAspect.byId("IGNIS").isPresent();
        assert AlchemyAspect.byId("ignis").isPresent();
        assert AlchemyAspect.byId("").isEmpty();
        assert AlchemyAspect.byId(null).isEmpty();

        // 2. Binary network serialization roundtrip test
        AspectVector original = AspectVector.builder()
                .astralPos(15.25, -34.75)
                .add(AlchemyAspect.IGNIS, 4.5)
                .add(AlchemyAspect.AETHER, 12.0)
                .add(AlchemyAspect.MERCURIUS, 1.25)
                .build();

        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);
            original.writeToNetwork(dos);
            byte[] bytes = baos.toByteArray();

            // Verify compact size: Float(4) + Float(4) + Short(2) + 3 * Float(4) = 22 bytes!
            assert bytes.length == 22 : "Network payload must be exactly 22 bytes, but got " + bytes.length;

            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(bytes);
            java.io.DataInputStream dis = new java.io.DataInputStream(bais);
            AspectVector restored = AspectVector.readFromNetwork(dis);

            assert Math.abs(restored.getX() - 15.25) < 1e-4;
            assert Math.abs(restored.getY() - (-34.75)) < 1e-4;
            assert Math.abs(restored.getAmount(AlchemyAspect.IGNIS) - 4.5) < 1e-4;
            assert Math.abs(restored.getAmount(AlchemyAspect.AETHER) - 12.0) < 1e-4;
            assert Math.abs(restored.getAmount(AlchemyAspect.MERCURIUS) - 1.25) < 1e-4;
            assert restored.getAmount(AlchemyAspect.AQUA) == 0.0;
        } catch (Exception e) {
            throw new AssertionError("Binary serialization failed", e);
        }

        // 3. Node radius division by zero edge case validation
        try {
            new AlchemyMapNode.Builder(
                    new net.minecraft.resources.ResourceLocation("test", "invalid"),
                    new net.minecraft.resources.ResourceLocation("test", "effect"),
                    0, 0
            ).radii(2.0, 2.0).build();
            assert false : "Must reject perfectRadius >= radius";
        } catch (IllegalArgumentException ignored) {
            // Expected
        }

        // 4. Stirring angle normalization test over 50,000 steps
        CauldronReactionState cauldron = new CauldronReactionState();
        for (int i = 0; i < 50000; i++) {
            cauldron.stir(true, 0.4);
            cauldron.tick(false, HeatLevel.COLD);
        }
        assert cauldron.getClockwiseTurns() > 1000 : "Should accumulate thousands of turns";
        assert Math.abs(cauldron.getStirProgress()) <= net.minecraft.util.Mth.TWO_PI
                : "Stir progress must stay normalized within 2PI";

        // 5. Saturation limit test
        AspectVector heavyMass = AspectVector.builder()
                .add(AlchemyAspect.TERRA, 300.0)
                .build();
        cauldron.addIngredient(heavyMass);
        AspectVector overflowMass = AspectVector.builder()
                .add(AlchemyAspect.TERRA, 50.0)
                .build();
        cauldron.addIngredient(overflowMass);
        assert cauldron.getTurbidity() > 0 : "Excess mass must turn into turbidity waste";

        System.out.println("PASSED!");
    }
}
