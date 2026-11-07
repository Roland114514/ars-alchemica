#!/usr/bin/env python3
"""
Ars Alchemica - 32x32 Dark Medieval Classic Texture Generator
Generates pixel-art textures with dithering, specular highlights, and edge shading.
"""

import os
import math
import random
from PIL import Image, ImageDraw

OUTPUT_DIR = "common/src/main/resources/assets/ars_alchemica/textures/block"
os.makedirs(OUTPUT_DIR, exist_ok=True)

random.seed(42)

def clamp(val, low=0, high=255):
    return max(low, min(high, int(val)))

def lerp_color(c1, c2, t):
    return tuple(clamp(c1[i] + (c2[i] - c1[i]) * t) for i in range(len(c1)))

# -----------------------------------------------------------------------------
# 1. 黑曜石研磨臼贴图: mortar_obsidian.png (32x32)
# -----------------------------------------------------------------------------
def generate_mortar_obsidian():
    img = Image.new("RGBA", (32, 32))
    # Obsidian base tones: very dark violet-black
    base_dark = (18, 15, 26, 255)
    base_mid = (30, 25, 42, 255)
    base_light = (48, 40, 68, 255)
    specular = (95, 75, 135, 255)
    rune_glow = (145, 95, 215, 255)

    for y in range(32):
        for x in range(32):
            # Perlin-like crystalline facets
            n = (math.sin(x * 0.4) * math.cos(y * 0.4) + math.sin((x + y) * 0.3)) * 0.5 + 0.5
            noise = (random.random() - 0.5) * 0.15
            val = clamp((n + noise) * 255)
            
            # Layered crystalline dithering
            if val < 80:
                color = base_dark
            elif val < 170:
                color = base_mid
            elif val < 230:
                color = base_light
            else:
                color = specular

            # Chiseled outer border / bevels
            if x in (0, 31) or y in (0, 31):
                color = lerp_color(color, (10, 8, 15, 255), 0.6)
            elif x in (1, 30) or y in (1, 30):
                color = lerp_color(color, specular, 0.3)

            img.putpixel((x, y), color)

    # Inlaid arcane runic glyphs in corners and center edge
    runes = [
        (4, 4), (5, 4), (4, 5), (6, 5), (5, 6),
        (26, 4), (27, 4), (27, 5), (25, 5), (26, 6),
        (4, 26), (5, 27), (4, 27), (6, 26),
        (27, 26), (26, 27), (25, 26), (27, 27),
        (15, 2), (16, 2), (15, 3), (16, 4),
        (15, 28), (16, 28), (15, 29), (16, 27)
    ]
    for rx, ry in runes:
        if 0 <= rx < 32 and 0 <= ry < 32:
            img.putpixel((rx, ry), rune_glow)

    img.save(os.path.join(OUTPUT_DIR, "mortar_obsidian.png"))
    print("✓ Generated mortar_obsidian.png")

# -----------------------------------------------------------------------------
# 2. 研磨臼磨削内壁: mortar_inner.png (32x32)
# -----------------------------------------------------------------------------
def generate_mortar_inner():
    img = Image.new("RGBA", (32, 32))
    base = (24, 20, 30, 255)
    herb_powder_green = (55, 75, 45, 255)
    herb_powder_gold = (90, 75, 35, 255)

    for y in range(32):
        for x in range(32):
            dx = x - 15.5
            dy = y - 15.5
            dist = math.sqrt(dx * dx + dy * dy) / 16.0
            
            # Deep bowl shading (darker towards center basin)
            shade = 0.5 + 0.5 * min(1.0, dist)
            r = int(base[0] * shade + (random.random() - 0.5) * 15)
            g = int(base[1] * shade + (random.random() - 0.5) * 15)
            b = int(base[2] * shade + (random.random() - 0.5) * 15)

            # Circular abrasive grinding marks
            angle = math.atan2(dy, dx)
            swirl = math.sin(dist * 12.0 + angle * 4.0)
            if swirl > 0.6:
                r = clamp(r + 12)
                g = clamp(g + 10)
                b = clamp(b + 16)

            # Random microscopic alchemical herb residue specks
            rand_val = random.random()
            if rand_val < 0.04 and dist < 0.8:
                col = herb_powder_green
            elif rand_val < 0.07 and dist < 0.7:
                col = herb_powder_gold
            else:
                col = (clamp(r), clamp(g), clamp(b), 255)

            img.putpixel((x, y), col)

    img.save(os.path.join(OUTPUT_DIR, "mortar_inner.png"))
    print("✓ Generated mortar_inner.png")

# -----------------------------------------------------------------------------
# 3. 压榨台木制框架: press_wood.png (32x32)
# -----------------------------------------------------------------------------
def generate_press_wood():
    img = Image.new("RGBA", (32, 32))
    # Aged dark oak / spruce palette
    dark_groove = (36, 23, 14, 255)
    wood_shadow = (48, 32, 19, 255)
    wood_mid = (65, 45, 28, 255)
    wood_light = (82, 58, 37, 255)
    wood_knot = (28, 17, 10, 255)

    for y in range(32):
        # Vertical timber plank lines every 8 pixels
        is_plank_groove = (x_mod := y % 16) in (0, 15)
        for x in range(32):
            # Horizontal wood grain wave
            grain = math.sin(x * 0.15 + math.sin(y * 0.6) * 1.5) * 0.5 + 0.5
            noise = (random.random() - 0.5) * 0.12
            val = grain + noise

            if is_plank_groove:
                color = dark_groove
            elif val < 0.35:
                color = wood_shadow
            elif val < 0.75:
                color = wood_mid
            else:
                color = wood_light

            # Weathered wood knot at (12, 18)
            dist_knot = math.hypot(x - 12, y - 18)
            if dist_knot < 2.5:
                color = wood_knot
            elif dist_knot < 4.0:
                color = wood_shadow

            # Subtle nail / peg holes
            if (x in (4, 27) and y in (4, 27)):
                color = (18, 12, 8, 255)

            img.putpixel((x, y), color)

    img.save(os.path.join(OUTPUT_DIR, "press_wood.png"))
    print("✓ Generated press_wood.png")

