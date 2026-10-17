#!/usr/bin/env python3
"""Генерирует текстуры силовой брони (Solo Rust): броня на теле, иконки частей, иконки встроенного оружия.

Запуск из корня репозитория:  python3 tools/gen_suit_textures.py
Палитры поколений — в PALETTES; поменял цвета, запустил, пересобрал мод.
"""
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "solorust", "textures")
EQUIP = os.path.join(ROOT, "entity", "equipment")
ITEM = os.path.join(ROOT, "item")

# base, dark, accent, glow
PALETTES = {
    "mk1": dict(base=(98, 98, 104), dark=(52, 52, 58), accent=(138, 82, 44), glow=(255, 168, 60), style="scrap"),
    "mk2": dict(base=(196, 202, 212), dark=(118, 124, 136), accent=(160, 170, 186), glow=(175, 240, 255), style="plain"),
    "mk3": dict(base=(42, 84, 172), dark=(20, 38, 92), accent=(226, 232, 242), glow=(80, 222, 255), style="panels"),
    "titan": dict(base=(228, 128, 32), dark=(38, 38, 44), accent=(250, 210, 40), glow=(255, 224, 80), style="hazard"),
    "nano": dict(base=(32, 32, 40), dark=(14, 14, 20), accent=(72, 60, 96), glow=(184, 112, 255), style="circuit"),
}


def clamp(v):
    return max(0, min(255, int(v)))


def shade(c, k):
    return tuple(clamp(x * k) for x in c)


def mix(a, b, t):
    return tuple(clamp(a[i] * (1 - t) + b[i] * t) for i in range(3))


