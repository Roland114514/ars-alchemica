package com.medievalmagic.arsalchemica.potion;

import com.medievalmagic.arsalchemica.api.toxicity.IToxicityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

/**
 * 玩家毒性状态标准 POJO 实现。具备严格的数值过滤（NaN/越界防护）。
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public class PlayerToxicityData implements IToxicityData {
    private static final String TAG_TOXICITY = "AlchemicalToxicity";
    private static final String TAG_MAX_TOXICITY = "MaxToxicity";
    private static final String TAG_METABOLISM = "MetabolismRate";

    private float toxicity = 0.0f;
    private float maxToxicity = DEFAULT_MAX_TOXICITY;
    private float metabolismRate = DEFAULT_METABOLISM_RATE;
    private boolean dirty = false;

    @Override
    public float getToxicity() {
        return this.toxicity;
    }

    @Override
    public void setToxicity(float value) {
        if (Float.isNaN(value)) value = 0.0f;
        float clamped = Mth.clamp(value, 0.0f, this.maxToxicity);
        if (Math.abs(this.toxicity - clamped) > 1e-4f) {
            this.toxicity = clamped;
            this.dirty = true;
        }
    }

    @Override
    public void addToxicity(float delta) {
        if (Float.isNaN(delta) || Math.abs(delta) < 1e-5f) return;
        setToxicity(this.toxicity + delta);
    }

    @Override
    public float getMaxToxicity() {
        return this.maxToxicity;
    }

    @Override
    public void setMaxToxicity(float max) {
        if (Float.isNaN(max) || max < 1.0f) max = 1.0f;
        if (Math.abs(this.maxToxicity - max) > 1e-4f) {
            this.maxToxicity = max;
            this.toxicity = Mth.clamp(this.toxicity, 0.0f, this.maxToxicity);
            this.dirty = true;
        }
    }

    @Override
    public float getMetabolismRate() {
        return this.metabolismRate;
    }

    @Override
    public void setMetabolismRate(float rate) {
        if (Float.isNaN(rate) || rate < 0.0f) rate = 0.0f;
        this.metabolismRate = rate;
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    @Override
    public void saveNBTData(CompoundTag tag) {
        tag.putFloat(TAG_TOXICITY, this.toxicity);
        tag.putFloat(TAG_MAX_TOXICITY, this.maxToxicity);
        tag.putFloat(TAG_METABOLISM, this.metabolismRate);
    }

    @Override
    public void loadNBTData(CompoundTag tag) {
        this.maxToxicity = tag.contains(TAG_MAX_TOXICITY) ? Math.max(1.0f, tag.getFloat(TAG_MAX_TOXICITY)) : DEFAULT_MAX_TOXICITY;
        this.toxicity = Mth.clamp(tag.getFloat(TAG_TOXICITY), 0.0f, this.maxToxicity);
        this.metabolismRate = tag.contains(TAG_METABOLISM) ? Math.max(0.0f, tag.getFloat(TAG_METABOLISM)) : DEFAULT_METABOLISM_RATE;
        this.dirty = true;
    }
}