# -----------------------------------------------------------------------------
# 4. 锻铁支架与夹板: press_iron.png (32x32)
# -----------------------------------------------------------------------------
def generate_press_iron():
    img = Image.new("RGBA", (32, 32))
    iron_dark = (26, 28, 32, 255)
    iron_mid = (42, 45, 52, 255)
    iron_light = (64, 68, 78, 255)
    iron_specular = (92, 98, 112, 255)
    brass_rivet = (184, 142, 68, 255)
    brass_shadow = (110, 80, 32, 255)

    for y in range(32):
        for x in range(32):
            # Hand-hammered wrought iron surface texture
            hammer = (math.sin(x * 0.8) + math.cos(y * 0.8)) * 0.25 + 0.5
            noise = (random.random() - 0.5) * 0.18
            val = hammer + noise

            if val < 0.38:
                col = iron_dark
            elif val < 0.72:
                col = iron_mid
            elif val < 0.90:
                col = iron_light
            else:
                col = iron_specular

            # Beveled metallic border
            if x in (0, 31) or y in (0, 31):
                col = iron_dark
            elif x in (1, 30) or y in (1, 30):
                col = iron_specular if (x == 1 or y == 1) else iron_dark

            img.putpixel((x, y), col)

    # Hexagonal brass reinforcement rivets at corners
    rivet_centers = [(5, 5), (26, 5), (5, 26), (26, 26), (15, 15), (16, 15)]
    for rx, ry in rivet_centers:
        for ox in (-1, 0, 1):
            for oy in (-1, 0, 1):
                if ox == 0 and oy == 0:
                    img.putpixel((rx, ry), brass_rivet)
                elif ox == -1 and oy == -1:
                    img.putpixel((rx + ox, ry + oy), (220, 185, 105, 255))
                elif ox == 1 and oy == 1:
                    img.putpixel((rx + ox, ry + oy), brass_shadow)

    img.save(os.path.join(OUTPUT_DIR, "press_iron.png"))
    print("✓ Generated press_iron.png")

# -----------------------------------------------------------------------------
# 5. 螺旋丝杆金属材质: press_screw.png (32x32)
# -----------------------------------------------------------------------------
def generate_press_screw():
    img = Image.new("RGBA", (32, 32))
    thread_dark = (22, 24, 28, 255)
    thread_shadow = (38, 42, 48, 255)
    thread_body = (60, 66, 76, 255)
    thread_crest = (115, 125, 142, 255)
    oil_shine = (75, 85, 95, 255)

    for y in range(32):
        # Helical pitch angle (diagonal stripes)
        for x in range(32):
            phase = (y * 2 + x) % 8
            if phase in (0, 1):
                col = thread_crest
            elif phase in (2, 3):
                col = thread_body
            elif phase in (4, 5):
                col = oil_shine
            else:
                col = thread_dark

            # Vertical cylindrical curvature shading
            u = abs(x - 15.5) / 16.0
            shade = 1.0 - u * 0.45
            col = tuple(clamp(col[i] * shade) if i < 3 else col[i] for i in range(4))

            img.putpixel((x, y), col)

    img.save(os.path.join(OUTPUT_DIR, "press_screw.png"))
    print("✓ Generated press_screw.png")

# -----------------------------------------------------------------------------
# 6. 风干架杉木与麻绳铁钩: drying_rack_wood.png (32x32)
# -----------------------------------------------------------------------------
def generate_drying_rack_wood():
    img = Image.new("RGBA", (32, 32))
    wood_bark = (45, 34, 24, 255)
    wood_base = (70, 52, 38, 255)
    wood_hi = (94, 72, 54, 255)
    rope_dark = (90, 72, 45, 255)
    rope_light = (145, 120, 80, 255)
    hook_iron = (40, 44, 50, 255)

    for y in range(32):
        for x in range(32):
            # Horizontal wood crossbeam (middle strip y=8..23)
            if 8 <= y <= 23:
                grain = math.sin(x * 0.2 + (y % 4) * 0.5) * 0.5 + 0.5
                n = (random.random() - 0.5) * 0.15
                v = grain + n
                if y in (8, 23):
                    col = wood_bark
                elif v < 0.4:
                    col = wood_base
                else:
                    col = wood_hi
            else:
                # Top/bottom slats
                col = wood_bark if (y % 4 == 0) else wood_base

            # Hemp cord bindings at x=6..8 and x=23..25
            if x in (6, 7, 8, 23, 24, 25):
                cord_twist = (y + x) % 3
                col = rope_light if cord_twist == 0 else rope_dark

            img.putpixel((x, y), col)

    # Forged hanging hooks along bottom edge (y=24..31)
    hook_positions = [6, 12, 19, 25]
    for hx in hook_positions:
        for hy in range(24, 30):
            img.putpixel((hx, hy), hook_iron)
        img.putpixel((hx + 1, 29), hook_iron)
        img.putpixel((hx + 2, 28), hook_iron)

    img.save(os.path.join(OUTPUT_DIR, "drying_rack_wood.png"))
    print("✓ Generated drying_rack_wood.png")

if __name__ == "__main__":
    generate_mortar_obsidian()
    generate_mortar_inner()
    generate_press_wood()
    generate_press_iron()
    generate_press_screw()
    generate_drying_rack_wood()
    print("All 6 apparatus textures successfully generated!")
