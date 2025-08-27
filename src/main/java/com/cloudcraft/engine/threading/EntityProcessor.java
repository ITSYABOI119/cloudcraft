package com.cloudcraft.engine.threading;

import com.cloudcraft.engine.CloudCraftEngine;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.*;
import java.util.UUID;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Collectors;

/**
 * Optimized entity processor using differential snapshots and async processing
 * Implements the snapshot-process-apply pattern to work around Bukkit's
 * threading constraints
 */
public class EntityProcessor {
    private final CloudCraftEngine plugin;

    // Snapshot system - minimal memory footprint
    private final Map<UUID, EntitySnapshot> currentSnapshot = new ConcurrentHashMap<>();
    private final Map<UUID, EntitySnapshot> previousSnapshot = new ConcurrentHashMap<>();

    // Async processing pipeline
    private final ExecutorService asyncProcessor = Executors.newVirtualThreadPerTaskExecutor();
    private final AtomicReference<CompletableFuture<List<EntityDecision>>> pendingWork = new AtomicReference<>();
    private final AtomicReference<List<EntityDecision>> pendingResults = new AtomicReference<>();

    // Spatial culling - only process entities near players
    private static final double PROCESSING_RADIUS = 64.0; // Only process within 64 blocks of players
    private final Set<UUID> activeEntities = ConcurrentHashMap.newKeySet();

    // Performance tracking
    private final AtomicLong totalProcessTime = new AtomicLong();
    private final AtomicLong totalApplyTime = new AtomicLong();
    private final AtomicInteger entitiesProcessed = new AtomicInteger();
    private final AtomicInteger entitiesCulled = new AtomicInteger();
    private final AtomicInteger tickCount = new AtomicInteger();

    private volatile boolean isRunning = true;
    
    public EntityProcessor(@NotNull CloudCraftEngine plugin) {
        this.plugin = plugin;
    }

    /**
     * Phase 1: Capture minimal snapshot of world state (main thread, fast)
     */
    public void captureSnapshot() {
        activeEntities.clear();
        
        // Get all player positions for spatial culling
        Set<Location> playerPositions = new HashSet<>();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            playerPositions.add(player.getLocation());
        }
        
        // Swap snapshots to avoid allocations
        Map<UUID, EntitySnapshot> temp = previousSnapshot;
        previousSnapshot.putAll(currentSnapshot);
        currentSnapshot.clear();
        temp.clear();

        // Capture only entities near players (spatial culling)
        for (World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Player)
                    continue;

