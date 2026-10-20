#!/usr/bin/env python3
"""Текстуры War Machines (Solo Rust): костюмы WARDEN, иконки их способностей, планшет, яйцо призыва и скины бандитов.

Запуск из корня репозитория:  python3 tools/gen_war_textures.py
Палитры костюмов — в WARDEN; поменял цвета, запустил, пересобрал мод.
Использует рисовалку брони из gen_suit_textures.py.
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))
import gen_suit_textures as g  # noqa: E402

ROOT = g.ROOT
ITEM = g.ITEM
ENTITY = os.path.join(ROOT, "entity", "bandit")

# base, dark, accent, glow, style
WARDEN = {
    # Common
    "w_hot_rod": dict(base=(178, 34, 34), dark=(70, 14, 14), accent=(232, 182, 60), glow=(255, 214, 120), style="panels"),
    "w_steel": dict(base=(150, 156, 164), dark=(70, 74, 82), accent=(190, 196, 204), glow=(200, 235, 255), style="plain"),
    "w_eco": dict(base=(92, 170, 92), dark=(36, 72, 40), accent=(210, 232, 190), glow=(180, 255, 160), style="panels"),
    "w_carbon": dict(base=(48, 50, 54), dark=(20, 20, 24), accent=(80, 84, 90), glow=(160, 200, 255), style="circuit"),
    "w_navy": dict(base=(40, 66, 130), dark=(18, 28, 60), accent=(220, 224, 232), glow=(110, 170, 255), style="panels"),
    "w_desert": dict(base=(196, 168, 112), dark=(110, 88, 56), accent=(150, 124, 80), glow=(255, 226, 160), style="camo"),
    "w_arctic": dict(base=(226, 234, 240), dark=(130, 146, 160), accent=(186, 204, 220), glow=(170, 230, 255), style="camo"),
    # Uncommon
    "w_disco": dict(base=(150, 60, 190), dark=(50, 20, 70), accent=(255, 110, 220), glow=(255, 255, 140), style="disco"),
    "w_swamp_raider": dict(base=(86, 100, 52), dark=(38, 44, 22), accent=(130, 110, 60), glow=(190, 255, 110), style="camo"),
    "w_woodlands": dict(base=(80, 110, 64), dark=(44, 56, 32), accent=(110, 84, 52), glow=(160, 230, 120), style="camo"),
    # Rare
    "w_soldier": dict(base=(108, 120, 72), dark=(48, 54, 32), accent=(150, 160, 110), glow=(220, 240, 150), style="panels"),
    "w_barrage": dict(base=(220, 112, 40), dark=(60, 44, 36), accent=(250, 210, 60), glow=(255, 190, 90), style="hazard"),
    "w_bumble_bee": dict(base=(242, 200, 40), dark=(28, 28, 30), accent=(28, 28, 30), glow=(140, 220, 255), style="stripes"),
    "w_mountain": dict(base=(140, 128, 118), dark=(70, 62, 58), accent=(200, 196, 190), glow=(255, 236, 200), style="rock"),
    "w_chroma": dict(base=(90, 220, 220), dark=(30, 40, 60), accent=(255, 120, 200), glow=(255, 255, 255), style="chroma"),
    # Epic
    "w_shemagh": dict(base=(204, 182, 140), dark=(90, 74, 50), accent=(60, 60, 64), glow=(255, 230, 170), style="cloth"),
    "w_night_ops": dict(base=(34, 38, 36), dark=(12, 14, 12), accent=(56, 62, 58), glow=(110, 255, 140), style="circuit"),
    "w_deadeye": dict(base=(44, 40, 44), dark=(16, 14, 16), accent=(200, 30, 30), glow=(255, 70, 70), style="panels"),
    "w_cryo": dict(base=(150, 210, 236), dark=(40, 80, 110), accent=(236, 248, 255), glow=(140, 245, 255), style="frost"),
    # Legendary
    "w_arsenal": dict(base=(92, 98, 106), dark=(32, 34, 38), accent=(150, 156, 164), glow=(255, 240, 200), style="scrap"),
    "w_cupid": dict(base=(250, 160, 196), dark=(150, 50, 90), accent=(255, 236, 244), glow=(255, 90, 150), style="hearts"),
    # Mythic
    "w_trickster": dict(base=(30, 110, 60), dark=(12, 40, 22), accent=(214, 176, 60), glow=(120, 255, 150), style="cloth"),
    "w_portalmancer": dict(base=(60, 30, 90), dark=(20, 10, 34), accent=(255, 150, 40), glow=(255, 170, 60), style="runes"),
    "w_infinity": dict(base=(200, 160, 40), dark=(90, 60, 10), accent=(130, 40, 160), glow=(255, 240, 140), style="stars"),
    "w_wraith": dict(base=(40, 32, 60), dark=(12, 8, 20), accent=(110, 90, 170), glow=(170, 140, 255), style="cracks"),
}

# ---------------------------------------------------------------- дополнительные узоры брони

_base_surface = g.surface


def surface(p, seed):
    style = p["style"]
    if style not in ("disco", "stripes", "rock", "chroma", "frost", "hearts", "runes"):
        return _base_surface(p, seed)

    rnd = random.Random(seed)
    base, dark, accent, glow = p["base"], p["dark"], p["accent"], p["glow"]

    def fn(x, y, w, h):
        n = rnd.uniform(0.9, 1.06)
        c = g.shade(base, n)
        if style == "disco":
            tile = ((x // 2) + (y // 2) * 3 + seed) % 5
            c = [base, accent, glow, g.mix(base, accent, 0.5), g.shade(base, 1.2)][tile]
            if (x + y) % 2 == 0:
                c = g.shade(c, 0.92)
        elif style == "stripes":
            if (y // 2) % 2 == 1:
                c = g.shade(accent, n)
        elif style == "rock":
            if rnd.random() < 0.2:
                c = g.shade(dark, rnd.uniform(0.9, 1.3))
            elif rnd.random() < 0.1:
                c = g.shade(accent, n)
        elif style == "chroma":
            t = ((x + y * 2 + seed) % 24) / 24.0
            r = int(127 + 127 * math.sin(t * 6.283))
            gg = int(127 + 127 * math.sin(t * 6.283 + 2.09))
            b = int(127 + 127 * math.sin(t * 6.283 + 4.19))
            c = g.mix((r, gg, b), base, 0.35)
        elif style == "frost":
            if (x * 3 + y * 5) % 9 == 0:
                c = accent
            elif y % 4 == 3:
                c = g.shade(base, 0.85)
        elif style == "hearts":
            if (x % 6, y % 6) in ((1, 1), (3, 1), (1, 2), (2, 2), (3, 2), (2, 3)):
                c = glow
            elif y % 6 == 5:
                c = g.shade(base, 0.92)
        elif style == "runes":
            if (x * 7 + y * 11) % 13 == 0 or ((x + y) % 8 == 0 and y % 3 == 0):
                c = g.mix(accent, glow, rnd.uniform(0.3, 0.9))
        edge = x == 0 or y == 0 or x == w - 1 or y == h - 1
        if edge:
            c = g.shade(dark, 1.0)
        return c

    return fn


g.surface = surface

# ---------------------------------------------------------------- иконки способностей


def weapon_icons():
    icons = {}

    # Наручная винтовка: ствол на запястье.
    cv = g.icon_canvas()
    M, D, Y = (150, 156, 164), (52, 54, 60), (255, 220, 120)
    for x in range(2, 12):
        cv.set(x, 8, M)
        cv.set(x, 9, D)
    for x in range(3, 8):
        for y in range(10, 13):
            cv.set(x, y, D)
    cv.set(12, 8, Y); cv.set(13, 8, Y); cv.set(14, 7, Y); cv.set(14, 9, Y)
    icons["wrist_rifle"] = cv

    # Меткий выстрел: прицел.
    cv = g.icon_canvas()
    R = (230, 40, 40)
    for a in range(32):
        cv.set(round(8 + math.cos(a / 32 * 6.283) * 6), round(8 + math.sin(a / 32 * 6.283) * 6), (220, 220, 220))
    g.seg(cv, 8, 1, 8, 5, R); g.seg(cv, 8, 11, 8, 15, R); g.seg(cv, 1, 8, 5, 8, R); g.seg(cv, 11, 8, 15, 8, R)
    cv.set(8, 8, R)
    icons["marksman"] = cv

    # Криоракета: снежинка.
    cv = g.icon_canvas()
    C, W = (120, 220, 255), (240, 252, 255)
    for k in range(3):
        a = k * math.pi / 3
        g.seg(cv, round(8 - math.cos(a) * 6), round(8 - math.sin(a) * 6), round(8 + math.cos(a) * 6), round(8 + math.sin(a) * 6), C)
    g.disc(cv, 8, 8, 1.5, W)
    icons["cryo_missile"] = cv

    # Нано-стрелы: стрела с сердцем.
    cv = g.icon_canvas()
    P, H = (255, 120, 170), (255, 60, 120)
    g.seg(cv, 2, 13, 12, 3, P)
    for x, y in ((12, 3), (11, 3), (12, 4), (10, 3), (12, 5)):
        cv.set(x, y, (255, 230, 240))
    for x, y in ((3, 10), (4, 10), (5, 10), (2, 11), (3, 11), (4, 11), (5, 11), (6, 11), (3, 12), (4, 12), (5, 12), (4, 13)):
        cv.set(x, y, H)
    icons["love_arrow"] = cv

    # Двойник: два силуэта.
    cv = g.icon_canvas()
    for ox, col in ((3, (60, 160, 90)), (8, (140, 255, 170))):
        g.disc(cv, ox + 2, 4, 1.6, col)
        for y in range(6, 12):
            for x in range(ox, ox + 5):
                cv.set(x, y, col)
        for y in range(12, 15):
            cv.set(ox + 1, y, col)
            cv.set(ox + 3, y, col)
    icons["decoy"] = cv

    # Вытягивание душ: фиолетовый вихрь.
    cv = g.icon_canvas()
    V, L = (110, 80, 200), (200, 180, 255)
    for i in range(40):
        t = i / 40 * 4 * math.pi
        r = 1 + i / 40 * 6
        cv.set(round(8 + math.cos(t) * r), round(8 + math.sin(t) * r), V if i % 3 else L)
    g.disc(cv, 8, 8, 1.2, L)
    icons["soul_drain"] = cv

    for name, cv in icons.items():
        cv.save(os.path.join(ITEM, f"suit_{name}.png"))


def tablet_icon():
    cv = g.Canvas(16, 16)
    frame, screen, glow = (50, 56, 64), (18, 34, 52), (63, 169, 245)
    for y in range(2, 14):
        for x in range(3, 13):
            cv.set(x, y, frame)
    for y in range(3, 12):
        for x in range(4, 12):
            cv.set(x, y, screen)
    # «Шлем» WARDEN на экране.
    for x, y in ((6, 5), (7, 5), (8, 5), (9, 5), (5, 6), (10, 6), (5, 7), (10, 7), (6, 8), (9, 8), (7, 9), (8, 9)):
        cv.set(x, y, glow)
    cv.set(6, 7, (255, 255, 255)); cv.set(9, 7, (255, 255, 255))
    cv.set(8, 12, (120, 128, 136))
    cv.save(os.path.join(ITEM, "warden_tablet.png"))


def spawn_egg_icon():
    cv = g.Canvas(16, 16)
    base, spot = (92, 98, 60), (40, 40, 40)
    for y in range(16):
        for x in range(16):
            dx, dy = (x - 7.5) / 5.2, (y - 8.5) / 6.6
            if dx * dx + dy * dy <= 1.0:
                c = base
                if (x * 3 + y * 5) % 7 == 0 or (x + y) % 9 == 0:
                    c = spot
                if dx * dx + dy * dy > 0.8:
                    c = g.shade(base, 0.7)
                cv.set(x, y, c)
    cv.save(os.path.join(ITEM, "bandit_spawn_egg.png"))


# ---------------------------------------------------------------- скины бандитов (64×64, раскладка игрока)

SKIN = (198, 150, 110)
SKIN_DARK = (160, 112, 80)

BANDITS = {
    "rifleman": dict(jacket=(86, 96, 56), pants=(110, 100, 70), boots=(60, 44, 30), head="balaclava", mask=(30, 30, 32), extra=(150, 140, 100)),
    "shotgunner": dict(jacket=(104, 64, 40), pants=(52, 62, 92), boots=(48, 34, 24), head="bandana", mask=(170, 30, 30), extra=(70, 44, 26)),
    "sniper": dict(jacket=(64, 84, 44), pants=(64, 84, 44), boots=(44, 40, 30), head="ghillie", mask=(50, 70, 36), extra=(96, 110, 60)),
    "grenadier": dict(jacket=(170, 150, 104), pants=(140, 120, 84), boots=(70, 56, 40), head="gasmask", mask=(70, 74, 70), extra=(110, 90, 60)),
    "machine_gunner": dict(jacket=(56, 58, 62), pants=(44, 46, 50), boots=(30, 30, 32), head="goggles", mask=(24, 24, 26), extra=(170, 30, 30)),
    "officer": dict(jacket=(32, 32, 36), pants=(40, 40, 44), boots=(18, 18, 20), head="beret", mask=(160, 28, 34), extra=(220, 180, 60)),
}


def noisy(rnd, c, k=0.08):
    n = rnd.uniform(1 - k, 1 + k)
    return g.shade(c, n)


def bandit_skin(name, s):
    rnd = random.Random(hash(name) & 0xFFFF)
    cv = g.Canvas(64, 64)
    jacket, pants, boots, mask, extra = s["jacket"], s["pants"], s["boots"], s["mask"], s["extra"]

    def fill_cube(u, v, w, h, d, color_fn):
        rects = [
            (u + d, v, w, d), (u + d + w, v, w, d),
            (u, v + d, d, h), (u + d, v + d, w, h), (u + d + w, v + d, d, h), (u + d + w + d, v + d, w, h),
        ]
        for i, (x0, y0, ww, hh) in enumerate(rects):
            for y in range(y0, y0 + hh):
                for x in range(x0, x0 + ww):
                    cv.set(x, y, color_fn(i, x - x0, y - y0, ww, hh))

    # ---- голова (0,0), 8×8×8; лицо — грань 3 (u+d, v+d)
    style = s["head"]

    def head(face, x, y, w, h):
        hair = (52, 38, 28)
        if style in ("balaclava", "goggles"):
            c = noisy(rnd, mask, 0.05)
            if face == 3 and y in (3, 4) and 1 <= x <= 6:
                c = SKIN if style == "balaclava" else (200, 40, 40)
                if style == "balaclava" and x in (2, 5) and y == 4:
                    c = (30, 30, 30)
                if style == "goggles" and x in (1, 6):
                    c = (40, 40, 40)
            return c
        if style == "ghillie":
            c = noisy(rnd, mask if rnd.random() < 0.6 else extra, 0.15)
            if face == 3 and 2 <= y <= 5 and 1 <= x <= 6:
                c = (40, 54, 30) if rnd.random() < 0.4 else SKIN_DARK
                if y == 3 and x in (2, 5):
                    c = (20, 20, 20)
            return c
        if style == "gasmask":
            if face in (0,):
                return noisy(rnd, extra, 0.08)
            c = noisy(rnd, extra, 0.06)
            if face == 3:
                c = noisy(rnd, mask, 0.05)
                if y == 3 and x in (1, 2, 5, 6):
                    c = (150, 200, 190)
                if y >= 5 and 3 <= x <= 4:
                    c = (40, 40, 40)
            return c
        # bandana / beret: лицо открыто
        c = SKIN
        if face == 0 or y < 2:
            c = hair
        if style == "beret" and (face == 0 or y < 2):
            c = noisy(rnd, mask, 0.05)
        if face == 3:
            if y == 3 and x in (2, 5):
                c = (40, 30, 26)
            if style == "bandana" and y >= 5:
                c = noisy(rnd, mask, 0.06)
            if style == "beret" and y == 5 and 2 <= x <= 5:
                c = hair  # усы
        return c

    fill_cube(0, 0, 8, 8, 8, head)

    # ---- торс (16,16) 8×12×4
    def body(face, x, y, w, h):
        c = noisy(rnd, jacket, 0.07)
        if face == 3:
            if y == 11:
                c = g.shade(pants, 0.7)  # ремень
            if style == "goggles" and 1 <= y <= 8 and 1 <= x <= 6:
                c = noisy(rnd, (40, 42, 46), 0.05)  # бронежилет
                if y == 2 and x in (2, 5):
                    c = (90, 92, 96)
            if name == "officer" and x in (3, 4):
                c = g.shade(jacket, 1.6)  # рубашка
                if x == 4 and 3 <= y <= 7:
                    c = (140, 24, 30)  # галстук
            if name == "grenadier" and (x + y) % 5 == 0 and 3 <= y <= 9:
                c = (70, 90, 60)  # гранаты на разгрузке
            if name == "rifleman" and 2 <= y <= 5 and x in (1, 2, 5, 6):
                c = noisy(rnd, extra, 0.05)  # подсумки
        if name == "sniper" and rnd.random() < 0.25:
            c = noisy(rnd, extra, 0.2)
        return c

    fill_cube(16, 16, 8, 12, 4, body)

    # ---- руки (40,16) 4×12×4
    def arm(face, x, y, w, h):
        c = noisy(rnd, jacket, 0.07)
        if face == 1 or y >= 10:
            c = SKIN_DARK if name not in ("machine_gunner", "officer", "grenadier") else (30, 30, 30)
        if name == "officer" and face != 1 and y == 0:
            c = extra  # погоны
        if name == "sniper" and rnd.random() < 0.25:
            c = noisy(rnd, extra, 0.2)
        return c

    fill_cube(40, 16, 4, 12, 4, arm)

    # ---- ноги (0,16) 4×12×4
    def leg(face, x, y, w, h):
        c = noisy(rnd, pants, 0.07)
        if y >= 9 or face == 1:
            c = noisy(rnd, boots, 0.05)
        if name == "sniper" and rnd.random() < 0.2:
            c = noisy(rnd, extra, 0.2)
        return c

    fill_cube(0, 16, 4, 12, 4, leg)

    # Нижняя половина (раскладка новых скинов) — копия левых конечностей, на случай модели игрока.
    for y in range(16, 32):
        for x in range(0, 16):
            cv.px[x + 16, y + 32] = cv.px[x, y]
        for x in range(40, 56):
            cv.px[x - 8, y + 32] = cv.px[x, y]

    cv.save(os.path.join(ENTITY, f"{name}.png"))


def main():
    for name, p in WARDEN.items():
        g.humanoid(name, p)
        g.humanoid_leggings(name, p)
        g.armor_icons(name, p)

    weapon_icons()
    tablet_icon()
    spawn_egg_icon()

    for name, s in BANDITS.items():
        bandit_skin(name, s)

    print("ok")


if __name__ == "__main__":
    main()
