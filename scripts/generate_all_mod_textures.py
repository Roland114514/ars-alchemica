#!/usr/bin/env python3
"""
Ars Alchemica - High-Fidelity 32x32 Dark Medieval Alchemical Pixel Art Generator
Implements:
- Hue-shifted color ramps (cool indigo shadows, warm gold/cyan highlights)
- Bayer 4x4 ordered dithering
- Double outlining & selective outlines (sel-out)
- Inner bevel highlights & ambient occlusion
- Fluid meniscus, internal bubbles & curved specular glass highlights
- Intricate plant anatomy for 7 canon alchemical herbs
"""

import os
import math
import random
from PIL import Image, ImageDraw

BLOCK_DIR = "common/src/main/resources/assets/ars_alchemica/textures/block"
ITEM_DIR = "common/src/main/resources/assets/ars_alchemica/textures/item"
os.makedirs(BLOCK_DIR, exist_ok=True)
os.makedirs(ITEM_DIR, exist_ok=True)

random.seed(42)

BAYER_4X4 = [
    [ 0,  8,  2, 10],
    [12,  4, 14,  6],
    [ 3, 11,  1,  9],
    [15,  7, 13,  5]
]

def clamp(val, low=0, high=255):
    return max(low, min(high, int(round(val))))

def lerp_color(c1, c2, t):
    t = max(0.0, min(1.0, t))
    r = clamp(c1[0] + (c2[0] - c1[0]) * t)
    g = clamp(c1[1] + (c2[1] - c1[1]) * t)
    b = clamp(c1[2] + (c2[2] - c1[2]) * t)
    a = clamp(c1[3] + (c2[3] - c1[3]) * t) if len(c1) > 3 and len(c2) > 3 else 255
    return (r, g, b, a)

def dither_val(x, y):
    return (BAYER_4X4[y % 4][x % 4] / 16.0) - 0.5

def hue_shift_ramp(base_rgb, light_level, x, y):
    """
    Shifts hue towards cold indigo in shadow (light_level < 0.5)
    and warm gold/cyan in highlight (light_level > 0.5).
    """
    d = dither_val(x, y) * 0.12
    lvl = max(0.0, min(1.0, light_level + d))
    
    r, g, b = base_rgb[:3]
    if lvl < 0.5:
        # Shadow: blend with cold dark indigo (18, 14, 28)
        t = (0.5 - lvl) * 1.5
        shadow_tint = (18, 14, 28)
        r = clamp(r * (lvl * 1.8) * (1 - t) + shadow_tint[0] * t)
        g = clamp(g * (lvl * 1.8) * (1 - t) + shadow_tint[1] * t)
        b = clamp(b * (lvl * 1.9) * (1 - t) + shadow_tint[2] * t)
    else:
        # Highlight: blend with pale warm gold/ivory (255, 245, 215)
        t = (lvl - 0.5) * 1.5
        hi_tint = (255, 245, 215)
        r = clamp(r * (1 - t) + hi_tint[0] * t)
        g = clamp(g * (1 - t) + hi_tint[1] * t)
        b = clamp(b * (1 - t) + hi_tint[2] * t)
    return (r, g, b, base_rgb[3] if len(base_rgb) > 3 else 255)

OUTLINE = (22, 18, 28, 255)
OUTLINE_SOFT = (35, 28, 44, 220)

# =============================================================================
# 10 CORE ITEMS
# =============================================================================

def gen_stirring_rod():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    # Diagonal staff from (5, 27) to (25, 7)
    for i in range(23):
        x = 5 + i
        y = 27 - i
        # Dark obsidian silver shaft (3px thick)
        for ox in (-1, 0, 1):
            for oy in (-1, 0, 1):
                if abs(ox) + abs(oy) <= 1:
                    # Brass spiral rings at intervals
                    if i in (5, 6, 11, 12, 17, 18):
                        c = (205, 160, 60, 255) if ox == -1 else (145, 100, 30, 255)
                    else:
                        lvl = 0.7 if (ox == -1 or oy == -1) else 0.3
                        c = hue_shift_ramp((60, 65, 80), lvl, x, y)
                    img.putpixel((x + ox, y + oy), c)
    # Bottom pommel: brass crescent counterweight
    for px, py in [(4, 28), (5, 28), (5, 29), (6, 29), (3, 27), (4, 26)]:
        img.putpixel((px, py), (175, 130, 45, 255))
    # Tip crystal: Octahedral Aether gem at (26, 6)
    gem_pts = [(24, 7), (25, 6), (26, 5), (27, 6), (26, 7), (25, 8), (26, 6)]
    for gx in range(23, 29):
        for gy in range(4, 9):
            dist = math.hypot(gx - 26, gy - 6)
            if dist <= 2.5:
                # Ethereal cyan/purple glow
                lvl = 1.0 - (dist / 2.5)
                c = hue_shift_ramp((130, 70, 230), lvl, gx, gy)
                img.putpixel((gx, gy), c)
    # Bright glint
    img.putpixel((26, 5), (240, 250, 255, 255))
    img.putpixel((25, 6), (210, 240, 255, 255))
    return img

