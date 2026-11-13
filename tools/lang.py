#!/usr/bin/env python3
"""Textes du mod (anglais et français) : python3 tools/lang.py régénère les fichiers de langue."""
import json, os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'seasick', 'lang')

T = {}
def t(key, en, fr):
    T[key] = (en, fr)

t('key.categories.seasick', 'Seasick', 'Seasick')
t('key.category.seasick.main', 'Seasick', 'Seasick')
t('key.seasick.close_eyes', 'Close your eyes (hold)', 'Fermer les yeux (maintenir)')
t('key.seasick.logbook', "Ship's log", 'Journal de bord')

stages = [('Fine', 'En forme'), ('Uneasy', 'Barbouillé'), ('Queasy', 'Nauséeux'), ('Sick', 'Malade'), ('About to be sick', 'Au bord du malaise')]
for i, (en, fr) in enumerate(stages):
    t(f'seasick.stage.{i}', en, fr)
t('seasick.stage.up.1', 'You feel a little uneasy…', 'Vous vous sentez un peu barbouillé…')
t('seasick.stage.up.2', 'Your mouth waters, your stomach turns…', "Vous salivez, l'estomac se retourne…")
t('seasick.stage.up.3', 'Cold sweat. You feel sick.', 'Sueurs froides. Vous êtes malade.')
t('seasick.stage.up.4', 'Oh no…', 'Oh non…')
t('seasick.better', 'Your stomach settles.', 'Votre estomac se calme.')

t('seasick.tip.horizon', 'Look at the horizon.', "Regardez l'horizon.")
t('seasick.tip.helm', 'Take the helm: whoever steers rarely gets sick.', 'Prenez la barre : celui qui pilote est rarement malade.')
t('seasick.tip.eyes', 'Close your eyes for a moment (hold %s).', 'Fermez les yeux un moment (%s enfoncé).')
t('seasick.tip.plain_food', 'Eat something plain: bread, dried kelp.', 'Mangez du sec : pain, algues séchées.')
t('seasick.tip.honey', 'A honey bottle settles the stomach.', "Une fiole de miel apaise l'estomac.")
t('seasick.tip.ashore', 'Get your feet back on solid ground.', 'Retrouvez la terre ferme.')
t('seasick.tip.no_reading', "Don't read at sea.", 'Ne lisez pas en mer.')

factors = {
    'horizon': ('Eyes on the horizon', "Les yeux sur l'horizon"),
    'horizon_dim': ('Horizon, barely visible', 'Horizon à peine visible'),
    'artificial_horizon': ('Artificial horizon', 'Horizon artificiel'),
    'looking_down': ('Staring at your feet', 'Le nez sur ses pieds'),
    'close_view': ('Nothing to see but a wall', 'Le regard bloqué par un mur'),
    'dark': ('No horizon in the dark', "Pas d'horizon dans le noir"),
    'reading': ('Reading at sea', 'Lire en mer'),
    'book': ('A book at sea', 'Un livre en mer'),
    'map': ('Reading a map', 'Le nez dans la carte'),
    'spyglass': ('Spyglass', 'Longue-vue'),
    'eyes_closed': ('Eyes closed', 'Yeux fermés'),
    'helm': ('At the helm', 'À la barre'),
    'passenger': ('Passenger', 'Passager'),
    'third_person': ('Seen from outside', 'Vue extérieure'),
    'empty_stomach': ('Empty stomach', 'Estomac vide'),
    'plain_food': ('Something plain in the stomach', "Un peu de sec dans l'estomac"),
    'honey': ('Honey settles the stomach', 'Le miel apaise'),
    'heavy_meal': ('Heavy meal', 'Repas lourd'),
    'milk': ('Milk', 'Du lait'),
    'spoiled': ('Something spoiled', 'Quelque chose de gâté'),
    'nausea': ('Nausea', 'Nausée'),
    'relief': ('Relief', 'Soulagement'),
    'sea_legs': ('Sea legs', 'Pied marin'),
    'susceptibility': ('Your sensitivity (settings)', 'Votre sensibilité (réglages)'),
}
for k, (en, fr) in factors.items():
    t(f'seasick.factor.{k}', en, fr)

