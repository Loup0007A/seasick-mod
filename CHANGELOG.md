# Changelog

## 1.1.0

- **All-new look**, inspired by the best-loved HUD mods (Cold Sweat, Tough As Nails) and Patchouli books. Every texture is now hand-tuned illustration drawn at 2–4× the interface density with smooth filtering, crisp at any GUI scale.
- **Ship's instrument** next to the hotbar: a varnished wood and brass plate with a **clinometer** whose needle follows the boat's real roll, a sea-state lamp, the sailor's face and a gradient gauge.
- **Six illustrated faces** (Fine → About to be sick, plus eyes closed).
- **Screen effects**: a drifting yellow-green haze when your colour turns, dark edges breathing with the swell, **cold-sweat droplets**, and soft, curved **eyelids** for yawns and closed eyes.
- **Artificial horizon** redrawn as a glowing line with a pitch ladder and an amber boat symbol.
- **Ship's log is now a book**: parchment pages, leather cover, red ribbon. Page 1 shows a little **animated painted scene** — swell to scale, your boat rolling, a wind arrow relative to where you look. Page 2: how you feel, forecast and what's affecting you. Turn the page (arrows, ← →) for your **sea legs ranks and memories** and the **ship's doctor's advice**.
- Coloured messages, page-turn sound, new illustrated logo.
- Nouveaux graphismes : instrument de bord en laiton avec clinomètre, visages illustrés, brume, sueurs froides, paupières, horizon lumineux et journal de bord en livre de parchemin avec scène animée.

## 1.0.0

- **First release.** Real motion sickness at sea, 100% client-side, for **Forge 1.21–1.21.1**, **NeoForge 1.21–1.21.1**, **Forge 1.20.1** and **NeoForge 26.3** (OpenGL and Vulkan).
- **A simulated sea.** Wind follows the weather (breeze when clear, gusts in the rain, gales in thunderstorms) and slowly turns. Wave height and period come from the wind (Pierson–Moskowitz), limited by the open water upwind (fetch), the depth (waves break over shallows), the biome (deep ocean, river, swamp, sea ice) and any roof over your head.
- **A boat that moves like one.** Roll, pitch, yaw and heave depend on where the waves come from (head, beam, following seas) and on your speed (encounter frequency). Waves come in sets: every seventh wave is bigger. The hull creaks at the end of each roll and slams into head seas.
- **A stomach that reacts like a real one.** Sickness builds from the weighted accelerations you feel (ISO 2631-1 motion-sickness weighting, most nauseating around 0.1–0.25 Hz) and recovers over time, so every situation has its own balance.
- **What helps, what hurts**: eyes on the horizon, taking the helm, closing your eyes (hold `Z`), plain food (bread, dried kelp), honey… versus reading (inventory, chat, books, maps), staring at your feet, peering through a spyglass, being a passenger, heavy, raw or spoiled food, milk, an empty stomach, darkness.
- **Five stages** — Fine, Uneasy, Queasy, Sick, About to be sick — with yawns and heavy eyelids, a pale tint, cold sweat, gulps, a churning stomach, trembling hands and slipping oars.
- **Being sick**: two heaves to warn you, time to lean over the side (it's cleaner and you feel better), the colour of your last meal, then relief — for a while. Dry heaves when there is nothing left.
- **Sea legs**: time at sea slowly makes you resistant, from Landlubber to Sea Dog. **Land sickness**: after a long voyage, the ground keeps swaying.
- **Camels**, the ships of the desert, rock you too.
- **Ship's log** (`O`): sea state (Douglas scale), wind (Beaufort), waves, what is affecting you right now and by how much, a forecast, and your memories at sea.
- **Artificial horizon** when you can't see the real one; comfort settings (`reduceMotion`, `motionScale`, `screenEffects`, `grossEffects`, `affectControls`, `susceptibility`).
- Commands: `/seasick status | logbook | sea <state> | set <0-100> | sealegs <0-100> | vomit | landsick | reset`.
- Première version : le mal de mer, pour de vrai — houle simulée, estomac réaliste, pied marin, mal de débarquement, journal de bord. Entièrement traduit en français.