def gen_alchemical_journal():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    # Isometric/angled heavy tome: (5, 4) to (27, 27)
    # Spine on left: x=5..7, y=6..26
    for y in range(6, 27):
        # Spine leather with raised horizontal ribs
        rib = 1 if (y in (7, 12, 17, 22, 26)) else 0
        for x in range(5, 8):
            base = (70 + rib * 30, 25 + rib * 10, 20 + rib * 10)
            img.putpixel((x, y), hue_shift_ramp(base, 0.4 if x == 5 else 0.6, x, y))
    
    # Cover face: x=8..26, y=5..26
    for y in range(5, 27):
        for x in range(8, 27):
            # Outer leather edge
            if x == 26 or y == 5 or y == 26:
                img.putpixel((x, y), OUTLINE)
                continue
            # Brass corner protectors
            if (x <= 11 and y <= 8) or (x >= 23 and y <= 8) or (x <= 11 and y >= 23) or (x >= 23 and y >= 23):
                metal_lvl = 0.8 if (x + y) % 3 == 0 else 0.5
                img.putpixel((x, y), hue_shift_ramp((190, 145, 45), metal_lvl, x, y))
                continue
            # Leather field with subtle Bayer grain
            lvl = 0.5 + dither_val(x, y) * 0.15
            img.putpixel((x, y), hue_shift_ramp((95, 35, 25), lvl, x, y))
    
    # Arcane seal carved in center (17, 15)
    cx, cy = 17, 15
    for y in range(cy - 5, cy + 6):
        for x in range(cx - 5, cx + 6):
            dist = math.hypot(x - cx, y - cy)
            if 3.8 <= dist <= 4.8 or dist <= 1.5:
                img.putpixel((x, y), (215, 175, 70, 255))
    # Glowing violet gem at center
    img.putpixel((cx, cy), (230, 160, 255, 255))
    img.putpixel((cx - 1, cy), (160, 70, 230, 255))
    img.putpixel((cx + 1, cy), (130, 50, 200, 255))
    return img

def gen_recipe_scroll():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    # Diagonal unrolled parchment: from top-left (7, 6) to bottom-right (25, 24)
    for y in range(6, 25):
        for x in range(7, 26):
            # Main diagonal band
            diag = (x - 7) + (y - 6)
            perp = (x - 7) - (y - 6)
            if -8 <= perp <= 8:
                # Spindles at upper-left and lower-right
                if diag <= 3 or diag >= 33:
                    img.putpixel((x, y), (55, 35, 20, 255)) # Dark ebony wood
                elif diag in (4, 32):
                    img.putpixel((x, y), (180, 140, 50, 255)) # Brass finial
                else:
                    # Parchment body with curled shading
                    edge_shading = 0.2 if abs(perp) >= 7 else 0.0
                    lvl = 0.7 - edge_shading + dither_val(x, y) * 0.12
                    c = hue_shift_ramp((225, 205, 155), lvl, x, y)
                    img.putpixel((x, y), c)
    
    # Runic cursive lines on parchment
    for rx, ry in [(12, 10), (14, 10), (15, 10), (11, 12), (12, 12), (14, 12), (18, 16), (20, 16), (19, 18), (21, 18)]:
        img.putpixel((rx, ry), (90, 70, 50, 220))
    
    # Red wax seal and scarlet ribbon in middle
    for sx in range(14, 19):
        for sy in range(14, 19):
            dist = math.hypot(sx - 16, sy - 16)
            if dist <= 2.2:
                wax = (210, 35, 35, 255) if (sx == 15 and sy == 15) else (145, 20, 20, 255)
                img.putpixel((sx, sy), wax)
    # Ribbon tails
    for rx, ry in [(16, 19), (16, 20), (17, 21), (15, 19), (14, 20), (14, 21)]:
        img.putpixel((rx, ry), (170, 25, 25, 255))
    return img

