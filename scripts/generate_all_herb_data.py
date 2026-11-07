#!/usr/bin/env python3
"""
Ars Alchemica - Worldgen & Data Pack Generator for 7 Canon Herbs
Generates:
1. Blockstates (assets/ars_alchemica/blockstates/)
2. Block Models (assets/ars_alchemica/models/block/)
3. Item Models (assets/ars_alchemica/models/item/)
4. Loot Tables (data/ars_alchemica/loot_tables/blocks/)
5. Tags (data/minecraft/tags/blocks/, data/ars_alchemica/tags/items/)
6. Worldgen Biome Tags (data/ars_alchemica/tags/worldgen/biome/has_herb/)
7. Configured Features (data/ars_alchemica/worldgen/configured_feature/)
8. Placed Features (data/ars_alchemica/worldgen/placed_feature/)
9. Forge Biome Modifiers (data/ars_alchemica/forge/biome_modifier/)
10. Localization updates (zh_cn.json, en_us.json)
"""

import json
import os

BASE_COMMON = "common/src/main/resources"
ASSETS = f"{BASE_COMMON}/assets/ars_alchemica"
DATA = f"{BASE_COMMON}/data/ars_alchemica"
MINECRAFT_DATA = f"{BASE_COMMON}/data/minecraft"

HERBS = [
    {
        "id": "cinder_thistle",
        "zh_name": "灰烬血蓟",
        "en_name": "Cinder Thistle",
        "desc_zh": "汲取熔岩与烬土生长的赤红荆棘，茎叶覆满烈焰火刺。",
        "desc_en": "A crimson briar thriving upon cinder soils and volcanic beds, its thorns seething with latent embers.",
        "biomes": [
            "minecraft:badlands",
            "minecraft:eroded_badlands",
            "minecraft:wooded_badlands",
            "minecraft:desert",
            "minecraft:crimson_forest",
            "minecraft:nether_wastes",
            "minecraft:basalt_deltas"
        ],
        "height_type": "surface",
        "rarity": 10,
        "props": {"age": "3"}
    },
    {
        "id": "frostvein_orchid",
        "zh_name": "凝霜幽兰",
        "en_name": "Frostvein Orchid",
        "desc_zh": "凝结万年玄冰灵气的极地幽兰，花瓣薄如冰晶，畏惧高温。",
        "desc_en": "A rare glacial orchid crystallizing frozen mana, its gossamer ice petals fragile under warmth.",
        "biomes": [
            "minecraft:frozen_peaks",
            "minecraft:jagged_peaks",
            "minecraft:snowy_slopes",
            "minecraft:grove",
            "minecraft:snowy_taiga",
            "minecraft:snowy_plains",
            "minecraft:ice_spikes"
        ],
        "height_type": "surface",
        "rarity": 12,
        "props": {"age": "3"}
    },
    {
        "id": "storm_marigold",
        "zh_name": "避雷金盏花",
        "en_name": "Storm Marigold",
        "desc_zh": "傲立于高山危崖的坚韧金盏，雷暴天引雷充能，绽放耀眼电弧。",
        "desc_en": "A resilient alpine flower clinging to windswept peaks, channeling lightning bolts through jagged petals.",
        "biomes": [
            "minecraft:jagged_peaks",
            "minecraft:stony_peaks",
            "minecraft:windswept_hills",
            "minecraft:windswept_gravelly_hills",
            "minecraft:windswept_forest"
        ],
        "height_type": "high_altitude",
        "rarity": 14,
        "props": {"age": "3", "charged": "false"}
    },
    {
        "id": "abyssal_lumina_mint",
        "zh_name": "深渊幽光薄荷",
        "en_name": "Abyssal Lumina Mint",
        "desc_zh": "隐匿于幽匿深渊的冷光草药，厌恶强光，在漆黑中散发幽蓝光泽。",
        "desc_en": "A subterranean bioluminescent mint dwelling in sculk depths, withering swiftly under solar glare.",
        "biomes": [
            "minecraft:deep_dark",
            "minecraft:lush_caves",
            "minecraft:dripstone_caves"
        ],
        "height_type": "underground",
        "rarity": 8,
        "props": {"age": "3"}
    },
    {
        "id": "screaming_mandragora",
        "zh_name": "哀嚎曼陀罗",
        "en_name": "Screaming Mandragora",
        "desc_zh": "根茎具人形怪诞面庞的魔性植物，徒手拔起会引发生理失调的刺耳尖叫。",
        "desc_en": "A grotesque tuber whose contorted visage unleashes a deafening acoustic shriek when roughly uprooted.",
        "biomes": [
            "minecraft:plains",
            "minecraft:sunflower_plains",
            "minecraft:meadow",
            "minecraft:forest",
            "minecraft:flower_forest"
        ],
        "height_type": "surface",
        "rarity": 18,
        "props": {"age": "3"}
    },
    {
        "id": "moonveil_belladonna",
        "zh_name": "月影颠茄",
        "en_name": "Moonveil Belladonna",
        "desc_zh": "暗夜林下盛开的剧毒幽花，于满月与新月之夜受月华滋润绽放。",
        "desc_en": "A midnight-blooming shade flora bearing glistening venomous berries, blooming in lunar syzygy.",
        "biomes": [
            "minecraft:dark_forest",
            "minecraft:swamp",
            "minecraft:mangrove_swamp",
            "minecraft:old_growth_birch_forest"
        ],
        "height_type": "surface",
        "rarity": 14,
        "props": {"age": "3", "blooming": "false"}
    },
    {
        "id": "carrion_bogmoss",
        "zh_name": "腐水食腐苔",
        "en_name": "Carrion Bogmoss",
        "desc_zh": "滋生于泥沼死水之上的吸毒藓类，能吸附体内沉积的烈性炼金毒素。",
        "desc_en": "A spongy saprophytic moss spreading across stagnant marshes, renowned for absorbing systemic toxicity.",
        "biomes": [
            "minecraft:swamp",
            "minecraft:mangrove_swamp"
        ],
        "height_type": "surface",
        "rarity": 8,
        "props": {"age": "3"}
    }
]

