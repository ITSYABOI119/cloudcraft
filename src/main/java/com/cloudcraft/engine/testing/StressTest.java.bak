package com.cloudcraft.engine.testing;

import com.cloudcraft.engine.CloudCraftEngine;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * Comprehensive stress testing framework for CloudCraft Engine
 */
public class StressTest {
    private final CloudCraftEngine plugin;
    private final ConcurrentHashMap<UUID, FakePlayer> fakePlayers;
    private final Random random;
    private final MetricsCollector metricsCollector;
    private final Path resultsDir;
    
    // Configuration
    private int targetPlayerCount = 500;
    private int warmupSeconds = 30;
    private int testDurationSeconds = 300;
    private int samplingIntervalTicks = 20;
    
    public StressTest(@NotNull CloudCraftEngine plugin) {
        this.plugin = plugin;
        this.fakePlayers = new ConcurrentHashMap<>();
        this.random = new Random();
        this.metricsCollector = new MetricsCollector(plugin);
        this.resultsDir = plugin.getDataFolder().toPath().resolve("stress-test-results");
        
        try {
            Files.createDirectories(resultsDir);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create results directory", e);
        }
    }
    
    /**
     * Starts the stress test
     */
    public void startTest() {
        plugin.getLogger().info("Starting stress test with " + targetPlayerCount + " players");
        plugin.getLogger().info("Warm-up period: " + warmupSeconds + " seconds");
        plugin.getLogger().info("Test duration: " + testDurationSeconds + " seconds");
        
        // Start metrics collection
        metricsCollector.start();
        
        // Spawn players gradually
        new BukkitRunnable() {
            final AtomicInteger spawnedPlayers = new AtomicInteger(0);
            
            @Override
            public void run() {
                if (spawnedPlayers.get() >= targetPlayerCount) {
                    this.cancel();
                    startTestPhase();
                    return;
                }
                
                spawnFakePlayer();
                spawnedPlayers.incrementAndGet();
            }
        }.runTaskTimer(plugin, 1L, 2L);
    }
    
    private void startTestPhase() {
        plugin.getLogger().info("All players spawned, starting warm-up phase");
        
        // Start behavior simulation
        startPlayerBehaviorSimulation();
        
        // Schedule test end
        new BukkitRunnable() {
            @Override
            public void run() {
                endTest();
            }
        }.runTaskLater(plugin, (warmupSeconds + testDurationSeconds) * 20L);
        
        // Start metrics sampling
        new BukkitRunnable() {
            @Override
            public void run() {
                metricsCollector.sample();
            }
        }.runTaskTimer(plugin, warmupSeconds * 20L, samplingIntervalTicks);
    }
    
    private void startPlayerBehaviorSimulation() {
        new BukkitRunnable() {
            @Override
            public void run() {
                fakePlayers.values().forEach(FakePlayer::simulateBehavior);
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
    
    private void spawnFakePlayer() {
        World world = plugin.getServer().getWorlds().get(0);
        Location spawnLoc = world.getSpawnLocation();
        
        // Randomize spawn position within 100 blocks
        spawnLoc.add(
            random.nextDouble() * 200 - 100,
            0,
            random.nextDouble() * 200 - 100
        );
        
        // Find safe spawn location
        spawnLoc.setY(world.getHighestBlockYAt(spawnLoc.getBlockX(), spawnLoc.getBlockZ()) + 1);
        
        FakePlayer fakePlayer = new FakePlayer(plugin, spawnLoc);
        fakePlayers.put(fakePlayer.getUUID(), fakePlayer);
    }
    
    private void endTest() {
        plugin.getLogger().info("Stress test completed");
        
        // Stop metrics collection
        metricsCollector.stop();
        
        // Generate reports
        generateReports();
        
        // Cleanup
        fakePlayers.values().forEach(FakePlayer::remove);
        fakePlayers.clear();
    }
    
    private void generateReports() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        
        // Generate CSV report
        Path csvPath = resultsDir.resolve("stress_test_" + timestamp + ".csv");
        metricsCollector.exportToCSV(csvPath);
        
        // Generate Markdown report
        Path mdPath = resultsDir.resolve("stress_test_" + timestamp + ".md");
        generateMarkdownReport(mdPath);
        
        plugin.getLogger().info("Reports generated:");
        plugin.getLogger().info("CSV: " + csvPath);
        plugin.getLogger().info("Markdown: " + mdPath);
    }
    
    private void generateMarkdownReport(Path path) {
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(path))) {
            writer.println("# CloudCraft Engine Stress Test Report");
            writer.println("\nTest conducted at: " + LocalDateTime.now());
            writer.println("\n## Configuration");
            writer.println("- Players: " + targetPlayerCount);
            writer.println("- Warm-up period: " + warmupSeconds + " seconds");
            writer.println("- Test duration: " + testDurationSeconds + " seconds");
            
            writer.println("\n## Performance Summary");
            writer.println("| Metric | Vanilla Paper | CloudCraft | Improvement |");
            writer.println("|--------|---------------|------------|-------------|");
            
            MetricsCollector.Summary summary = metricsCollector.getSummary();
            writer.printf("| Average TPS | %.2f | %.2f | %.1f%% |%n",
                summary.vanillaTps(), summary.cloudcraftTps(),
                (summary.cloudcraftTps() / summary.vanillaTps() - 1) * 100);
            
            writer.printf("| Average MSPT | %.2f | %.2f | %.1f%% |%n",
                summary.vanillaMspt(), summary.cloudcraftMspt(),
                (1 - summary.cloudcraftMspt() / summary.vanillaMspt()) * 100);
            
            writer.printf("| Memory Usage | %dMB | %dMB | %.1f%% |%n",
                summary.vanillaMemory(), summary.cloudcraftMemory(),
                (1 - (double)summary.cloudcraftMemory() / summary.vanillaMemory()) * 100);
            
            writer.println("\n## Detailed Metrics");
            // Add detailed metrics, graphs, etc.
            
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to generate Markdown report", e);
        }
    }
    
    // Builder-style configuration methods
    public StressTest withPlayerCount(int count) {
        this.targetPlayerCount = count;
        return this;
    }
    
    public StressTest withWarmup(int seconds) {
        this.warmupSeconds = seconds;
        return this;
    }
    
    public StressTest withDuration(int seconds) {
        this.testDurationSeconds = seconds;
        return this;
    }
    
    public StressTest withSamplingInterval(int ticks) {
        this.samplingIntervalTicks = ticks;
        return this;
    }
}
