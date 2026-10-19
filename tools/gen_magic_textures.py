"""Генерирует текстуры и модели перчатки бесконечности, камней и кольца Стрэнджа.

Запуск из корня репозитория: python3 tools/gen_magic_textures.py
"""
import json
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "solorust")
TEX = os.path.join(ROOT, "textures", "item", "magic")
MODELS = os.path.join(ROOT, "models", "item", "magic")
ITEMS = os.path.join(ROOT, "items")

STONES = [  # порядок как в InfinityStone
    ("power_stone", (0x9B, 0x30, 0xFF)),
    ("space_stone", (0x2E, 0x7B, 0xFF)),
    ("reality_stone", (0xE0, 0x18, 0x2D)),
    ("soul_stone", (0xFF, 0x8A, 0x1F)),
    ("time_stone", (0x2B, 0xD9, 0x4A)),
    ("mind_stone", (0xFF, 0xD2, 0x1F)),
]


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c) + (255,)


def lighten(c, k):
    return tuple(int(v + (255 - v) * k) for v in c) + (255,)


def outline(img, color):
    w, h = img.size
    px = img.load()
    edge = []
    for y in range(h):
        for x in range(w):
            if px[x, y][3] != 0:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and px[nx, ny][3] != 0 and px[nx, ny] != color:
                    edge.append((x, y))
                    break
    for p in edge:
        px[p] = color


# ------------------------------------------------------------------ перчатка

GOLD = (0xE0, 0xA8, 0x2E, 255)
GOLD_LIGHT = (0xFF, 0xD8, 0x6B, 255)
GOLD_DARK = (0xA8, 0x74, 0x1A, 255)
OUTLINE = (0x4A, 0x2C, 0x08, 255)
SOCKET = (0x2E, 0x1E, 0x0A, 255)

# Гнёзда камней: (пиксели), порядок как в STONES.
SOCKETS = [
    [(4, 6), (5, 6)],                      # Сила — указательный
    [(7, 6), (8, 6)],                      # Пространство — средний
    [(10, 6), (11, 6)],                    # Реальность — безымянный
    [(2, 9), (2, 10)],                     # Душа — большой палец
    [(13, 6)],                             # Время — мизинец
    [(7, 9), (8, 9), (7, 10), (8, 10)],    # Разум — тыльная сторона
]


