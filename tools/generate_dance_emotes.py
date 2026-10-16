"""Hand-keyed video adaptations. Run from any directory; requires Pillow.

Each pose explicitly drives all six rigid Minecraft bones. The last pose equals
the first pose, including every position channel, so the loop has no seam.
See dance-references/README.md for source clips and movement timestamps.
The ziga gesture is a one-shot; dances loop.
"""
import copy
import json
import math
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/pizzadlc"


def pose(dip=0, side=0, twist=0, r=(0, 0, 12), l=(0, 0, -12),
         rk=(0, 0, 0), lk=(0, 0, 0), spread=1.9, rz=0, lz=0,
         rx=0, lx=0, lean=0):
    def bone(x, y, z, rotation):
        return dict(zip(("x", "y", "z", "pitch", "yaw", "roll"),
                        (x, y, z, *rotation)))

    def grounded_leg(rotation, outward):
        pitch, yaw, roll = rotation
        vertical = 12 * math.cos(math.radians(pitch))
        if dip > 0 and vertical > 12 - dip:
            # Without knees, spread the supporting leg during a low squat so
            # lowering the pelvis does not push the foot through the floor.
            minimum = math.degrees(math.acos((12 - dip) / vertical))
            if abs(roll) < minimum:
                roll = minimum * (1 if roll > 0 else -1 if roll < 0 else outward)
        return pitch, yaw, roll

    # Positive y lowers the torso; negative y lifts the whole pose during hops.
    return {
        "head": bone(side, dip, -math.sin(math.radians(lean)) * 10,
                     (-lean * .4, twist * -.35, -side * 2)),
        "body": bone(side, dip, 0, (lean, twist, -side * 2)),
        "rightArm": bone(-5 + side, 2 + dip, 0, r),
        "leftArm": bone(5 + side, 2 + dip, 0, l),
        "rightLeg": bone(-spread + side + rx, 12 + dip, rz, grounded_leg(rk, 1)),
        "leftLeg": bone(spread + side + lx, 12 + dip, lz, grounded_leg(lk, -1)),
    }