def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
    print(f"  Wrote: {path}")

# =============================================================================
# 1. Blockstates & Models
# =============================================================================
print("\n--- Generating Blockstates and Models ---")
for h in HERBS:
    hid = h["id"]
    # Blockstate using multipart (matches any property state safely)
    bs_path = f"{ASSETS}/blockstates/{hid}.json"
    bs_data = {
        "multipart": [
            {
                "apply": {
                    "model": f"ars_alchemica:block/{hid}"
                }
            }
        ]
    }
    write_json(bs_path, bs_data)

    # Block Model (cross model with cutout render type)
    bm_path = f"{ASSETS}/models/block/{hid}.json"
    bm_data = {
        "parent": "minecraft:block/cross",
        "render_type": "minecraft:cutout",
        "textures": {
            "cross": f"ars_alchemica:block/{hid}"
        }
    }
    write_json(bm_path, bm_data)

    # Item Model (generated item texture)
    im_path = f"{ASSETS}/models/item/{hid}.json"
    im_data = {
        "parent": "minecraft:item/generated",
        "textures": {
            "layer0": f"ars_alchemica:item/{hid}"
        }
    }
    write_json(im_path, im_data)

# Processed herb item model
write_json(f"{ASSETS}/models/item/processed_herb.json", {
    "parent": "minecraft:item/generated",
    "textures": {
        "layer0": "ars_alchemica:item/processed_herb"
    }
})

# =============================================================================
# 2. Block Loot Tables
# =============================================================================
print("\n--- Generating Loot Tables ---")
for h in HERBS:
    hid = h["id"]
    lt_path = f"{DATA}/loot_tables/blocks/{hid}.json"
    lt_data = {
        "type": "minecraft:block",
        "pools": [
            {
                "rolls": 1.0,
                "bonus_rolls": 0.0,
                "entries": [
                    {
                        "type": "minecraft:item",
                        "name": f"ars_alchemica:{hid}"
                    }
                ],
                "conditions": [
                    {
                        "condition": "minecraft:survives_explosion"
                    }
                ]
            },
            {
                "rolls": 1.0,
                "bonus_rolls": 0.0,
                "entries": [
                    {
                        "type": "minecraft:item",
                        "name": f"ars_alchemica:{hid}"
                    }
                ],
                "conditions": [
                    {
                        "condition": "minecraft:survives_explosion"
                    },
                    {
                        "condition": "minecraft:block_state_property",
                        "block": f"ars_alchemica:{hid}",
                        "properties": {
                            "age": "3"
                        }
                    }
                ]
            }
        ]
    }
    write_json(lt_path, lt_data)

# =============================================================================
# 3. Tags
# =============================================================================
print("\n--- Generating Tags ---")
all_herb_ids = [f"ars_alchemica:{h['id']}" for h in HERBS]

# minecraft:flowers (blocks)
flower_blocks_tag = f"{MINECRAFT_DATA}/tags/blocks/flowers.json"
write_json(flower_blocks_tag, {
    "replace": False,
    "values": all_herb_ids
})

# minecraft:flowers (items)
flower_items_tag = f"{MINECRAFT_DATA}/tags/items/flowers.json"
write_json(flower_items_tag, {
    "replace": False,
    "values": all_herb_ids
})

# ars_alchemica:herbs (items)
herb_items_tag = f"{DATA}/tags/items/herbs.json"
write_json(herb_items_tag, {
    "replace": False,
    "values": all_herb_ids + ["ars_alchemica:processed_herb"]
})

# =============================================================================
# 4. Worldgen Biome Tags
# =============================================================================
print("\n--- Generating Worldgen Biome Tags ---")
for h in HERBS:
    hid = h["id"]
    tag_path = f"{DATA}/tags/worldgen/biome/has_herb/{hid}.json"
    write_json(tag_path, {
        "replace": False,
        "values": h["biomes"]
    })

