#!/usr/bin/env python3
"""Textures du mod, dessinées en code : python3 tools/textures.py

Tout est dessiné suréchantillonné (4×) puis réduit avec un filtre de Lanczos, à 2 à 4 fois la
densité de l'interface : le jeu les affiche avec un filtrage linéaire, nettes à toutes les
échelles d'interface. Les couleurs débordent sous les pixels transparents (« bleed ») pour que
le filtrage linéaire ne fasse pas de liseré sombre.

Style : instruments de marine en bois verni et laiton (inspiré des jauges de Cold Sweat et
Tough As Nails), voiles d'écran doux, journal de bord en parchemin façon livre Patchouli."""
import math
import os
import random

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, 'src', 'main', 'resources')
GUI = os.path.join(RES, 'assets', 'seasick', 'textures', 'gui')
os.makedirs(GUI, exist_ok=True)
S = 4  # suréchantillonnage


def rgba(h, a=255):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255, a)


def mix(c1, c2, t):
    return tuple(int(round(c1[i] + (c2[i] - c1[i]) * t)) for i in range(len(c1)))


def canvas(w, h):
    return Image.new('RGBA', (w * S, h * S), (0, 0, 0, 0))


def finish(img, w, h):
    return bleed(img.resize((w, h), Image.LANCZOS))


