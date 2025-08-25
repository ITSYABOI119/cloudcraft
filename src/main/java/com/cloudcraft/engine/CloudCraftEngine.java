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

public class CloudCraftEngine extends JavaPlugin {
    private @Nullable EntityProcessor entityProcessor;
    private @Nullable MetricsCollector metricsCollector;
    private @Nullable StressTest stressTest;

    @Override
    public void onEnable() {
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