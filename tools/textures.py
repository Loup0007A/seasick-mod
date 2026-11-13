#!/usr/bin/env python3
"""Textures du mod, dessinées en code : python3 tools/textures.py
- assets/seasick/textures/gui/faces.png : les 5 visages de la jauge (16×16 chacun)
- assets/seasick/textures/gui/vignette.png : bords sombres (blanc + alpha, teinté en jeu)
- seasick_logo.png (liste des mods) et curseforge/avatar.png (400×400)"""
import math, os
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, 'src', 'main', 'resources')
GUI = os.path.join(RES, 'assets', 'seasick', 'textures', 'gui')
os.makedirs(GUI, exist_ok=True)

def hexc(h, a=255):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255, a)

OUT = hexc(0x3B2A20); INK = hexc(0x2A1E16); SWEAT = hexc(0x8FD3FF); BLUSH = hexc(0xF29A8A)
SKINS = [0xF2C49B, 0xE6D4A6, 0xCFE0A8, 0xAFD08A, 0x8DBF6A]

def face(stage):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    skin = hexc(SKINS[stage]); dark = hexc(int(SKINS[stage] * 1) & 0xFFFFFF)
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if d <= 6.6: px[x, y] = skin
            elif d <= 7.5: px[x, y] = OUT
    def p(x, y, c): px[x, y] = c
    shade = tuple(max(0, v - 38) for v in skin[:3]) + (255,)
    if stage == 0:
        for x, y in [(5, 5), (5, 6), (10, 5), (10, 6)]: p(x, y, INK)
        for x, y in [(4, 9), (5, 10), (6, 11), (7, 11), (8, 11), (9, 11), (10, 10), (11, 9)]: p(x, y, INK)
        p(3, 8, BLUSH); p(12, 8, BLUSH)
    elif stage == 1:
        for x in (4, 5, 6): p(x, 6, INK)
        for x in (9, 10, 11): p(x, 6, INK)
        for x in range(6, 10): p(x, 11, INK)
    elif stage == 2:
        for x, y in [(5, 6), (10, 6)]: p(x, y, INK)
        for x, y in [(4, 5), (11, 5)]: p(x, y, INK)
        for x, y in [(5, 11), (6, 10), (7, 11), (8, 10), (9, 11), (10, 10)]: p(x, y, INK)
        for x, y in [(13, 3), (13, 4), (12, 5)]: p(x, y, SWEAT)
    elif stage == 3:
        for x, y in [(4, 5), (5, 6), (6, 5), (9, 5), (10, 6), (11, 5)]: p(x, y, INK)
        for x, y in [(3, 9), (4, 9), (3, 10), (4, 10), (11, 9), (12, 9), (11, 10), (12, 10)]: p(x, y, shade)
        for x, y in [(6, 11), (7, 10), (8, 11), (9, 10)]: p(x, y, INK)
        for x, y in [(13, 2), (13, 3), (12, 4), (2, 4), (2, 5)]: p(x, y, SWEAT)
    else:
        for x, y in [(4, 4), (6, 6), (6, 4), (4, 6), (5, 5), (9, 4), (11, 6), (11, 4), (9, 6), (10, 5)]: p(x, y, INK)
        for x in range(2, 6):
            for y in range(8, 12): p(x, y, shade)
        for x in range(10, 14):
            for y in range(8, 12): p(x, y, shade)
        for x, y in [(6, 10), (7, 10), (8, 10), (9, 10), (6, 12), (7, 12), (8, 12), (9, 12), (6, 11), (9, 11)]: p(x, y, INK)
        p(7, 11, hexc(0x5E7B2E)); p(8, 11, hexc(0x5E7B2E))
        for x, y in [(13, 2), (13, 3), (12, 4), (2, 3), (2, 4)]: p(x, y, SWEAT)
    return img

sheet = Image.new('RGBA', (80, 16), (0, 0, 0, 0))
for s in range(5):
    sheet.paste(face(s), (s * 16, 0))
sheet.save(os.path.join(GUI, 'faces.png'))

