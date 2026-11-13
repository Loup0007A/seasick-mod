# Seasick — consignes pour Claude

- **Toujours pousser directement sur `main`** (pas de branche, pas de PR), à la demande de Loup0007A.
- Avant de pousser : le code doit compiler pour **chaque cible**. Ensuite, vérifier le résultat de la CI : sur la branche `ci-logs`, `main/status.txt` puis, par test, `main/<test>/clienttest.log` et `main/<test>/screenshots/`.
- Après chaque push sur `main`, la CI ajoute un commit « Jars compilés … » : faire `git fetch` + rebase avant le push suivant.

## Multi-versions (même organisation que fate-flip)
- `src/main` = la variante de référence **Forge 1.21–1.21.1** (compile telle quelle avec le `build.gradle` racine).
- Les autres cibles sont dans `targets/<cible>/` (build Gradle propre + `target.json` + `overrides/`). Leurs sources sont fabriquées par `python3 tools/port.py <cible>` : blocs `//#if neoforge` / `//#else` / `//#endif` (lignes inactives préfixées `//$$ `), remplacements de `target.json`, puis fichiers de `overrides/`. `python3 tools/port.py --check` vérifie que `src/main` est cohérent.
- Ce qui diffère d'un loader à l'autre passe par `platform/Platform.java` ; d'une version de Minecraft à l'autre par `platform/Compat.java` et `platform/Gfx.java` (version 26.x entière dans `targets/neoforge-26.3/overrides`, avec `MoveInput`).
- 26.x : interface « extraite » (`GuiGraphicsExtractor`), `Identifier`, entrées en enregistrement immuable. Les gestionnaires qui touchent à Minecraft (`SeaClient`, `ClientSelfTest`) sont inscrits dans `FMLClientSetupEvent`, pas par `@EventBusSubscriber`.
- Compiler localement sans Maven : bibliothèques des branches `mc-libs-<cible>` (workflow « Export libs » ; celles de NeoForge 1.21.1, Forge 1.20.1 et NeoForge 26.3 sont identiques à celles de fate-flip). Pour 26.3 sans JDK 25, une copie des jars dont les `.class` sont ramenés à la version 65 suffit à javac 21 pour vérifier les signatures.
- CI : un job de build par cible, un job de test en jeu par couple (cible, version du loader) via `tools/ci_test.sh` (serveur : bassin, phare et bateau ; client : `debug/ClientSelfTest`), puis release (tous les jars) et CurseForge (un fichier par cible, `curseforge/upload.sh`).
- Nouvelle version = augmenter `mod_version` dans `gradle.properties` + ajouter une section `## x.y.z` en haut de `CHANGELOG.md` (notes de la release GitHub et de CurseForge).

## Le modèle
- `tools/model.py` reproduit les formules (houle, rencontre, pondération Wf, dose) pour régler les constantes hors jeu : le garder en phase avec `sea/Swell.java` et `sick/Sickness.java`.
- Tout texte : `tools/lang.py` (EN + FR) puis régénérer les fichiers de langue. Textures : `tools/textures.py`.
- Le mod reste **100 % client** : il ne change que ce que l'on voit, entend et la façon dont les commandes répondent (rien qui demande le serveur, pas de faim retirée).