# Angles are (pitch, yaw, roll), in degrees. Straight arms stand in for the
# bent elbows visible in the clips; feet use local offsets for sliding steps.
DANCES = {
    "ziga": ("Зига", "PizzaDLC gesture", 12, [
        pose(r=(0, 0, 0), l=(0, 0, 0)),
        pose(r=(-135, -8, -4), l=(0, 0, 0), lean=-2),
        pose(r=(-135, -8, -4), l=(0, 0, 0), lean=-2),
        pose(-.15, r=(-135, -8, -4), l=(0, 0, 0), lean=-2),
        pose(r=(-135, -8, -4), l=(0, 0, 0), lean=-2),
        pose(r=(-135, -8, -4), l=(0, 0, 0), lean=-2),
    ]),
    "rampage_2": ("Рампейдж 2", "insidix", 4, [
        pose(-1, -.6, -8, r=(-65, -20, 62), l=(-28, 18, -30), rk=(38, 0, 5), lk=(-10, 0, -4)),
        pose(0, .4, 8, r=(-30, -10, 30), l=(-65, 20, -62), rk=(-8, 0, 0), lk=(38, 0, -5)),
        pose(1, 0, 0, r=(-25, 0, 78), l=(-25, 0, -78), rk=(0, 0, 18), lk=(0, 0, -18)),
        pose(-1, -.8, -10, r=(-75, -25, 38), l=(-75, 25, -38), rk=(-48, 0, 24), lk=(6, 0, -6), spread=2.2),
        pose(.5, -.4, -4, r=(-35, -10, 35), l=(-35, 10, -35), rk=(8, 18, -8), lk=(-12, -10, 8), rx=1.5, lx=-1.5),
        pose(-1, .8, 10, r=(-75, -25, 38), l=(-75, 25, -38), rk=(6, 0, 6), lk=(-48, 0, -24), spread=2.2),
        pose(.5, .4, 4, r=(-35, -10, 35), l=(-35, 10, -35), rk=(-12, 10, -8), lk=(8, -18, 8), rx=1.5, lx=-1.5),
        pose(1, 0, 0, r=(0, 0, 18), l=(0, 0, -18)),
        pose(0, -1.2, -12, r=(-40, -25, 5), l=(12, 15, -30), rk=(-28, 0, 20), lk=(15, 0, -4), spread=2.5),
        pose(-.8, -.4, -3, r=(-62, -10, 10), l=(20, 0, -12), rk=(28, 0, -8), lk=(-12, 0, 6), rx=1.5, lx=-1),
        pose(0, 1.2, 12, r=(12, -15, 30), l=(-40, 25, -5), rk=(15, 0, 4), lk=(-28, 0, -20), spread=2.5),
        pose(-.8, .4, 3, r=(20, 0, 12), l=(-62, 10, -10), rk=(-12, 0, -6), lk=(28, 0, 8), rx=1, lx=-1.5),
        pose(0, -.8, -10, r=(-25, -10, 90), l=(-20, 5, -35), rk=(-22, 0, 12), lk=(8, 0, -5)),
        pose(-1, 0, 0, r=(-50, -15, 45), l=(-50, 15, -45), rk=(18, 12, -6), lk=(-15, -12, 6), rx=1.2, lx=-1.2),
        pose(0, .8, 10, r=(-20, -5, 35), l=(-25, 10, -90), rk=(8, 0, 5), lk=(-22, 0, -12)),
        pose(1, 0, 0, r=(-25, 0, 10), l=(-25, 0, -10)),
    ]),
    "rampage_3": ("Рампейдж 3", "jerseyyjoe", 4, [
        pose(-1, -.5, -6, r=(-15, -8, 15), l=(-15, 15, -155), rk=(-18, 0, 5), lk=(25, 0, -5)),
        pose(.5, .8, 8, r=(-20, -10, 60), l=(-20, 12, -145), rk=(10, 0, 8), lk=(-20, 0, -15)),
        pose(-1.5, -.6, -10, r=(-20, -12, 85), l=(-35, 15, -150), rk=(-25, 0, 8), lk=(20, 0, -4)),
        pose(.5, .8, 12, r=(-15, -20, 70), l=(-20, 10, -140), rk=(12, 0, 8), lk=(-15, 0, -12)),
        pose(-1, -.7, -8, r=(-18, -10, 80), l=(-25, 20, -150), rk=(-18, 0, 12), lk=(28, 0, -5)),
        pose(0, .6, 8, r=(-10, -15, 45), l=(-10, 12, -125), rk=(12, 0, 5), lk=(-22, 0, -15)),
        pose(.8, 0, 0, r=(-28, -25, -22), l=(-28, 25, 22), rk=(0, 0, 6), lk=(0, 0, -6)),
        pose(-.8, -1.4, -18, r=(-42, -20, 34), l=(10, 20, -45), rk=(-22, 0, 24), lk=(16, 0, -6), spread=2.6),
        pose(.8, -.5, -8, r=(-28, -30, -24), l=(-28, 30, 24), rk=(5, 10, -4), lk=(-5, -10, 4), rx=.8, lx=-.8),
        pose(-.8, 1.4, 18, r=(10, -20, 45), l=(-42, 20, -34), rk=(16, 0, 6), lk=(-22, 0, -24), spread=2.6),
        pose(.8, .5, 8, r=(-28, -30, -24), l=(-28, 30, 24), rk=(-5, 10, -4), lk=(5, -10, 4), rx=.8, lx=-.8),
        pose(-1.2, -1.4, -14, r=(-35, -18, 42), l=(15, 12, -30), rk=(-32, 0, 18), lk=(12, 0, -6), spread=2.4),
        pose(.5, 0, 0, r=(-25, -20, -20), l=(-25, 20, 20)),
        pose(-1.2, 1.4, 14, r=(15, -12, 30), l=(-35, 18, -42), rk=(12, 0, 6), lk=(-32, 0, -18), spread=2.4),
        pose(.5, .3, 4, r=(-22, -20, -20), l=(-22, 20, 20)),
        pose(-.6, -.6, -8, r=(-25, -10, 18), l=(-80, 10, -110), rk=(-16, 0, 8), lk=(18, 0, -6)),
    ]),
    "rampage_4": ("Рампейдж 4", "n_exodus_shuffle: 27–31 s", 4, [
        pose(.8, -.4, -10, r=(-45, -35, -30), l=(-45, 35, 30), rk=(10, 0, 8), lk=(-55, 0, -16)),
        pose(-1, -.8, -14, r=(-20, -25, 65), l=(-80, 20, -20), rk=(8, 0, 6), lk=(-70, 0, -12)),
        pose(1, 0, 0, r=(-40, -30, -24), l=(-40, 30, 24), rk=(0, 0, 10), lk=(0, 0, -10)),
        pose(-1, .8, 14, r=(-80, -20, 20), l=(-20, 25, -65), rk=(-70, 0, 12), lk=(8, 0, -6)),
        pose(.8, .4, 10, r=(-45, -35, -30), l=(-45, 35, 30), rk=(-55, 0, 16), lk=(10, 0, -8)),
        pose(-1.4, 0, 0, r=(-50, -20, 50), l=(-50, 20, -50), rk=(18, 0, 20), lk=(18, 0, -20)),
        pose(1, 0, 0, r=(-48, -35, -28), l=(-48, 35, 28), rk=(0, 0, 10), lk=(0, 0, -10)),
        pose(-.5, -.6, -8, r=(-35, -15, 55), l=(-60, 15, -30), rk=(12, 0, 6), lk=(-40, 0, -12)),
    ]),
    "sturdy_drill": ("Sturdy (drill)", "n_exodus_shuffle: 3–7 s", 3, [
        pose(2, -.5, -12, r=(-35, -15, 55), l=(20, 10, -35), rk=(-10, 0, 18), lk=(-42, 0, -14), lean=12),
        pose(5, 0, 0, r=(-30, -25, 25), l=(-30, 25, -25), rk=(-45, 0, 30), lk=(-45, 0, -30), lean=15),
        pose(-1, .7, 18, r=(20, -15, 25), l=(-50, 15, -60), rk=(55, 0, 12), lk=(4, 0, -6)),
        pose(1, .4, 10, r=(-25, -12, 45), l=(15, 15, -35), rk=(-45, 0, 10), lk=(5, 0, -8), lean=8),
        pose(3, 0, 0, r=(-50, -25, 20), l=(-50, 25, -20), rk=(-25, 0, 20), lk=(-25, 0, -20), lean=15),
        pose(-1, -.7, -18, r=(-50, -15, 60), l=(20, 15, -25), rk=(4, 0, 6), lk=(55, 0, -12)),
        pose(1, -.4, -10, r=(15, -15, 35), l=(-25, 12, -45), rk=(5, 0, 8), lk=(-45, 0, -10), lean=8),
        pose(4, 0, 0, r=(-40, -20, 35), l=(-40, 20, -35), rk=(-35, 0, 25), lk=(-35, 0, -25), lean=15),
    ]),
    "aus_shuffle": ("AUS shuffle", "n_exodus_shuffle: 13–17 s", 3, [
        pose(-.5, -.4, -12, r=(25, -5, 14), l=(-45, 10, -20), rk=(-48, 0, 4), lk=(10, 0, -4), rz=-2, lz=2, lean=5),
        pose(.5, 0, -6, r=(10, -5, 12), l=(-20, 5, -12), rk=(-8, 0, 0), lk=(18, 0, 0), rz=-4, lz=3),
        pose(-1.5, .4, 12, r=(-45, -10, 20), l=(25, 5, -14), rk=(10, 0, 4), lk=(-48, 0, -4), rz=2, lz=-2, lean=5),
        pose(.5, 0, 6, r=(-20, -5, 12), l=(10, 5, -12), rk=(18, 0, 0), lk=(-8, 0, 0), rz=3, lz=-4),
        pose(-1.5, -.5, -15, r=(20, -5, 18), l=(-40, 10, -18), rk=(-45, 0, 5), lk=(15, 0, -3), rz=-2, lz=2),
        pose(.5, -.3, -22, r=(-20, -10, 20), l=(10, 5, -18), rk=(0, 25, 8), lk=(0, -25, -8), rx=-1, lx=1),
        pose(-1, .5, 18, r=(-40, -10, 18), l=(20, 5, -18), rk=(15, 0, 3), lk=(-45, 0, -5), rz=2, lz=-2),
        pose(.5, .3, 22, r=(10, -5, 18), l=(-20, 10, -20), rk=(0, -25, -8), lk=(0, 25, 8), rx=1, lx=-1),
    ]),
    "dnb_step": ("DnB step", "n_exodus_shuffle: 17–20 s", 3, [
        pose(0, -.5, -20, r=(-25, -10, 24), l=(25, 10, -18), rk=(-30, 18, 8), lk=(8, -25, -5), rz=-1.5, lz=1),
        pose(-1, 0, 0, r=(-35, -15, 30), l=(-35, 15, -30), rk=(-50, 10, -8), lk=(0, -10, 8), rx=1.5, lx=-1.5),
        pose(.6, .4, 20, r=(20, -10, 18), l=(-25, 10, -24), rk=(5, -25, 5), lk=(-25, 18, -8), rz=1, lz=-1.5),
        pose(-1, 0, 0, r=(-35, -15, 30), l=(-35, 15, -30), rk=(0, 10, -8), lk=(-50, -10, 8), rx=1.5, lx=-1.5),
        pose(0, -.5, -25, r=(-30, -20, 25), l=(20, 10, -20), rk=(-25, 25, 18), lk=(8, -15, -4), spread=2.3),
        pose(-1.2, 0, -8, r=(-15, -15, 18), l=(-15, 15, -18), rk=(15, 18, -10), lk=(-15, -18, 10), rx=1.5, lx=-1.5),
        pose(0, .5, 25, r=(20, -10, 20), l=(-30, 20, -25), rk=(8, 15, 4), lk=(-25, -25, -18), spread=2.3),
        pose(-1.2, 0, 8, r=(-15, -15, 18), l=(-15, 15, -18), rk=(-15, 18, -10), lk=(15, -18, 10), rx=1.5, lx=-1.5),
    ]),
    "jumpstyle": ("Jumpstyle", "n_exodus_shuffle: 20–24 s", 3, [
        pose(0, 0, 20, r=(-50, -10, 18), l=(35, 10, -20), rk=(-45, 0, 6), lk=(8, 0, -5), lean=7),
        pose(-2, 0, 20, r=(-25, -10, 22), l=(15, 10, -22), rk=(18, 0, 6), lk=(18, 0, -6)),
        pose(.5, 0, 20, r=(35, -10, 20), l=(-50, 10, -18), rk=(10, 0, 5), lk=(55, 0, -6), lean=7),
        pose(-2, 0, 20, r=(15, -10, 22), l=(-25, 10, -22), rk=(18, 0, 6), lk=(18, 0, -6)),
        pose(0, 0, 20, r=(35, -10, 20), l=(-50, 10, -18), rk=(8, 0, 5), lk=(-45, 0, -6), lean=7),
        pose(-2, 0, 20, r=(15, -10, 22), l=(-25, 10, -22), rk=(18, 0, 6), lk=(18, 0, -6)),
        pose(.5, 0, 20, r=(-50, -10, 18), l=(35, 10, -20), rk=(55, 0, 6), lk=(10, 0, -5), lean=7),
        pose(-2, 0, 20, r=(-25, -10, 22), l=(15, 10, -22), rk=(18, 0, 6), lk=(18, 0, -6)),
    ]),
    "hopak": ("Гопак", "n_exodus_shuffle: 31–34 s", 4, [
        pose(8, -.5, -5, r=(-70, -45, -25), l=(-70, 45, 25), rk=(-78, 0, 12), lk=(-38, 0, -35), lean=10),
        pose(9, 0, 0, r=(-70, -45, -25), l=(-70, 45, 25), rk=(-55, 0, 35), lk=(-55, 0, -35), lean=12),
        pose(8, .5, 5, r=(-70, -45, -25), l=(-70, 45, 25), rk=(-38, 0, 35), lk=(-78, 0, -12), lean=10),
        pose(7, 0, 0, r=(-70, -45, -25), l=(-70, 45, 25), rk=(-55, 0, 35), lk=(-55, 0, -35), lean=8),
        pose(8, -.5, -5, r=(-70, -45, -25), l=(-70, 45, 25), rk=(-82, 0, 8), lk=(-40, 0, -32), lean=10),
        pose(9, 0, 0, r=(-70, -45, -25), l=(-70, 45, 25), rk=(-55, 0, 35), lk=(-55, 0, -35), lean=12),
        pose(8, .5, 5, r=(-70, -45, -25), l=(-70, 45, 25), rk=(-40, 0, 32), lk=(-82, 0, -8), lean=10),
        pose(7, 0, 0, r=(-70, -45, -25), l=(-70, 45, 25), rk=(-55, 0, 35), lk=(-55, 0, -35), lean=8),
    ]),
    "slip_back": ("Slip Back", "n_exodus_shuffle: 34–37 s", 3, [
        pose(-.4, -.3, -8, r=(-20, -5, 20), l=(15, 5, -18), rk=(-38, 0, 2), lk=(3, 0, -2), rz=-1, lz=3, lean=4),
        pose(0, 0, 0, r=(-10, -5, 16), l=(8, 5, -16), rk=(-8, 0, 0), lk=(0, 0, 0), rz=-3, lz=1),
        pose(.3, .3, 8, r=(15, -5, 18), l=(-20, 5, -20), rk=(2, 0, 2), lk=(-12, 0, -2), rz=-1, lz=-1),
        pose(-.5, .3, 8, r=(15, -5, 18), l=(-20, 5, -20), rk=(3, 0, 2), lk=(-38, 0, -2), rz=3, lz=-1, lean=4),
        pose(0, 0, 0, r=(8, -5, 16), l=(-10, 5, -16), rk=(0, 0, 0), lk=(-8, 0, 0), rz=1, lz=-3),
        pose(.3, -.3, -8, r=(-20, -5, 20), l=(15, 5, -18), rk=(-12, 0, 2), lk=(2, 0, -2), rz=-1, lz=-1),
    ]),
    # IMG_30902: chest crosses, a diagonal arm sweep, return and opening.
    # Keep the close stance and small knee pulses of the outlined character.
    "larp333": ("LARP333", "pasting/IMG_30902.mp4, 1.3–8.3 s", 4, [
        pose(.3, -.25, -5, r=(-68, 8, -48), l=(-74, -8, 48), rk=(0, 0, 3), lk=(-12, 0, -4)),
        pose(-.6, .35, 5, r=(-76, 12, -52), l=(-68, -12, 52), rk=(-8, 0, 3), lk=(18, 0, -6)),
        pose(.7, .25, 3, r=(-65, 6, -44), l=(-72, -6, 44), rk=(0, 0, 5), lk=(-8, 0, -5)),
        pose(-.5, -.35, -5, r=(-74, 12, -52), l=(-78, -12, 52), rk=(12, 0, 4), lk=(0, 0, -3)),
        pose(.4, -.2, -3, r=(-70, 8, -46), l=(-68, -8, 46), rk=(-5, 0, 4), lk=(10, 0, -5)),
        pose(-.7, .4, 6, r=(-78, 14, -54), l=(-72, -14, 54), rk=(-8, 0, 3), lk=(20, 0, -6)),
        pose(.6, .2, 3, r=(-66, 8, -46), l=(-72, -8, 46), rk=(0, 0, 4), lk=(-10, 0, -4)),
        pose(-.5, -.35, -5, r=(-74, 10, -50), l=(-78, -10, 50), rk=(12, 0, 5), lk=(0, 0, -3)),
        pose(.5, -.2, -4, r=(-65, 8, -40), l=(-74, -10, 48), rk=(0, 0, 4), lk=(8, 0, -5)),
        pose(-.4, .2, 4, r=(-35, -8, 65), l=(-78, -15, 48), rk=(-6, 0, 3), lk=(15, 0, -5)),
        pose(.3, -.3, -7, r=(-16, -12, 127), l=(-78, -15, 48), rk=(0, 0, 4), lk=(-10, 0, -5)),
        pose(-.6, .35, 5, r=(-12, -10, 132), l=(-82, -18, 52), rk=(-8, 0, 3), lk=(20, 0, -6)),
        pose(.6, .2, 3, r=(-18, -12, 126), l=(-76, -12, 48), rk=(0, 0, 5), lk=(-8, 0, -5)),
        pose(-.6, -.4, -7, r=(-12, -10, 131), l=(-80, -18, 52), rk=(14, 0, 5), lk=(0, 0, -3)),
        pose(.4, -.2, -3, r=(-17, -12, 127), l=(-76, -12, 48), rk=(-5, 0, 4), lk=(8, 0, -4)),
        pose(-.7, .4, 6, r=(-12, -8, 133), l=(-82, -18, 52), rk=(-8, 0, 3), lk=(20, 0, -6)),
        pose(.6, .25, 4, r=(-18, -12, 125), l=(-76, -12, 48), rk=(0, 0, 5), lk=(-10, 0, -4)),
        pose(-.5, -.35, -6, r=(-12, -10, 131), l=(-80, -18, 52), rk=(12, 0, 5), lk=(0, 0, -3)),
        pose(.3, -.2, -4, r=(-18, -12, 126), l=(-76, -12, 48), rk=(-4, 0, 4), lk=(8, 0, -4)),
        pose(-.4, .2, 3, r=(-40, -8, 65), l=(-65, -8, 40), rk=(-8, 0, 4), lk=(14, 0, -5)),
        pose(.5, -.25, -5, r=(-65, 8, -46), l=(-70, -8, 46), rk=(0, 0, 4), lk=(-10, 0, -5)),
        pose(-.6, .35, 5, r=(-76, 12, -52), l=(-72, -12, 52), rk=(-8, 0, 3), lk=(18, 0, -6)),
        pose(.6, .2, 3, r=(-68, 8, -46), l=(-72, -8, 46), rk=(0, 0, 5), lk=(-8, 0, -5)),
        pose(-.5, -.35, -5, r=(-74, 12, -52), l=(-78, -12, 52), rk=(12, 0, 4), lk=(0, 0, -3)),
        pose(.4, -.2, -3, r=(-70, 8, -46), l=(-68, -8, 46), rk=(-5, 0, 4), lk=(10, 0, -5)),
        pose(-.6, .35, 5, r=(-60, 8, -25), l=(-60, -8, 25), rk=(-8, 0, 3), lk=(18, 0, -6)),
        pose(.4, .15, 2, r=(-35, -10, 72), l=(-35, 10, -72), rk=(0, 0, 4), lk=(-8, 0, -4)),
        pose(-.5, -.35, -5, r=(-28, -8, 86), l=(-28, 8, -86), rk=(12, 0, 5), lk=(0, 0, -3)),
        pose(.3, -.2, -3, r=(-35, -10, 75), l=(-35, 10, -75), rk=(-5, 0, 4), lk=(8, 0, -4)),
        pose(-.5, .35, 5, r=(-42, 0, 45), l=(-42, 0, -45), rk=(-8, 0, 3), lk=(18, 0, -5)),
        pose(.3, .15, 2, r=(-58, 6, -20), l=(-58, -6, 20), rk=(0, 0, 4), lk=(-8, 0, -4)),
        pose(-.4, -.2, -3, r=(-72, 10, -48), l=(-76, -10, 48), rk=(10, 0, 4), lk=(0, 0, -3)),
    ]),
    # IMG_58509: four distinct phrases, followed by pointing/cross steps.
    # Each four-pose cell is one alternating heel/knee step cycle.
    "larp333_2": ("LARP333 2", "pasting/IMG_58509.mp4, 2.5–14.4 s", 4, [
        # Low sweeps and heel lifts (2.5–5.8 s).
        pose(2, -.9, -12, r=(-25, -12, 38), l=(-65, -12, 45), rk=(8, 0, 12), lk=(45, 0, -8), spread=2.2, lean=20),
        pose(-.8, 0, 0, r=(-38, -10, 18), l=(-38, 10, -18), rk=(18, 0, 5), lk=(18, 0, -5), lean=12),
        pose(2, .9, 12, r=(-65, 12, -45), l=(-25, 12, -38), rk=(45, 0, 8), lk=(8, 0, -12), spread=2.2, lean=20),
        pose(.5, .3, 5, r=(-50, 12, -25), l=(-45, 10, -24), rk=(25, 0, 6), lk=(0, 0, -5), rx=.6, lx=-.6, lean=14),
        pose(2, -.9, -12, r=(-25, -12, 38), l=(-65, -12, 45), rk=(8, 0, 12), lk=(45, 0, -8), spread=2.2, lean=20),
        pose(-.8, 0, 0, r=(-38, -10, 18), l=(-38, 10, -18), rk=(18, 0, 5), lk=(18, 0, -5), lean=12),
        pose(2, .9, 12, r=(-65, 12, -45), l=(-25, 12, -38), rk=(45, 0, 8), lk=(8, 0, -12), spread=2.2, lean=20),
        pose(.5, .3, 5, r=(-50, 12, -25), l=(-45, 10, -24), rk=(25, 0, 6), lk=(0, 0, -5), rx=.6, lx=-.6, lean=14),
        pose(2.2, -1, -15, r=(-28, -15, 45), l=(-62, -15, 48), rk=(5, 0, 14), lk=(50, 0, -10), spread=2.3, lean=22),
        pose(-.8, 0, 0, r=(-40, -10, 20), l=(-40, 10, -20), rk=(20, 0, 5), lk=(20, 0, -5), lean=12),
        pose(2.2, 1, 15, r=(-62, 15, -48), l=(-28, 15, -45), rk=(50, 0, 10), lk=(5, 0, -14), spread=2.3, lean=22),
        pose(.5, .3, 5, r=(-52, 15, -28), l=(-45, 10, -25), rk=(28, 0, 6), lk=(0, 0, -5), rx=.8, lx=-.8, lean=14),
        pose(2, -.9, -12, r=(-25, -12, 38), l=(-65, -12, 45), rk=(8, 0, 12), lk=(45, 0, -8), spread=2.2, lean=20),
        pose(-.8, 0, 0, r=(-38, -10, 18), l=(-38, 10, -18), rk=(18, 0, 5), lk=(18, 0, -5), lean=12),
        pose(2, .9, 12, r=(-65, 12, -45), l=(-25, 12, -38), rk=(45, 0, 8), lk=(8, 0, -12), spread=2.2, lean=20),
        pose(.5, .3, 5, r=(-45, 0, 55), l=(-30, 10, -18), rk=(25, 0, 6), lk=(0, 0, -5), lean=14),
        # Diagonal sweep with the other arm behind the hip (5.9–9.0 s).
        pose(1.6, -.9, -12, r=(-18, -15, 128), l=(18, 8, -12), rk=(8, 0, 12), lk=(46, 0, -10), lean=12),
        pose(-1.2, 0, 0, r=(-12, -12, 132), l=(22, 8, -10), rk=(20, 0, 4), lk=(20, 0, -4), lean=7),
        pose(1.6, .9, 12, r=(-20, -15, 126), l=(15, 8, -15), rk=(46, 0, 10), lk=(8, 0, -12), lean=12),
        pose(.4, .3, 5, r=(-15, -12, 130), l=(20, 8, -12), rk=(28, 0, 5), lk=(0, 0, -5), rx=.8, lx=-.8, lean=8),
        pose(1.6, -.9, -12, r=(-18, -15, 128), l=(18, 8, -12), rk=(8, 0, 12), lk=(46, 0, -10), lean=12),
        pose(-1.2, 0, 0, r=(-12, -12, 132), l=(22, 8, -10), rk=(20, 0, 4), lk=(20, 0, -4), lean=7),
        pose(1.6, .9, 12, r=(-20, -15, 126), l=(15, 8, -15), rk=(46, 0, 10), lk=(8, 0, -12), lean=12),
        pose(.4, .3, 5, r=(-15, -12, 130), l=(20, 8, -12), rk=(28, 0, 5), lk=(0, 0, -5), rx=.8, lx=-.8, lean=8),
        pose(1.8, -1, -14, r=(-18, -15, 128), l=(18, 8, -12), rk=(8, 0, 14), lk=(50, 0, -10), lean=14),
        pose(-1.2, 0, 0, r=(-12, -12, 132), l=(22, 8, -10), rk=(20, 0, 4), lk=(20, 0, -4), lean=7),
        pose(1.8, 1, 14, r=(-20, -15, 126), l=(15, 8, -15), rk=(50, 0, 10), lk=(8, 0, -14), lean=14),
        pose(.4, .3, 5, r=(-15, -12, 130), l=(20, 8, -12), rk=(28, 0, 5), lk=(0, 0, -5), rx=.8, lx=-.8, lean=8),
        pose(1.6, -.9, -12, r=(-18, -15, 128), l=(18, 8, -12), rk=(8, 0, 12), lk=(46, 0, -10), lean=12),
        pose(-1.2, 0, 0, r=(-12, -12, 132), l=(22, 8, -10), rk=(20, 0, 4), lk=(20, 0, -4), lean=7),
        pose(1.6, .9, 12, r=(-20, -15, 122), l=(15, 8, -15), rk=(46, 0, 10), lk=(8, 0, -12), lean=12),
        pose(.4, .3, 5, r=(-55, 5, 25), l=(-55, -5, 25), rk=(28, 0, 5), lk=(0, 0, -5), lean=8),
        # Forearms crossed in front of the face (9.1–11.1 s).
        pose(1.5, -.8, -10, r=(-82, 15, -50), l=(-88, -15, 50), rk=(8, 0, 10), lk=(46, 0, -8), lean=8),
        pose(-1.4, 0, 0, r=(-88, 18, -54), l=(-92, -18, 54), rk=(20, 0, 4), lk=(20, 0, -4), lean=4),
        pose(1.5, .8, 10, r=(-82, 15, -50), l=(-88, -15, 50), rk=(46, 0, 8), lk=(8, 0, -10), lean=8),
        pose(.3, .3, 5, r=(-85, 16, -52), l=(-90, -16, 52), rk=(28, 0, 5), lk=(0, 0, -5), rx=.8, lx=-.8, lean=5),
        pose(1.5, -.8, -10, r=(-82, 15, -50), l=(-88, -15, 50), rk=(8, 0, 10), lk=(46, 0, -8), lean=8),
        pose(-1.4, 0, 0, r=(-88, 18, -54), l=(-92, -18, 54), rk=(20, 0, 4), lk=(20, 0, -4), lean=4),
        pose(1.5, .8, 10, r=(-82, 15, -50), l=(-88, -15, 50), rk=(46, 0, 8), lk=(8, 0, -10), lean=8),
        pose(.3, .3, 5, r=(-85, 16, -52), l=(-90, -16, 52), rk=(28, 0, 5), lk=(0, 0, -5), rx=.8, lx=-.8, lean=5),
        pose(1.5, -.8, -10, r=(-82, 15, -50), l=(-88, -15, 50), rk=(8, 0, 10), lk=(46, 0, -8), lean=8),
        pose(-1.1, 0, 0, r=(-60, 8, 35), l=(-60, -8, -35), rk=(18, 0, 4), lk=(18, 0, -4), lean=4),
        # Wide opening, then push both hands forward (11.2–13.2 s).
        pose(1.3, .8, 10, r=(-12, -10, 88), l=(-12, 10, -88), rk=(42, 0, 8), lk=(8, 0, -10), lean=7),
        pose(.3, .3, 5, r=(-8, -8, 92), l=(-8, 8, -92), rk=(25, 0, 5), lk=(0, 0, -5), lean=5),
        pose(1.3, -.8, -10, r=(-12, -10, 88), l=(-12, 10, -88), rk=(8, 0, 10), lk=(42, 0, -8), lean=7),
        pose(-1.1, 0, 0, r=(-8, -8, 92), l=(-8, 8, -92), rk=(18, 0, 4), lk=(18, 0, -4), lean=4),
        pose(1.3, .8, 10, r=(-12, -10, 88), l=(-12, 10, -88), rk=(42, 0, 8), lk=(8, 0, -10), lean=7),
        pose(.3, .3, 5, r=(-8, -8, 92), l=(-8, 8, -92), rk=(25, 0, 5), lk=(0, 0, -5), lean=5),
        pose(1.3, -.8, -10, r=(-42, -5, 50), l=(-42, 5, -50), rk=(8, 0, 10), lk=(42, 0, -8), lean=7),
        pose(-1.1, 0, 0, r=(-70, -12, 20), l=(-70, 12, -20), rk=(18, 0, 4), lk=(18, 0, -4), lean=4),
        pose(1.3, .8, 10, r=(-90, -12, 12), l=(-90, 12, -12), rk=(42, 0, 8), lk=(8, 0, -10), lean=-4),
        pose(.3, .3, 5, r=(-96, -15, 8), l=(-96, 15, -8), rk=(25, 0, 5), lk=(0, 0, -5), lean=-6),
        # Forward hands and a crossing/side step, then blend into the low phrase.
        pose(1, -.8, -12, r=(-86, -15, -12), l=(-90, -8, -15), rk=(-25, 12, -8), lk=(10, -10, 8), rx=1.4, lx=-1.4, lean=-4),
        pose(-1, 0, 0, r=(-94, -8, 5), l=(-94, 8, -5), rk=(22, 0, 4), lk=(22, 0, -4), lean=-5),
        pose(1, .9, 15, r=(-88, 8, 15), l=(-84, 15, 12), rk=(10, 10, -8), lk=(-25, -12, 8), rx=1.4, lx=-1.4, lean=-4),
        pose(.2, .4, 8, r=(-92, 10, 10), l=(-90, 15, 8), rk=(0, 0, 5), lk=(-20, 0, -18), spread=2.8, lean=-3),
        pose(1.2, -.6, -10, r=(-82, -5, 0), l=(-82, -12, 8), rk=(-20, 0, 16), lk=(0, 0, -5), spread=2.8, lean=-4),
        pose(-.7, 0, 0, r=(-70, 0, 12), l=(-70, 0, -12), rk=(18, 0, 4), lk=(18, 0, -4), lean=0),
        pose(.8, .5, 8, r=(-45, 10, -28), l=(-35, 10, -25), rk=(35, 0, 8), lk=(0, 0, -6), lean=12),
        pose(-.5, 0, 0, r=(-38, -8, 18), l=(-45, 8, -18), rk=(18, 0, 4), lk=(18, 0, -4), lean=15),
    ]),
}


