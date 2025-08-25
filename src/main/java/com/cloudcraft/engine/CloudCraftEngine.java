package com.cloudcraft.engine;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import com.cloudcraft.engine.threading.EntityProcessor;
import com.cloudcraft.engine.metrics.PerformanceMonitor;
import com.cloudcraft.engine.testing.StressTest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CloudCraftEngine extends JavaPlugin {
    private @Nullable EntityProcessor entityProcessor;
    private @Nullable PerformanceMonitor performanceMonitor;
    private @Nullable StressTest stressTest;

    @Override
    public void onEnable() {
        // Initialize with fallback to single-threaded mode if virtual threads are not available
        try {
            this.entityProcessor = new EntityProcessor(this);
            this.performanceMonitor = new PerformanceMonitor(this);
            getLogger().info("CloudCraft Engine initialized with virtual thread support");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize virtual thread processor: " + e.getMessage());
            getLogger().warning("Falling back to single-threaded mode");
            // TODO: Implement fallback mode
        }
        
        // Register commands
        getCommand("stresstest").setExecutor(this);
    }

    @Override
    public void onDisable() {
        if (entityProcessor != null) {
            entityProcessor.shutdown();
        }
        if (performanceMonitor != null) {
            performanceMonitor.shutdown();
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("stresstest")) {
            if (!sender.isOp()) {
                sender.sendMessage("§cYou must be an operator to run stress tests!");
                return true;
            }

            int players = 500;
            int duration = 300;
            int warmup = 30;

            if (args.length >= 1) {
                try {
                    players = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid player count: " + args[0]);
                    return false;
                }
            }

            if (args.length >= 2) {
                try {
                    duration = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid duration: " + args[1]);
                    return false;
                }
            }

            if (args.length >= 3) {
                try {
                    warmup = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid warmup time: " + args[2]);
                    return false;
                }
            }

            sender.sendMessage("§aStarting stress test with " + players + " players");
            sender.sendMessage("§7Warmup: " + warmup + " seconds");
            sender.sendMessage("§7Duration: " + duration + " seconds");

            this.stressTest = new StressTest(this)
                .withPlayerCount(players)
                .withWarmup(warmup)
                .withDuration(duration)
                .withSamplingInterval(20);

            this.stressTest.startTest();
            return true;
        }

        return false;
    }

    public @Nullable EntityProcessor getEntityProcessor() {
        return entityProcessor;
    }
}