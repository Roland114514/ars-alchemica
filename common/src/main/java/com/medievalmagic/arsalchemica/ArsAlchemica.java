package com.medievalmagic.arsalchemica;

import net.minecraft.resources.ResourceLocation;

/**
 * 《Ars Alchemica》全局核心常量与元数据。
 *
 * @author MedievalMagic Architecture Team
 */
public final class ArsAlchemica {

    public static final String MOD_ID = "ars_alchemica";
    public static final String MOD_NAME = "Ars Alchemica";
    public static final String MOD_VERSION = "0.1.0-alpha";

    private ArsAlchemica() {}

    /**
     * 创建基于本模组命名空间的 ResourceLocation。
     * 统一入口，为后续平滑迁移至 1.21+ (ResourceLocation.fromNamespaceAndPath) 提供单点适配层。
     */
    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
