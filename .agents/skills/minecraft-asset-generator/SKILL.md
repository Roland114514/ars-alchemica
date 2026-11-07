---
name: minecraft-asset-generator
description: Use this skill when generating or validating Minecraft mod assets, including Blockbench models, textures, blockstate/item JSON models, lang localizations (zh_cn, en_us), sound events, and data generation templates.
---

# Minecraft Asset & DataGen Generator Skill

Building a mega-mod requires hundreds of blockstates, models, textures, localization strings, recipes, and sound definitions. This skill standardizes the asset pipeline and generation workflows.

---

## 1. Directory Structure Conventions

Under `src/main/resources/assets/<modid>/` and `data/<modid>/`:

```text
assets/<modid>/
├── blockstates/          # Custom blockstates (normal, variants, directional)
├── lang/
│   ├── en_us.json        # English localization
│   └── zh_cn.json        # Chinese (Simplified) localization
├── models/
│   ├── block/            # Block models (cube_all, custom geo)
│   └── item/             # Handheld, generated, or parented block models
├── sounds.json           # Sound definitions
└── textures/
    ├── block/            # 16x16 or 32x32 textures
    ├── item/
    ├── entity/           # Mob / Boss skins
    └── gui/              # Custom machine screens and widget sprites
```

---

## 2. Dual-Language Localization Standard

Whenever registering an item, block, creative tab, tooltip, or GUI container, **always generate both English and Simplified Chinese simultaneously**:

```json
// assets/<modid>/lang/en_us.json
{
  "item.<modid>.quantum_core": "Quantum Core",
  "block.<modid>.fusion_reactor": "Fusion Reactor Controller",
  "itemGroup.<modid>.main_tab": "Mega Mod Tech",
  "tooltip.<modid>.quantum_core.desc": "Harnesses subatomic energy fluctuations."
}

// assets/<modid>/lang/zh_cn.json
{
  "item.<modid>.quantum_core": "量子核心",
  "block.<modid>.fusion_reactor": "聚变反应堆控制器",
  "itemGroup.<modid>.main_tab": "超大型模组：科技总览",
  "tooltip.<modid>.quantum_core.desc": "控制亚原子层级的能量涨落。"
}
```

---

## 3. Automated Model & Blockstate Conventions

### Simple Item Model Template
```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "<modid>:item/<item_id>"
  }
}
```

### Handheld Tool Model Template
```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "layer0": "<modid>:item/<item_id>"
  }
}
```

### Directional Horizontal Blockstate Template
```json
{
  "variants": {
    "facing=north": { "model": "<modid>:block/<block_id>" },
    "facing=south": { "model": "<modid>:block/<block_id>", "y": 180 },
    "facing=west":  { "model": "<modid>:block/<block_id>", "y": 270 },
    "facing=east":  { "model": "<modid>:block/<block_id>", "y": 90 }
  }
}
```

---

## 5. Data Assets: Loot Tables, Recipes, and Tags Standards

Whenever any block or item is introduced, corresponding data JSON files must be created immediately under `src/main/resources/data/<modid>/` and `data/minecraft/tags/`:

### 1. Block Loot Table (`data/<modid>/loot_tables/blocks/<block>.json`)
```json
{
  "type": "minecraft:block",
  "pools": [
    {
      "rolls": 1.0,
      "bonus_rolls": 0.0,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "<modid>:<block>"
        }
      ],
      "conditions": [
        {
          "condition": "minecraft:survives_explosion"
        }
      ]
    }
  ]
}
```

### 2. Crafting Recipe (`data/<modid>/recipes/<recipe>.json`)
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": [
    "ABA",
    " C ",
    "AAA"
  ],
  "key": {
    "A": { "item": "minecraft:iron_ingot" },
    "B": { "item": "minecraft:amethyst_shard" },
    "C": { "tag": "minecraft:planks" }
  },
  "result": {
    "item": "<modid>:<item>",
    "count": 1
  }
}
```

### 3. Tool & Mining Tags (`data/minecraft/tags/blocks/mineable/...`)
- `mineable/pickaxe.json`: list stone/metal apparatus blocks.
- `mineable/axe.json`: list wooden apparatus blocks.
- `needs_stone_tool.json` / `needs_iron_tool.json`: mining tier prerequisites.