vig = Image.new('RGBA', (256, 256))
vp = vig.load()
for y in range(256):
    for x in range(256):
        dx, dy = (x - 127.5) / 127.5, (y - 127.5) / 127.5
        r = math.hypot(dx * 1.05, dy)
        a = max(0.0, min(1.0, (r - 0.32) / 0.8)) ** 1.35
        vp[x, y] = (255, 255, 255, int(a * 255))
vig.save(os.path.join(GUI, 'vignette.png'))

# ── logo : un petit bateau dans la houle, un marin vert penché par-dessus bord ──
N = 50
logo = Image.new('RGBA', (N, N))
lp = logo.load()
def lerp(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)
sky_top, sky_bot = hexc(0x2B4C7E), hexc(0xF2B880)
for y in range(N):
    for x in range(N):
        lp[x, y] = lerp(sky_top, sky_bot, y / 30)
# soleil couchant
for y in range(N):
    for x in range(N):
        if math.hypot(x - 37, y - 22) < 5.5: lp[x, y] = hexc(0xFFE08A)
def wave_y(x, base, amp, ph): return base + amp * math.sin(x / 50 * 2 * math.pi * 1.6 + ph)
for x in range(N):
    for layer, (base, amp, ph, col, crest) in enumerate([(28, 1.6, 0.0, 0x24507A, 0x5B8FC0), (33, 2.0, 1.4, 0x1B3F63, 0x4A7FB0), (40, 2.4, 2.6, 0x12304E, 0x3E6E9E)]):
        top = wave_y(x, base, amp, ph)
        for y in range(N):
            if y >= top: lp[x, y] = hexc(col)
        ty = int(round(top))
        if 0 <= ty < N: lp[x, ty] = hexc(crest)
        if 0 <= ty + 1 < N and (x + layer) % 5 == 0: lp[x, ty + 1] = hexc(crest)
# bateau incliné
tilt = math.radians(-14)
def rot(x, y, cx=24, cy=31):
    dx, dy = x - cx, y - cy
    return cx + dx * math.cos(tilt) - dy * math.sin(tilt), cy + dx * math.sin(tilt) + dy * math.cos(tilt)
hull, hull_dark, rim = hexc(0x8A5A2B), hexc(0x5E3A1A), hexc(0xC08A4A)
for y in range(N):
    for x in range(N):
        # coordonnées dans le repère du bateau
        dx, dy = x - 24, y - 31
        bx = dx * math.cos(-tilt) - dy * math.sin(-tilt)
        by = dx * math.sin(-tilt) + dy * math.cos(-tilt)
        if -1 <= by <= 4 and abs(bx) <= 13 - by * 1.2:
            lp[x, y] = rim if by < 0.2 else (hull_dark if by > 2.6 else hull)
# le marin : tête verte penchée par-dessus le bord gauche
head = (14, 22)
skin = hexc(0x9CCB78)
for y in range(N):
    for x in range(N):
        if math.hypot(x - head[0], y - head[1]) <= 3.4: lp[x, y] = skin
        elif math.hypot(x - head[0], y - head[1]) <= 4.2 and y < 23: lp[x, y] = hexc(0x3B2A20)
for x, y in [(12, 21), (15, 21)]: lp[x, y] = hexc(0x2A1E16)
for x, y in [(13, 24), (14, 24)]: lp[x, y] = hexc(0x2A1E16)
for x, y in [(17, 18), (17, 19)]: lp[x, y] = hexc(0x8FD3FF)
# corps / bras sur le plat-bord
for x in range(15, 22):
    for y in range(24, 28):
        if (x - 15) + (27 - y) >= 2: lp[x, y] = hexc(0x2E5E8E)
# filet vert qui tombe à la mer
for i, (x, y) in enumerate([(12, 26), (12, 27), (11, 28), (11, 29), (11, 30)]):
    lp[x, y] = hexc(0xA8C84A if i % 2 else 0x86A83A)
lp[10, 31] = hexc(0xCFE8FF); lp[12, 31] = hexc(0xCFE8FF)

logo.resize((100, 100), Image.NEAREST).save(os.path.join(RES, 'seasick_logo.png'))
os.makedirs(os.path.join(ROOT, 'curseforge'), exist_ok=True)
logo.resize((400, 400), Image.NEAREST).save(os.path.join(ROOT, 'curseforge', 'avatar.png'))
print('textures écrites')