# =============================================================================
# 5. Configured Features
# =============================================================================
print("\n--- Generating Configured Features ---")
for h in HERBS:
    hid = h["id"]
    cf_path = f"{DATA}/worldgen/configured_feature/patch_herb_{hid}.json"
    cf_data = {
        "type": "minecraft:random_patch",
        "config": {
            "tries": 32,
            "xz_spread": 6,
            "y_spread": 2,
            "feature": {
                "feature": {
                    "type": "minecraft:simple_block",
                    "config": {
                        "to_place": {
                            "type": "minecraft:simple_state_provider",
                            "state": {
                                "Name": f"ars_alchemica:{hid}",
                                "Properties": h["props"]
                            }
                        }
                    }
                },
                "placement": [
                    {
                        "type": "minecraft:block_predicate_filter",
                        "predicate": {
                            "type": "minecraft:matching_blocks",
                            "blocks": "minecraft:air"
                        }
                    }
                ]
            }
        }
    }
    write_json(cf_path, cf_data)

# =============================================================================
# 6. Placed Features
# =============================================================================
print("\n--- Generating Placed Features ---")
for h in HERBS:
    hid = h["id"]
    pf_path = f"{DATA}/worldgen/placed_feature/patch_herb_{hid}.json"
    
    placement = [
        {
            "type": "minecraft:rarity_filter",
            "chance": h["rarity"]
        },
        {
            "type": "minecraft:in_square"
        }
    ]
    
    if h["height_type"] == "surface":
        placement.append({
            "type": "minecraft:heightmap",
            "heightmap": "MOTION_BLOCKING_NO_LEAVES"
        })
    elif h["height_type"] == "high_altitude":
        placement.append({
            "type": "minecraft:heightmap",
            "heightmap": "MOTION_BLOCKING_NO_LEAVES"
        })
        placement.append({
            "type": "minecraft:height_range",
            "height": {
                "type": "minecraft:uniform",
                "min_inclusive": {"absolute": 95},
                "max_inclusive": {"absolute": 256}
            }
        })
    elif h["height_type"] == "underground":
        placement.append({
            "type": "minecraft:height_range",
            "height": {
                "type": "minecraft:uniform",
                "min_inclusive": {"absolute": -58},
                "max_inclusive": {"absolute": 30}
            }
        })
    
    placement.append({
        "type": "minecraft:biome"
    })
    
    pf_data = {
        "feature": f"ars_alchemica:patch_herb_{hid}",
        "placement": placement
    }
    write_json(pf_path, pf_data)

# =============================================================================
# 7. Forge Biome Modifiers
# =============================================================================
print("\n--- Generating Forge Biome Modifiers ---")
for h in HERBS:
    hid = h["id"]
    bm_path = f"{DATA}/forge/biome_modifier/add_herb_{hid}.json"
    step = "underground_decoration" if h["height_type"] == "underground" else "vegetal_decoration"
    bm_data = {
        "type": "forge:add_features",
        "biomes": f"#ars_alchemica:has_herb/{hid}",
        "features": f"ars_alchemica:patch_herb_{hid}",
        "step": step
    }
    write_json(bm_path, bm_data)

# =============================================================================
# 8. Localization Synchronization (zh_cn & en_us)
# =============================================================================
print("\n--- Synchronizing Localization ---")
zh_path = f"{ASSETS}/lang/zh_cn.json"
en_path = f"{ASSETS}/lang/en_us.json"

with open(zh_path, "r", encoding="utf-8") as f:
    zh_dict = json.load(f)

with open(en_path, "r", encoding="utf-8") as f:
    en_dict = json.load(f)

for h in HERBS:
    hid = h["id"]
    zh_dict[f"block.ars_alchemica.{hid}"] = h["zh_name"]
    zh_dict[f"item.ars_alchemica.{hid}"] = h["zh_name"]
    zh_dict[f"tooltip.ars_alchemica.herb.{hid}.desc"] = h["desc_zh"]

    en_dict[f"block.ars_alchemica.{hid}"] = h["en_name"]
    en_dict[f"item.ars_alchemica.{hid}"] = h["en_name"]
    en_dict[f"tooltip.ars_alchemica.herb.{hid}.desc"] = h["desc_en"]

zh_dict["item.ars_alchemica.processed_herb"] = "炼金草药提取物"
en_dict["item.ars_alchemica.processed_herb"] = "Processed Herb Extract"

with open(zh_path, "w", encoding="utf-8") as f:
    json.dump(zh_dict, f, indent=2, ensure_ascii=False)
print(f"  Updated: {zh_path}")

with open(en_path, "w", encoding="utf-8") as f:
    json.dump(en_dict, f, indent=2, ensure_ascii=False)
print(f"  Updated: {en_path}")

print("\nAll data packs, assets, tags, worldgen files, and translations successfully generated!")
