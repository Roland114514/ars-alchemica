---
name: minecraft-mod-architect
description: Use this skill when designing, scaffolding, or implementing Minecraft mod architecture, multi-loader setups (NeoForge/Fabric/Architectury), registry abstraction, defensive Mixins, data attachments/capabilities, or DataGen pipelines.
---

# Minecraft Mod Architect Skill

This skill provides the architectural foundation and code patterns for building modular, high-maintainability, multi-platform Minecraft mods (supporting modern Minecraft 1.20.x and 1.21.x on NeoForge and Fabric).

---

## 1. Architectural Philosophy: The Common-First Multi-Loader

In a mega-mod, **80–90% of all code must reside in the platform-agnostic `common` module**.
- `common/`: Core game logic, block/item logic, abstract networking, registries definitions, DataGen. Zero direct imports of `net.neoforged.*` or `net.fabricmc.*`.
- `neoforge/`: NeoForge event hooks, `IEventBus`, deferred registers, NeoForge capabilities / attachments.
- `fabric/`: Fabric entrypoints (`ModInitializer`), Fabric API events, Fabric registry hooks.

### Pattern A: Service Locator (`ServiceLoader`)
When `common` needs platform-specific behavior (e.g., config directory, energy capability, client check):
```java
// In common:
public interface PlatformHelper {
    boolean isClientSide();
    boolean isModLoaded(String modId);
    Path getConfigDir();
}

public class Services {
    public static final PlatformHelper PLATFORM = load(PlatformHelper.class);

    public static <T> T load(Class<T> clazz) {
        return ServiceLoader.load(clazz)
            .findFirst()
            .orElseThrow(() -> new NullPointerException("No service found for " + clazz.getName()));
    }
}
```
Implement in `neoforge` and `fabric` modules and declare in `META-INF/services/com.example.mymod.PlatformHelper`.

---

## 2. Deferred & Unified Registry Pattern

Do not scatter direct registry calls. Define a clean supplier-based registry abstraction:
1. Provide a `RegistrationProvider<T>` interface in `common`.
2. Wrap `DeferredRegister` in `neoforge` and direct `Registry.register` in `fabric`.
3. In `common`, declare all items, blocks, block entities, and menus as suppliers:
   ```java
   public static final RegistryObject<Item> QUANTUM_CORE = 
       ITEMS.register("quantum_core", () -> new Item(new Item.Properties()));
   ```

---

## 3. Defensive Mixin Guidelines

Mixins are the #1 cause of modpack crashes. Apply strict defensive rules:
1. **Target Interfaces First**: Inject into shared interfaces or abstract classes rather than monolithic final classes when possible.
2. **Minimal Injections**: Keep `@Inject` code under 5 lines. Delegate immediately to a static helper method in `common`:
   ```java
   @Inject(method = "tick", at = @At("TAIL"))
   private void onPlayerTick(CallbackInfo ci) {
       PlayerHookHandler.handleTick((Player)(Object)this);
   }
   ```
3. **Avoid `@Redirect` & `@Overwrite`**: Prefer `@Inject` with `cancellable = true` or `@ModifyVariable` / `@WrapOperation`.
4. **Soft Interop**: When touching other mods, use `require = 0` in `@Inject` annotations and check `Services.PLATFORM.isModLoaded(...)`.

---

## 4. Modern Data Components (1.20.5+ / 1.21+) vs NBT

For 1.20.5+:
- Vanilla replaced arbitrary item NBT with typed **Data Components** (`DataComponentType`).
- Define custom data components via `DataComponentType.builder().persistent(Codec).networkSynchronized(StreamCodec).build()`.
- Avoid storing mutable game states in items; keep items functional and immutable.

---

## 5. Automated Data Generation (DataGen)

**Never manually write JSON for items, blocks, recipes, loot tables, or tags.**
- Write DataGen providers in `common` or shared data module.
- Extend `RecipeProvider`, `BlockStateModelProvider`, `ItemModelProvider`, and `LootTableProvider`.
- Trigger via Gradle task `./gradlew runData` / `./gradlew neoforge:runData`.

---

## 6. In-Game Content Visibility & Multi-Loader Creative Tabs

Every time items or blocks are added to the mod, **they must be registered to a dedicated `CreativeModeTab` across all loaders**:

### NeoForge / Forge 1.20.1
```java
public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

public static final RegistryObject<CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register(
        "main",
        () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + MOD_ID + ".main"))
                .icon(() -> new ItemStack(MY_ICON_ITEM.get()))
                .displayItems((parameters, output) -> {
                    output.accept(MY_ITEM.get());
                    output.accept(MY_BLOCK_ITEM.get());
                })
                .build()
);
```

### Fabric 1.20.1
```java
CreativeModeTab mainTab = FabricItemGroup.builder()
        .title(Component.translatable("itemGroup." + MOD_ID + ".main"))
        .icon(() -> new ItemStack(myIconItem))
        .displayItems((parameters, output) -> {
            output.accept(myItem);
            output.accept(myBlockItem);
        })
        .build();
Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, new ResourceLocation(MOD_ID, "main"), mainTab);
```

### Mandatory Datapack Coverage
1. **Loot Tables**: Every block must have `loot_tables/blocks/<name>.json`.
2. **Recipes**: All machinery, tools, and consumables must have `recipes/<name>.json`.
3. **Tags**: Blocks must be in `mineable/pickaxe` or `mineable/axe` and tool tier tags. Items must have functional tags.