def gauntlet_base():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()

    def fill(x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                px[x, y] = c

    # Пальцы.
    for fx, top in ((4, 2), (7, 1), (10, 2), (13, 3)):
        width = 2 if fx < 13 else 1
        fill(fx, top, fx + width - 1, 6, GOLD)
        px[fx, top] = GOLD_LIGHT
        px[fx, top + 2] = GOLD_DARK  # сустав
        if width == 2:
            px[fx + 1, top + 2] = GOLD_DARK
    # Костяшки и тыльная сторона.
    fill(3, 6, 14, 7, GOLD)
    fill(3, 8, 13, 12, GOLD)
    fill(3, 8, 3, 12, GOLD_LIGHT)
    fill(13, 8, 13, 12, GOLD_DARK)
    fill(4, 12, 12, 12, GOLD_DARK)
    # Большой палец.
    fill(1, 8, 2, 11, GOLD)
    px[1, 8] = GOLD_LIGHT
    # Манжета.
    fill(4, 13, 12, 15, GOLD_DARK)
    fill(4, 14, 12, 14, GOLD)
    fill(5, 13, 11, 13, GOLD_LIGHT)

    for sock in SOCKETS:
        for p in sock:
            px[p] = SOCKET

    outline(img, OUTLINE)
    return img


def gauntlet(mask):
    img = gauntlet_base()
    px = img.load()
    for i, (name, color) in enumerate(STONES):
        if mask & (1 << i):
            pixels = SOCKETS[i]
            for j, p in enumerate(pixels):
                px[p] = lighten(color, 0.45) if j == 0 else color + (255,)
    return img


# ------------------------------------------------------------------ камни

GEM = [
    "................",
    "................",
    "......####......",
    ".....#hhcc#.....",
    "....#hhcccc#....",
    "...#hhcccccd#...",
    "...#hcccccdd#...",
    "..#hccccccddd#..",
    "..#cccccccddd#..",
    "...#ccccccdd#...",
    "...#cccccddd#...",
    "....#cccddd#....",
    ".....#cddd#.....",
    "......#dd#......",
    ".......##.......",
    "................",
]


def stone(color):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(GEM):
        for x, ch in enumerate(row):
            if ch == "#":
                px[x, y] = shade(color, 0.35)
            elif ch == "h":
                px[x, y] = lighten(color, 0.6)
            elif ch == "c":
                px[x, y] = color + (255,)
            elif ch == "d":
                px[x, y] = shade(color, 0.7)
    px[6, 4] = (255, 255, 255, 255)
    return img


# ------------------------------------------------------------------ кольцо-портал

RING = [
    "................",
    "................",
    "................",
    "...ooo....ooo...",
    "..oHGGo..oHGGo..",
    ".oH...Go.H...Go.",
    ".oG...GooG...Go.",
    ".oG...GEEG...Go.",
    ".oG...GEEG...Go.",
    ".oG...GooG...Do.",
    "..oGGDo..oGGDo..",
    "...ooo....ooo...",
    "................",
    "................",
    "................",
    "................",
]


def ring():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    colors = {
        "o": OUTLINE,
        "G": GOLD,
        "H": GOLD_LIGHT,
        "D": GOLD_DARK,
        "E": (0xFF, 0x7A, 0x1A, 255),
    }
    for y, row in enumerate(RING):
        for x, ch in enumerate(row):
            if ch in colors:
                px[x, y] = colors[ch]
    px[7, 7] = (0xFF, 0xD5, 0x80, 255)
    return img


# ------------------------------------------------------------------ артефакты героев

HEROES = {
    # Боевой молот с кожаной обмоткой рукояти.
    "mjolnir": ([
        "................",
        "..oooooooooooo..",
        ".oHHHHHHHHHHHSo.",
        ".oHSSSSSSSSSSDo.",
        ".oHSSSSSSSSSSDo.",
        ".oSDDDDDDDDDDDo.",
        "..oooooooooooo..",
        "......oLLo......",
        "......oLlo......",
        "......oLLo......",
        "......oLlo......",
        "......oLLo......",
        "......oLlo......",
        ".......oo.......",
        "......o..o......",
        ".......oo.......",
    ], {"o": (0x2A, 0x2A, 0x30), "H": (0xE8, 0xEC, 0xF0), "S": (0xA8, 0xB0, 0xBA), "D": (0x70, 0x78, 0x84),
        "L": (0x6D, 0x45, 0x26), "l": (0x8D, 0x5F, 0x38)}),
    # Красная тиара.
    "scarlet_tiara": ([
        "................",
        "................",
        "................",
        "...o........o...",
        "..oRo......oRo..",
        "..oRRo....oRRo..",
        "..oRRRo..oRRRo..",
        "..oRRRRooRRRRo..",
        "..oRrRRRRRRrRo..",
        "...oRrRRRRrRo...",
        "....oRRggRRo....",
        ".....oRggRo.....",
        "......oRRo......",
        ".......oo.......",
        "................",
        "................",
    ], {"o": (0x3A, 0x05, 0x14), "R": (0xC2, 0x18, 0x3C), "r": (0xFF, 0x5C, 0x7A), "g": (0xFF, 0xC9, 0x4A)}),
    # Сгусток живой чёрной слизи.
    "symbiote": ([
        "................",
        "......oo........",
        ".....oBBo...o...",
        "....oBBBBo.oBo..",
        "...oBBhBBBoBBo..",
        "..oBBhhBBBBBBo..",
        "..oBhBBBBBBBBBo.",
        ".oBBBBBBBBBBBBo.",
        ".oBBBBBBBBBBGBo.",
        ".oBBBBBBBBBGGBo.",
        "..oBBBBBBBGGBo..",
        "..oBBoBBBBBBo...",
        "...oo.oBBBBo....",
        "......oBBo......",
        ".......oo.......",
        "................",
    ], {"o": (0x00, 0x00, 0x00), "B": (0x14, 0x14, 0x1C), "h": (0x5A, 0x5A, 0x72), "G": (0x26, 0x26, 0x34)}),
    # Золотое кольцо с янтарным камнем.
    "flash_ring": ([
        "................",
        "................",
        "......oooo......",
        ".....oYYYYo.....",
        "....oYyyyYYo....",
        "....oYyRRYYo....",
        "....oYRRRRYo....",
        "....oYYRRYYo....",
        "...ooYYYYYYoo...",
        "..oGGoooooooGo..",
        ".oGo........oGo.",
        ".oG..........Go.",
        ".oG..........Go.",
        ".oGo........oGo.",
        "..oGGGGGGGGGGo..",
        "...oooooooooo...",
    ], {"o": (0x4A, 0x2C, 0x08), "Y": (0xFF, 0xC1, 0x07), "y": (0xFF, 0xF1, 0x76), "R": (0xE6, 0x51, 0x00),
        "G": (0xE0, 0xA8, 0x2E)}),
    # Светящийся кристалл.
    "kryptonian_crystal": ([
        ".......o........",
        "......oWo.......",
        "......oWCo......",
        ".....oWCCo......",
        ".....oWCCCo.....",
        "....oWCCCCo.....",
        "....oWCCCCDo....",
        "...oWCCCCCDo....",
        "...oWCCCCCDDo...",
        "...oWCCCCCDDo...",
        "....oWCCCCDo....",
        "....oWCCCDDo....",
        ".....oCCCDo.....",
        ".....oCCDDo.....",
        "......oCDo......",
        ".......oo.......",
    ], {"o": (0x0D, 0x3B, 0x66), "W": (0xF2, 0xFB, 0xFF), "C": (0x9F, 0xE6, 0xFF), "D": (0x4F, 0xA8, 0xD8)}),
    # Флакон с кровью.
    "blood_vial": ([
        "................",
        "......oooo......",
        "......oKKo......",
        "......oKKo......",
        ".....oGGGGo.....",
        "......oGGo......",
        ".....oGGGGo.....",
        "....oGRRRRGo....",
        "...oGRRrRRRGo...",
        "...oGRrRRRRGo...",
        "...oGRRRRRDGo...",
        "...oGRRRRDDGo...",
        "...oGRRRDDDGo...",
        "....oGDDDDGo....",
        ".....oooooo.....",
        "................",
    ], {"o": (0x2A, 0x10, 0x10), "K": (0x8D, 0x5F, 0x38), "G": (0xD8, 0xE6, 0xEC), "R": (0xB0, 0x00, 0x00),
        "r": (0xFF, 0x52, 0x52), "D": (0x5A, 0x00, 0x00)}),
    # Тёмное сердце-кристалл с трещинами хаоса.
    "chaos_heart": ([
        "................",
        "................",
        "..oooo....oooo..",
        ".oVVVVo..oVVVVo.",
        "oVvVVVVooVVVVMVo",
        "oVvVVMVVVVVVMVVo",
        "oVVVVVMVVVVMVVVo",
        "oVVVVVVMMMMVVVVo",
        ".oVVVVVVMgVVVVo.",
        "..oVVVVVVMVVVo..",
        "...oVVVVVMVVo...",
        "....oVVVVMVo....",
        ".....oVVVVo.....",
        "......oVVo......",
        ".......oo.......",
        "................",
    ], {"o": (0x0B, 0x02, 0x16), "V": (0x2C, 0x0A, 0x52), "v": (0x7A, 0x3C, 0xC8), "M": (0xF7, 0x25, 0x85),
        "g": (0x7C, 0xFC, 0x00)}),
}


def from_grid(rows, palette):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in palette:
                px[x, y] = tuple(palette[ch]) + (255,)
    return img


# ------------------------------------------------------------------ запись

def model(path, texture):
    with open(path, "w", encoding="utf-8") as f:
        json.dump({"parent": "minecraft:item/handheld", "textures": {"layer0": texture}}, f, indent=2)
        f.write("\n")


def item_def(name, model_ref):
    with open(os.path.join(ITEMS, name + ".json"), "w", encoding="utf-8") as f:
        json.dump({"model": {"type": "minecraft:model", "model": model_ref}}, f, indent=2)
        f.write("\n")


def main():
    os.makedirs(os.path.join(TEX, "gauntlet"), exist_ok=True)
    os.makedirs(os.path.join(MODELS, "gauntlet"), exist_ok=True)

    cases = []
    for mask in range(64):
        gauntlet(mask).save(os.path.join(TEX, "gauntlet", f"gauntlet_{mask}.png"))
        model(os.path.join(MODELS, "gauntlet", f"gauntlet_{mask}.json"), f"solorust:item/magic/gauntlet/gauntlet_{mask}")
        if mask:
            cases.append({"when": str(mask), "model": {"type": "minecraft:model", "model": f"solorust:item/magic/gauntlet/gauntlet_{mask}"}})

    with open(os.path.join(ITEMS, "infinity_gauntlet.json"), "w", encoding="utf-8") as f:
        json.dump({"model": {
            "type": "minecraft:select",
            "property": "minecraft:custom_model_data",
            "index": 0,
            "cases": cases,
            "fallback": {"type": "minecraft:model", "model": "solorust:item/magic/gauntlet/gauntlet_0"},
        }}, f, indent=1)
        f.write("\n")

    for name, color in STONES:
        stone(color).save(os.path.join(TEX, name + ".png"))
        model(os.path.join(MODELS, name + ".json"), f"solorust:item/magic/{name}")
        item_def(name, f"solorust:item/magic/{name}")

    ring().save(os.path.join(TEX, "sorcerer_ring.png"))
    model(os.path.join(MODELS, "sorcerer_ring.json"), "solorust:item/magic/sorcerer_ring")
    item_def("sorcerer_ring", "solorust:item/magic/sorcerer_ring")

    for name, (rows, palette) in HEROES.items():
        assert len(rows) == 16 and all(len(r) == 16 for r in rows), name
        from_grid(rows, palette).save(os.path.join(TEX, name + ".png"))
        model(os.path.join(MODELS, name + ".json"), f"solorust:item/magic/{name}")
        item_def(name, f"solorust:item/magic/{name}")


if __name__ == "__main__":
    main()
