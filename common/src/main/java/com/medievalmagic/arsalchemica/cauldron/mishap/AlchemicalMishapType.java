package com.medievalmagic.arsalchemica.cauldron.mishap;

/**
 * 炼金灾变与失控事故分类枚举 (Alchemical Mishap / Catastrophe Type)。
 * <p>
 * 涵盖物理爆裂、环境火灾、剧毒瘴气、物质黑泥化以及干烧熔毁等多种中世纪暗黑魔法惩罚。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public enum AlchemicalMishapType {
    /** 状态安稳，无任何事故 */
    NONE("mishap.ars_alchemica.none", 0),

    /** 药性圆满定型成功（退火成功） */
    CRYSTALLIZE_SUCCESS("mishap.ars_alchemica.success", 0),

    /** 以太瘴气大爆发：失稳溢出，喷发大范围剧毒、失明与凋零滞留云 */
    TOXIC_MIASMA_OUTBREAK("mishap.ars_alchemica.toxic_miasma", 2),

    /** 炼狱烈焰蔓延：高挥发易燃质点在沸腾极温下爆燃，引发周边火海 */
    INFERNAL_CONFLAGRATION("mishap.ars_alchemica.conflagration", 2),

    /** 炼金败物凝固黑泥：浑浊度超标，溶液化为剧毒暗黑淤泥，需铁铲清除 */
    SLUDGE_COAGULATION("mishap.ars_alchemica.sludge", 1),

    /** 以太裂解大殉爆：极端相克对撞或超压，剧烈爆炸震毁坩埚并散落铁渣 */
    ARCANE_DETONATION("mishap.ars_alchemica.arcane_detonation", 3),

    /** 干烧过热熔毁：高温无水持续空烧导致金属应力极限崩溃，爆碎损毁 */
    DRY_BOIL_MELTDOWN("mishap.ars_alchemica.dry_boil_meltdown", 3);

    private final String translationKey;
    private final int severityLevel; // 0=安全, 1=轻微污损, 2=中度环境危害, 3=重度破坏

    AlchemicalMishapType(String translationKey, int severityLevel) {
        this.translationKey = translationKey;
        this.severityLevel = severityLevel;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public int getSeverityLevel() {
        return severityLevel;
    }

    public boolean isCatastrophic() {
        return this.severityLevel >= 2;
    }
}
