---
name: minecraft-performance-optimizer
description: Use this skill when optimizing Minecraft mod performance, diagnosing server TPS drops or client FPS lag, optimizing Block Entity ticking and networking sync, caching capability lookups, or designing lightweight multi-block machinery.
---

# Minecraft Performance Optimizer Skill

In a mega-mod containing hundreds of machines, complex multiblock networks, custom entities, and dimensions, performance optimization is critical. A poorly optimized mod will cause 5 TPS server lag or client FPS stutter.

---

## 1. Block Entity (BE) Ticking Optimization

Block Entities running `tick()` every single frame/tick are the #1 source of server tick lag.

### Rule 1: Never Tick When Inactive
Do not attach a `BlockEntityTicker` to a block entity that is idle. Return `null` from `getTicker()` if the block is disabled, has no energy, or is unpowered.

### Rule 2: Gated Server Execution
Block entity game calculations must **only** execute on the server side:
```java
if (level.isClientSide()) {
    return;
}
```

### Rule 3: Staggered / Modulo Ticking (Lazy Evaluation)
Heavy operations (such as pathfinding, scanning surrounding blocks, sorting inventories, or capability search) must NEVER happen 20 times a second.
```java
// Run heavy lookup only once per second (every 20 ticks) with phase offset:
if ((level.getGameTime() + this.worldPosition.hashCode()) % 20 == 0) {
    scanNeighboringNetworks();
}
```
*Tip: Adding `worldPosition.hashCode()` prevents all machines in a chunk from executing their heavy tick on the exact same tick, smoothing out tick spikes.*

---

## 2. Capability & Directional Lookup Caching

Calling `level.getCapability(...)` or `level.getBlockEntity(...)` on 6 sides every tick introduces massive CPU cache thrashing.

### Best Practice: Invalidation-Based Cache
1. Cache neighbor capability references (`ItemHandler`, `EnergyStorage`, `FluidHandler`) in an array `[6]`.
2. Do **not** query neighbors again until `Block#neighborChanged` or `BlockEntity#setRemoved` fires.
3. Invalidate your local cache only when a physical block update occurs on that coordinate.

---

## 3. Network Synchronization: Delta Syncs & Lightweight Tags

Syncing large NBT compounds across the network creates network buffer saturation and micro-stutters.

### Rule 1: Avoid `getUpdateTag()` Over-Serialization
Default `getUpdateTag()` returning `saveWithFullMetadata()` serializes the entire inventory and internal state when chunks sync.
- Create a lightweight `getUpdateTag()` that serializes **only what the client renderer strictly requires** (e.g. active state boolean, facing direction, current progress percentage).

### Rule 2: Delta Packets for UI & Container
- Container GUIs should sync numbers via standard `ContainerData` integers handled by vanilla menus, not raw full-sync packets.
- For custom visuals, send single-purpose event packets (e.g., `MachineStartEventPacket(pos)`) rather than pushing the entire machine state continuously.

---

## 4. Client-Side Rendering Optimization

Dynamic Block Entity Renderers (`BlockEntityRenderer`) are drawn every frame via the CPU.
- **Static Meshing First**: If a machine does not have continuously moving parts, bake its states into the chunk mesh via BlockState models and textures.
- **Frustum Culling**: Ensure any custom BER returns an accurate bounding box (`getRenderBoundingBox()`). Blocks behind the player or behind solid walls should be culled immediately.

---

## 5. Performance Diagnostics with Spark

When testing and benchmarking:
1. Run `/spark sampler --split-thread` on the test server.
2. Identify hot methods taking >1% of tick time.
3. Verify memory allocation rate: avoid creating temporary short-lived objects (e.g. `new BlockPos(...)`, `new Vec3(...)`) inside high-frequency loops. Use `BlockPos.MutableBlockPos` for iterative scans.