                // Spatial culling: only process entities near players
                if (isNearAnyPlayer(entity.getLocation(), playerPositions)) {
                    UUID id = entity.getUniqueId();
                    currentSnapshot.put(id, new EntitySnapshot(entity));
                    activeEntities.add(id);
                } else {
                    entitiesCulled.incrementAndGet();
                }
            }
        }
    }

    /**
     * Phase 2: Trigger async processing (non-blocking)
     */
    public void processAsync() {
        // Don't start new work if previous work isn't done
        CompletableFuture<List<EntityDecision>> currentWork = pendingWork.get();
        if (currentWork != null && !currentWork.isDone()) {
            return; // Still processing previous tick
        }

        // Create immutable snapshot for async processing
        Map<UUID, EntitySnapshot> snapshotCopy = Map.copyOf(currentSnapshot);

        CompletableFuture<List<EntityDecision>> newWork = CompletableFuture.supplyAsync(() -> {
            long startTime = System.nanoTime();
            List<EntityDecision> decisions = processSnapshotAsync(snapshotCopy);
            long processTime = (System.nanoTime() - startTime) / 1_000_000;
            totalProcessTime.addAndGet(processTime);
            return decisions;
        }, asyncProcessor);

        // Store result when complete (non-blocking)
        newWork.thenAccept(decisions -> {
            pendingResults.set(decisions);
            entitiesProcessed.addAndGet(decisions.size());
        });

        pendingWork.set(newWork);
    }
    
    /**
     * Phase 3: Apply previous tick's results (main thread, fast)
     */
    public void applyPendingResults() {
        List<EntityDecision> decisions = pendingResults.getAndSet(null);
        if (decisions == null)
            return;

        long startTime = System.nanoTime();

        for (EntityDecision decision : decisions) {
            decision.apply(plugin.getServer());
        }

        long applyTime = (System.nanoTime() - startTime) / 1_000_000;
        totalApplyTime.addAndGet(applyTime);
        tickCount.incrementAndGet();

        // Track metrics in plugin for debug command
        for (EntityDecision decision : decisions) {
            plugin.getLastProcessTimes().put(decision.entityId, System.nanoTime());
            plugin.getProcessingCounts()
                    .computeIfAbsent(decision.entityId, k -> new AtomicInteger(0))
                    .incrementAndGet();
        }
    }

    /**
     * Spatial culling helper
     */
    private boolean isNearAnyPlayer(Location entityLoc, Set<Location> playerPositions) {
        for (Location playerLoc : playerPositions) {
            if (entityLoc.getWorld().equals(playerLoc.getWorld()) &&
                    entityLoc.distance(playerLoc) <= PROCESSING_RADIUS) {
                return true;
            }
        }
        return false;
    }

    /**
     * Core async processing - pure computation, no Bukkit API calls
     */
    private List<EntityDecision> processSnapshotAsync(Map<UUID, EntitySnapshot> snapshot) {
        // Build spatial index for fast neighbor queries
        SpatialIndex spatialIndex = new SpatialIndex(snapshot.values());

        return snapshot.values().parallelStream()
                .map(entity -> processEntityAI(entity, spatialIndex))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * AI processing for individual entity (pure computation)
     */
    private EntityDecision processEntityAI(EntitySnapshot entity, SpatialIndex spatialIndex) {
        EntityDecision.Builder decision = new EntityDecision.Builder(entity.id, entity.type);

        switch (entity.type) {
            case ZOMBIE, SKELETON, CREEPER -> processHostileAI(entity, spatialIndex, decision);
            case COW, SHEEP, PIG, CHICKEN -> processPassiveAI(entity, spatialIndex, decision);
            case DROPPED_ITEM -> processItemAI(entity, spatialIndex, decision);
            default -> {
                // No special processing for other entity types
            }
        }

        return decision.hasActions() ? decision.build() : null;
    }

    /**
     * Hostile mob AI (targeting, combat, pathfinding)
     */
    private void processHostileAI(EntitySnapshot entity, SpatialIndex spatialIndex, EntityDecision.Builder decision) {
        // Find nearest player within 16 blocks
        EntitySnapshot nearestPlayer = spatialIndex.findNearestPlayer(entity.position, 16.0);

        if (nearestPlayer != null) {
            double distance = entity.position.distance(nearestPlayer.position);

            // Attack if close
            if (distance < 2.0) {
                decision.attack(nearestPlayer.id, 3.0);
            }
            // Move toward target if medium distance
            else if (distance < 16.0) {
                Vector direction = nearestPlayer.position.toVector()
                        .subtract(entity.position.toVector())
                        .normalize()
                        .multiply(0.2);
                decision.move(direction);
                decision.setTarget(nearestPlayer.id);
            }
        } else {
            // Random wandering if no target
            Vector randomDirection = new Vector(
                    (Math.random() - 0.5) * 0.1,
                    0,
                    (Math.random() - 0.5) * 0.1);
            decision.move(randomDirection);
        }
    }

    /**
     * Passive mob AI (breeding, wandering)
     */
    private void processPassiveAI(EntitySnapshot entity, SpatialIndex spatialIndex, EntityDecision.Builder decision) {
        // Process only every 5 ticks to reduce load
        if (entity.ticksLived % 5 != 0)
            return;

        // Find nearby same-type entities for breeding
        List<EntitySnapshot> sameType = spatialIndex.findNearbyEntities(entity.position, 8.0)
                .stream()
                .filter(e -> e.type == entity.type && e.canBreed && !e.isInLove)
                .collect(Collectors.toList());

        if (!sameType.isEmpty() && entity.canBreed && !entity.isInLove) {
            decision.startBreeding(sameType.get(0).id);
        }

        // Random wandering
        if (entity.ticksLived % 100 == 0) {
            Vector randomDirection = new Vector(
                    (Math.random() - 0.5) * 0.15,
                    0,
                    (Math.random() - 0.5) * 0.15);
            decision.move(randomDirection);
        }
    }
    
    /**
     * Item merging AI
     */
    private void processItemAI(EntitySnapshot entity, SpatialIndex spatialIndex, EntityDecision.Builder decision) {
        // Process only every 20 ticks
        if (entity.ticksLived % 20 != 0)
            return;

        // Find nearby items of same type for merging
        List<EntitySnapshot> nearbyItems = spatialIndex.findNearbyEntities(entity.position, 2.0)
                .stream()
                .filter(e -> e.type == EntityType.DROPPED_ITEM)
                .collect(Collectors.toList());

        if (!nearbyItems.isEmpty()) {
            decision.mergeWith(nearbyItems.get(0).id);
        }
    }
    
    public void shutdown() {
        isRunning = false;
        asyncProcessor.shutdown();
    }

    public boolean isRunning() {
        return isRunning;
    }

    public PerformanceMetrics getMetrics() {
        int ticks = tickCount.get();
        if (ticks == 0)
            return new PerformanceMetrics(0, 0, 0, 0);

        return new PerformanceMetrics(
                (int) (totalProcessTime.get() / ticks),
                (int) (totalApplyTime.get() / ticks),
                entitiesProcessed.get(),
                entitiesCulled.get());
    }

    public record PerformanceMetrics(
            int avgProcessTime,
            int avgApplyTime,
            int entitiesProcessed,
            int entitiesCulled) {
    }

    /**
     * Minimal entity snapshot - only essential data
     */
    private static class EntitySnapshot {
        final UUID id;
        final EntityType type;
        final Location position;
        final int ticksLived;
        final boolean canBreed;
        final boolean isInLove;

        EntitySnapshot(Entity entity) {
            this.id = entity.getUniqueId();
            this.type = entity.getType();
            this.position = entity.getLocation().clone();
            this.ticksLived = entity.getTicksLived();

            if (entity instanceof Animals animals) {
                this.canBreed = animals.canBreed();
                this.isInLove = animals.isLoveMode();
            } else {
                this.canBreed = false;
                this.isInLove = false;
            }
        }
    }

    /**
     * Simple spatial index for fast neighbor queries
     */
    private static class SpatialIndex {
        private final List<EntitySnapshot> entities;

        SpatialIndex(Collection<EntitySnapshot> entities) {
            this.entities = new ArrayList<>(entities);
        }

        EntitySnapshot findNearestPlayer(Location center, double radius) {
            // Players are processed as PLAYER type entities in snapshot
            return entities.stream()
                    .filter(e -> e.type == EntityType.PLAYER)
                    .filter(e -> e.position.distance(center) <= radius)
                    .min(Comparator.comparingDouble(e -> e.position.distance(center)))
                    .orElse(null);
        }

        List<EntitySnapshot> findNearbyEntities(Location center, double radius) {
            return entities.stream()
                    .filter(e -> e.position.distance(center) <= radius)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Entity decision - actions to apply on main thread
     */
    private static class EntityDecision {
        final UUID entityId;
        final List<Action> actions;

        EntityDecision(UUID entityId, List<Action> actions) {
            this.entityId = entityId;
            this.actions = actions;
        }

        boolean apply(org.bukkit.Server server) {
            for (World world : server.getWorlds()) {
                Entity entity = world.getEntity(entityId);
                if (entity != null && entity.isValid()) {
                    for (Action action : actions) {
                        action.apply(entity, server);
                    }
                    return true;
                }
            }
            return false;
        }

        static class Builder {
            private final UUID entityId;
            private final List<Action> actions = new ArrayList<>();

            Builder(UUID entityId, EntityType entityType) {
                this.entityId = entityId;
            }

            void move(Vector velocity) {
                actions.add(new MoveAction(velocity));
            }

            void attack(UUID targetId, double damage) {
                actions.add(new AttackAction(targetId, damage));
            }

            void setTarget(UUID targetId) {
                actions.add(new SetTargetAction(targetId));
            }

            void startBreeding(UUID mateId) {
                actions.add(new BreedAction(mateId));
            }

            void mergeWith(UUID otherId) {
                actions.add(new MergeAction(otherId));
            }

            boolean hasActions() {
                return !actions.isEmpty();
            }

            EntityDecision build() {
                return new EntityDecision(entityId, new ArrayList<>(actions));
            }
        }
    }

    /**
     * Action implementations - Bukkit API calls happen here
     */
    private interface Action {
        void apply(Entity entity, org.bukkit.Server server);
    }

    private record MoveAction(Vector velocity) implements Action {
        @Override
        public void apply(Entity entity, org.bukkit.Server server) {
            entity.setVelocity(velocity);
        }
    }

    private record AttackAction(UUID targetId, double damage) implements Action {
        @Override
        public void apply(Entity entity, org.bukkit.Server server) {
            for (World world : server.getWorlds()) {
                Entity target = world.getEntity(targetId);
                if (target instanceof LivingEntity living) {
                    living.damage(damage, entity);
                    break;
                }
            }
        }
    }
    
    private record SetTargetAction(UUID targetId) implements Action {
        @Override
        public void apply(Entity entity, org.bukkit.Server server) {
            if (entity instanceof Mob mob) {
                for (World world : server.getWorlds()) {
                    Entity target = world.getEntity(targetId);
                    if (target instanceof LivingEntity living) {
                        mob.setTarget(living);
                        break;
                    }
                }
            }
        }
    }

    private record BreedAction(UUID mateId) implements Action {
        @Override
        public void apply(Entity entity, org.bukkit.Server server) {
            if (entity instanceof Animals animal) {
                animal.setLoveModeTicks(600);
                for (World world : server.getWorlds()) {
                    Entity mate = world.getEntity(mateId);
                    if (mate instanceof Animals mateAnimal) {
                        mateAnimal.setLoveModeTicks(600);
                        break;
                    }
                }
            }
        }
    }

    private record MergeAction(UUID otherId) implements Action {
        @Override
        public void apply(Entity entity, org.bukkit.Server server) {
            if (entity instanceof Item item1) {
                for (World world : server.getWorlds()) {
                    Entity other = world.getEntity(otherId);
                    if (other instanceof Item item2 && !item1.equals(item2)) {
                        // Check if items can be merged (same type, not too old)
                        if (canMergeItems(item1, item2)) {
                            mergeItems(item1, item2);
                        }
                        break;
                    }
                }
            }
        }
        
        private boolean canMergeItems(Item item1, Item item2) {
            // Check if items are the same type and can stack
            return item1.getItemStack().isSimilar(item2.getItemStack()) &&
                   item1.getItemStack().getAmount() + item2.getItemStack().getAmount() <= 
                   item1.getItemStack().getMaxStackSize() &&
                   item1.getTicksLived() > 10 && item2.getTicksLived() > 10; // Prevent immediate merging
        }
        
        private void mergeItems(Item item1, Item item2) {
            int totalAmount = item1.getItemStack().getAmount() + item2.getItemStack().getAmount();
            item1.getItemStack().setAmount(totalAmount);
            item2.remove(); // Remove the merged item
        }
    }
}