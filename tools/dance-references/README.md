# Dance references

The bundled dances are hand-keyed adaptations for Minecraft's six rigid model parts.
The model has no elbows or knees; low squats and bent-arm gestures are approximated
with limb rotation and local offsets. They are not motion-capture exports.

| Resource | Reference | Selected movement |
| --- | --- | --- |
| `rampage` (existing) | n_exodus_shuffle, 7–10 s, RAMPAGE | Wide squat and arm swings |
| `rampage_2` | insidix, 0–15 s; n_exodus_shuffle, 10–13 s, RAMPAGE #2 | Cross steps, outward kicks, shoulder pumps |
| `rampage_3` | jerseyyjoe, 1.5–15 s; n_exodus_shuffle, 24–27 s, RAMPAGE #3 | Raised arm, side hops, low crossed-arm swings |
| `rampage_4` | n_exodus_shuffle, 27–31 s, RAMPAGE #4 | Alternating knee lifts and crossing arm sweeps |
| `sturdy_drill` | n_exodus_shuffle, 3–7 s, sturdy (drill) | Low bounce, heel kicks and alternating knee lifts |
| `aus_shuffle` | n_exodus_shuffle, 13–17 s, AUS style shuffle | Running-man slide and T-step |
| `dnb_step` | n_exodus_shuffle, 17–20 s, dnb step | Heel-toe pivots, crossing feet and short kicks |
| `jumpstyle` | n_exodus_shuffle, 20–24 s, jumpstyle | Alternating forward and backward kicks with hops |
| `hopak` | n_exodus_shuffle, 31–34 s, гопак (в присядку) | Low alternating kicks with folded arms |
| `slip_back` | n_exodus_shuffle, 34–37 s, slip Back | Alternating planted and sliding feet |
| `larp333` | User-provided `pasting/IMG_30902.mp4`, 1.3–8.3 s | Close-stance bounce, chest crosses, diagonal arm sweep and opening; 6.4 s loop |
| `larp333_2` | User-provided `pasting/IMG_58509.mp4`, 2.5–14.4 s | Low heel/knee steps, diagonal sweep, face-level crosses, wide opening and forward hands; 12 s loop |

Sources:

- https://www.tiktok.com/@insidix/video/7690138063141391636
- https://www.tiktok.com/@jerseyyjoe/video/7576056486879612173
- https://www.tiktok.com/@n_exodus_shuffle/video/7643762095183367444
- https://www.tiktok.com/@onda871/video/7664039351491300626
- https://www.tiktok.com/@cheeseqwc/video/7675501461207289106

Run `python tools/generate_dance_emotes.py` to regenerate the eleven dance animations
and the one-shot `ziga` gesture (3.6 seconds),
and their pose icons. Source videos and contact sheets are development references;
they are outside `src/main/resources` and are not bundled in the mod.

The LARP333 clips were inspected from the supplied local MP4 files after online
viewing failed. Both dances have complete six-part poses, matching first/last
frames and distinct numbered icons. Their additions retain all existing registry
indices; the wheel now holds 25 entries across three scrollable pages.
Run `python tools/preview_dance_reference.py pasting/IMG_30902.mp4` (or the second
filename) to regenerate a timestamped contact sheet under `build/dance-previews`.

`ziga` temporarily uses `textures/emotes/skins/ziga.png`, copied unchanged from
`pasting/skin.png`. The client overrides skin reads only while that emote is
playing. Stopping it, switching emotes, finishing it or clearing playback restores
the normal skin. PlayerInfo and account data are not modified. Its skin uses the
wide player model and takes precedence over Custom Models during playback.