class Canvas:
    def __init__(self, w, h):
        self.im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        self.px = self.im.load()

    def set(self, x, y, c, a=255):
        if 0 <= x < self.im.width and 0 <= y < self.im.height:
            self.px[x, y] = (c[0], c[1], c[2], a)

    def rect(self, x0, y0, x1, y1, fn):
        for y in range(y0, y1):
            for x in range(x0, x1):
                c = fn(x - x0, y - y0, x1 - x0, y1 - y0)
                if c is not None:
                    self.set(x, y, c)

    def save(self, path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        self.im.save(path)


# ---------------------------------------------------------------- поверхность металла

def surface(p, seed):
    rnd = random.Random(seed)
    base, dark, accent, glow, style = p["base"], p["dark"], p["accent"], p["glow"], p["style"]

    def fn(x, y, w, h):
        n = rnd.uniform(0.9, 1.06)
        c = shade(base, n)
        edge = x == 0 or y == 0 or x == w - 1 or y == h - 1
        if style == "scrap":
            if rnd.random() < 0.12:
                c = shade(accent, rnd.uniform(0.8, 1.1))  # ржавчина
            if (x + y * 3) % 7 == 0 and rnd.random() < 0.3:
                c = shade(dark, 0.9)
            if (x in (1, w - 2)) and (y in (1, h - 2)):
                c = (170, 170, 170)  # заклёпки
        elif style == "panels":
            if y % 4 == 3:
                c = shade(base, 0.75)
            if w >= 8 and (x < 2 or x >= w - 2) and y % 4 < 3:
                c = shade(accent, n)
        elif style == "hazard":
            if (x + y) % 6 < 2 and y >= h - 3:
                c = shade(dark, 1.1)
            if y % 5 == 4:
                c = shade(base, 0.8)
        elif style == "circuit":
            if (x * 7 + y * 13) % 11 == 0:
                c = mix(base, glow, 0.55)
            elif y % 5 == 2 and x % 3 != 0:
                c = shade(accent, n)
        elif style == "plain":
            if (x + y) % 9 == 0:
                c = shade(base, 1.08)
        if edge:
            c = shade(dark, 1.0)
        return c

    return fn


def face(cv, x0, y0, w, h, p, seed):
    cv.rect(x0, y0, x0 + w, y0 + h, surface(p, seed))


def cube(cv, u, v, w, h, d, p, seed, skip_rows_top=0):
    """Раскладка куба Minecraft: (u, v) — левый верх, w×h×d — размеры."""
    # top / bottom
    face(cv, u + d, v, w, d, p, seed + 1)
    face(cv, u + d + w, v, w, d, p, seed + 2)
    # right, front, left, back
    face(cv, u, v + d, d, h, p, seed + 3)
    face(cv, u + d, v + d, w, h, p, seed + 4)
    face(cv, u + d + w, v + d, d, h, p, seed + 5)
    face(cv, u + d + w + d, v + d, w, h, p, seed + 6)
    if skip_rows_top:
        for y in range(v + d, v + d + skip_rows_top):
            for x in range(u, u + 2 * (d + w)):
                cv.set(x, y, (0, 0, 0), 0)
        for y in range(v, v + d):
            for x in range(u + d, u + d + w):
                cv.set(x, y, (0, 0, 0), 0)


def humanoid(name, p):
    cv = Canvas(64, 32)
    glow, dark, accent = p["glow"], p["dark"], p["accent"]

    # Шлем
    cube(cv, 0, 0, 8, 8, 8, p, 10)
    # визор: прорезь глаз и «челюсть»
    for x in range(9, 15):
        cv.set(x, 11, dark)
    for x in (9, 10, 13, 14):
        cv.set(x, 11, glow)
    if p["style"] == "scrap":
        for x in range(10, 14):
            cv.set(x, 13, dark)
    else:
        for y in range(13, 16):
            cv.set(11, y, dark)
            cv.set(12, y, dark)
    # гребень сверху
    for y in range(0, 8):
        cv.set(11, y, accent)
        cv.set(12, y, accent)

    # Торс
    cube(cv, 16, 16, 8, 12, 4, p, 20)
    # энергоядро на груди
    core = [(23, 23), (24, 23), (23, 24), (24, 24)]
    ring = [(22, 23), (22, 24), (25, 23), (25, 24), (23, 22), (24, 22), (23, 25), (24, 25)]
    for x, y in ring:
        cv.set(x, y, shade(glow, 0.65))
    for x, y in core:
        cv.set(x, y, glow)
    # пояс
    for x in range(20, 28):
        cv.set(x, 30, dark)
    # спина — «ранец» двигателей
    for y in range(22, 30):
        cv.set(34, y, dark)
        cv.set(37, y, dark)

    # Руки (правая и левая используют одну раскладку)
    cube(cv, 40, 16, 4, 12, 4, p, 30)
    # репульсор в ладони (низ руки) и полосы
    for x in range(48, 52):
        for y in range(16, 20):
            cv.set(x, y, shade(glow, 0.7))
    cv.set(49, 17, glow)
    cv.set(50, 18, glow)
    for x in range(40, 56):
        cv.set(x, 26, dark)

    # Ботинки: нижняя часть ног
    cube(cv, 0, 16, 4, 12, 4, p, 40, skip_rows_top=6)
    for x in range(0, 16):
        cv.set(x, 31, dark)
    for x in range(8, 12):
        for y in range(16, 20):
            cv.set(x, y, shade(glow, 0.8))  # сопла в подошве

    cv.save(os.path.join(EQUIP, "humanoid", f"suit_{name}.png"))


def humanoid_leggings(name, p):
    cv = Canvas(64, 32)
    dark, accent = p["dark"], p["accent"]
    # Таз (низ торса)
    cube(cv, 16, 16, 8, 12, 4, p, 50, skip_rows_top=7)
    # Ноги
    cube(cv, 0, 16, 4, 12, 4, p, 60)
    for x in range(0, 16):
        cv.set(x, 26, dark)  # колено
    for y in range(20, 32):
        cv.set(5, y, accent)
    cv.save(os.path.join(EQUIP, "humanoid_leggings", f"suit_{name}.png"))


# ---------------------------------------------------------------- иконки (16×16)

ICONS = {
    "helmet": [
        "................",
        "................",
        "....AAAAAAAA....",
        "...ABBBBBBBBA...",
        "..ABBBBAABBBBA..",
        "..ABBBBAABBBBA..",
        "..ABBBBBBBBBBA..",
        "..ADGGDDDDGGDA..",
        "..ABDDDBBDDDBA..",
        "..ABBBBDDBBBBA..",
        "..ABBBBDDBBBBA..",
        "...ABBBDDBBBA...",
        "....AABBBBAA....",
        "......AAAA......",
        "................",
        "................",
    ],
    "chestplate": [
        "................",
        "..AAAA....AAAA..",
        ".ABBBBAAAABBBBA.",
        ".ABBBBBBBBBBBBA.",
        ".ABBBBBBBBBBBBA.",
        ".AAABBBCCBBBAAA.",
        "...ABBCGGCBBA...",
        "...ABBCGGCBBA...",
        "...ABBBCCBBBA...",
        "...ABBBBBBBBA...",
        "...ABDBBBBDBA...",
        "...ABBDDDDBBA...",
        "...ABBBBBBBBA...",
        "...ADDDDDDDDA...",
        "....AAAAAAAA....",
        "................",
    ],
    "leggings": [
        "................",
        "...AAAAAAAAAA...",
        "...ADDDDDDDDA...",
        "...ABBBBBBBBA...",
        "...ABBBAABBBA...",
        "...ABBBAABBBA...",
        "...ABBA..ABBA...",
        "...ABBA..ABBA...",
        "...ADDA..ADDA...",
        "...ABBA..ABBA...",
        "...ABBA..ABBA...",
        "...ABBA..ABBA...",
        "...ABBA..ABBA...",
        "...AAAA..AAAA...",
        "................",
        "................",
    ],
    "boots": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "..AAAA....AAAA..",
        "..ABBA....ABBA..",
        "..ABBA....ABBA..",
        "..ADDA....ADDA..",
        "..ABBA....ABBA..",
        ".ABBBA...ABBBA..",
        ".ABBBBA..ABBBBA.",
        ".ADDDDA..ADDDDA.",
        ".AGGGGA..AGGGGA.",
        "..AAAA....AAAA..",
        "................",
    ],
}


