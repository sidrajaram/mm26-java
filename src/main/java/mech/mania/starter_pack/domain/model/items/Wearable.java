package mech.mania.starter_pack.domain.model.items;

public abstract class Wearable extends Item {
    protected StatusModifier stats;

    public Wearable(StatusModifier stats) {
        super(1); // Wearables can't stack in inventory
        this.stats = stats;
    }

    public StatusModifier getStats() {
        return stats;
    }
}
