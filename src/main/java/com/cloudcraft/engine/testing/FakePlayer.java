package com.cloudcraft.engine.testing;

import com.cloudcraft.engine.CloudCraftEngine;
import org.bukkit.Location;
import org.bukkit.World;
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

        // Keep player in bounds
        keepInBounds();
        
        // Ensure valid ground position
        maintainValidPosition();
    }
    
    private void switchBehavior() {
        // Randomly select new behavior
        BehaviorState[] states = BehaviorState.values();
        state = states[random.nextInt(states.length)];
    }
    
    private void simulateWalking() {
        double angle = random.nextDouble() * Math.PI * 2;
        double dx = Math.cos(angle) * WALK_SPEED;
        double dz = Math.sin(angle) * WALK_SPEED;
        location.add(dx, 0, dz);
    }
    
    private void simulateSprinting() {
        double angle = random.nextDouble() * Math.PI * 2;
        double dx = Math.cos(angle) * SPRINT_SPEED;
        double dz = Math.sin(angle) * SPRINT_SPEED;
        location.add(dx, 0, dz);
    }
    
    private void simulateMining() {
        // Simulate mining activity (no actual block changes)
        location.add(
                random.nextDouble() * 0.2 - 0.1,
                0,
                random.nextDouble() * 0.2 - 0.1);
    }
    
    private void simulateBuilding() {
        // Simulate building activity (no actual block changes)
        location.add(
                random.nextDouble() * 0.2 - 0.1,
                0,
                random.nextDouble() * 0.2 - 0.1);
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