def draw_icon(rows, colors, path):
    cv = Canvas(16, 16)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in colors:
                cv.set(x, y, colors[ch])
    cv.save(path)


def armor_icons(name, p):
    colors = {
        "A": shade(p["dark"], 0.7),
        "B": p["base"],
        "C": shade(p["glow"], 0.6),
        "D": p["accent"] if p["style"] != "plain" else p["dark"],
        "G": p["glow"],
    }
    for piece, rows in ICONS.items():
        draw_icon(rows, colors, os.path.join(ITEM, f"suit_{name}_{piece}.png"))


# Иконки встроенных систем: символы + своя палитра.
WEAPON_ICONS = {
    "repulsor": (["................",
                  "......AAAA......",
                  "....AABBBBAA....",
                  "...ABBCCCCBBA...",
                  "...ABCCGGCCBA...",
                  "..ABCCGWWGCCBA..",
                  "..ABCGWWWWGCBA..",
                  "..ABCGWWWWGCBA..",
                  "..ABCCGWWGCCBA..",
                  "...ABCCGGCCBA...",
                  "...ABBCCCCBBA...",
                  "....AABBBBAA....",
                  "......AAAA......",
                  "................",
                  "................",
                  "................"], {"A": (40, 50, 60), "B": (120, 130, 145), "C": (60, 160, 210), "G": (130, 230, 255), "W": (240, 252, 255)}),
    "chest_beam": (["................",
                    ".G.....G.....G..",
                    "..G....G....G...",
                    "...G...G...G....",
                    "....AAAAAAA.....",
                    "...ACCCCCCCA....",
                    "GGGACGGWGGCAGGG.",
                    "...ACGWWWGCA....",
                    "GGGACGGWGGCAGGG.",
                    "...ACCCCCCCA....",
                    "....AAAAAAA.....",
                    "...G...G...G....",
                    "..G....G....G...",
                    ".G.....G.....G..",
                    "................",
                    "................"], {"A": (50, 60, 75), "C": (60, 170, 220), "G": (140, 235, 255), "W": (255, 255, 255)}),
    "micro_missiles": (["................",
                        "..W.....W.....W.",
                        ".AWA...AWA...AWA",
                        ".ABA...ABA...ABA",
                        ".ABA...ABA...ABA",
                        ".ABA...ABA...ABA",
                        ".ABA...ABA...ABA",
                        "AABAA.AABAA.AABA",
                        ".FFF...FFF...FFF",
                        "..F.....F.....F.",
                        "..S.....S.....S.",
                        ".S.S...S.S...S.S",
                        "................",
                        "................",
                        "................",
                        "................"], {"A": (60, 60, 70), "B": (200, 205, 215), "W": (230, 60, 50), "F": (255, 170, 50), "S": (150, 150, 150)}),
    "shoulder_guns": (["................",
                       "................",
                       "..........AAAA..",
                       "..........ABBA..",
                       "AAAAAAAAAAABBA..",
                       "BBBBBBBBBBBBBA..",
                       "AAAAAAAAAAABBA..",
                       "BBBBBBBBBBBBBA..",
                       "AAAAAAAAAAABBA..",
                       "..........ABBA..",
                       ".........ACCCCA.",
                       ".........ACCCCA.",
                       ".........AAAAAA.",
                       "................",
                       "................",
                       "................"], {"A": (40, 42, 48), "B": (140, 145, 155), "C": (90, 95, 105)}),
    "cutting_laser": (["................",
                       "...............R",
                       "..............R.",
                       ".............R..",
                       "............R...",
                       "...........R....",
                       "..........R.....",
                       ".........W......",
                       "....AAAAW.......",
                       "...ABBBBA.......",
                       "..ABBBBA........",
                       ".ABBBBA.........",
                       ".ABBBA..........",
                       ".AAAA...........",
                       "................",
                       "................"], {"A": (40, 42, 50), "B": (130, 135, 150), "R": (255, 50, 40), "W": (255, 220, 220)}),
    "flares": (["................",
                "...Y.......Y....",
                "....Y..W..Y.....",
                ".....YWWWY......",
                "..Y..WWOWW..Y...",
                "...YWWOOOWWY....",
                "..WWOOOROOOWW...",
                "...YWWOOOWWY....",
                "..Y..WWOWW..Y...",
                ".....YWWWY......",
                "....Y..W..Y.....",
                "...Y.......Y....",
                "................",
                "................",
                "................",
                "................"], {"Y": (255, 220, 80), "W": (255, 250, 220), "O": (255, 160, 40), "R": (255, 80, 30)}),
    "scanner": (["................",
                 ".....AAAAAA.....",
                 "...AA......AA...",
                 "..A....G.....A..",
                 ".A.....G......A.",
                 ".A....GG..R...A.",
                 "A.....GG.......A",
                 "A.....GGGG.....A",
                 "A..........R...A",
                 ".A............A.",
                 ".A...R........A.",
                 "..A..........A..",
                 "...AA......AA...",
                 ".....AAAAAA.....",
                 "................",
                 "................"], {"A": (80, 220, 160), "G": (150, 255, 200), "R": (255, 90, 80)}),
    "flamethrower": (["................",
                      "...........O.Y..",
                      ".........OOYO.Y.",
                      "........OYYYOO..",
                      ".......OYWWYO.Y.",
                      "AAAAAAAOYWWYOO..",
                      "BBBBBBBCOYYOO.Y.",
                      "AAAAAAACOOOO....",
                      "..ABBA..O.O.....",
                      "..ABBA..........",
                      "..ABBA..........",
                      "..AAAA..........",
                      "................",
                      "................",
                      "................",
                      "................"], {"A": (45, 45, 50), "B": (110, 100, 95), "C": (140, 82, 44), "O": (255, 120, 30), "Y": (255, 200, 60), "W": (255, 245, 210)}),
    "wrist_rocket": (["................",
                      "................",
                      "................",
                      "..........RR....",
                      ".........RBBR...",
                      "........ABBBA...",
                      ".......ABBBA....",
                      "......ABBBA.....",
                      ".....ABBBA......",
                      "....AABBA.......",
                      "...AAAAA........",
                      "..FFAA..........",
                      ".FYF............",
                      "FFF.............",
                      "................",
                      "................"], {"A": (70, 70, 76), "B": (150, 150, 158), "R": (200, 60, 40), "F": (255, 150, 40), "Y": (255, 230, 100)}),
    "pile_driver": (["................",
                     "..AAAAAAAAAAA...",
                     "..ABBBBBBBBBA...",
                     "..ABYYBBYYBBA...",
                     "..ABBBBBBBBBA...",
                     "..AAAAAAAAAAA...",
                     ".....ACCCA......",
                     ".....ACCCA......",
                     ".....ACCCA......",
                     "...AAAAAAAAA....",
                     "...ABBBBBBBA....",
                     "...ABBBBBBBA....",
                     "...AAAAAAAAA....",
                     "..W..W...W..W...",
                     ".W...W...W...W..",
                     "................"], {"A": (35, 35, 40), "B": (230, 130, 35), "Y": (250, 210, 40), "C": (170, 170, 175), "W": (220, 220, 220)}),
    "heavy_rockets": (["................",
                       "..RR......RR....",
                       ".RBBR....RBBR...",
                       ".ABBA....ABBA...",
                       ".ABBA....ABBA...",
                       ".AYYA....AYYA...",
                       ".ABBA....ABBA...",
                       ".ABBA....ABBA...",
                       ".ABBA....ABBA...",
                       "AAAAAA..AAAAAA..",
                       ".FFFF....FFFF...",
                       "..FF......FF....",
                       "..S........S....",
                       ".S.S......S.S...",
                       "................",
                       "................"], {"A": (40, 40, 45), "B": (225, 128, 32), "Y": (250, 210, 40), "R": (60, 60, 65), "F": (255, 160, 50), "S": (150, 150, 150)}),
    "nano_blade": (["...............G",
                    "..............GW",
                    ".............GWG",
                    "............GWG.",
                    "...........GWG..",
                    "..........GWG...",
                    ".........GWG....",
                    "........GWG.....",
                    "...A...GWG......",
                    "...AA.GWG.......",
                    "....AAWG........",
                    ".....AA.........",
                    "....AABA........",
                    "...AA..AA.......",
                    "..AA............",
                    "................"], {"A": (40, 36, 52), "B": (184, 112, 255), "G": (150, 90, 230), "W": (230, 210, 255)}),
    "nano_shield": (["................",
                     "....GGGGGGGG....",
                     "...GPPPPPPPPG...",
                     "..GPPWPPPPWPPG..",
                     "..GPPPPPPPPPPG..",
                     "..GPPPPGGPPPPG..",
                     "..GPPPGWWGPPPG..",
                     "..GPPPGWWGPPPG..",
                     "..GPPPPGGPPPPG..",
                     "...GPPPPPPPPG...",
                     "....GPPPPPPG....",
                     ".....GPPPPG.....",
                     "......GPPG......",
                     ".......GG.......",
                     "................",
                     "................"], {"G": (184, 112, 255), "P": (60, 40, 100), "W": (235, 220, 255)}),
    "nano_cannon": (["................",
                     "................",
                     "............GGG.",
                     "..AAAAAAAAAAGWG.",
                     ".ABBBBBBBBBBGWWG",
                     ".ABGBGBGBGBBGWWG",
                     ".ABBBBBBBBBBGWG.",
                     "..AAAAAAAAAAGGG.",
                     "....ABBA........",
                     "....ABBA........",
                     "....AAAA........",
                     "................",
                     "................",
                     "................",
                     "................",
                     "................"], {"A": (20, 18, 28), "B": (48, 44, 62), "G": (184, 112, 255), "W": (240, 225, 255)}),
}

CORE = (["................",
         "................",
         ".....AAAAAA.....",
         "....ACCCCCCA....",
         "...ACGGGGGGCA...",
         "..ACGGBBBBGGCA..",
         "..ACGBWWWWBGCA..",
         "..ACGBWWWWBGCA..",
         "..ACGBWWWWBGCA..",
         "..ACGBWWWWBGCA..",
         "..ACGGBBBBGGCA..",
         "...ACGGGGGGCA...",
         "....ACCCCCCA....",
         ".....AAAAAA.....",
         "................",
         "................"], {"A": (45, 50, 60), "C": (140, 150, 165), "G": (80, 200, 255), "B": (160, 235, 255), "W": (250, 255, 255)})


def main():
    for name, p in PALETTES.items():
        humanoid(name, p)
        humanoid_leggings(name, p)
        armor_icons(name, p)

    for weapon, (rows, colors) in WEAPON_ICONS.items():
        draw_icon(rows, colors, os.path.join(ITEM, f"suit_{weapon}.png"))

    draw_icon(CORE[0], CORE[1], os.path.join(ITEM, "suit_power_core.png"))
    print("ok")


if __name__ == "__main__":
    main()
