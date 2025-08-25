package com.cloudcraft.engine.testing;

import com.cloudcraft.engine.CloudCraftEngine;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.UUID;

/**
 * Simulates a player for stress testing
 */
public class FakePlayer {
    private final CloudCraftEngine plugin;
    private final UUID uuid;
    private Location location;
    private final Random random;
    private final Vector velocity;
    private int behaviorTicks;
    private BehaviorState state;
    
    private static final double WALK_SPEED = 0.2;
    private static final double SPRINT_SPEED = 0.4;
    private static final int BEHAVIOR_CHANGE_TICKS = 100;
    
    public FakePlayer(@NotNull CloudCraftEngine plugin, @NotNull Location spawnLocation) {
        this.plugin = plugin;
        this.uuid = UUID.randomUUID();
        this.location = spawnLocation.clone();
        this.random = new Random();
        this.velocity = new Vector();
        this.behaviorTicks = 0;
        this.state = BehaviorState.IDLE;
    }
    
    /**
     * Simulates realistic player behavior
     */
    public void simulateBehavior() {
        behaviorTicks++;
        
        if (behaviorTicks >= BEHAVIOR_CHANGE_TICKS) {
            behaviorTicks = 0;
            switchBehavior();
        }
        
        switch (state) {
            case WALKING -> simulateWalking();
            case SPRINTING -> simulateSprinting();
            case MINING -> simulateMining();
            case BUILDING -> simulateBuilding();
            case IDLE -> simulateIdle();
        }
        
        // Update location
        location.add(velocity);
        
        // Keep player in bounds
        keepInBounds();
        
        // Ensure valid ground position
        maintainValidPosition();
    }
    
    private void switchBehavior() {
        // Randomly select new behavior
        BehaviorState[] states = BehaviorState.values();
        state = states[random.nextInt(states.length)];
        
        // Reset velocity
        velocity.zero();
    }
    
    private void simulateWalking() {
        double angle = random.nextDouble() * Math.PI * 2;
        velocity.setX(Math.cos(angle) * WALK_SPEED);
        velocity.setZ(Math.sin(angle) * WALK_SPEED);
    }
    
    private void simulateSprinting() {
        double angle = random.nextDouble() * Math.PI * 2;
        velocity.setX(Math.cos(angle) * SPRINT_SPEED);
        velocity.setZ(Math.sin(angle) * SPRINT_SPEED);
    }
    
    private void simulateMining() {
        // Find nearby block to mine
        Location target = location.clone();
        target.add(random.nextDouble() * 4 - 2, 0, random.nextDouble() * 4 - 2);
        target.setY(target.getWorld().getHighestBlockYAt(target.getBlockX(), target.getBlockZ()));
        
        if (target.getBlock().getType() != Material.AIR) {
            target.getBlock().setType(Material.AIR);
        }
    }
    
    private void simulateBuilding() {
        // Find nearby location to place block
        Location target = location.clone();
        target.add(random.nextDouble() * 4 - 2, 0, random.nextDouble() * 4 - 2);
        target.setY(target.getWorld().getHighestBlockYAt(target.getBlockX(), target.getBlockZ()) + 1);
        
        if (target.getBlock().getType() == Material.AIR) {
            target.getBlock().setType(Material.STONE);
        }
    }
    
    private void simulateIdle() {
        // Just look around randomly
        location.setYaw(location.getYaw() + (random.nextFloat() * 10 - 5));
        location.setPitch(Math.max(-90, Math.min(90, location.getPitch() + (random.nextFloat() * 10 - 5))));
    }
    
    private void keepInBounds() {
        World world = location.getWorld();
        double radius = 200; // Keep within 200 blocks of spawn
        
        Location spawn = world.getSpawnLocation();
        double dx = location.getX() - spawn.getX();
        double dz = location.getZ() - spawn.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        
        if (distance > radius) {
            // Move back towards spawn
            double angle = Math.atan2(dz, dx);
            location.setX(spawn.getX() + Math.cos(angle) * radius);
            location.setZ(spawn.getZ() + Math.sin(angle) * radius);
            velocity.zero();
        }
    }
    
    private void maintainValidPosition() {
        World world = location.getWorld();
        int x = location.getBlockX();
        int z = location.getBlockZ();
        int y = world.getHighestBlockYAt(x, z);
        
        location.setY(y + 1);
    }
    
    /**
     * Removes the fake player
     */
    public void remove() {
        // Cleanup any resources
    }
    
    public UUID getUUID() {
        return uuid;
    }
    
    public Location getLocation() {
        return location.clone();
    }
    
    private enum BehaviorState {
        IDLE,
        WALKING,
        SPRINTING,
        MINING,
        BUILDING
    }
}
