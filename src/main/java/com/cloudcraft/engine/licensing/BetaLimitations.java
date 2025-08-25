package com.cloudcraft.engine.licensing;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Handles beta version limitations and restrictions
 */
public class BetaLimitations {
    public static final int MAX_PLAYERS_BETA = 200;
    public static final Instant BETA_EXPIRY = Instant.parse("2025-03-01T00:00:00Z");
    public static final String UPGRADE_URL = "https://github.com/yourusername/cloudcraft";
    
    /**
     * Checks if the beta version can process the given number of players
     */
    public static boolean canProcessPlayers(int playerCount) {
        if (playerCount > MAX_PLAYERS_BETA) {
            Bukkit.broadcast(
                Component.text("Beta version is limited to " + MAX_PLAYERS_BETA + " players. ")
                    .color(NamedTextColor.RED)
                    .append(Component.text("Get the full version at: " + UPGRADE_URL)
                        .color(NamedTextColor.YELLOW))
            );
            return false;
        }
        return !isExpired();
    }
    
    /**
     * Checks if the beta version has expired
     */
    public static boolean isExpired() {
        return Instant.now().isAfter(BETA_EXPIRY);
    }
    
    /**
     * Gets the number of days remaining in the beta period
     */
    public static long getDaysRemaining() {
        return ChronoUnit.DAYS.between(Instant.now(), BETA_EXPIRY);
    }
    
    /**
     * Gets the beta version watermark for display in stress test results
     */
    public static Component getBetaWatermark() {
        return Component.text("CloudCraft Engine BETA - Limited to " + MAX_PLAYERS_BETA + " players")
            .color(NamedTextColor.GRAY)
            .append(Component.newline())
            .append(Component.text("Expires: March 1, 2025 - Get the full version at: " + UPGRADE_URL)
                .color(NamedTextColor.GRAY));
    }
}