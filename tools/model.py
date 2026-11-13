#!/usr/bin/env python3
"""Prototype du modèle de Seasick (mêmes formules que sea/SeaState, sea/Swell et sick/Sickness),
pour régler les constantes : python3 tools/model.py"""
import math

G = 9.81
F0 = 60.0          # longueur d'eau libre (blocs) pour 63 % du développement de la houle
K = 0.016          # gain de dose (réglé pour des traversées de quelques minutes)
TAU_SEA = 70.0     # récupération en mer (s), allongée quand on est déjà mal
BOAT_LEN = 1.4

def wf(f):
    """Pondération Wf de la norme ISO 2631-1 (mal des transports), approchée : bande 0,08–0,63 Hz."""
    hp = (f / 0.05) ** 2 / (1 + (f / 0.05) ** 2)
    lp = 1 / math.sqrt(1 + (f / 0.25) ** 4)
    return min(1.0, 1.15 * hp * lp)

def sea(U, fetch, depth, biome=1.0):
    dev = 1 - math.exp(-fetch / F0)
    hs = 0.0214 * U * U * dev * biome
    hs = min(hs, 0.6 * depth, 10.0)
    hs = max(hs, 0.04)
    tp = max(1.4, 0.73 * U * dev ** 0.3)
    return hs, tp

def motion(hs, tp, v_along, beta_sin, beta_cos):
    w0 = 2 * math.pi / tp
    # fréquence de rencontre ; la vitesse « arcade » du bateau est ramenée à 40 % et la houle irrégulière
    # empêche de suivre une vague indéfiniment : on reste entre 0,5 et 2 fois la fréquence propre
    we = min(2.0 * w0, max(0.5 * w0, abs(w0 - w0 * w0 * 0.4 * v_along / G)))
    L = 1.56 * tp * tp
    slope = math.degrees(math.atan(math.pi * hs / L))
    resp = max(0.15, min(1.0, (L - 2 * BOAT_LEN) / 8))
    roll = min(24, (1.2 * slope + 1.6 * hs) * resp) * (0.3 + 0.7 * abs(beta_sin))
    pitch = min(12, (0.8 * slope + 0.9 * hs) * resp) * (0.3 + 0.7 * abs(beta_cos))
    return we, roll, pitch

def chop(U):
    """Clapot et déferlement : vagues courtes levées par le vent, en plus de la houle (m/s², vers 0,3 Hz)."""
    return 0.03 * max(0.0, U - 5.0) ** 1.4

def intensity(hs, we, roll, pitch, U=0.0, lat=0.0):
    f = we / (2 * math.pi)
    w = wf(f)
    a_h = (hs / 2) * we * we / math.sqrt(2)
    a_r = 1.2 * math.radians(roll) * we * we / math.sqrt(2)
    a_p = 0.8 * math.radians(pitch) * we * we / math.sqrt(2)
    return (w * a_h) ** 2 + 0.5 * (w * a_r) ** 2 + 0.4 * (w * a_p) ** 2 + (wf(0.3) * chop(U)) ** 2 + (0.6 * lat) ** 2

def run(I2, M, secs=600, s0=0.0):
    s, t_sick = s0, None
    for i in range(int(secs * 20)):
        tau = TAU_SEA * (1 + 0.6 * s)
        s += (K * I2 * M - s / tau) * 0.05
        if s >= 1 and t_sick is None:
            t_sick = i / 20
    return s, t_sick

scen = [
    ("lac calme, vent 4 m/s, 20 blocs d'eau libre", 4, 20, 6),
    ("mer belle, vent 6 m/s", 6, 200, 25),
    ("mer agitée (pluie), vent 10 m/s", 10, 200, 25),
    ("mer forte (pluie), vent 13 m/s", 13, 200, 35),
    ("tempête, vent 19 m/s", 19, 200, 35),
    ("tempête, vent 24 m/s", 24, 200, 40),
]
for name, U, F, d in scen:
    hs, tp = sea(U, F, d)
    print(f"\n{name}: Hs={hs:.2f} m Tp={tp:.1f} s")
    for label, v, sb, cb in [("arrêt, houle de travers", 0, 1, 0), ("8 b/s face aux vagues", -8, 0, 1), ("8 b/s vent arrière", 8, 0, 1)]:
        we, roll, pitch = motion(hs, tp, v, sb, cb)
        I2 = intensity(hs, we, roll, pitch, U)
        line = f"  {label:24s} Te={2*math.pi/we:4.1f}s roulis={roll:4.1f}° tangage={pitch:4.1f}° I²={I2:.3f}"
        for mlabel, M in [("horizon+barre", 0.55 * 0.65), ("normal", 1.0), ("passager+inventaire", 1.25 * 1.6)]:
            s, ts = run(I2, M)
            line += f" | {mlabel}: Q*={100*s:5.0f}" + (f" (malade à {ts:.0f}s)" if ts else "")
        print(line)
