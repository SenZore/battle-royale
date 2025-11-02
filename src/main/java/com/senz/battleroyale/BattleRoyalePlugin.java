package com.senz.battleroyale;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class BattleRoyalePlugin extends JavaPlugin {

    private GameManager gameManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        WorldManager worldManager = new WorldManager(this);
        LootManager lootManager = new LootManager(this);
        ScoreboardHandler scoreboardHandler = new ScoreboardHandler(this);
        gameManager = new GameManager(this, worldManager, lootManager, scoreboardHandler);

        Bukkit.getPluginManager().registerEvents(new GameListener(), this);

        // Re-add online players (server reload)
        for (Player player : Bukkit.getOnlinePlayers()) {
            gameManager.handleJoin(player);
        }
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.shutdown();
        }
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    private class GameListener implements Listener {
        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            gameManager.handleJoin(event.getPlayer());
        }

        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event) {
            gameManager.handleQuit(event.getPlayer());
        }

        @EventHandler
        public void onPlayerDeath(PlayerDeathEvent event) {
            gameManager.handleDeath(event.getEntity(), event.getEntity().getKiller());
        }
    }
}
