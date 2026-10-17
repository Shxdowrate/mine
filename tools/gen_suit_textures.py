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
    "light": dict(base=(214, 220, 222), dark=(46, 60, 66), accent=(64, 196, 170), glow=(127, 255, 212), style="panels"),
    "singularity": dict(base=(26, 24, 52), dark=(8, 8, 20), accent=(230, 190, 80), glow=(255, 214, 107), style="stars"),
    "archmage": dict(base=(28, 84, 64), dark=(14, 40, 32), accent=(214, 170, 70), glow=(255, 179, 71), style="cloth"),
    "apex": dict(base=(38, 38, 42), dark=(14, 14, 16), accent=(190, 196, 204), glow=(242, 242, 242), style="plain"),
    "miner": dict(base=(230, 190, 40), dark=(40, 40, 44), accent=(64, 64, 70), glow=(255, 240, 180), style="hazard"),
    "builder": dict(base=(240, 130, 50), dark=(60, 60, 64), accent=(235, 235, 235), glow=(255, 210, 120), style="panels"),
    "diver": dict(base=(30, 90, 110), dark=(12, 40, 52), accent=(200, 220, 210), glow=(79, 227, 193), style="panels"),
    "scout": dict(base=(70, 92, 58), dark=(30, 40, 26), accent=(110, 130, 80), glow=(143, 209, 107), style="camo"),
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
        elif style == "stars":
            if rnd.random() < 0.06:
                c = (255, 255, 230) if rnd.random() < 0.5 else glow
            elif (x + y) % 7 == 0:
                c = mix(base, accent, 0.25)
        elif style == "cloth":
            if y % 3 == 0:
                c = shade(base, 0.88)
            if x in (0, 1) or x in (w - 2, w - 1):
                c = shade(accent, n)
        elif style == "camo":
            v = (x * 3 + y * 5 + (x * y) % 7) % 11
            if v < 3:
                c = shade(dark, n * 1.3)
            elif v < 5:
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


CONTAINER_ICONS = {
    "suitcase": (["................",
                  "................",
                  "......AAAA......",
                  "......A..A......",
                  "..AAAAAAAAAAAA..",
                  ".ABBBBBBBBBBBBA.",
                  ".ABBBBBBBBBBBBA.",
                  ".ADDDDDGGDDDDDA.",
                  ".ABBBBBGGBBBBBA.",
                  ".ABBBBBBBBBBBBA.",
                  ".ABBBBBBBBBBBBA.",
                  ".ABBBBBBBBBBBBA.",
                  "..AAAAAAAAAAAA..",
                  "................",
                  "................",
                  "................"], {"A": (40, 48, 52), "B": (200, 206, 210), "D": (64, 196, 170), "G": (230, 255, 245)}),
    "nano_capsule": (["................",
                      "................",
                      "......AAAA......",
                      ".....ABBBBA.....",
                      "....ABGGGGBA....",
                      "...ABGPPPPGBA...",
                      "...ABGPWWPGBA...",
                      "...ABGPWWPGBA...",
                      "...ABGPPPPGBA...",
                      "....ABGGGGBA....",
                      ".....ABBBBA.....",
                      "......AAAA......",
                      "................",
                      "................",
                      "................",
                      "................"], {"A": (14, 14, 20), "B": (40, 38, 52), "G": (140, 90, 220), "P": (184, 112, 255), "W": (240, 228, 255)}),
    "titan_beacon": ([".......W........",
                      ".......Y........",
                      "......AYA.......",
                      "......ABA.......",
                      ".....ABBBA......",
                      ".....ABYBA......",
                      ".....ABBBA......",
                      ".....ABYBA......",
                      ".....ABBBA......",
                      "....ABBBBBA.....",
                      "....ADDDDDA.....",
                      "...ADD.D.DDA....",
                      "...AD.....DA....",
                      "...A.......A....",
                      "................",
                      "................"], {"A": (35, 35, 40), "B": (228, 128, 32), "D": (70, 70, 76), "Y": (255, 224, 80), "W": (255, 255, 230)}),
}


