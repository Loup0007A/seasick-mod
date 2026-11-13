package com.seasick.sea;

/** Ce qui vous berce en ce moment. */
public enum Context {
    /** Pieds sur terre (ou rien qui bouge). */
    LAND(false),
    /** Dans un bateau ou un radeau, sur l'eau. */
    BOAT(true),
    /** Dans un bateau, mais sur la glace ou la terre : pas de vagues, seulement les virages. */
    BOAT_DRY(false),
    /** En train de nager, ballotté par les vagues. */
    SWIM(true),
    /** Sur un dromadaire : le « vaisseau du désert ». */
    CAMEL(false);

    /** Les vagues vous portent. */
    public final boolean onWater;

    Context(boolean onWater) {
        this.onWater = onWater;
    }

    public boolean moving() {
        return this != LAND;
    }
}
