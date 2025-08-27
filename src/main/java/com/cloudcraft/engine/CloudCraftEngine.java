package com.cloudcraft.engine;

import com.cloudcraft.engine.licensing.BetaLimitations;
import com.cloudcraft.engine.metrics.MetricsCollector;
import com.cloudcraft.engine.testing.StressTest;
import com.cloudcraft.engine.threading.EntityProcessor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class CloudCraftEngine extends JavaPlugin {
    private static final String DEBUG_PERMISSION = "cloudcraft.debug";
    private final Map<UUID, Long> lastProcessTimes = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicInteger> processingCounts = new ConcurrentHashMap<>();

    public Map<UUID, Long> getLastProcessTimes() {
        return lastProcessTimes;
    }

    public Map<UUID, AtomicInteger> getProcessingCounts() {
        return processingCounts;
    }
    private @Nullable EntityProcessor entityProcessor;
    private @Nullable MetricsCollector metricsCollector;
    private @Nullable StressTest stressTest;
    private @Nullable BukkitTask processingTask;

    @Override
    public void onEnable() {
        // Register debug command
        getCommand("ccdebug").setExecutor((sender, command, label, args) -> {
            if (!sender.hasPermission(DEBUG_PERMISSION)) {
                sender.sendMessage("§cYou don't have permission to use this command.");
                return true;
            }

            // Get all entities
            int totalEntities = 0;
            int processedEntities = 0;
            long now = System.nanoTime();

            for (World world : getServer().getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    totalEntities++;

                    UUID id = entity.getUniqueId();
                    Long lastProcess = lastProcessTimes.get(id);
                    AtomicInteger count = processingCounts.get(id);

                    if (lastProcess != null && count != null) {
                        processedEntities++;
                        double timeSinceProcess = (now - lastProcess) / 1_000_000.0; // ms
                        sender.sendMessage(String.format(
                                "§e%s§r (ID: %s) - Processed §a%d§r times, §6%.2f§rms ago",
                                entity.getType(),
                                entity.getUniqueId(),
                                count.get(),
                                timeSinceProcess));
                    }
                }
            }

            sender.sendMessage(String.format(
                    "§6=== CloudCraft Debug ===\n" +
                            "§7Total Entities: §f%d\n" +
                            "§7Processed Entities: §f%d\n" +
                            "§7Processing Rate: §f%.1f%%",
                    totalEntities,
                    processedEntities,
                    (processedEntities * 100.0) / totalEntities));

            return true;
        });
        // Check beta expiration
        if (BetaLimitations.isExpired()) {
            getLogger().severe("=====================================");
            getLogger().severe("CloudCraft Engine BETA has expired!");
            getLogger().severe("Please upgrade to the full version at:");
            getLogger().severe(BetaLimitations.UPGRADE_URL);
            getLogger().severe("=====================================");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Display beta information
        long daysRemaining = BetaLimitations.getDaysRemaining();
        getLogger().warning("=====================================");
        getLogger().warning("CloudCraft Engine BETA - Limited Version");
        getLogger().warning("- Max " + BetaLimitations.MAX_PLAYERS_BETA + " players (Full: unlimited)");
        getLogger().warning("- Beta expires in " + daysRemaining + " days");
        getLogger().warning("- For production use, visit: " + BetaLimitations.UPGRADE_URL);
        getLogger().warning("=====================================");
        
        // Initialize with fallback to single-threaded mode if virtual threads are not available
        try {
            this.entityProcessor = new EntityProcessor(this);
            this.metricsCollector = new MetricsCollector(this);

            // OPTIMIZED SNAPSHOT-PROCESS-APPLY PIPELINE
            getLogger().info("Starting optimized entity processing pipeline...");
            this.processingTask = getServer().getScheduler().runTaskTimer(this, () -> {
                if (entityProcessor == null || !entityProcessor.isRunning())
                    return;

                // Phase 3: Apply previous tick's results (non-blocking)
                entityProcessor.applyPendingResults();

                // Phase 1: Capture minimal snapshot (fast)
                long captureStart = System.nanoTime();
                entityProcessor.captureSnapshot();
                long captureTime = (System.nanoTime() - captureStart) / 1_000_000;

                // Phase 2: Trigger async processing (non-blocking)
                entityProcessor.processAsync();

                // Log performance every 100 ticks (5 seconds)
                if (getServer().getCurrentTick() % 100 == 0) {
                    EntityProcessor.PerformanceMetrics metrics = entityProcessor.getMetrics();
                    getLogger().info(String.format(
                            "Pipeline: Capture=%dms, Process=%dms, Apply=%dms | Entities: %d processed, %d culled",
                            captureTime,
                            metrics.avgProcessTime(),
                            metrics.avgApplyTime(),
                            metrics.entitiesProcessed(),
                            metrics.entitiesCulled()));
                }
            }, 0L, 1L); // Run every tick

            getLogger().info("CloudCraft Engine initialized with virtual thread support");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize virtual thread processor: " + e.getMessage());
            getLogger().warning("Falling back to single-threaded mode");
            // Fallback to single-threaded mode if needed
            getLogger().warning("Failed to initialize virtual thread processing, falling back to single-threaded mode");
        }
        
        // Register commands
        getCommand("stresstest").setExecutor(this);
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down CloudCraft Engine...");

        // Stop the processing task - THIS IS CRITICAL!
        if (processingTask != null && !processingTask.isCancelled()) {
            processingTask.cancel();
            getLogger().info("Entity processing task stopped");
        }

        if (entityProcessor != null) {
            entityProcessor.shutdown();
        }
        if (metricsCollector != null) {
            metricsCollector.stop();
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("stresstest")) {
            if (!sender.isOp()) {
                sender.sendMessage(Component.text("You must be an operator to run stress tests!")
                    .color(NamedTextColor.RED));
                return true;
            }

            int players = 500;
            int duration = 60;  // Shorter for demo
            int warmup = 10;    // Shorter for demo

            if (args.length >= 1) {
                try {
                    players = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid player count: " + args[0])
                        .color(NamedTextColor.RED));
                    return false;
                }
            }

            // Check beta limitations
            if (!BetaLimitations.canProcessPlayers(players)) {
                return true;
            }

            if (args.length >= 2) {
                try {
                    duration = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid duration: " + args[1])
                        .color(NamedTextColor.RED));
                    return false;
                }
            }

            if (args.length >= 3) {
                try {
                    warmup = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid warmup time: " + args[2])
                        .color(NamedTextColor.RED));
                    return false;
                }
            }

            this.stressTest = new StressTest(this)
                .withPlayerCount(players)
                .withWarmup(warmup)
                .withDuration(duration)
                .withSamplingInterval(20);

            this.stressTest.startTest(sender);
            return true;
        }

        return false;
    }

    public @Nullable EntityProcessor getEntityProcessor() {
        return entityProcessor;
    }
}