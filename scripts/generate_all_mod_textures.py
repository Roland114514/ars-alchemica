#!/usr/bin/env python3
"""
Ars Alchemica - Texture Generator for Items and Alchemical Cauldron
Generates 32x32 pixel art textures matching dark fantasy medieval aesthetics.
"""

import os
import math
import random
from PIL import Image, ImageDraw

BLOCK_DIR = "common/src/main/resources/assets/ars_alchemica/textures/block"
ITEM_DIR = "common/src/main/resources/assets/ars_alchemica/textures/item"
os.makedirs(BLOCK_DIR, exist_ok=True)
os.makedirs(ITEM_DIR, exist_ok=True)

random.seed(1337)

def clamp(val, low=0, high=255):
    return max(low, min(high, int(val)))

def lerp(c1, c2, t):
    return tuple(clamp(c1[i] + (c2[i] - c1[i]) * t) for i in range(len(c1)))

# --- Cauldron Block Textures ---
def generate_cauldron_textures():
    # 1. cauldron_side.png (32x32)
    img_side = Image.new("RGBA", (32, 32))
    base_iron = (35, 36, 42, 255)
    rivet_iron = (55, 58, 68, 255)
    brass_band = (165, 125, 45, 255)
    brass_hi = (215, 175, 75, 255)
    rune_purple = (150, 75, 210, 255)

    for y in range(32):
        for x in range(32):
            noise = (random.random() - 0.5) * 15
            # Brass band at y=10..13 and y=24..27
            if (10 <= y <= 13) or (24 <= y <= 27):
                c = brass_hi if (y == 10 or y == 24) and x % 4 != 0 else brass_band
            elif y < 3: # Top lip rim
                c = rivet_iron
            else:
                c = base_iron
            
            # Add noise
            r = clamp(c[0] + noise)
            g = clamp(c[1] + noise)
            b = clamp(c[2] + noise)
            img_side.putpixel((x, y), (r, g, b, 255))
    
    # Draw central arcane glyph in purple
    draw_side = ImageDraw.Draw(img_side)
    draw_side.line([(16, 5), (16, 9)], fill=rune_purple, width=1)
    draw_side.line([(14, 7), (18, 7)], fill=rune_purple, width=1)
    draw_side.line([(16, 15), (16, 22)], fill=rune_purple, width=1)
    draw_side.ellipse([(14, 17), (18, 21)], outline=rune_purple)
    img_side.save(os.path.join(BLOCK_DIR, "cauldron_side.png"))

    # 2. cauldron_top.png (32x32)
    img_top = Image.new("RGBA", (32, 32))
    for y in range(32):
        for x in range(32):
            dist = math.sqrt((x - 15.5)**2 + (y - 15.5)**2)
            noise = (random.random() - 0.5) * 12
            if dist > 14: # Outer edge
                c = (30, 31, 36, 255)
            elif dist > 11: # Rim top
                c = (60, 62, 70, 255) if (x + y) % 4 == 0 else (45, 47, 54, 255)
            else: # Inner rim
                c = (25, 26, 30, 255)
            img_top.putpixel((x, y), (clamp(c[0] + noise), clamp(c[1] + noise), clamp(c[2] + noise), 255))
    img_top.save(os.path.join(BLOCK_DIR, "cauldron_top.png"))

    # 3. cauldron_bottom.png (32x32)
    img_bot = Image.new("RGBA", (32, 32))
    for y in range(32):
        for x in range(32):
            noise = (random.random() - 0.5) * 10
            c = (28, 29, 34, 255)
            img_bot.putpixel((x, y), (clamp(c[0] + noise), clamp(c[1] + noise), clamp(c[2] + noise), 255))
    img_bot.save(os.path.join(BLOCK_DIR, "cauldron_bottom.png"))

    # 4. cauldron_inner.png (32x32)
    img_in = Image.new("RGBA", (32, 32))
    for y in range(32):
        for x in range(32):
            noise = (random.random() - 0.5) * 14
            # Dark soot with subtle copper/patina green
            patina = 5 if (x * y) % 7 == 0 else 0
            c = (22, 24 + patina, 28 + patina, 255)
            img_in.putpixel((x, y), (clamp(c[0] + noise), clamp(c[1] + noise), clamp(c[2] + noise), 255))
    img_in.save(os.path.join(BLOCK_DIR, "cauldron_inner.png"))