def gen_ancient_parchment():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    # Ragged aged vellum with burnt jagged edges
    for y in range(5, 27):
        for x in range(6, 26):
            # Perlin-like erosion mask
            edge_noise = (math.sin(x * 1.7) * math.cos(y * 1.5) + math.sin(x * y * 0.2)) * 1.5
            is_edge = (x in (6, 7, 24, 25) or y in (5, 6, 25, 26))
            if is_edge and edge_noise < -0.3:
                continue
            
            # Burnt rim
            dist_to_border = min(x - 6, 25 - x, y - 5, 26 - y)
            if dist_to_border <= 1:
                img.putpixel((x, y), (65, 45, 30, 255))
                continue
            elif dist_to_border == 2:
                img.putpixel((x, y), (135, 95, 55, 255))
                continue
            
            # Vellum fiber body + tea stain
            stain = math.hypot(x - 13, y - 17) / 12.0
            lvl = 0.65 - stain * 0.25 + dither_val(x, y) * 0.15
            img.putpixel((x, y), hue_shift_ramp((215, 185, 130), lvl, x, y))
            
    # Glowing teal runic inscriptions & celestial circle
    for y in (11, 15, 19, 23):
        for x in range(10, 22):
            if (x + y * 2) % 3 == 0 and x % 2 == 0:
                img.putpixel((x, y), (60, 210, 230, 240))
    # Arcane circle drawing
    for a in range(16):
        ang = a * (math.pi / 8)
        cx = int(16 + math.cos(ang) * 4)
        cy = int(15 + math.sin(ang) * 4)
        img.putpixel((cx, cy), (40, 185, 220, 220))
    return img

def gen_scholia_fragment():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    # Two overlapping torn manuscript scraps
    # Base scrap:
    for y in range(8, 24):
        for x in range(8, 24):
            if (x - 8) + (y - 8) >= 5 and (23 - x) + (23 - y) >= 3:
                lvl = 0.6 + dither_val(x, y) * 0.14
                img.putpixel((x, y), hue_shift_ramp((205, 175, 120), lvl, x, y))
    # Scorched border
    for y in range(8, 24):
        for x in range(8, 24):
            if img.getpixel((x, y))[3] > 0:
                # check neighbors
                has_empty = any(img.getpixel((x+dx, y+dy))[3] == 0 for dx, dy in [(-1,0),(1,0),(0,-1),(0,1)] if 0<=x+dx<32 and 0<=y+dy<32)
                if has_empty:
                    img.putpixel((x, y), (70, 40, 25, 255))
    # Gold illuminated drop cap letter 'A' at (11, 11)
    for gx, gy in [(11, 11), (11, 12), (11, 13), (12, 11), (12, 13), (13, 11), (13, 12), (13, 13)]:
        img.putpixel((gx, gy), (225, 180, 50, 255))
    # Vermilion annotation seal at (19, 18)
    for sx in (19, 20):
        for sy in (18, 19):
            img.putpixel((sx, sy), (195, 40, 30, 255))
    return img

def gen_alchemical_slag():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    # Porous jagged metallic crust chunk
    for y in range(6, 26):
        for x in range(6, 26):
            dist = math.hypot(x - 16, y - 16)
            angle = math.atan2(y - 16, x - 16)
            # Irregular crystalline outline
            radius = 8.5 + math.sin(angle * 5) * 1.8 + math.cos(angle * 3) * 1.2
            if dist <= radius:
                # Pitted porous craters
                is_crater = any(math.hypot(x - cx, y - cy) <= 1.6 for cx, cy in [(13, 13), (19, 14), (15, 20), (12, 18)])
                if is_crater:
                    img.putpixel((x, y), (20, 16, 24, 255))
                else:
                    # Iridescent rainbow temper patina (titanium/copper oxides)
                    t = (x + y * 1.3) / 40.0
                    patina_r = clamp(80 + 75 * math.sin(t * 7.0))
                    patina_g = clamp(70 + 70 * math.sin(t * 7.0 + 2.1))
                    patina_b = clamp(110 + 90 * math.sin(t * 7.0 + 4.2))
                    lvl = 0.5 + dither_val(x, y) * 0.15
                    c = hue_shift_ramp((patina_r, patina_g, patina_b), lvl, x, y)
                    img.putpixel((x, y), c)
    # Outline edge
    for y in range(5, 27):
        for x in range(5, 27):
            if img.getpixel((x, y))[3] > 0:
                has_empty = any(img.getpixel((x+dx, y+dy))[3] == 0 for dx, dy in [(-1,0),(1,0),(0,-1),(0,1)] if 0<=x+dx<32 and 0<=y+dy<32)
                if has_empty:
                    img.putpixel((x, y), OUTLINE)
    return img