t('seasick.food.wont_stay', "That won't stay down…", 'Ça ne va pas rester…')
t('seasick.food.honey', 'The honey settles your stomach a little.', 'Le miel apaise un peu votre estomac.')
t('seasick.food.heavy', 'Heavy food at sea… bad idea.', 'Manger lourd en mer… mauvaise idée.')

t('seasick.vomit.lean', "You're going to be sick — lean over the side!", 'Vous allez être malade — penchez-vous par-dessus bord !')
t('seasick.vomit.coming', "You're going to be sick…", 'Ça vient…')
t('seasick.vomit.dry', 'Nothing left but dry heaves.', 'Il ne reste plus que des haut-le-cœur à sec.')
t('seasick.vomit.clean', 'Over the rail. You feel better… for now.', "Par-dessus bord. Ça va mieux… pour l'instant.")
t('seasick.vomit.messy', 'Right in the boat. Lovely.', 'En plein dans le bateau. Charmant.')

t('seasick.land.start', 'The ground still seems to sway…', 'Le sol semble encore tanguer…')
t('seasick.land.end', 'Your land legs are back.', 'Vous avez retrouvé vos jambes de terrien.')

ranks = [('Landlubber', 'Terrien', 'Every wave is the enemy.', 'Chaque vague est une ennemie.'),
         ('Deckhand', 'Mousse', "You're getting used to it.", 'Vous commencez à vous habituer.'),
         ('Sailor', 'Matelot', 'The sea no longer scares you.', 'La mer ne vous fait plus peur.'),
         ('Old Salt', 'Marin aguerri', 'Storms barely bother you.', 'Les tempêtes vous impressionnent peu.'),
         ('Sea Dog', 'Loup de mer', 'Born on a boat.', 'Né sur un bateau.')]
for i, (en, fr, den, dfr) in enumerate(ranks):
    t(f'seasick.rank.{i}', en, fr)
    t(f'seasick.rank.desc.{i}', den, dfr)
t('seasick.rank.toast', 'Sea legs', 'Pied marin')

douglas = [('Calm (glassy)', 'Calme (plate)'), ('Calm (rippled)', 'Calme (ridée)'), ('Smooth', 'Belle'), ('Slight', 'Peu agitée'),
           ('Moderate', 'Agitée'), ('Rough', 'Forte'), ('Very rough', 'Très forte'), ('High', 'Grosse'), ('Very high', 'Très grosse'),
           ('Phenomenal', 'Énorme')]
for i, (en, fr) in enumerate(douglas):
    t(f'seasick.douglas.{i}', en, fr)
beaufort = [('calm', 'calme'), ('light air', 'très légère brise'), ('light breeze', 'légère brise'), ('gentle breeze', 'petite brise'),
            ('moderate breeze', 'jolie brise'), ('fresh breeze', 'bonne brise'), ('strong breeze', 'vent frais'), ('near gale', 'grand frais'),
            ('gale', 'coup de vent'), ('strong gale', 'fort coup de vent'), ('storm', 'tempête'), ('violent storm', 'violente tempête'),
            ('hurricane force', 'ouragan')]
for i, (en, fr) in enumerate(beaufort):
    t(f'seasick.beaufort.{i}', en, fr)
headings = [('following seas', 'mer de l\'arrière'), ('quartering seas', 'mer de trois-quarts arrière'), ('beam seas', 'mer de travers'),
            ('bow seas', 'mer de trois-quarts avant'), ('head seas', 'mer de face')]
for i, (en, fr) in enumerate(headings):
    t(f'seasick.heading.{i}', en, fr)