# --- Item Textures (32x32) ---
def generate_item_textures():
    # 1. stirring_rod.png
    rod = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(rod)
    for i in range(24):
        x = 5 + i
        y = 26 - i
        wood = (75, 45, 25, 255) if i < 18 else (220, 180, 50, 255) # Gold tip
        rod.putpixel((x, y), wood)
        rod.putpixel((x + 1, y), (clamp(wood[0] + 30), clamp(wood[1] + 25), clamp(wood[2] + 20), 255))
        rod.putpixel((x, y + 1), (clamp(wood[0] - 25), clamp(wood[1] - 20), clamp(wood[2] - 15), 255))
    rod.save(os.path.join(ITEM_DIR, "stirring_rod.png"))

    # 2. alchemical_journal.png
    book = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    for y in range(5, 27):
        for x in range(6, 26):
            # Leather base
            leather = (65, 30, 20, 255)
            if x == 6 or x == 25 or y == 5 or y == 26:
                leather = (40, 18, 12, 255) # Border
            elif (x <= 8 and y <= 7) or (x >= 23 and y <= 7) or (x <= 8 and y >= 24) or (x >= 23 and y >= 24):
                leather = (200, 160, 60, 255) # Brass corner clasps
            book.putpixel((x, y), leather)
    # Gem in center
    for y in range(13, 19):
        for x in range(13, 19):
            if abs(x - 15.5) + abs(y - 15.5) <= 2.5:
                book.putpixel((x, y), (170, 70, 240, 255))
    book.putpixel((15, 15), (230, 180, 255, 255))
    book.save(os.path.join(ITEM_DIR, "alchemical_journal.png"))

    # 3. recipe_scroll.png
    scroll = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    for y in range(8, 24):
        for x in range(8, 24):
            if abs((x - 8) - (y - 8)) <= 5:
                parch = (215, 195, 145, 255)
                if x % 3 == 0 and y % 3 == 0:
                    parch = (185, 165, 115, 255)
                scroll.putpixel((x, y), parch)
    # Crimson ribbon & seal
    for i in range(7):
        scroll.putpixel((12 + i, 19 - i), (160, 25, 25, 255))
    scroll.putpixel((15, 16), (220, 40, 40, 255))
    scroll.putpixel((16, 16), (220, 40, 40, 255))
    scroll.save(os.path.join(ITEM_DIR, "recipe_scroll.png"))

    # 4. ancient_parchment.png
    parch = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    for y in range(6, 26):
        for x in range(7, 25):
            # Jagged edges
            if (x == 7 or x == 24 or y == 6 or y == 25) and (x + y) % 3 == 0:
                continue
            base = (175, 145, 95, 255)
            # Glowing runes
            if (y in (10, 14, 18, 22)) and (9 <= x <= 22) and x % 2 == 0:
                base = (110, 195, 220, 255) # Cyan arcane ink
            parch.putpixel((x, y), base)
    parch.save(os.path.join(ITEM_DIR, "ancient_parchment.png"))

    # 5. scholia_fragment.png
    frag = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    for y in range(10, 22):
        for x in range(9, 23):
            if x + y >= 19 and (22 - x) + (22 - y) >= 2:
                base = (205, 180, 120, 255)
                if y % 3 == 0 and x % 2 == 0:
                    base = (190, 140, 40, 255) # Gold marginalia
                frag.putpixel((x, y), base)
    frag.save(os.path.join(ITEM_DIR, "scholia_fragment.png"))

    # 6. alchemical_slag.png
    slag = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    for y in range(32):
        for x in range(32):
            dist = math.sqrt((x - 16)**2 + (y - 16)**2)
            noise = (random.random() - 0.5) * 4
            if dist + noise < 8:
                # Iridescent rainbow sheen over porous dark ore
                t = (x + y) / 32.0
                rainbow = (clamp(50 + 60 * math.sin(t * 6)), clamp(50 + 60 * math.sin(t * 6 + 2)), clamp(60 + 60 * math.sin(t * 6 + 4)), 255)
                if (x * y) % 5 == 0: # porous hole
                    slag.putpixel((x, y), (20, 20, 24, 255))
                else:
                    slag.putpixel((x, y), rainbow)
    slag.save(os.path.join(ITEM_DIR, "alchemical_slag.png"))

    # 7. alchemical_sludge.png
    sludge = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    for y in range(32):
        for x in range(32):
            dist = math.sqrt((x - 16)**2 + (y - 17)**2)
            if dist < 8:
                # Pitch black bubbling slime with toxic green glow
                if dist < 3 and random.random() < 0.4:
                    c = (60, 210, 70, 255) # Toxic green bubble
                elif (x + y) % 4 == 0:
                    c = (25, 45, 25, 255)
                else:
                    c = (15, 16, 18, 255)
                sludge.putpixel((x, y), c)
    sludge.save(os.path.join(ITEM_DIR, "alchemical_sludge.png"))

    # 8. elixir.png
    elixir = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    # Glass bottle neck (x: 14..17, y: 7..11), cork (y: 5..6), body (x: 10..21, y: 12..25)
    for y in range(5, 7):
        for x in range(14, 18):
            elixir.putpixel((x, y), (140, 95, 50, 255)) # Cork
    for y in range(7, 12):
        for x in range(14, 18):
            elixir.putpixel((x, y), (180, 210, 230, 180)) # Glass neck
    for y in range(12, 26):
        for x in range(10, 22):
            dist = math.sqrt((x - 15.5)**2 + (y - 18.5)**2)
            if dist <= 6.5:
                # Glowing blue/magenta elixir liquid
                liquid = (120, 60, 220, 230)
                if x == 12 and 14 <= y <= 20: # Specular glass highlight
                    liquid = (240, 240, 255, 255)
                elixir.putpixel((x, y), liquid)
    elixir.save(os.path.join(ITEM_DIR, "elixir.png"))

    # 9. incense.png
    incense = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    # Brass holder + incense stick
    for i in range(18):
        x = 9 + i
        y = 23 - i
        incense.putpixel((x, y), (90, 65, 40, 255))
    # Glowing ember at tip
    incense.putpixel((26, 6), (255, 120, 30, 255))
    incense.putpixel((27, 5), (255, 200, 80, 255))
    # Mystical swirling smoke
    smoke = [(25, 4), (24, 3), (22, 3), (21, 2), (22, 1)]
    for sx, sy in smoke:
        incense.putpixel((sx, sy), (180, 160, 220, 180))
    # Brass base
    for x in range(7, 13):
        incense.putpixel((x, 24), (185, 145, 55, 255))
        incense.putpixel((x, 25), (140, 110, 40, 255))
    incense.save(os.path.join(ITEM_DIR, "incense.png"))

    # 10. blade_oil.png
    oil = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    # Dark triangular flask
    for y in range(8, 25):
        w = int((y - 7) * 0.55)
        for x in range(16 - w, 17 + w):
            base = (30, 32, 38, 240)
            if y >= 14: # Viscous metallic silver-purple oil
                base = (110, 40, 150, 255)
                if x == 16 - w + 1:
                    base = (180, 160, 220, 255) # Viscous shine
            oil.putpixel((x, y), base)
    # Cork stopper
    for y in range(5, 8):
        for x in range(15, 18):
            oil.putpixel((x, y), (130, 85, 45, 255))
    oil.save(os.path.join(ITEM_DIR, "blade_oil.png"))

print("Generating Cauldron Block Textures...")
generate_cauldron_textures()
print("Generating 10 Item Textures...")
generate_item_textures()
print("Done! All textures successfully generated.")