def gen_alchemical_sludge():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    # Viscous puddle with dripping surface tension and glowing toxic bubbles
    for y in range(12, 27):
        for x in range(5, 27):
            dist_x = abs(x - 16)
            max_y = 25 - (dist_x * 0.4)
            min_y = 17 - math.sin(x * 0.8) * 3.5
            if min_y <= y <= max_y:
                # Dark obsidian-green sludge
                lvl = 0.35 + dither_val(x, y) * 0.1
                img.putpixel((x, y), hue_shift_ramp((20, 45, 28), lvl, x, y))
    
    # 3 Glowing bubbling fluid domes
    for bx, by, br in [(12, 17, 3.2), (18, 15, 2.5), (22, 19, 2.0)]:
        for y in range(int(by - br), int(by + br + 1)):
            for x in range(int(bx - br), int(bx + br + 1)):
                d = math.hypot(x - bx, y - by)
                if d <= br:
                    # Acid green dome with bright highlight
                    if d <= 1.0 and (x <= bx and y <= by):
                        img.putpixel((x, y), (200, 255, 140, 255)) # Specular gleam
                    elif d <= br * 0.7:
                        img.putpixel((x, y), (70, 220, 60, 255))
                    else:
                        img.putpixel((x, y), (25, 95, 40, 255))
    # Dripping threads at bottom
    for dx, dy in [(9, 26), (15, 27), (20, 26)]:
        img.putpixel((dx, dy), (45, 160, 55, 240))
    return img

def gen_elixir():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    # Gothic octagonal cut-crystal flask
    # Silver filigree neck: x=14..17, y=6..11
    # Stopper: x=14..17, y=3..5
    for y in range(3, 6):
        for x in range(14, 18):
            img.putpixel((x, y), (170, 180, 205, 255)) # Silver ornate stopper
    img.putpixel((15, 3), (240, 245, 255, 255)) # Stopper highlight
    
    # Neck glass
    for y in range(6, 12):
        for x in range(14, 18):
            if x in (14, 17):
                img.putpixel((x, y), (140, 170, 200, 220)) # Outer glass edge
            else:
                img.putpixel((x, y), (200, 230, 255, 120))
    
    # Body: Bulbous multifaceted crystal vessel, y=12..27, x=8..23
    for y in range(12, 28):
        for x in range(8, 24):
            dist = math.hypot(x - 15.5, y - 19.5)
            if dist <= 7.8:
                # Glass wall vs liquid
                is_wall = (dist >= 6.8)
                if is_wall:
                    img.putpixel((x, y), (180, 210, 240, 220))
                else:
                    # Liquid inside (fills y=15..26)
                    if y >= 16:
                        # Liquid meniscus curve at y=16
                        if y == 16:
                            img.putpixel((x, y), (240, 180, 255, 255)) # Meniscus rim
                        else:
                            # Glowing ether-amethyst potion with depth
                            depth_t = (y - 16) / 10.0
                            pot_c = lerp_color((175, 80, 255, 240), (80, 25, 170, 255), depth_t)
                            img.putpixel((x, y), pot_c)
                    else:
                        # Air cavity inside flask
                        img.putpixel((x, y), (210, 235, 255, 80))
    
    # Internal micro-bubbles in liquid
    for bx, by in [(13, 22), (18, 24), (16, 20), (14, 18)]:
        img.putpixel((bx, by), (255, 220, 255, 255))
    
    # Vertical curved glass specular reflection streak
    for y in range(14, 25):
        img.putpixel((11, y), (255, 255, 255, 220))
    img.putpixel((10, 18), (255, 255, 255, 180))
    img.putpixel((10, 19), (255, 255, 255, 180))
    # Secondary faint reflection on right
    for y in range(16, 23):
        img.putpixel((21, y), (220, 240, 255, 120))
    return img

