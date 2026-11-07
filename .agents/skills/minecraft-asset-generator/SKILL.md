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

## 4. Blockbench & GeckoLib / AzureLib Guidelines

For custom animated entities, bosses, or animated multiblock machines:
1. Use **Blockbench** with the GeckoLib / AzureLib plugin.
2. Export as:
   - `<name>.geo.json` (Bone hierarchy and pivot points)
   - `<name>.animation.json` (Keyframe animations: idle, walk, attack, active)
   - `<name>.png` (Texture map)
3. Ensure root bone is centered at `[0, 0, 0]` to avoid desynced hitboxes.
