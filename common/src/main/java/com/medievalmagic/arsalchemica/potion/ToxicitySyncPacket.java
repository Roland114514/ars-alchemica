package com.medievalmagic.arsalchemica.potion;

import com.medievalmagic.arsalchemica.ArsAlchemica;
import com.medievalmagic.arsalchemica.api.toxicity.ToxicityThreshold;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.function.Supplier;

/**
 * 极简二进制毒性网络同步包 (S2C)。
 * <p>
 * 单包体积仅 9 Bytes (Float toxicity + Float maxToxicity + Byte stateMask)，彻底消除 NBT 字符串冗余。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public record ToxicitySyncPacket(float toxicity, float maxToxicity, byte stateMask) {

    public static final ResourceLocation ID = ArsAlchemica.id("toxicity_sync");

    /**
     * 极简二进制编码：仅写入 9 字节。
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeFloat(this.toxicity);
        buf.writeFloat(this.maxToxicity);
        buf.writeByte(this.stateMask);
    }

    /**
     * 极简二进制解码：精准读取 9 字节。
     */
    public static ToxicitySyncPacket decode(FriendlyByteBuf buf) {
        float tox = buf.readFloat();
        float maxTox = buf.readFloat();
        byte mask = buf.readByte();
        return new ToxicitySyncPacket(tox, maxTox, mask);
    }

    public ToxicityThreshold getDecodedThreshold() {
        int idx = this.stateMask & 0x03;
        ToxicityThreshold[] values = ToxicityThreshold.values();
        return idx < values.length ? values[idx] : ToxicityThreshold.SAFE;
    }

    /**
     * 客户端接收逻辑。
     */
    public void handleClient(Supplier<Player> clientPlayerSupplier) {
        Player player = clientPlayerSupplier.get();
        if (player == null) return;

        PlayerToxicityManager.getData(player).ifPresent(data -> {
            data.setMaxToxicity(this.maxToxicity);
            data.setToxicity(this.toxicity);
            data.setDirty(false);
        });
    }
}
