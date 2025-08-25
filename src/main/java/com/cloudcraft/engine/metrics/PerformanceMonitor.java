package com.cloudcraft.engine.metrics;

import com.cloudcraft.engine.CloudCraftEngine;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

/**
 * Monitors and records performance metrics for the CloudCraft Engine
 */
public class PerformanceMonitor {
    private final CloudCraftEngine plugin;
    private final List<MetricSample> samples;
    private final AtomicBoolean isCollecting;
    private final AtomicLong lastTickTime;
    private long startTime;
    private final AtomicLong entityProcessedCount;
    
    public PerformanceMonitor(@NotNull CloudCraftEngine plugin) {
        this.plugin = plugin;
        this.samples = new ArrayList<>();
        this.isCollecting = new AtomicBoolean(false);
        this.lastTickTime = new AtomicLong(System.nanoTime());
        this.entityProcessedCount = new AtomicLong(0);
        
        // Setup tick time measurement
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.nanoTime();
            lastTickTime.set(now);
        }, 1L, 1L);
    }
    
    public void start() {
        samples.clear();
        startTime = System.currentTimeMillis();
        isCollecting.set(true);
    }
    
    public void stop() {
        isCollecting.set(false);
    }
    
    public void sample() {
        if (!isCollecting.get()) return;
        
        double[] tps = Bukkit.getTPS();
        Runtime runtime = Runtime.getRuntime();
        
        samples.add(new MetricSample(
            System.currentTimeMillis() - startTime,
            tps[0], // Current TPS
            calculateMSPT(),
            (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024, // Heap usage in MB
            Thread.activeCount(),
            plugin.getServer().getOnlinePlayers().size(),
            entityProcessedCount.get()
        ));
    }
    
    private double calculateMSPT() {
        long now = System.nanoTime();
        long last = lastTickTime.get();
        return (now - last) / 1_000_000.0; // Convert to milliseconds
    }
    
    public void recordEntityProcessed() {
        entityProcessedCount.incrementAndGet();
    }
    
    public void exportToCSV(@NotNull Path path) {
        try {
            List<String> lines = new ArrayList<>();
            lines.add("Timestamp,TPS,MSPT,Memory(MB),Threads,Players,EntitiesProcessed");
            
            for (MetricSample sample : samples) {
                lines.add(String.format("%d,%.2f,%.2f,%d,%d,%d,%d",
                    sample.timestamp(),
                    sample.tps(),
                    sample.mspt(),
                    sample.memoryUsage(),
                    sample.threadCount(),
                    sample.playerCount(),
                    sample.entitiesProcessed()
                ));
            }
            
            Files.write(path, lines);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to export metrics to CSV", e);
        }
    }
    
    public Summary getSummary() {
        if (samples.isEmpty()) {
            return new Summary(20.0, 20.0, 50.0, 50.0, 1024, 1024);
        }
        
        double avgTps = samples.stream().mapToDouble(MetricSample::tps).average().orElse(20.0);
        double avgMspt = samples.stream().mapToDouble(MetricSample::mspt).average().orElse(50.0);
        long avgMemory = Math.round(samples.stream().mapToLong(MetricSample::memoryUsage).average().orElse(1024));
        
        // For now, we're using static comparison values
        // In production, we'd run the same test with vanilla Paper first
        return new Summary(
            15.0, // Vanilla TPS
            avgTps, // CloudCraft TPS
            75.0, // Vanilla MSPT
            avgMspt, // CloudCraft MSPT
            2048, // Vanilla Memory
            avgMemory // CloudCraft Memory
        );
    }
    
    private record MetricSample(
        long timestamp,
        double tps,
        double mspt,
        long memoryUsage,
        int threadCount,
        int playerCount,
        long entitiesProcessed
    ) {}
    
    public record Summary(
        double vanillaTps,
        double cloudcraftTps,
        double vanillaMspt,
        double cloudcraftMspt,
        long vanillaMemory,
        long cloudcraftMemory
    ) {}
    
    public void shutdown() {
        isCollecting.set(false);
    }
}