compass_en = ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW']
compass_fr = ['N', 'NE', 'E', 'SE', 'S', 'SO', 'O', 'NO']
for i in range(8):
    t(f'seasick.compass.{i}', compass_en[i], compass_fr[i])

t('seasick.logbook.title', "Ship's log", 'Journal de bord')
t('seasick.logbook.sea', 'THE SEA', 'LA MER')
t('seasick.logbook.you', 'YOU', 'VOUS')
t('seasick.logbook.why', "WHAT'S AFFECTING YOU", 'CE QUI JOUE')
t('seasick.logbook.log', 'MEMORIES', 'SOUVENIRS')
t('seasick.logbook.period', 'Waves every %s s · %s', 'Vagues toutes les %s s · %s')
t('seasick.logbook.wind', 'Wind force %s (%s) from %s', 'Vent force %s (%s) de %s')
t('seasick.logbook.water', '%s blocks of open water upwind · depth %s', "%s blocs d'eau libre au vent · fond %s")
t('seasick.logbook.covered', 'Sheltered: no wind under a roof.', "À l'abri : pas de vent sous un toit.")
t('seasick.logbook.preset', 'Sea forced by command: %s', 'Mer imposée par commande : %s')
t('seasick.logbook.camel', 'On a camel: the ship of the desert.', 'À dos de dromadaire : le vaisseau du désert.')
t('seasick.logbook.dry', 'Boat out of the water: only the turns shake you.', 'Bateau hors de l\'eau : seuls les virages secouent.')
t('seasick.logbook.ashore', 'You are on solid ground.', 'Vous êtes sur la terre ferme.')
t('seasick.logbook.land', 'Land sickness: %s s left', 'Mal de débarquement : encore %s s')
t('seasick.logbook.eyes', 'Eyes closed.', 'Yeux fermés.')
t('seasick.logbook.nothing', 'Nothing in particular.', 'Rien de particulier.')
t('seasick.logbook.legs', 'Sea legs: %s%%', 'Pied marin : %s %%')
t('seasick.logbook.time', '%sh%s at sea · %s blocks · %s voyages', '%sh%s en mer · %s blocs · %s traversées')
t('seasick.logbook.sick', 'Sick %s times (%s over the rail) · %s dry heaves', 'Malade %s fois (%s par-dessus bord) · %s à sec')
t('seasick.logbook.calm', 'Longest voyage without being sick: %s:%s', 'Plus longue traversée sans être malade : %s:%s')

t('seasick.forecast.sick', 'If nothing changes: sick in %s:%s', 'Si rien ne change : malade dans %s:%s')
t('seasick.forecast.later', 'If nothing changes: you will be sick', 'Si rien ne change : vous serez malade')
t('seasick.forecast.better', 'You are getting better', 'Vous allez mieux')
t('seasick.forecast.settle', 'If nothing changes: you will settle at "%s"', 'Si rien ne change : vous resterez « %s »')

t('seasick.cmd.status', '%s (%s%%) · Sea: %s (%s m) · Wind force %s · Sea legs %s%%',
  '%s (%s %%) · Mer : %s (%s m) · Vent force %s · Pied marin %s %%')
t('seasick.cmd.unknown', 'Unknown sea state: %s', 'État de la mer inconnu : %s')
t('seasick.cmd.sea', 'Sea set to: %s', 'Mer imposée : %s')
t('seasick.cmd.sealegs', 'Sea legs: %s%%', 'Pied marin : %s %%')
t('seasick.cmd.reset', 'Ship\'s log cleared.', 'Journal de bord effacé.')

os.makedirs(OUT, exist_ok=True)
for i, name in enumerate(['en_us', 'fr_fr']):
    with open(os.path.join(OUT, name + '.json'), 'w', encoding='utf-8') as f:
        json.dump({k: v[i] for k, v in T.items()}, f, ensure_ascii=False, indent=2)
        f.write('\n')
print(f'{len(T)} textes écrits')
