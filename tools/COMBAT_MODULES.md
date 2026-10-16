# Native combat modules

AttackAura and ElytraTarget live in `client/modules/impl/combat`; ElytraMotion and
ElytraBooster live in `client/modules/impl/movement`. Overtake is the ElytraTarget
option `Элитра перегон`. The modules directly extend PizzaDLC's Module, use its
native settings, friends, module manager and Orbit event subscriptions.

Rotation algorithms live in `api/rotation/combat`. Physics, inventory and target
helpers live in `api/util/combat`; target effects live in `api/util/render/combat`.
Effects use the `pizzadlc` resource namespace. Required Minecraft hooks belong to
`pizzadlc.client.mixins.json` and explicitly disable remapping for Minecraft 26.2.

Neuro reads `config/standalone-onnx-aim/policy.bin`; the original fallback is used
when the file is missing. The user-supplied sources in `pasting/dimasik` remain
unchanged. The previous adaptation and migration scripts are recoverable from
`build/combat-refactor-before.zip` and `build/obsolete-combat-import-tools`.

`gradlew.bat --offline verifyCombatModules` checks native settings and lifecycle,
flight profiles, both overtake modes, stop/restore motion, clean capture, resource
hashes and full injection signatures including the 26.2 WebBlock boolean argument.

AutoTotem replaces the previous module at `client/modules/impl/combat/AutoTotem`.
The supplied health, absorption, elytra, fall, crystal, TNT, armor and anchor checks
use native settings and events. `Режим свапов` and `Тики остановки` carry the source
GuiMove swap options into AutoTotem. Swap button 40 targets offhand slot 45.
Original offhand items are tracked across totem pops; pending clicks are discarded
when their item, container, world or module state changes. Enchanted totems are
preserved when a normal totem exists, with an enchanted fallback; protected
Dragonite totems are skipped by their supplied custom-name prefixes.
`gradlew.bat --offline verifyAutoTotem` exercises these paths using actual
Minecraft item stacks, InventoryMenu slots and entity-status packets.


InventoryMove adapts the supplied GuiMove to the existing keyboard hook, typing
screen exclusions and mouse/keyboard key mappings. Its Grim bypass and movement
blockers use the native SwapSystem. SwapSettings retains the existing packet
profiles and adds the source task modes: Rage, Legit (stationary input, then a
configurable tick hold) and Matrix (sprint-free swaps). AutoTotem uses the global
mode by default; its previous per-module overrides remain loadable.
InventorySwapQueue serializes complete operations, suppresses duplicate named
tasks and cancels stale module/world/container actions. Partial firework tasks
restore their selected slot and borrowed item when the owner is disabled.

ElytraHelper incorporates ElytraSwap's best-chestplate selection, armor swap,
bound rocket and optional takeoff. Legit rockets keep the borrowed hotbar slot
for one tick, then restore it; inventory rockets also pause movement. Automatic
ElytraTarget rockets use the same global settings. Shield breaking queues the
complete weapon/attack/restore operation so delayed clicks cannot separate it.

ElytraTarget can replace a flying target beyond the configurable escape distance
(default 12 blocks) with the closest valid grounded candidate inside that distance.
AttackAura applies the existing friends/bots/type filters before this choice.
ElytraMotion restores saved flight velocity and avoids stopping beside that ground
replacement; normal stopping resumes when the target changes or becomes airborne.

TargetPearl uses the supplied trajectory search, 320-step drag/gravity integration,
entity/water checks, owner inference, target memory and optional jump compensation.
The old activation/bind/target selection modes are preserved. Tracked use sends the
explicit yaw and pitch in both position and item-use packets and restores the hand
in a finally block. Only actual sends mark enemy pearls as handled. The optional
named-heart trap behavior retains the source ReallyWorld server restriction.

`gradlew.bat --offline verifyInventoryFlight` executes queue ordering, movement
holds, delayed restores, stale contexts, actual item-use packet angles, hotbar,
inventory and offhand use, automatic pearl throwing, armor swap/takeoff and ground
retarget with flight retention against native Minecraft objects and a fake network.
Live multiplayer behavior still requires testing on the intended server.
