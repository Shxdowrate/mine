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


if __name__ == "__main__":
    main()
