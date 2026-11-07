package com.medievalmagic.arsalchemica.recipe.knowledge;

import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家配方知识服务端会话管理器。
 * <p>
 * 提供跨生命周期、死亡继承与平台无关的玩家学者知识持久映射。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public final class PlayerKnowledgeManager {

    private static final Map<UUID, PlayerRecipeKnowledge> KNOWLEDGE_MAP = new ConcurrentHashMap<>();

    private PlayerKnowledgeManager() {}

    public static PlayerRecipeKnowledge getKnowledge(Player player) {
        if (player == null) return new PlayerRecipeKnowledge();
        return getKnowledge(player.getUUID());
    }

    public static PlayerRecipeKnowledge getKnowledge(UUID uuid) {
        if (uuid == null) return new PlayerRecipeKnowledge();
        return KNOWLEDGE_MAP.computeIfAbsent(uuid, k -> new PlayerRecipeKnowledge());
    }

    public static void setKnowledge(UUID uuid, PlayerRecipeKnowledge knowledge) {
        if (uuid != null && knowledge != null) {
            KNOWLEDGE_MAP.put(uuid, knowledge);
        }
    }

    public static void remove(UUID uuid) {
        if (uuid != null) {
            KNOWLEDGE_MAP.remove(uuid);
        }
    }

    public static void clearAll() {
        KNOWLEDGE_MAP.clear();
    }
}
