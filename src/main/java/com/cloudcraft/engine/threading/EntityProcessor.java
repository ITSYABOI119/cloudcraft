package com.cloudcraft.engine.threading;

import com.cloudcraft.engine.CloudCraftEngine;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * EntityProcessor - Core of CloudCraft's multithreaded entity processing system
 */
public class EntityProcessor {
    private final CloudCraftEngine plugin;
    private final ConcurrentHashMap<RegionKey, ConcurrentHashMap<Integer, Entity>> regionMap;
    private final ExecutorService executorService;
    private final AtomicBoolean isRunning;
    private final ConcurrentLinkedQueue<EntityMovementTask> movementQueue;

    // Configuration
    private static final int REGION_SIZE = 16; // chunks
    private static final int MAX_ENTITIES_PER_REGION = 1000;
    
    public EntityProcessor(@NotNull CloudCraftEngine plugin) {
        this.plugin = plugin;
        this.regionMap = new ConcurrentHashMap<>();
        this.movementQueue = new ConcurrentLinkedQueue<>();
        this.isRunning = new AtomicBoolean(true);
        
        // Create virtual thread executor
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();

        // Start the movement processor
        startMovementProcessor();
    }

    /**
     * Processes entities in a specific region
     */
    private void processRegion(@NotNull RegionKey region) {
        try {
            var entities = regionMap.get(region);
            if (entities == null || entities.isEmpty()) return;

            // Process each entity in the region
            for (Entity entity : entities.values()) {
                if (!entity.isValid()) {
                    entities.remove(entity.getEntityId());
                    continue;
                }

                // Handle entity movement between regions
                var newRegion = getRegionForLocation(entity.getLocation());
                if (!region.equals(newRegion)) {
                    movementQueue.offer(new EntityMovementTask(entity, region, newRegion));
                }

                // Custom entity processing logic here
                processEntity(entity);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error processing region " + region, e);
        }
    }

    /**
     * Processes a single entity
     */
    private void processEntity(@NotNull Entity entity) {
        // TODO: Implement entity-specific processing logic
        // This is where we'll add custom behavior, AI, etc.
    }

    /**
     * Starts the movement processor thread
     */
    private void startMovementProcessor() {
        Thread.startVirtualThread(() -> {
            while (isRunning.get()) {
                try {
                    var task = movementQueue.poll();
                    if (task != null) {
                        handleEntityMovement(task);
                    }
                    Thread.sleep(1); // Prevent busy-waiting
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    /**
     * Handles entity movement between regions
     */
    private void handleEntityMovement(@NotNull EntityMovementTask task) {
        var sourceRegion = regionMap.get(task.sourceRegion());
        var targetRegion = regionMap.computeIfAbsent(task.targetRegion(),
            k -> new ConcurrentHashMap<>(MAX_ENTITIES_PER_REGION));

        if (sourceRegion != null) {
            sourceRegion.remove(task.entity().getEntityId());
        }
        targetRegion.put(task.entity().getEntityId(), task.entity());
    }

    /**
     * Gets the region key for a location
     */
    private @NotNull RegionKey getRegionForLocation(@NotNull org.bukkit.Location location) {
        int regionX = location.getBlockX() >> 4 >> 4; // Convert to region coordinates
        int regionZ = location.getBlockZ() >> 4 >> 4;
        return new RegionKey(location.getWorld().getUID(), regionX, regionZ);
    }

    /**
     * Shuts down the entity processor
     */
    public void shutdown() {
        isRunning.set(false);
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Record representing a region in the world
     */
    private record RegionKey(@NotNull java.util.UUID worldId, int x, int z) {}

    /**
     * Record representing an entity movement task
     */
    private record EntityMovementTask(@NotNull Entity entity, @NotNull RegionKey sourceRegion, @NotNull RegionKey targetRegion) {}
}