def block_textures():
    blk = os.path.join(ROOT, "block")
    rnd = random.Random(7)
    steel, dark, glow = (120, 126, 136), (52, 56, 64), (80, 222, 255)

    def plate(c, n=0.06):
        return shade(c, rnd.uniform(1 - n, 1 + n))

    # Станция: верх — платформа с отметкой для ног, бок — стойка с манипуляторами, низ — основание.
    cv = Canvas(16, 16)
    cv.rect(0, 0, 16, 16, lambda x, y, w, h: dark if x in (0, 15) or y in (0, 15) else plate(steel))
    for x, y in [(5, 5), (6, 5), (9, 5), (10, 5), (5, 10), (6, 10), (9, 10), (10, 10)]:
        cv.set(x, y, glow)
    for i in range(3, 13):
        cv.set(i, 3, shade(glow, 0.6)); cv.set(i, 12, shade(glow, 0.6))
    cv.save(os.path.join(blk, "suit_station_top.png"))

    for name, loaded in (("suit_station_side", False), ("suit_station_side_loaded", True)):
        cv = Canvas(16, 16)
        cv.rect(0, 0, 16, 16, lambda x, y, w, h: dark if x in (0, 15) or y in (0, 15) else plate(shade(steel, 0.8)))
        for y in range(2, 14):
            cv.set(2, y, steel); cv.set(13, y, steel)  # стойки
        for x in range(2, 14):
            cv.set(x, 2, steel)
        if loaded:
            # силуэт костюма на станции
            sil = ["..AAAA..", "..AGGA..", "..AAAA..", "AAAAAAAA", "A.ACCA.A", "A.AAAA.A", "..A..A..", "..A..A..", "..A..A..", ".AA..AA."]
            for yy, row in enumerate(sil):
                for xx, ch in enumerate(row):
                    if ch == "A":
                        cv.set(4 + xx, 3 + yy, (60, 90, 170))
                    elif ch == "G":
                        cv.set(4 + xx, 3 + yy, glow)
                    elif ch == "C":
                        cv.set(4 + xx, 3 + yy, glow)
        else:
            for x in range(5, 11):
                cv.set(x, 13, shade(glow, 0.5))
        cv.save(os.path.join(blk, name + ".png"))

    cv = Canvas(16, 16)
    cv.rect(0, 0, 16, 16, lambda x, y, w, h: dark if (x + y) % 5 == 0 else plate(shade(steel, 0.6)))
    cv.save(os.path.join(blk, "suit_station_bottom.png"))

    # Капсула «Титана»: оранжево-чёрная, со светящимися полосами.
    orange, black, yellow = (228, 128, 32), (36, 36, 42), (255, 224, 80)
    cv = Canvas(16, 16)
    cv.rect(0, 0, 16, 16, lambda x, y, w, h: black if x in (0, 15) or y in (0, 15) or (y >= 12 and (x + y) % 4 < 2) else plate(orange))
    for y in range(2, 11):
        cv.set(7, y, yellow); cv.set(8, y, yellow)
    cv.save(os.path.join(blk, "titan_pod_side.png"))
    cv = Canvas(16, 16)
    cv.rect(0, 0, 16, 16, lambda x, y, w, h: black if x in (0, 15) or y in (0, 15) or abs(x - 7.5) + abs(y - 7.5) > 9 else plate(orange))
    for x, y in [(7, 7), (8, 7), (7, 8), (8, 8)]:
        cv.set(x, y, yellow)
    cv.save(os.path.join(blk, "titan_pod_top.png"))


def icon_canvas():
    return Canvas(16, 16)


def disc(cv, cx, cy, r, c, ring=None):
    for y in range(16):
        for x in range(16):
            d = ((x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2) ** 0.5
            if d <= r:
                cv.set(x, y, c)
            elif ring and d <= r + 1:
                cv.set(x, y, ring)


def seg(cv, x0, y0, x1, y1, c):
    n = max(abs(x1 - x0), abs(y1 - y0), 1)
    for i in range(n + 1):
        cv.set(round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n), c)


def core_icon(color, inner):
    cv = icon_canvas()
    disc(cv, 8, 8, 6, (30, 28, 50), (230, 190, 80))
    disc(cv, 8, 8, 4, color)
    disc(cv, 8, 8, 1.6, inner)
    return cv


