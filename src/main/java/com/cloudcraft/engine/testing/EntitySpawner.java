package com.cloudcraft.engine.testing;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Spawns and manages real entities for stress testing
 */
public class EntitySpawner {
    private final JavaPlugin plugin;
    private final ConcurrentHashMap<EntityType, AtomicInteger> entityCounts;
    private final List<Entity> spawnedEntities;
    private final Random random;
    
    // Entity type weights for realistic distribution
    private static final EntityType[] ENTITY_TYPES = {
        // Hostile mobs (40% chance)
        EntityType.ZOMBIE, EntityType.ZOMBIE, EntityType.ZOMBIE, EntityType.ZOMBIE,
        EntityType.SKELETON, EntityType.SKELETON, EntityType.SKELETON,
        EntityType.CREEPER, EntityType.CREEPER,
        
        // Passive mobs (60% chance)
        EntityType.COW, EntityType.COW, EntityType.COW,
        EntityType.SHEEP, EntityType.SHEEP, EntityType.SHEEP,
        EntityType.PIG, EntityType.PIG,
        EntityType.CHICKEN, EntityType.CHICKEN,
        EntityType.RABBIT, EntityType.RABBIT,
        EntityType.HORSE, EntityType.HORSE
    };
    
    public EntitySpawner(JavaPlugin plugin) {
        this.plugin = plugin;
        this.entityCounts = new ConcurrentHashMap<>();
        this.spawnedEntities = new ArrayList<>();
        this.random = new Random();
        
        // Initialize counters for each entity type
        for (EntityType type : ENTITY_TYPES) {
            entityCounts.put(type, new AtomicInteger(0));
        }
    }
    
    /**
     * Spawns test entities in batches around the world spawn
     * @param count Total number of entities to spawn
     * @param batchSize Number of entities to spawn per batch
     * @param ticksBetweenBatches Ticks to wait between batches
     * @return True if spawning started successfully
     */
    public boolean spawnTestEntities(int count, int batchSize, int ticksBetweenBatches) {
        World world = plugin.getServer().getWorlds().get(0);
        Location spawnLoc = world.getSpawnLocation();
        
        // Validate parameters
        if (count <= 0 || batchSize <= 0 || ticksBetweenBatches < 0) {
            plugin.getLogger().warning("Invalid spawn parameters");
            return false;
        }
        
        // Calculate number of batches
        int totalBatches = (count + batchSize - 1) / batchSize;
        AtomicInteger batchesSpawned = new AtomicInteger(0);
        
        // Schedule batch spawning
        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            if (batchesSpawned.get() >= totalBatches) {
                task.cancel();
                plugin.getLogger().info("Finished spawning " + spawnedEntities.size() + " entities");
                return;
            }
            
            int remaining = count - (batchesSpawned.get() * batchSize);
            int currentBatchSize = Math.min(batchSize, remaining);
            
            // Spawn batch
            for (int i = 0; i < currentBatchSize; i++) {
                // Get random location in 200x200 area around spawn
                Location randomLoc = spawnLoc.clone().add(
                    random.nextInt(200) - 100,
                    0,
                    random.nextInt(200) - 100
                );
                randomLoc.setY(world.getHighestBlockYAt(randomLoc) + 1);
                
                // Spawn random entity
                Entity entity = spawnRandomEntity(world, randomLoc);
                if (entity != null) {
                    spawnedEntities.add(entity);
                    entityCounts.get(entity.getType()).incrementAndGet();
                }
            }
            
            batchesSpawned.incrementAndGet();
            
            // Log progress
            if (batchesSpawned.get() % 10 == 0 || batchesSpawned.get() == totalBatches) {
                plugin.getLogger().info(String.format(
                    "Spawned %d/%d entities (%.1f%%)",
                    spawnedEntities.size(),
                    count,
                    (spawnedEntities.size() * 100.0) / count
                ));
            }
        }, 1L, ticksBetweenBatches);
        
        return true;
    }
    
    /**
     * Spawns a random entity at the given location
     */
    private Entity spawnRandomEntity(World world, Location loc) {
        try {
            EntityType type = ENTITY_TYPES[random.nextInt(ENTITY_TYPES.length)];
            Entity entity = world.spawnEntity(loc, type);
            
            // Configure entity properties
            if (entity instanceof LivingEntity living) {
                living.setRemoveWhenFarAway(false); // Prevent despawning
                
                if (entity instanceof Ageable ageable) {
                    // 20% chance to spawn as baby
                    if (random.nextDouble() < 0.2) {
                        ageable.setBaby();
                    } else {
                        ageable.setAdult();
                    }
                }
            }
            
            return entity;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to spawn entity: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Removes all spawned test entities
     */
    public void removeAll() {
        int removed = 0;
        for (Entity entity : spawnedEntities) {
            if (entity != null && entity.isValid()) {
                entity.remove();
                removed++;
            }
        }
        spawnedEntities.clear();
        entityCounts.values().forEach(count -> count.set(0));
        
        plugin.getLogger().info("Removed " + removed + " test entities");
    }
    
    /**
     * Gets the current count of spawned entities by type
     */
    public ConcurrentHashMap<EntityType, Integer> getEntityCounts() {
        ConcurrentHashMap<EntityType, Integer> counts = new ConcurrentHashMap<>();
        entityCounts.forEach((type, count) -> counts.put(type, count.get()));
        return counts;
    }
    
    /**
     * Gets the total number of spawned entities
     */
    public int getTotalEntityCount() {
        return spawnedEntities.size();
    }
}