def crossed_arms(frame, high=False):
    """Represent the reference's bent forearms using whole rigid arms."""
    pitch, roll = (-62, 70) if high else (-46, 60)
    frame["rightArm"].update(pitch=pitch, yaw=-52, roll=-roll, z=-.4)
    frame["leftArm"].update(pitch=pitch - 4, yaw=52, roll=roll, z=-.9)


# Forearm direction differs from upper-arm direction in the reference. Use
# shoulder yaw as well as roll to reach across the chest on a model with no elbow.
for index in (*range(9), *range(20, 25), 30, 31):
    crossed_arms(DANCES["larp333"][3][index])
for index in range(10, 19):
    DANCES["larp333"][3][index]["leftArm"].update(pitch=-54, yaw=52, roll=65, z=-.8)
for index in range(32, 41):
    crossed_arms(DANCES["larp333_2"][3][index], high=True)
for index in range(16):
    frame = DANCES["larp333_2"][3][index]
    if frame["leftArm"]["roll"] > 30:
        frame["leftArm"].update(pitch=-48, yaw=50, roll=60, z=-.8)
    elif frame["rightArm"]["roll"] < -30:
        frame["rightArm"].update(pitch=-48, yaw=-50, roll=-60, z=-.8)


def icon(frame, index):
    """Draw a new transparent block-figure icon from the authored pose."""
    scale = 4
    im = Image.new("RGBA", (64 * scale, 64 * scale))
    d = ImageDraw.Draw(im)
    color = (245, 248, 255, 255)
    accent = (95, 185, 255, 255)

    def point(x, y, z=0):
        return ((32 + x * 1.65 + z * .65) * scale, (14 + y * 1.65 - z * .2) * scale)

    for key in ("rightLeg", "leftLeg", "body", "rightArm", "leftArm"):
        b = frame[key]
        pitch, roll = math.radians(b["pitch"]), math.radians(b["roll"])
        length = 11 if key == "body" else 10
        x, y, z = b["x"], b["y"], b["z"]
        end = point(x - math.sin(roll) * length,
                    y + math.cos(roll) * math.cos(pitch) * length,
                    z + math.sin(pitch) * length)
        width = (10 if key == "body" else 4) * scale
        d.line((point(x, y, z), end), fill=color, width=width)
    h = frame["head"]
    x, y = point(h["x"], h["y"] - 1)
    d.rectangle((x - 6 * scale, y - 6 * scale, x + 6 * scale, y + 4 * scale), fill=color)
    # Numeric badge keeps closely related Rampage poses distinguishable.
    if index:
        from PIL import ImageFont
        font = ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", 15 * scale)
        d.rounded_rectangle((44 * scale, 43 * scale, 63 * scale, 63 * scale), 5 * scale, fill=accent)
        d.text((53 * scale, 53 * scale), str(index), fill=(15, 25, 40), font=font, anchor="mm")
    return im.resize((64, 64), Image.Resampling.LANCZOS)


def main():
    for id_, (name, source, step, frames) in DANCES.items():
        moves = []
        for i, frame in enumerate([*frames, copy.deepcopy(frames[0])]):
            moves.append({"tick": i * step, "easing": "inOutSine", **frame})
        duration = len(frames) * step
        data = {"name": name, "author": "PizzaDLC", "description": "Hand-keyed Minecraft adaptation: " + source,
                "emote": {"degrees": True, "isLoop": id_ != "ziga", "beginTick": 0,
                          "endTick": duration, "stopTick": duration, "moves": moves}}
        (ASSETS / "emotes" / (id_ + ".json")).write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        poster = frames[{"ziga": 1, "larp333": 10, "larp333_2": 16}.get(id_, 0)]
        badge = int(id_[-1]) if id_.startswith("rampage_") else {"larp333": 1, "larp333_2": 2}.get(id_, 0)
        icon(poster, badge).save(ASSETS / "textures/emotes" / (id_ + ".png"))
        print(f"{id_}: {len(moves)} poses, {duration / 20:g} seconds")


if __name__ == "__main__":
    main()
