package com.medievalmagic.arsalchemica.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * 《Ars Alchemica》Fabric 客户端入口类。
 *
 * @author Ars Alchemica Architecture Team
 */
@Environment(EnvType.CLIENT)
public class ArsAlchemicaFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 注册客户端 HUD 与透视渲染器
    }
}
