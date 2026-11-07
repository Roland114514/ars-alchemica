package com.medievalmagic.arsalchemica.village;

import com.medievalmagic.arsalchemica.ArsAlchemica;
import net.minecraft.resources.ResourceLocation;

/**
 * 药剂师村民职业元数据与工作站点定义 (Apothecary Villager Profession)。
 * <p>
 * 工作站点方块：{@code ars_alchemica:alchemical_cauldron} (多态温控大坩埚)
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public final class ApothecaryProfession {

    public static final ResourceLocation PROFESSION_ID = ArsAlchemica.id("apothecary");
    public static final ResourceLocation POI_ID = ArsAlchemica.id("apothecary_poi");

    private ApothecaryProfession() {}
}