def gen_incense():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    # Ornate tripod brass burner bowl at base (y=21..28)
    for y in range(21, 26):
        for x in range(9, 23):
            dist_x = abs(x - 15.5)
            if dist_x <= (6.5 - (y - 21) * 0.5):
                lvl = 0.7 if x < 15 else 0.4
                img.putpixel((x, y), hue_shift_ramp((190, 145, 50), lvl, x, y))
    # Tripod legs
    for lx, ly in [(9, 26), (9, 27), (15, 26), (15, 27), (22, 26), (22, 27)]:
        img.putpixel((lx, ly), (140, 100, 30, 255))
    
    # Incense cone/stick sitting in bowl (y=13..21)
    for y in range(13, 22):
        w = int((y - 12) * 0.45)
        for x in range(15 - w, 17 + w):
            img.putpixel((x, y), (85, 60, 45, 255))
    
    # Glowing ember at tip of cone (x=15..16, y=12..13)
    img.putpixel((15, 12), (255, 220, 90, 255))
    img.putpixel((16, 12), (255, 140, 30, 255))
    img.putpixel((15, 13), (255, 80, 20, 255))
    
    # Twisting aromatic incense smoke tendrils (y=2..11)
    smoke_path1 = [(15, 11), (14, 10), (13, 9), (13, 8), (14, 7), (16, 6), (18, 5), (19, 4), (18, 3), (17, 2)]
    smoke_path2 = [(16, 11), (17, 10), (18, 9), (17, 8), (15, 7), (14, 6), (13, 5), (14, 4), (15, 3)]
    for idx, (sx, sy) in enumerate(smoke_path1):
        alpha = int(220 * (1.0 - idx / len(smoke_path1)))
        img.putpixel((sx, sy), (190, 185, 225, alpha))
        img.putpixel((sx + 1, sy), (160, 155, 205, alpha // 2))
    for idx, (sx, sy) in enumerate(smoke_path2):
        alpha = int(180 * (1.0 - idx / len(smoke_path2)))
        img.putpixel((sx, sy), (180, 195, 235, alpha))
    return img

def gen_blade_oil():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    # Heavy military rectangular flask strapped with leather harness
    # Neck & cork
    for y in range(4, 7):
        for x in range(14, 18):
            img.putpixel((x, y), (140, 95, 55, 255)) # Cork
    for y in range(7, 11):
        for x in range(13, 19):
            img.putpixel((x, y), (70, 75, 85, 255)) # Dark heavy glass neck
    
    # Flask body: y=11..27, x=8..23
    for y in range(11, 28):
        for x in range(8, 24):
            # Outer dark iron/glass frame
            if x in (8, 23) or y in (11, 27):
                img.putpixel((x, y), OUTLINE)
            else:
                # Heavy viscous crimson/amber blade oil inside (tilted liquid line)
                tilt_y = 15 + int((x - 8) * 0.25)
                if y >= tilt_y:
                    oil_c = (195, 45, 20, 255) if (y == tilt_y) else (135, 25, 15, 255)
                    # Specular highlight streak on oil
                    if x == 11 and y in range(tilt_y, tilt_y + 7):
                        oil_c = (255, 175, 100, 255)
                    img.putpixel((x, y), oil_c)
                else:
                    img.putpixel((x, y), (40, 45, 55, 180)) # Empty flask glass
    
    # Diagonal crossed leather straps with brass buckle
    for i in range(14):
        img.putpixel((9 + i, 13 + i), (80, 45, 25, 255))
        img.putpixel((22 - i, 13 + i), (80, 45, 25, 255))
    # Central brass buckle
    for bx in (15, 16):
        for by in (19, 20):
            img.putpixel((bx, by), (215, 170, 50, 255))
    return img

def gen_processed_herb():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    # Folded parchment apothecary pouch containing glowing vibrant herb dust
    for y in range(10, 27):
        w = int((y - 9) * 0.6)
        for x in range(16 - w, 17 + w):
            lvl = 0.6 + dither_val(x, y) * 0.15
            img.putpixel((x, y), hue_shift_ramp((210, 185, 135), lvl, x, y))
    # Twine cord around pouch neck at y=13
    for x in range(13, 20):
        img.putpixel((x, 13), (120, 85, 40, 255))
    # Shimmering magical alchemical essence dust emerging from top (y=4..11)
    dust_pts = [
        (16, 5, (255, 215, 0)),
        (14, 7, (80, 240, 180)),
        (18, 6, (180, 100, 255)),
        (13, 9, (255, 100, 150)),
        (19, 8, (60, 200, 255)),
        (16, 8, (255, 240, 160)),
        (15, 10, (120, 255, 140))
    ]
    for dx, dy, col in dust_pts:
        img.putpixel((dx, dy), (*col, 255))
        img.putpixel((dx + 1, dy), (*col, 160))
        img.putpixel((dx, dy - 1), (255, 255, 255, 200))
    return img

# =============================================================================
# 7 CANON ALCHEMICAL HERBS (Block Cross & Item Textures)
# =============================================================================

def gen_cinder_thistle():
    # 1. Cinder Thistle: Charred briar stem, sharp obsidian thorns, radiant fiery thistle blossom
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    # Main stem: (16, 30) to (16, 12) with thorny curves
    for y in range(12, 31):
        x = int(16 + math.sin(y * 0.5) * 1.5)
        # Charred stem
        img.putpixel((x, y), (40, 30, 30, 255))
        img.putpixel((x + 1, y), (25, 18, 18, 255))
        # Lateral thorns
        if y in (16, 20, 24, 28):
            tx = x - 3 if y % 2 == 0 else x + 3
            img.putpixel((tx, y - 1), (180, 35, 20, 255))
            img.putpixel((tx + (1 if y % 2 == 0 else -1), y), (90, 20, 15, 255))
    
    # Thistle blossom crown: y=5..13, radiant lava gradient
    cx, cy = 16, 10
    for y in range(5, 14):
        for x in range(10, 23):
            dist = math.hypot(x - cx, (y - cy) * 1.3)
            if dist <= 5.2:
                # Radial gradient from white-hot core to deep ember red
                t = dist / 5.2
                if t < 0.35:
                    c = (255, 248, 180, 255) # White-hot core
                elif t < 0.7:
                    c = (255, 110, 20, 255) # Intense orange
                else:
                    c = (160, 25, 15, 255) # Deep ember
                img.putpixel((x, y), c)
    
    # Embers floating above
    for ex, ey in [(14, 3), (17, 2), (19, 4), (13, 4)]:
        img.putpixel((ex, ey), (255, 210, 60, 240))
    return img

def gen_frostvein_orchid():
    # 2. Frostvein Orchid: Frosted crystalline petals, glowing ice-blue veins, floating frost crystals
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    # Elegant curving stem: y=13..30
    for y in range(13, 31):
        x = int(16 + math.cos(y * 0.3) * 2.2)
        img.putpixel((x, y), (70, 120, 130, 255))
        img.putpixel((x + 1, y), (40, 75, 85, 255))
    
    # 5 Orchid petals radiating from (16, 12)
    cx, cy = 16, 12
    for angle_deg in (0, 72, 144, 216, 288):
        rad = math.radians(angle_deg - 90)
        for d in range(1, 8):
            px = int(cx + math.cos(rad) * d)
            py = int(cy + math.sin(rad) * d)
            if 0 <= px < 32 and 0 <= py < 32:
                # Petal surface: pale ice crystal
                c = (215, 245, 255, 230) if d >= 4 else (120, 210, 235, 255)
                img.putpixel((px, py), c)
                # Petal width
                wx = int(px - math.sin(rad) * 1.2)
                wy = int(py + math.cos(rad) * 1.2)
                if 0 <= wx < 32 and 0 <= wy < 32:
                    img.putpixel((wx, wy), (160, 225, 245, 200))
    # Center core: glowing cyan stamen
    img.putpixel((cx, cy), (255, 255, 255, 255))
    img.putpixel((cx, cy + 1), (100, 245, 255, 255))
    # Floating frost dust
    for fx, fy in [(10, 8), (22, 9), (13, 6), (20, 18), (9, 16)]:
        img.putpixel((fx, fy), (230, 250, 255, 220))
    return img

def gen_storm_marigold():
    # 3. Storm Marigold: Slate-teal stem, dramatic lightning-edged golden petals, electric arc core
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    # Sturdy fibrous stem: y=13..30
    for y in range(13, 31):
        x = 16
        img.putpixel((x, y), (50, 85, 75, 255))
        img.putpixel((x + 1, y), (35, 60, 50, 255))
        if y in (19, 25):
            img.putpixel((x - 2, y - 1), (55, 100, 80, 255))
            img.putpixel((x + 3, y - 1), (55, 100, 80, 255))
    
    # Flower head: Jagged lightning-like zigzag petals, y=4..15, x=9..23
    cx, cy = 16, 11
    for y in range(4, 16):
        for x in range(9, 24):
            dist = math.hypot(x - cx, y - cy)
            if dist <= 6.5:
                # Zigzag edge petal variation
                ang = math.atan2(y - cy, x - cx)
                ray = math.sin(ang * 12.0)
                if dist <= 4.0 or (dist <= 6.5 and ray > 0.1):
                    # Rich electric gold
                    lvl = 0.8 if dist < 3.0 else 0.5
                    c = hue_shift_ramp((255, 185, 30), lvl, x, y)
                    img.putpixel((x, y), c)
    # Electric arc spark in center
    for ax, ay in [(16, 10), (16, 11), (17, 11), (15, 12), (16, 12)]:
        img.putpixel((ax, ay), (140, 230, 255, 255)) # Electric cyan arc
    img.putpixel((16, 11), (255, 255, 255, 255))
    return img

def gen_abyssal_lumina_mint():
    # 4. Abyssal Lumina Mint: Deep sculk/oceanic blue-black foliage, pulsing bioluminescent cyan margin nodules
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    # Central stem
    for y in range(10, 31):
        img.putpixel((16, y), (14, 32, 42, 255))
        img.putpixel((15, y), (8, 20, 28, 255))
    
    # Paired opposite serrated mint leaves
    leaf_tiers = [(14, 5.0), (19, 6.0), (24, 5.5)]
    for ly, span in leaf_tiers:
        for x in range(int(16 - span), int(17 + span)):
            dist_c = abs(x - 16)
            curve = math.sin((dist_c / span) * math.pi) * 2.2
            py = int(ly - curve)
            if 0 <= py < 32:
                # Dark sculk leaf
                img.putpixel((x, py), (12, 38, 48, 255))
                # Bioluminescent glowing nodes on leaf margins
                if dist_c >= span - 1.2 or (dist_c >= 2.0 and dist_c % 2 == 0):
                    img.putpixel((x, py), (0, 240, 255, 255)) # Cyan glow
    # Glowing dew drop at apex
    img.putpixel((16, 8), (0, 255, 230, 255))
    img.putpixel((16, 9), (0, 200, 210, 255))
    return img

def gen_screaming_mandragora():
    # 5. Screaming Mandragora: Grotesque anthropomorphic root with agonizing expression, crowned with leafy rosette
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    # Top ruffled emerald leaf rosette (y=4..13)
    for y in range(4, 13):
        for x in range(9, 24):
            dist = math.hypot(x - 16, y - 8)
            if dist <= 5.5 and (x + y * 2) % 4 != 0:
                img.putpixel((x, y), (35, 115, 45, 255))
                if dist <= 2.5:
                    img.putpixel((x, y), (65, 165, 65, 255))
    
    # Anthropomorphic tuber root (y=12..28, x=11..21)
    for y in range(12, 26):
        w = 3.5 if y < 20 else 2.5
        for x in range(int(16 - w), int(17 + w)):
            lvl = 0.6 + dither_val(x, y) * 0.15
            c = hue_shift_ramp((190, 160, 105), lvl, x, y)
            img.putpixel((x, y), c)
    
    # Split root legs
    for y in range(25, 30):
        img.putpixel((14, y), (150, 120, 75, 255))
        img.putpixel((18, y), (150, 120, 75, 255))
    
    # Screaming face cavities (hollow dark fissures)
    # Eyes at (14, 15) and (18, 15)
    img.putpixel((14, 15), (45, 30, 18, 255))
    img.putpixel((18, 15), (45, 30, 18, 255))
    # Wide open screaming mouth at (16, 18..20)
    for my in (18, 19, 20):
        img.putpixel((16, my), (35, 20, 12, 255))
    return img

def gen_moonveil_belladonna():
    # 6. Moonveil Belladonna: Midnight-violet arching stem, drooping purple bell flowers, glistening black berries
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    # Arching graceful stem: from (16, 30) curving to (12, 10)
    for y in range(10, 31):
        x = int(16 - math.sin((30 - y) / 20.0 * math.pi * 0.6) * 4.5)
        img.putpixel((x, y), (45, 30, 65, 255))
        img.putpixel((x + 1, y), (30, 20, 45, 255))
    
    # Serrated deep teal leaves
    for lx, ly in [(11, 22), (10, 21), (9, 22), (20, 24), (21, 23), (22, 24)]:
        img.putpixel((lx, ly), (25, 65, 55, 255))
    
    # Drooping bell-shaped purple flowers at (10, 12) and (18, 16)
    for fx, fy in [(10, 13), (19, 17)]:
        for dx in range(-2, 3):
            for dy in range(0, 4):
                if abs(dx) <= (dy * 0.8):
                    c = (145, 60, 185, 255) if dy == 3 else (90, 35, 125, 255)
                    img.putpixel((fx + dx, fy + dy), c)
        # Golden glowing stamen inside bell
        img.putpixel((fx, fy + 4), (255, 220, 90, 255))
    
    # Glossy pitch-black nightshade berries
    for bx, by in [(13, 17), (16, 21)]:
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                if abs(dx) + abs(dy) <= 1:
                    img.putpixel((bx + dx, by + dy), (15, 12, 22, 255))
        img.putpixel((bx - 1, by - 1), (220, 210, 255, 255)) # Sharp specular highlight
    return img

def gen_carrion_bogmoss():
    # 7. Carrion Bogmoss: Low-lying spreading swamp moss carpet, olive-green decay, glowing phosphor spore caps
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    # Low-lying moss carpet: y=22..30, spreading horizontally
    for y in range(22, 31):
        for x in range(3, 29):
            dist_x = abs(x - 16)
            curve = math.sin(x * 0.6) * 1.5
            if y >= 25 - curve:
                lvl = 0.5 + dither_val(x, y) * 0.15
                c = hue_shift_ramp((55, 80, 35), lvl, x, y)
                if y >= 28:
                    c = (30, 35, 20, 255) # Peaty decay base
                img.putpixel((x, y), c)
    
    # Spore stalk stalks and glowing chartreuse spore caps
    stalks = [(8, 20), (13, 18), (17, 19), (21, 17), (25, 21)]
    for sx, sy in stalks:
        for py in range(sy, 26):
            img.putpixel((sx, py), (60, 90, 45, 255))
        # Glowing spore cap
        img.putpixel((sx, sy), (210, 255, 50, 255)) # Glowing yellow-green
        img.putpixel((sx - 1, sy + 1), (150, 215, 30, 255))
        img.putpixel((sx + 1, sy + 1), (150, 215, 30, 255))
    return img

# =============================================================================
# MAIN RUNNER
# =============================================================================

print("Rendering 10 core mod items...")
items = {
    "stirring_rod.png": gen_stirring_rod(),
    "alchemical_journal.png": gen_alchemical_journal(),
    "recipe_scroll.png": gen_recipe_scroll(),
    "ancient_parchment.png": gen_ancient_parchment(),
    "scholia_fragment.png": gen_scholia_fragment(),
    "alchemical_slag.png": gen_alchemical_slag(),
    "alchemical_sludge.png": gen_alchemical_sludge(),
    "elixir.png": gen_elixir(),
    "incense.png": gen_incense(),
    "blade_oil.png": gen_blade_oil(),
    "processed_herb.png": gen_processed_herb()
}

for filename, img in items.items():
    img.save(os.path.join(ITEM_DIR, filename))
    print(f"  [Item] Generated {filename}")

print("\nRendering 7 alchemical herbs (Block & Item textures)...")
herbs = {
    "cinder_thistle": gen_cinder_thistle(),
    "frostvein_orchid": gen_frostvein_orchid(),
    "storm_marigold": gen_storm_marigold(),
    "abyssal_lumina_mint": gen_abyssal_lumina_mint(),
    "screaming_mandragora": gen_screaming_mandragora(),
    "moonveil_belladonna": gen_moonveil_belladonna(),
    "carrion_bogmoss": gen_carrion_bogmoss()
}

for herb_name, img in herbs.items():
    # Save block cross texture
    block_file = f"{herb_name}.png"
    img.save(os.path.join(BLOCK_DIR, block_file))
    print(f"  [Block] Generated {block_file}")
    
    # Save item icon texture
    item_file = f"{herb_name}.png"
    img.save(os.path.join(ITEM_DIR, item_file))
    print(f"  [Item]  Generated {item_file}")

print("\nAll 32x32 textures successfully generated with dark medieval aesthetics!")