def ability_icons():
    W = (255, 255, 255)
    icons = {}
    # Ядра «Сингулярности»: одна палитра, разные символы.
    G = (127, 227, 255)
    cv = core_icon((20, 40, 70), (20, 40, 70))
    for x in (6, 10):
        seg(cv, x, 5, x, 10, G); cv.set(x - 1, 9, G); cv.set(x + 1, 9, G)
    icons["gravity_grip"] = cv
    cv = core_icon((20, 40, 70), (20, 40, 70))
    seg(cv, 5, 5, 11, 5, G); seg(cv, 5, 11, 11, 11, G); seg(cv, 5, 5, 11, 11, G); seg(cv, 11, 5, 5, 11, G)
    icons["time_dilation"] = cv
    cv = core_icon((20, 40, 70), (20, 40, 70))
    seg(cv, 8, 4, 12, 8, G); seg(cv, 12, 8, 8, 12, G); seg(cv, 8, 12, 4, 8, G); seg(cv, 4, 8, 8, 4, G)
    icons["space_jump"] = cv
    cv = core_icon((20, 40, 70), (20, 40, 70))
    for x in range(5, 11):
        cv.set(x, 5, G); cv.set(x, 10, G)
    for y in range(5, 11):
        cv.set(5, y, G); cv.set(10, y, G)
    icons["matter_shift"] = cv
    cv = core_icon((20, 40, 70), (20, 40, 70))
    seg(cv, 4, 8, 6, 6, G); seg(cv, 6, 6, 10, 6, G); seg(cv, 10, 6, 12, 8, G); seg(cv, 4, 8, 6, 10, G); seg(cv, 6, 10, 10, 10, G); seg(cv, 10, 10, 12, 8, G)
    disc(cv, 8, 8, 1, G)
    icons["mind_link"] = cv
    cv = core_icon((20, 40, 70), (20, 40, 70))
    seg(cv, 8, 4, 8, 12, G); seg(cv, 4, 8, 12, 8, G); seg(cv, 7, 4, 7, 12, G); seg(cv, 4, 7, 12, 7, G)
    icons["life_surge"] = cv
    cv = icon_canvas()
    disc(cv, 8, 8, 6.5, (10, 10, 16), (255, 214, 107))
    for a in range(8):
        import math
        seg(cv, 8, 8, round(8 + math.cos(a * math.pi / 4) * 6), round(8 + math.sin(a * math.pi / 4) * 6), (255, 240, 200))
    disc(cv, 8, 8, 2, W)
    icons["collapse"] = cv
    # маг
    cv = icon_canvas()
    seg(cv, 2, 13, 13, 2, (255, 179, 71)); seg(cv, 3, 13, 14, 2, (255, 220, 150)); disc(cv, 13, 3, 1.6, W)
    icons["mystic_bolt"] = cv
    cv = icon_canvas()
    for y in range(16):
        for x in range(16):
            d = (((x + 0.5 - 8) / 5) ** 2 + ((y + 0.5 - 8) / 7) ** 2) ** 0.5
            if 0.75 <= d <= 1.0:
                cv.set(x, y, (255, 140, 26) if (x + y) % 2 else (255, 200, 90))
    icons["portal"] = cv
    cv = icon_canvas()
    disc(cv, 8, 8, 6, (90, 60, 10), (255, 179, 71))
    disc(cv, 8, 8, 3, (255, 179, 71))
    seg(cv, 8, 2, 8, 14, (255, 230, 170)); seg(cv, 2, 8, 14, 8, (255, 230, 170))
    icons["rune_shield"] = cv
    cv = icon_canvas()
    disc(cv, 8, 5, 3, (120, 220, 255)); seg(cv, 8, 8, 8, 13, (120, 220, 255)); seg(cv, 5, 10, 11, 10, (120, 220, 255))
    seg(cv, 8, 13, 5, 15, (120, 220, 255)); seg(cv, 8, 13, 11, 15, (120, 220, 255))
    icons["astral_form"] = cv
    cv = icon_canvas()
    disc(cv, 8, 8, 6, (30, 60, 40), (255, 179, 71))
    seg(cv, 8, 8, 8, 4, W); seg(cv, 8, 8, 11, 9, W)
    seg(cv, 2, 4, 4, 2, (255, 179, 71))
    icons["time_loop"] = cv
    # апекс
    cv = icon_canvas()
    disc(cv, 5, 6, 1.6, (255, 60, 30)); disc(cv, 11, 6, 1.6, (255, 60, 30))
    seg(cv, 5, 7, 2, 15, (255, 80, 40)); seg(cv, 11, 7, 14, 15, (255, 80, 40))
    icons["heat_vision"] = cv
    cv = icon_canvas()
    for r in (3, 5, 7):
        for y in range(16):
            for x in range(16):
                d = ((x - 2) ** 2 + (y - 8) ** 2) ** 0.5
                if abs(d - r * 1.6) < 0.6 and x > 3:
                    cv.set(x, y, (220, 230, 255))
    disc(cv, 2, 8, 1.8, W)
    icons["sonic_clap"] = cv
    cv = icon_canvas()
    for x, y in [(4, 6), (6, 5), (8, 5), (10, 6), (4, 7), (11, 7), (4, 8), (11, 8), (5, 9), (10, 9), (6, 10), (9, 10), (6, 11), (9, 11)]:
        cv.set(x, y, (230, 200, 170))
    seg(cv, 5, 6, 5, 4, (230, 200, 170)); seg(cv, 7, 5, 7, 3, (230, 200, 170)); seg(cv, 9, 5, 9, 3, (230, 200, 170))
    icons["super_grab"] = cv
    # рабочие
    cv = icon_canvas()
    for i in range(6):
        seg(cv, 3 + i, 12 - i, 6 + i, 12 - i, (200, 200, 210) if i % 2 else (150, 150, 160))
    seg(cv, 2, 13, 4, 11, (255, 210, 60)); disc(cv, 12, 4, 1.5, (255, 240, 180))
    icons["drill"] = cv
    cv = icon_canvas()
    for x in range(16):
        y = 8 + round(4 * __import__("math").sin(x * 1.1)) * (1 if 3 < x < 13 else 0)
        cv.set(x, y, (255, 210, 60))
    disc(cv, 8, 13, 1.5, (92, 242, 255))
    icons["seismic"] = cv
    cv = icon_canvas()
    disc(cv, 8, 8, 3, (255, 250, 210), (255, 220, 120))
    for d in [(0, -6), (0, 6), (-6, 0), (6, 0), (-4, -4), (4, 4), (-4, 4), (4, -4)]:
        cv.set(8 + d[0], 8 + d[1], (255, 240, 180))
    icons["light_flare"] = cv
    brick = (190, 100, 70)
    cv = icon_canvas()
    for y in range(4, 13):
        for x in range(2, 14):
            cv.set(x, y, (120, 60, 40) if (y % 3 == 0 or (x + (y // 3) * 2) % 4 == 0) else brick)
    icons["build_wall"] = cv
    cv = icon_canvas()
    for x in range(1, 15):
        cv.set(x, 10, brick); cv.set(x, 11, (120, 60, 40))
    for x in (3, 12):
        seg(cv, x, 12, x, 15, (120, 120, 130))
    seg(cv, 2, 7, 13, 7, (255, 138, 61)); cv.set(12, 6, (255, 138, 61)); cv.set(12, 8, (255, 138, 61))
    icons["build_bridge"] = cv
    cv = icon_canvas()
    for y in range(6, 12):
        for x in range(1 + (11 - y), 15 - (11 - y) // 2):
            cv.set(x, y, brick if (x + y) % 3 else (120, 60, 40))
    icons["build_floor"] = cv
    cv = icon_canvas()
    for x, y in [(3, 3), (6, 4), (11, 3), (4, 9), (12, 10), (8, 12), (2, 13), (13, 6)]:
        cv.set(x, y, brick); cv.set(x + 1, y, (120, 60, 40))
    disc(cv, 8, 8, 2.5, (255, 200, 80))
    icons["demolish"] = cv
    cv = icon_canvas()
    for y in range(16):
        for x in range(16):
            if (x + y) % 3 == 0 and abs(x - 8) + abs(y - 8) < 8:
                cv.set(x, y, (143, 209, 107))
    disc(cv, 6, 7, 1, W); disc(cv, 10, 7, 1, W)
    icons["cloak"] = cv
    cv = icon_canvas()
    seg(cv, 2, 14, 12, 4, (180, 180, 190)); seg(cv, 12, 4, 14, 2, (220, 220, 230)); seg(cv, 12, 4, 10, 2, (220, 220, 230)); seg(cv, 12, 4, 14, 6, (220, 220, 230))
    icons["grapple"] = cv
    for name, cv in icons.items():
        cv.save(os.path.join(ITEM, f"suit_{name}.png"))


def main():
    for name, p in PALETTES.items():
        humanoid(name, p)
        humanoid_leggings(name, p)
        armor_icons(name, p)

    for weapon, (rows, colors) in WEAPON_ICONS.items():
        draw_icon(rows, colors, os.path.join(ITEM, f"suit_{weapon}.png"))

    draw_icon(CORE[0], CORE[1], os.path.join(ITEM, "suit_power_core.png"))

    for name, (rows, colors) in CONTAINER_ICONS.items():
        draw_icon(rows, colors, os.path.join(ITEM, f"{name}.png"))

    block_textures()
    ability_icons()
    print("ok")


if __name__ == "__main__":
    main()