def bleed(img):
    """Couleurs étalées sous les pixels transparents (alpha conservé)."""
    a = np.asarray(img).astype(np.float32)
    alpha = a[..., 3] / 255.0
    rgb = a[..., :3]
    out = rgb.copy()
    known = alpha > 0.02
    pm = rgb * alpha[..., None]
    for radius in (2, 6, 16, 40):
        bl_a = np.asarray(Image.fromarray((alpha * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(radius))).astype(np.float32) / 255
        bl = np.stack([np.asarray(Image.fromarray(np.clip(pm[..., i], 0, 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(radius))).astype(np.float32)
                       for i in range(3)], -1)
        fill = (~known) & (bl_a > 1e-3)
        out[fill] = (bl[fill] / bl_a[fill][..., None])
        known = known | fill
    res = np.dstack([np.clip(out, 0, 255), a[..., 3]]).astype(np.uint8)
    return Image.fromarray(res, 'RGBA')


def fbm(w, h, octaves=5, seed=0, base=4):
    rng = np.random.default_rng(seed)
    out = np.zeros((h, w), np.float32)
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        n = base * 2 ** o
        grid = rng.random((n + 1, n + 1)).astype(np.float32)
        grid[-1, :] = grid[0, :]
        grid[:, -1] = grid[:, 0]
        img = Image.fromarray((grid * 255).astype(np.uint8)).resize((w + w // n, h + h // n), Image.BICUBIC).crop((0, 0, w, h))
        out += np.asarray(img).astype(np.float32) / 255 * amp
        tot += amp
        amp *= 0.5
    return out / tot


def radial(w, h, cx, cy, r):
    y, x = np.mgrid[0:h, 0:w]
    return np.sqrt((x - cx) ** 2 + (y - cy) ** 2) / r


def vgrad(img, box, top, bottom, mask=None):
    """Dégradé vertical dans une boîte, découpé par un masque (L) facultatif."""
    x0, y0, x1, y1 = [int(v) for v in box]
    g = Image.new('RGBA', (x1 - x0, y1 - y0))
    px = np.zeros((y1 - y0, x1 - x0, 4), np.float32)
    t = np.linspace(0, 1, y1 - y0)[:, None]
    for i in range(4):
        px[..., i] = top[i] + (bottom[i] - top[i]) * t
    g = Image.fromarray(px.astype(np.uint8), 'RGBA')
    if mask is not None:
        m = mask.crop((x0, y0, x1, y1))
        g.putalpha(Image.fromarray((np.asarray(g)[..., 3].astype(np.float32) * np.asarray(m).astype(np.float32) / 255).astype(np.uint8)))
    img.alpha_composite(g, (x0, y0))


def mask_rrect(size, box, r):
    m = Image.new('L', size, 0)
    ImageDraw.Draw(m).rounded_rectangle(box, r, fill=255)
    return m


def mask_ellipse(size, box):
    m = Image.new('L', size, 0)
    ImageDraw.Draw(m).ellipse(box, fill=255)
    return m


def paste_masked(img, layer, mask):
    l = layer.copy()
    a = np.asarray(l)[..., 3].astype(np.float32) * np.asarray(mask).astype(np.float32) / 255
    l.putalpha(Image.fromarray(a.astype(np.uint8)))
    img.alpha_composite(l)


def glow(img, mask, color, radius):
    g = Image.new('RGBA', img.size, color[:3] + (0,))
    a = np.asarray(mask.filter(ImageFilter.GaussianBlur(radius))).astype(np.float32) * color[3] / 255
    g.putalpha(Image.fromarray(a.astype(np.uint8)))
    img.alpha_composite(g)


def line(d, pts, color, width):
    d.line(pts, fill=color, width=int(width), joint='curve')
    r = width / 2
    for p in (pts[0], pts[-1]):
        d.ellipse((p[0] - r, p[1] - r, p[0] + r, p[1] + r), fill=color)


# ───────────────────────── visages (64 px chacun, affichés en 16-18) ─────────────────────────

FACES = [
    # peau, ombre, joues
    (0xF6C79E, 0xD48A5A, 0xF2A090),   # 0 en forme
    (0xEED6A2, 0xC7A062, 0xE8B890),   # 1 barbouillé
    (0xDCE6A8, 0xA4B866, 0xC8D888),   # 2 nauséeux
    (0xBCDB8E, 0x78A452, 0x9CC470),   # 3 malade
    (0x98C870, 0x588A3A, 0x7DB058),   # 4 au bord du malaise
    (0xE6DAB0, 0xB8A070, 0xE0BC98),   # 5 yeux fermés
]
INK = rgba(0x3A2418)


def face(stage):
    N = 64 * S
    img = Image.new('RGBA', (N, N), (0, 0, 0, 0))
    base, shade, cheek = (rgba(c) for c in FACES[stage])
    cx = cy = N / 2
    R = N * 0.44
    # ombre portée
    sh = mask_ellipse(img.size, (cx - R + 3 * S, cy - R + 5 * S, cx + R + 3 * S, cy + R + 5 * S))
    glow(img, sh, (0, 0, 0, 110), 4 * S)
    # tête : dégradé radial (lumière en haut à gauche)
    rr = radial(N, N, cx - R * 0.35, cy - R * 0.45, R * 1.75)
    t = np.clip(rr, 0, 1)[..., None]
    px = np.array(base, np.float32) * (1 - t) + np.array(shade, np.float32) * t
    head = Image.fromarray(np.clip(px, 0, 255).astype(np.uint8), 'RGBA')
    m = mask_ellipse(img.size, (cx - R, cy - R, cx + R, cy + R))
    paste_masked(img, head, m)
    d = ImageDraw.Draw(img)
    d.ellipse((cx - R, cy - R, cx + R, cy + R), outline=mix(shade, (40, 25, 15, 255), 0.45), width=int(2.2 * S))
    # reflet
    hl = mask_ellipse(img.size, (cx - R * 0.62, cy - R * 0.78, cx - R * 0.05, cy - R * 0.38))
    glow(img, hl, (255, 255, 255, 70), 3 * S)

    ex, ey, es = R * 0.36, -R * 0.12, R * 0.13

    def cheeks(alpha, puff=1.0):
        for sx in (-1, 1):
            m2 = mask_ellipse(img.size, (cx + sx * R * 0.58 - R * 0.2 * puff, cy + R * 0.12, cx + sx * R * 0.58 + R * 0.2 * puff, cy + R * 0.12 + R * 0.26 * puff))
            glow(img, m2, cheek[:3] + (alpha,), 2.5 * S)

    def eye_dot(sx, scale=1.0):
        x, y = cx + sx * ex, cy + ey
        d.ellipse((x - es * scale * 0.8, y - es * scale, x + es * scale * 0.8, y + es * scale), fill=INK)
        d.ellipse((x - es * 0.35, y - es * 0.75, x + es * 0.05, y - es * 0.3), fill=(255, 255, 255, 230))

    def sweat(x, y, s):
        pts = [(x, y - s * 1.3)]
        for k in range(0, 181, 15):
            a = math.radians(k)
            pts.append((x + math.cos(a) * s * 0.8, y + math.sin(a) * s * 0.8))
        d.polygon(pts, fill=(140, 205, 255, 235), outline=(70, 130, 190, 255))
        d.ellipse((x - s * 0.35, y - s * 0.2, x - s * 0.05, y + s * 0.15), fill=(255, 255, 255, 220))

    def mouth_wave(y, amp, width, n=3, w=2.6):
        pts = []
        for i in range(41):
            u = i / 40
            pts.append((cx - width / 2 + u * width, y + math.sin(u * math.pi * n) * amp))
        line(d, pts, INK, w * S)

    if stage == 0:
        cheeks(120)
        eye_dot(-1)
        eye_dot(1)
        pts = [(cx - R * 0.42 + i / 30 * R * 0.84, cy + R * 0.28 + math.sin(i / 30 * math.pi) * R * 0.2) for i in range(31)]
        line(d, pts, INK, 2.6 * S)
    elif stage == 1:
        cheeks(70)
        for sx in (-1, 1):
            x, y = cx + sx * ex, cy + ey
            d.chord((x - es * 1.1, y - es * 0.9, x + es * 1.1, y + es * 1.1), 0, 180, fill=INK)
            line(d, [(x - es * 1.3, y - es * 0.05), (x + es * 1.3, y - es * 0.05)], mix(shade, INK, 0.5), 2 * S)
        line(d, [(cx - R * 0.28, cy + R * 0.42), (cx + R * 0.28, cy + R * 0.38)], INK, 2.4 * S)
    elif stage == 2:
        eye_dot(-1, 0.85)
        eye_dot(1, 0.85)
        for sx in (-1, 1):
            line(d, [(cx + sx * (ex - es * 1.4), cy + ey - es * 2.0), (cx + sx * (ex + es * 0.8), cy + ey - es * 1.5)], INK, 2 * S)
        mouth_wave(cy + R * 0.42, R * 0.07, R * 0.7, 3)
        sweat(cx + R * 0.78, cy - R * 0.55, R * 0.16)
    elif stage == 3:
        cheeks(190, 1.25)
        for sx in (-1, 1):
            x, y = cx + sx * ex, cy + ey
            line(d, [(x - sx * es * 1.1, y - es * 0.9), (x + sx * es * 0.9, y), (x - sx * es * 1.1, y + es * 0.9)], INK, 2.4 * S)
        mouth_wave(cy + R * 0.45, R * 0.06, R * 0.5, 4, 2.4)
        sweat(cx + R * 0.8, cy - R * 0.5, R * 0.16)
        sweat(cx - R * 0.86, cy - R * 0.25, R * 0.12)
    elif stage == 4:
        cheeks(230, 1.6)
        for sx in (-1, 1):
            x, y = cx + sx * ex, cy + ey
            pts = []
            for k in range(70):
                a = k / 69 * math.pi * 3.2
                r = es * 1.3 * k / 69
                pts.append((x + math.cos(a * sx) * r, y + math.sin(a * sx) * r))
            line(d, pts, INK, 1.8 * S)
        mw, mh = R * 0.26, R * 0.2
        d.ellipse((cx - mw, cy + R * 0.32, cx + mw, cy + R * 0.32 + mh * 2), fill=(70, 95, 40, 255), outline=INK, width=int(2 * S))
        sweat(cx + R * 0.82, cy - R * 0.5, R * 0.17)
        sweat(cx - R * 0.84, cy - R * 0.4, R * 0.14)
        sweat(cx + R * 0.6, cy - R * 0.85, R * 0.11)
    else:
        cheeks(90)
        for sx in (-1, 1):
            x, y = cx + sx * ex, cy + ey
            d.arc((x - es * 1.2, y - es * 1.2, x + es * 1.2, y + es * 0.9), 20, 160, fill=INK, width=int(2.4 * S))
        line(d, [(cx - R * 0.16, cy + R * 0.42), (cx + R * 0.16, cy + R * 0.42)], INK, 2.4 * S)
    return img.resize((64, 64), Image.LANCZOS)


sheet = Image.new('RGBA', (64 * len(FACES), 64), (0, 0, 0, 0))
for s in range(len(FACES)):
    sheet.alpha_composite(face(s), (s * 64, 0))
bleed(sheet).save(os.path.join(GUI, 'faces.png'))

# ───────────────────────── instrument de bord (66×28 en jeu, densité 4) ─────────────────────────
IW, IH = 66, 28
D = 4  # densité finale


def instrument():
    W, H = IW * D * S, IH * D * S
    k = D * S  # pixels source par unité d'interface
    img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    size = img.size
    plate = mask_rrect(size, (0.5 * k, 0.5 * k, W - 0.5 * k, H - 0.5 * k), 5 * k)
    glow(img, plate, (0, 0, 0, 120), 1.2 * k)
    # bois verni
    wood = np.zeros((H, W, 4), np.float32)
    n = fbm(W, H, 4, seed=3, base=3)
    y = np.linspace(0, 1, H)[:, None]
    x = np.linspace(0, 1, W)[None, :]
    grain = 0.5 + 0.5 * np.sin((y * 38 + n * 6 + np.sin(x * 9) * 0.6) * math.pi)
    top, bot = np.array(rgba(0x84552F), np.float32), np.array(rgba(0x4A2D17), np.float32)
    base = top * (1 - y[..., None]) + bot * y[..., None]
    wood[...] = base
    wood[..., :3] *= (0.86 + 0.14 * grain[..., None]) * (0.92 + 0.16 * n[..., None])
    wood[..., 3] = 255
    paste_masked(img, Image.fromarray(np.clip(wood, 0, 255).astype(np.uint8), 'RGBA'), plate)
    d = ImageDraw.Draw(img)
    # liseré de laiton
    for i, c in enumerate([0x5E4416, 0xB8902E, 0xF0D27A, 0xB38A2C]):
        inset = (0.5 + i * 0.35) * k
        d.rounded_rectangle((inset, inset, W - inset, H - inset), 5 * k - i * 0.35 * k, outline=rgba(c), width=max(1, int(0.4 * k)))
    # vis aux coins
    for (sx, sy) in ((3.2, 3.2), (IW - 3.2, 3.2), (3.2, IH - 3.2), (IW - 3.2, IH - 3.2)):
        X, Y = sx * k, sy * k
        d.ellipse((X - 1.1 * k, Y - 1.1 * k, X + 1.1 * k, Y + 1.1 * k), fill=rgba(0xC9A24A), outline=rgba(0x6A4E1A), width=int(0.25 * k))
        d.line((X - 0.7 * k, Y - 0.2 * k, X + 0.7 * k, Y + 0.2 * k), fill=rgba(0x6A4E1A), width=int(0.3 * k))

    # clinomètre : demi-cadran
    cx, cy, R = 17 * k, 21.5 * k, 13.5 * k
    ring = mask_ellipse(size, (cx - R - 1.3 * k, cy - R - 1.3 * k, cx + R + 1.3 * k, cy + R + 1.3 * k))
    half = Image.new('L', size, 0)
    ImageDraw.Draw(half).rectangle((0, 0, W, cy + 0.6 * k), fill=255)
    ring = Image.fromarray(np.minimum(np.asarray(ring), np.asarray(half)))
    glow(img, ring, (0, 0, 0, 160), 0.8 * k)
    brass = Image.new('RGBA', size)
    vgrad(brass, (0, 0, W, H), rgba(0xF4D884), rgba(0x8E6A22))
    paste_masked(img, brass, ring)
    dial = mask_ellipse(size, (cx - R, cy - R, cx + R, cy + R))
    dial = Image.fromarray(np.minimum(np.asarray(dial), np.asarray(half)))
    face_l = Image.new('RGBA', size)
    vgrad(face_l, (0, 0, W, H), rgba(0xFBF3DC), rgba(0xD9C79E))
    paste_masked(img, face_l, dial)
    # zone rouge au-delà de 25°
    for sgn in (-1, 1):
        for a in np.linspace(25, 50, 60):
            ang = math.radians(-90 + sgn * a)
            x0, y0 = cx + math.cos(ang) * (R - 2.6 * k), cy + math.sin(ang) * (R - 2.6 * k)
            x1, y1 = cx + math.cos(ang) * (R - 0.6 * k), cy + math.sin(ang) * (R - 0.6 * k)
            d.line((x0, y0, x1, y1), fill=(196, 52, 40, 255), width=int(0.5 * k))
    # graduations
    for a in range(-50, 51, 5):
        ang = math.radians(-90 + a)
        big = a % 25 == 0
        mid = a % 10 == 0
        L = 3.4 if big else (2.4 if mid else 1.4)
        x0, y0 = cx + math.cos(ang) * (R - 0.6 * k), cy + math.sin(ang) * (R - 0.6 * k)
        x1, y1 = cx + math.cos(ang) * (R - L * k), cy + math.sin(ang) * (R - L * k)
        d.line((x0, y0, x1, y1), fill=rgba(0x2E2014), width=int((0.45 if big else 0.3) * k))
    # ombre intérieure du verre
    glass = Image.new('RGBA', size)
    vgrad(glass, (0, 0, W, H), (255, 255, 255, 60), (255, 255, 255, 0))
    hl = mask_ellipse(size, (cx - R * 0.8, cy - R * 0.95, cx + R * 0.5, cy - R * 0.35))
    paste_masked(img, glass, hl)

    # alvéole du visage
    fx, fy, fr = 46.5 * k, 12.5 * k, 10.2 * k
    sock = mask_ellipse(size, (fx - fr - 1.2 * k, fy - fr - 1.2 * k, fx + fr + 1.2 * k, fy + fr + 1.2 * k))
    paste_masked(img, brass, sock)
    inner = mask_ellipse(size, (fx - fr, fy - fr, fx + fr, fy + fr))
    dark = Image.new('RGBA', size)
    vgrad(dark, (0, 0, W, H), rgba(0x1A100A), rgba(0x3A2614))
    paste_masked(img, dark, inner)

    # fente de la jauge
    bx0, by0, bx1, by1 = 34.5 * k, 23.0 * k, 61.5 * k, 26.6 * k
    slot = mask_rrect(size, (bx0, by0, bx1, by1), 1.8 * k)
    paste_masked(img, brass, mask_rrect(size, (bx0 - 0.7 * k, by0 - 0.7 * k, bx1 + 0.7 * k, by1 + 0.7 * k), 2.4 * k))
    paste_masked(img, dark, slot)

    # voyant (état de la mer) : verre sombre, la couleur est ajoutée en jeu
    lx, ly, lr = 4.6 * k, 24.0 * k, 2.1 * k
    paste_masked(img, brass, mask_ellipse(size, (lx - lr - 0.7 * k, ly - lr - 0.7 * k, lx + lr + 0.7 * k, ly + lr + 0.7 * k)))
    paste_masked(img, dark, mask_ellipse(size, (lx - lr, ly - lr, lx + lr, ly + lr)))
    return img.resize((IW * D, IH * D), Image.LANCZOS)


bleed(instrument()).save(os.path.join(GUI, 'instrument.png'))


def needle():
    w, h = 6, 18  # unités d'interface ; pivot en (3, 15)
    k = D * S
    img = Image.new('RGBA', (w * k, h * k), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    px, py = 3 * k, 15 * k
    sh = Image.new('L', img.size, 0)
    ImageDraw.Draw(sh).polygon([(px - 0.9 * k + 0.4 * k, py + 0.4 * k), (px + 0.4 * k, 1.2 * k + 0.4 * k), (px + 0.9 * k + 0.4 * k, py + 0.4 * k)], fill=255)
    glow(img, sh, (0, 0, 0, 140), 0.5 * k)
    d.polygon([(px - 0.85 * k, py), (px, 1.0 * k), (px + 0.85 * k, py)], fill=rgba(0xC8342A))
    d.polygon([(px, py), (px, 1.0 * k), (px + 0.85 * k, py)], fill=rgba(0x9A2018))
    d.polygon([(px - 0.6 * k, py), (px - 0.6 * k, py + 2.2 * k), (px + 0.6 * k, py + 2.2 * k), (px + 0.6 * k, py)], fill=rgba(0x2A1A10))
    r = 1.7 * k
    d.ellipse((px - r, py - r, px + r, py + r), fill=rgba(0xD9B65A), outline=rgba(0x6A4E1A), width=int(0.3 * k))
    d.ellipse((px - r * 0.45, py - r * 0.55, px + r * 0.05, py - r * 0.1), fill=(255, 245, 210, 220))
    return img.resize((w * D, h * D), Image.LANCZOS)


bleed(needle()).save(os.path.join(GUI, 'needle.png'))

# jauge : dégradé vert → jaune → orange → rouge, avec reflet
bw, bh = 256, 16
bar = np.zeros((bh, bw, 4), np.float32)
stops = [(0.0, 0x5FCB5A), (0.35, 0xC9D84C), (0.6, 0xF2B638), (0.85, 0xEE7A30), (1.0, 0xE0402C)]
for x in range(bw):
    u = x / (bw - 1)
    for i in range(len(stops) - 1):
        if stops[i][0] <= u <= stops[i + 1][0]:
            t = (u - stops[i][0]) / (stops[i + 1][0] - stops[i][0])
            c = mix(rgba(stops[i][1]), rgba(stops[i + 1][1]), t)
            break
    for y in range(bh):
        v = y / (bh - 1)
        l = 1.25 if v < 0.35 else (1.0 if v < 0.7 else 0.78)
        bar[y, x] = [min(255, c[0] * l), min(255, c[1] * l), min(255, c[2] * l), 255]
Image.fromarray(bar.astype(np.uint8), 'RGBA').save(os.path.join(GUI, 'bar.png'))

# voyant lumineux (blanc, teinté en jeu)
L = 64
lr_ = radial(L, L, L / 2 - 0.5, L / 2 - 0.5, L / 2)
lamp = np.zeros((L, L, 4), np.float32)
core = np.clip(1 - lr_ / 0.42, 0, 1)
halo = np.clip(1 - lr_, 0, 1) ** 2
lamp[..., :3] = 255
lamp[..., 3] = np.clip(core * 255 + halo * 120, 0, 255)
hl = radial(L, L, L * 0.42, L * 0.38, L * 0.12)
lamp[..., 3] = np.maximum(lamp[..., 3], np.clip(1 - hl, 0, 1) * 255)
Image.fromarray(lamp.astype(np.uint8), 'RGBA').save(os.path.join(GUI, 'lamp.png'))

# ───────────────────────── voiles d'écran ─────────────────────────
# vignette douce (blanc + alpha, teintée en jeu)
V = 512
vr = radial(V, V, V / 2, V / 2, V / 2)
y_, x_ = np.mgrid[0:V, 0:V]
r_ell = np.sqrt(((x_ - V / 2) / (V / 2) * 0.95) ** 2 + ((y_ - V / 2) / (V / 2)) ** 2)
va = np.clip((r_ell - 0.32) / 0.85, 0, 1) ** 1.6
vig = np.dstack([np.full((V, V), 255), np.full((V, V), 255), np.full((V, V), 255), va * 255]).astype(np.uint8)
Image.fromarray(vig, 'RGBA').save(os.path.join(GUI, 'vignette.png'))

# brume nauséeuse : taches jaune-vert qui dérivent
H_ = 512
n1 = fbm(H_, H_, 5, seed=11, base=3)
n2 = fbm(H_, H_, 4, seed=12, base=5)
blot = np.clip((n1 - 0.42) / 0.3, 0, 1) ** 1.5
col = np.stack([200 + 30 * n2, 214 + 20 * n2, 96 + 40 * n2], -1)
haze = np.dstack([col, blot * 200]).astype(np.uint8)
bleed(Image.fromarray(haze, 'RGBA')).save(os.path.join(GUI, 'haze.png'))

# sueurs froides : gouttes sur les bords (blanc + alpha, teinté en jeu)
SW = 512
sweat = Image.new('RGBA', (SW * 2, SW * 2), (0, 0, 0, 0))
sd = ImageDraw.Draw(sweat)
rnd = random.Random(7)
for _ in range(110):
    while True:
        x, y = rnd.random(), rnd.random()
        edge = max(abs(x - 0.5), abs(y - 0.5) * 1.15) * 2
        if rnd.random() < edge ** 5:
            break
    r = (3 + rnd.random() ** 2.5 * 13) * (0.5 + edge * 0.6)
    X, Y = x * SW * 2, y * SW * 2
    elong = 1.05 + rnd.random() * 0.4
    sd.ellipse((X - r, Y - r * elong, X + r, Y + r * elong), fill=(215, 232, 238, 34), outline=(70, 90, 100, 60), width=2)
    sd.ellipse((X - r * 0.5, Y - r * elong * 0.55, X - r * 0.12, Y - r * elong * 0.22), fill=(255, 255, 255, 150))
sweat = sweat.filter(ImageFilter.GaussianBlur(0.8)).resize((SW, SW), Image.LANCZOS)
bleed(sweat).save(os.path.join(GUI, 'sweat.png'))

# paupières : bord courbe, plis de peau, cils, bord flou
EW, EH = 512, 192


def eyelid(bottom):
    img = Image.new('RGBA', (EW * S, EH * S), (0, 0, 0, 0))
    W, H = img.size
    m = Image.new('L', img.size, 0)
    md = ImageDraw.Draw(m)
    # bord inférieur de la paupière : arc (plus bas au centre)
    pts = [(0, 0), (W, 0)]
    sag = 0.32 if not bottom else 0.18
    edge = []
    for i in range(101):
        u = i / 100
        yy = H * (0.62 + sag * math.sin(u * math.pi) ** 0.8)
        edge.append((u * W, yy))
    md.polygon(pts + edge[::-1], fill=255)
    m = m.filter(ImageFilter.GaussianBlur(6 * S))
    skin = np.zeros((H, W, 4), np.float32)
    yv = np.linspace(0, 1, H)[:, None]
    dark = np.array(rgba(0x070303), np.float32)
    warm = np.array(rgba(0x3C1A12), np.float32)
    skin[...] = dark * (1 - yv[..., None] ** 3) + warm * (yv[..., None] ** 3)
    skin[..., 3] = 255
    lid = Image.fromarray(skin.astype(np.uint8), 'RGBA')
    paste_masked(img, lid, m)
    # lumière qui passe à travers la peau, juste au-dessus du bord
    rim = Image.new('L', img.size, 0)
    ImageDraw.Draw(rim).line(edge, fill=255, width=int(26 * S))
    rim = rim.filter(ImageFilter.GaussianBlur(12 * S))
    glow_layer = Image.new('RGBA', img.size, (70, 30, 20, 0))
    glow_layer.putalpha(Image.fromarray((np.asarray(rim).astype(np.float32) * np.asarray(m).astype(np.float32) / 255 * 0.3).astype(np.uint8)))
    img.alpha_composite(glow_layer)
    out = img.resize((EW, EH), Image.LANCZOS)
    if bottom:
        out = out.transpose(Image.FLIP_TOP_BOTTOM)
    return bleed(out)


eyelid(False).save(os.path.join(GUI, 'eyelid_top.png'))
eyelid(True).save(os.path.join(GUI, 'eyelid_bottom.png'))

# horizon artificiel : trait lumineux, effacé aux bouts et au centre
HW, HH = 512, 24
hz = np.zeros((HH, HW, 4), np.float32)
xs = np.linspace(-1, 1, HW)[None, :]
ys = np.linspace(-1, 1, HH)[:, None]
along = np.clip(1 - np.abs(xs) ** 6, 0, 1) * np.clip((np.abs(xs) - 0.09) / 0.08, 0, 1)
core = np.exp(-(ys / 0.12) ** 2)
halo = np.exp(-(ys / 0.55) ** 2) * 0.35
hz[..., :3] = 255
hz[..., 3] = np.clip((core + halo) * along * 255, 0, 255)
Image.fromarray(hz.astype(np.uint8), 'RGBA').save(os.path.join(GUI, 'horizon.png'))


def boat_symbol():
    w, h = 64, 16
    k = 4 * S
    img = Image.new('RGBA', (w * k, h * k), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = w * k / 2, h * k / 2
    pts = [(cx - 28 * k, cy), (cx - 9 * k, cy), (cx - 4 * k, cy + 4 * k), (cx, cy), (cx + 4 * k, cy + 4 * k), (cx + 9 * k, cy), (cx + 28 * k, cy)]
    sh = Image.new('L', img.size, 0)
    ImageDraw.Draw(sh).line(pts, fill=255, width=int(3.6 * k), joint='curve')
    glow(img, sh, (0, 0, 0, 200), 1.2 * k)
    line(d, pts, rgba(0xFFC84A), 1.9 * k)
    d.ellipse((cx - 1.4 * k, cy - 1.4 * k, cx + 1.4 * k, cy + 1.4 * k), fill=rgba(0xFFE7A0))
    return bleed(img.resize((w * 4, h * 4), Image.LANCZOS))


boat_symbol().save(os.path.join(GUI, 'boat_symbol.png'))

# ───────────────────────── journal de bord : livre ouvert (340×210 en jeu, densité 2) ─────────────────────────
BW, BH = 340, 210
BD = 2


def book():
    k = BD * 2  # suréchantillonnage 2 suffit pour de grandes surfaces
    W, H = BW * k, BH * k
    img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    size = img.size
    cover = mask_rrect(size, (1 * k, 2 * k, W - 1 * k, H - 1 * k), 7 * k)
    glow(img, cover, (0, 0, 0, 170), 3 * k)
    # cuir
    n = fbm(W, H, 5, seed=21, base=4)
    yy = np.linspace(0, 1, H)[:, None]
    leather = np.zeros((H, W, 4), np.float32)
    c0, c1 = np.array(rgba(0x6A3E22), np.float32), np.array(rgba(0x3E2212), np.float32)
    leather[...] = c0 * (1 - yy[..., None]) + c1 * yy[..., None]
    leather[..., :3] *= (0.8 + 0.35 * n[..., None])
    leather[..., 3] = 255
    paste_masked(img, Image.fromarray(np.clip(leather, 0, 255).astype(np.uint8), 'RGBA'), cover)
    d = ImageDraw.Draw(img)
    # piqûres
    for i in range(0, W, 7 * k // 2):
        for (yy0) in (4.2 * k, H - 4.2 * k):
            if 8 * k < i < W - 8 * k:
                d.line((i, yy0, i + 2 * k, yy0), fill=(214, 176, 120, 160), width=int(0.5 * k))
    # tranche des pages (feuilles empilées)
    for i in range(4):
        off = (4 - i) * 0.7 * k
        pm = mask_rrect(size, (7 * k - off, 6 * k + off * 0.2, W - 7 * k + off, H - 6 * k + off), 2 * k)
        layer = Image.new('RGBA', size, rgba(0xE4D3AE if i % 2 else 0xCDB98E))
        paste_masked(img, layer, pm)
    # pages
    page_mask = mask_rrect(size, (8 * k, 6 * k, W - 8 * k, H - 8 * k), 2 * k)
    pn = fbm(W, H, 6, seed=31, base=6)
    pn2 = fbm(W, H, 3, seed=32, base=2)
    paper = np.zeros((H, W, 4), np.float32)
    base_c = np.array(rgba(0xF4E7C6), np.float32)
    paper[...] = base_c
    paper[..., :3] *= (0.93 + 0.09 * pn[..., None]) * (0.96 + 0.06 * pn2[..., None])
    xx = np.linspace(0, 1, W)[None, :]
    spine = np.exp(-((xx - 0.5) / 0.028) ** 2) * 0.32 + np.exp(-((xx - 0.5) / 0.09) ** 2) * 0.1
    edge = np.clip(1 - np.minimum(np.minimum(xx, 1 - xx) / 0.05, 1), 0, 1) * 0.12 \
        + np.clip(1 - np.minimum(np.minimum(yy, 1 - yy) / 0.06, 1), 0, 1) * 0.10
    paper[..., :3] *= (1 - spine - edge)[..., None]
    # taches de vieillissement
    stain = np.clip((fbm(W, H, 4, seed=41, base=3) - 0.62) / 0.2, 0, 1) * 0.08
    paper[..., 0] *= 1 - stain * 0.3
    paper[..., 1] *= 1 - stain * 0.55
    paper[..., 2] *= 1 - stain * 0.9
    paper[..., 3] = 255
    paste_masked(img, Image.fromarray(np.clip(paper, 0, 255).astype(np.uint8), 'RGBA'), page_mask)
    # pli central
    d.line((W / 2, 6 * k, W / 2, H - 8 * k), fill=(120, 96, 64, 120), width=int(0.6 * k))
    # rose des vents en filigrane (page de droite, en bas)
    rx, ry, rr = W * 0.78, H * 0.78, 22 * k
    wm = Image.new('RGBA', size, (0, 0, 0, 0))
    wd = ImageDraw.Draw(wm)
    for a in range(0, 360, 45):
        L2 = rr if a % 90 == 0 else rr * 0.6
        ang = math.radians(a - 90)
        p1 = (rx + math.cos(ang) * L2, ry + math.sin(ang) * L2)
        p2 = (rx + math.cos(ang + 0.35) * rr * 0.18, ry + math.sin(ang + 0.35) * rr * 0.18)
        p3 = (rx + math.cos(ang - 0.35) * rr * 0.18, ry + math.sin(ang - 0.35) * rr * 0.18)
        wd.polygon([p1, p2, (rx, ry), p3], fill=(90, 60, 30, 26))
    wd.ellipse((rx - rr * 0.75, ry - rr * 0.75, rx + rr * 0.75, ry + rr * 0.75), outline=(90, 60, 30, 26), width=int(0.6 * k))
    img.alpha_composite(wm)
    # signet rouge
    sx = W * 0.86
    rib = [(sx - 3.5 * k, 2 * k), (sx + 3.5 * k, 2 * k), (sx + 3.5 * k, 34 * k), (sx, 30 * k), (sx - 3.5 * k, 34 * k)]
    rs = Image.new('L', size, 0)
    ImageDraw.Draw(rs).polygon([(p[0] + 0.8 * k, p[1] + 0.8 * k) for p in rib], fill=255)
    glow(img, rs, (0, 0, 0, 120), 1 * k)
    d.polygon(rib, fill=rgba(0xA3202A))
    d.line((sx - 1.2 * k, 2 * k, sx - 1.2 * k, 31 * k), fill=(200, 70, 70, 160), width=int(0.6 * k))
    return img.resize((BW * BD, BH * BD), Image.LANCZOS)


bleed(book()).save(os.path.join(GUI, 'book.png'))


def little_boat():
    w, h = 32, 18
    k = 4 * S
    img = Image.new('RGBA', (w * k, h * k), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    hull = [(2 * k, 9 * k), (30 * k, 9 * k), (26 * k, 15 * k), (6 * k, 15 * k)]
    d.polygon(hull, fill=rgba(0x8A5A2E), outline=rgba(0x3A2412))
    d.line((2 * k, 9.4 * k, 30 * k, 9.4 * k), fill=rgba(0xC08A4A), width=int(1.2 * k))
    d.line((5 * k, 12.3 * k, 27.5 * k, 12.3 * k), fill=rgba(0x6A4220), width=int(0.6 * k))
    # le marin, penché par-dessus bord, tout vert
    d.ellipse((5 * k, 2.5 * k, 12 * k, 9.5 * k), fill=rgba(0xA9D07C), outline=rgba(0x3A2412), width=int(0.6 * k))
    d.ellipse((7 * k, 4.8 * k, 8.2 * k, 6 * k), fill=rgba(0x3A2412))
    d.rectangle((9 * k, 7 * k, 17 * k, 9.2 * k), fill=rgba(0x2E5E8E))
    d.line((16 * k, 9 * k, 22 * k, 1.5 * k), fill=rgba(0x6A4220), width=int(0.8 * k))  # rame
    return bleed(img.resize((w * 4, h * 4), Image.LANCZOS))


little_boat().save(os.path.join(GUI, 'little_boat.png'))


def arrow():
    w = 16
    k = 4 * S
    img = Image.new('RGBA', (w * k, w * k), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = w * k / 2
    d.polygon([(c, 1 * k), (c + 5 * k, 7 * k), (c + 1.8 * k, 7 * k), (c + 1.8 * k, 15 * k), (c - 1.8 * k, 15 * k), (c - 1.8 * k, 7 * k), (c - 5 * k, 7 * k)],
              fill=(255, 255, 255, 255))
    return bleed(img.resize((w * 4, w * 4), Image.LANCZOS))


arrow().save(os.path.join(GUI, 'arrow.png'))

# ───────────────────────── logo et avatar ─────────────────────────


def logo(N=400):
    k = S
    W = N * k
    img = Image.new('RGBA', (W, W), (0, 0, 0, 0))
    size = img.size
    circle = mask_ellipse(size, (4 * k, 4 * k, W - 4 * k, W - 4 * k))
    # ciel d'orage
    sky = np.zeros((W, W, 4), np.float32)
    yy = np.linspace(0, 1, W)[:, None]
    c0, c1 = np.array(rgba(0x22355A), np.float32), np.array(rgba(0xE6B98A), np.float32)
    sky[...] = c0 * (1 - yy[..., None]) + c1 * yy[..., None]
    clouds = fbm(W, W, 5, seed=51, base=3)
    sky[..., :3] *= (0.78 + 0.4 * clouds[..., None])
    sky[..., 3] = 255
    scene = Image.fromarray(np.clip(sky, 0, 255).astype(np.uint8), 'RGBA')
    sd = ImageDraw.Draw(scene)
    # lune malade
    mx, my, mr = W * 0.73, W * 0.27, W * 0.11
    glow(scene, mask_ellipse(size, (mx - mr * 1.6, my - mr * 1.6, mx + mr * 1.6, my + mr * 1.6)), (220, 240, 160, 90), 18 * k)
    moon = face(3).resize((int(mr * 2.3), int(mr * 2.3)), Image.LANCZOS)
    mm = mask_ellipse(moon.size, (moon.width * 0.06, moon.height * 0.06, moon.width * 0.94, moon.height * 0.94))
    moon.putalpha(Image.fromarray(np.minimum(np.asarray(moon)[..., 3], np.asarray(mm))))
    scene.alpha_composite(moon, (int(mx - mr * 1.15), int(my - mr * 1.15)))

    # houles successives
    def wave(base, amp, length, phase, col, crest, foam):
        pts = []
        for i in range(0, 401):
            x = i / 400 * W
            u = x / length + phase
            y = base - amp * (math.sin(u * 2 * math.pi) + 0.35 * math.sin(u * 4 * math.pi + 1.3))
            pts.append((x, y))
        poly = pts + [(W, W), (0, W)]
        layer = Image.new('RGBA', size, (0, 0, 0, 0))
        ld = ImageDraw.Draw(layer)
        ld.polygon(poly, fill=rgba(col))
        lm = Image.new('L', size, 0)
        ImageDraw.Draw(lm).polygon(poly, fill=255)
        grad = Image.new('RGBA', size)
        vgrad(grad, (0, int(base - amp * 2.5), W, min(W, int(base + amp * 6))), rgba(crest), rgba(col))
        if base + amp * 6 < W: ld.rectangle((0, int(base + amp * 6), W, W), fill=rgba(col))
        paste_masked(layer, grad, lm)
        # ombre sous l'écume
        sh = Image.new('L', size, 0)
        ImageDraw.Draw(sh).line([(x, y + 6 * k) for x, y in pts], fill=255, width=int(8 * k))
        shl = Image.new('RGBA', size, (6, 20, 40, 0))
        shl.putalpha(Image.fromarray((np.asarray(sh.filter(ImageFilter.GaussianBlur(5 * k))).astype(np.float32) * np.asarray(lm).astype(np.float32) / 255 * 0.5).astype(np.uint8)))
        layer.alpha_composite(shl)
        line(ld, pts, rgba(foam), 3.2 * k)
        rr_ = random.Random(int(base))
        for (x, y) in pts[::9]:
            if rr_.random() < 0.5:
                r3 = rr_.uniform(1.2, 2.6) * k
                ld.ellipse((x - r3, y - r3 - 2 * k, x + r3, y + r3 - 2 * k), fill=rgba(foam))
        scene.alpha_composite(layer)

    wave(W * 0.60, W * 0.03, W * 0.40, 0.1, 0x2A5680, 0x6AA6CE, 0xD8EEFA)
    # mouettes
    for (gx, gy, gs) in ((0.22, 0.24, 0.05), (0.32, 0.17, 0.035), (0.15, 0.36, 0.03)):
        X, Y, R2 = gx * W, gy * W, gs * W
        line(sd, [(X - R2, Y), (X - R2 * 0.45, Y - R2 * 0.35), (X, Y)], (230, 236, 240, 255), 1.6 * k)
        line(sd, [(X, Y), (X + R2 * 0.45, Y - R2 * 0.35), (X + R2, Y)], (230, 236, 240, 255), 1.6 * k)
    wave(W * 0.70, W * 0.05, W * 0.55, 0.30, 0x1E4A74, 0x5A96C4, 0xE2F2FC)
    # bateau incliné sur la crête
    boat = little_boat().resize((int(W * 0.46), int(W * 0.46 * 18 / 32)), Image.LANCZOS).rotate(16, resample=Image.BICUBIC, expand=True)
    scene.alpha_composite(boat, (int(W * 0.24), int(W * 0.40)))
    wave(W * 0.80, W * 0.04, W * 0.38, 0.55, 0x163A5E, 0x3C78A8, 0xE2F2FC)
    wave(W * 0.93, W * 0.03, W * 0.30, 0.2, 0x0F2C4A, 0x2E6494, 0xD8ECF8)

    paste_masked(img, scene, circle)
    d = ImageDraw.Draw(img)
    for i, c in enumerate([0x4A3410, 0xB8902E, 0xF0D27A, 0xA07A26]):
        inset = (4 + i * 2.2) * k
        d.ellipse((inset, inset, W - inset, W - inset), outline=rgba(c), width=int(2.6 * k))
    return img.resize((N, N), Image.LANCZOS)


big = logo(400)
os.makedirs(os.path.join(ROOT, 'curseforge'), exist_ok=True)
big.save(os.path.join(ROOT, 'curseforge', 'avatar.png'))
big.resize((128, 128), Image.LANCZOS).save(os.path.join(RES, 'seasick_logo.png'))
print('textures écrites')
