package net.minecraft.world.damagesource;

public class DamageSources {
    private final DamageSource inFire = new DamageSource();
    private final DamageSource onFire = new DamageSource();
    private final DamageSource magic = new DamageSource();
    private final DamageSource lightning = new DamageSource();

    public DamageSource inFire() { return inFire; }
    public DamageSource onFire() { return onFire; }
    public DamageSource magic() { return magic; }
    public DamageSource lightningBolt() { return lightning; }
}
