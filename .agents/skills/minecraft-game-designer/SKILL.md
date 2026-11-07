---
name: minecraft-game-designer
description: Use this skill when planning game design, tech trees, progression tiers, game balance, multi-block machinery concepts, dimension loops, or writing Game Design Documents (GDD) for a Minecraft mega-mod.
---

# Minecraft Game Designer Skill

A mega-mod is not just a collection of random items; it is a complete, cohesive gameplay ecosystem. This skill provides the blueprint for game systems design, tech tree gating, mathematical balance, and Game Design Document (GDD) authoring.

---

## 1. The Core Progression Loop (Macro Architecture)

Design progression as a cycle of: **Exploration -> Harvest -> Bottleneck -> Automation -> Mastery -> New Frontier**.

Divide the player journey into distinct **Progression Tiers**:

| Tier | Phase Name | Primary Gating Mechanics | Core Unlocks & Player Fantasy |
| :--- | :--- | :--- | :--- |
| **Tier 0** | **Genesis / Primitive** | Basic material processing, stone/flint constraints | Primitive shelter, basic survival tools, fire/heat mechanics. |
| **Tier 1** | **Foundry & Mechanical** | Metallurgy (Bronze/Steel), kinetic power, simple fuels | Early automation, manual ore doubling, waterwheel/steam power. |
| **Tier 2** | **Industrial / Aetheric** | Electricity (RF/FE) or Mana, specialized multi-blocks | Circuit fabrication, automated quarries, chemical reactors. |
| **Tier 3** | **Quantum & Dimensional** | Boss essence, dimensional portal keys, particle fusion | Flight, custom pocket dimensions, spatial storage, matter warping. |
| **Tier 4** | **Cosmic / Endgame** | Singularity matrix, mega-multiblock structures | Creative-tier abilities, absolute resource mastery, world reshaping. |

---

## 2. Anti-Power-Creep & Economic Balance Framework

### Rule 1: The Bottleneck Resource Principle
Never allow a single technology path to scale indefinitely in isolation. Every tier must require a cross-domain "choke point" resource (e.g., a special flux alloy requiring both deep mining, chemical refining, and a rare biological catalyst).

### Rule 2: Utility Scaling Over Number Inflation
- Bad balance: Armor with 1,000,000 durability and swords dealing 500 damage (makes all vanilla mobs and bosses obsolete).
- Good balance: Equipment granting horizontal utility (dash mechanics, magnet aura, environmental immunity, custom enchant slots, modular attachments).

### Rule 3: Meaningful Automation (No Free Handouts)
Automation should solve logistics challenges, not eliminate game loops:
- Automated miners should produce raw slurry/byproducts that require waste management or disposal.
- High-efficiency machines should generate heat, pollution, or wear, encouraging clever engineering solutions.

---

## 3. Tech Tree Topology

Design tech trees using a **directed graph (web layout)** rather than a strictly linear checklist:
1. **Hub Nodes (Mandatory Milestones)**: Foundational technologies required to unlock the next Tier (e.g., *Blast Furnace Construction*).
2. **Branching Nodes (Player Specialization)**: Divergent paths allowing players to choose their focus (e.g., choosing *Biochemical Engineering* vs. *High-Voltage Electronics*).
3. **Sink Nodes (Prestige & Mastery)**: Expensive, late-game resource sinks providing cosmetic, convenience, or bragging-right rewards.

---

## 4. GDD Node Template

When authoring specifications for new features or multiblock machines, format them with this standard template:

```markdown
### [Feature ID]: Feature Name
- **Tier**: [Tier 1 / 2 / 3 / 4]
- **Category**: [Machinery / Dimension / Entity / Tool / Magic]
- **Prerequisites**: [Required prior tech or advancements]
- **Player Fantasy**: [1 sentence describing what the player feels achieving this]

#### Gameplay Mechanics & Loop
1. Input requirements (Energy, Items, Fluids, Catalyst).
2. Processing time, byproduct chance, and efficiency curve.
3. Output and downstream utility.

#### Multiblock / Structure Specs (if applicable)
- Dimensions (e.g., 3x3x4).
- Casing requirements and controller block placement.
- Environmental hazard (radiation, heat, explosion risk on failure).

#### Balance & Exploit Prevention
- How does this prevent AFK infinite duplication?
- What is the failure state if overfed or improperly configured?
```
