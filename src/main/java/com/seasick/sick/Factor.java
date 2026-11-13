package com.seasick.sick;

import net.minecraft.network.chat.Component;

/**
 * Ce qui aggrave ou soulage le mal de mer. Chaque facteur multiplie la dose reçue.
 *
 * Le mal des transports vient d'un conflit entre l'oreille interne (qui sent le bateau bouger)
 * et les yeux (qui voient un décor immobile tout proche). Regarder l'horizon remet les deux
 * d'accord ; lire, fixer ses pieds ou regarder dans une longue-vue les fâchent davantage.
 * Celui qui tient la barre anticipe les mouvements et souffre bien moins que ses passagers.
 */
public enum Factor {
    HORIZON(0.55),
    HORIZON_DIM(0.75),
    ARTIFICIAL_HORIZON(0.85),
    LOOKING_DOWN(1.35),
    CLOSE_VIEW(1.3),
    DARK(1.15),
    READING(1.5),
    BOOK(1.9),
    MAP(1.6),
    SPYGLASS(2.2),
    EYES_CLOSED(0.45),
    HELM(0.65),
    PASSENGER(1.25),
    THIRD_PERSON(0.85),
    EMPTY_STOMACH(1.3),
    PLAIN_FOOD(0.75),
    HONEY(0.6),
    HEAVY_MEAL(1.35),
    MILK(1.25),
    SPOILED(1.5),
    NAUSEA(1.8),
    RELIEF(0.4),
    SEA_LEGS(1.0),
    SUSCEPTIBILITY(1.0);

    public final double base;

    Factor(double base) {
        this.base = base;
    }

    public String key() {
        return "seasick.factor." + name().toLowerCase();
    }

    public Component label() {
        return Component.translatable(key());
    }

    /** Un facteur actif et son effet réel (certains dépendent de l'état : pied marin, sensibilité). */
    public record Active(Factor factor, double mult, int ticksLeft) {}
}
