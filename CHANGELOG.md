# Changelog

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
