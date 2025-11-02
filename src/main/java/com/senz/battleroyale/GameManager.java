package com.senz.battleroyale;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class GameManager {

    private final BattleRoyalePlugin plugin;
    private final WorldManager worldManager;
    private final LootManager lootManager;
    private final ScoreboardHandler scoreboardHandler;

    private final Set<UUID> queuedPlayers = new HashSet<>();
    private final Set<UUID> alivePlayers = new HashSet<>();
    private final Set<UUID> spectators = new HashSet<>();
    private final Map<UUID, Integer> kills = new HashMap<>();

    private GameState state = GameState.LOBBY;
    private BukkitTask countdownTask;
    private BukkitTask gameTimerTask;
    private BukkitTask scoreboardTask;
    private int countdownSeconds;
    private int timeRemaining;
    private World gameWorld;

    public GameManager(BattleRoyalePlugin plugin, WorldManager worldManager, LootManager lootManager, ScoreboardHandler scoreboardHandler) {
        this.plugin = plugin;
        this.worldManager = worldManager;
        this.lootManager = lootManager;
        this.scoreboardHandler = scoreboardHandler;
        startScoreboardLoop();
    }

    public void handleJoin(Player player) {
        kills.putIfAbsent(player.getUniqueId(), 0);

        if (state == GameState.RUNNING || state == GameState.ENDING) {
            player.setGameMode(GameMode.SPECTATOR);
            spectators.add(player.getUniqueId());
            player.sendMessage(ChatColor.RED + "A round is currently in progress. You are spectating.");
        } else {
            queuedPlayers.add(player.getUniqueId());
            player.setGameMode(GameMode.ADVENTURE);
            player.teleport(getLobbySpawn());
            broadcast(ChatColor.GREEN + player.getName() + ChatColor.YELLOW + " joined the lobby. (" + queuedPlayers.size() + "/" + getMinPlayers() + ")");
            tryStartCountdown();
        }

        scoreboardHandler.updateFor(player);
    }

    public void handleQuit(Player player) {
        UUID uuid = player.getUniqueId();
        queuedPlayers.remove(uuid);
        alivePlayers.remove(uuid);
        spectators.remove(uuid);
        lootManager.cancel(player.getUniqueId());

        if (state == GameState.STARTING && queuedPlayers.size() < getMinPlayers()) {
            cancelCountdown(ChatColor.RED + "Not enough players to start. Countdown cancelled.");
        }

        if (state == GameState.RUNNING) {
            checkForWinner();
        }

        scoreboardHandler.reset(player);
    }

    public void handleDeath(Player victim, Player killer) {
        UUID victimId = victim.getUniqueId();
        alivePlayers.remove(victimId);
        lootManager.cancel(victimId);
        spectators.add(victimId);
        victim.setGameMode(GameMode.SPECTATOR);
        victim.sendMessage(ChatColor.GRAY + "You are now spectating.");

        if (killer != null) {
            UUID killerId = killer.getUniqueId();
            kills.put(killerId, kills.getOrDefault(killerId, 0) + 1);
            Bukkit.broadcastMessage(ChatColor.RED + victim.getName() + ChatColor.GRAY + " was eliminated by " + ChatColor.GOLD + killer.getName() + ChatColor.GRAY + "!");
            killer.getWorld().strikeLightningEffect(victim.getLocation());
        } else {
            Bukkit.broadcastMessage(ChatColor.RED + victim.getName() + ChatColor.GRAY + " has fallen!");
            if (victim.getWorld() != null) {
                victim.getWorld().strikeLightningEffect(victim.getLocation());
            }
        }

        checkForWinner();
    }

    public void shutdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
        }
        if (gameTimerTask != null) {
            gameTimerTask.cancel();
        }
        if (scoreboardTask != null) {
            scoreboardTask.cancel();
        }
        lootManager.stopAll();
        if (gameWorld != null) {
            worldManager.deleteWorldAsync(gameWorld);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            scoreboardHandler.reset(player);
            player.setGameMode(GameMode.ADVENTURE);
            player.teleport(getLobbySpawn());
        }
        state = GameState.LOBBY;
    }

    private void tryStartCountdown() {
        if (state != GameState.LOBBY) {
            return;
        }
        if (queuedPlayers.size() >= getMinPlayers()) {
            startCountdown();
        }
    }

    private void startCountdown() {
        state = GameState.STARTING;
        countdownSeconds = plugin.getConfig().getInt("startCountdownSeconds", 30);
        broadcast(ChatColor.GOLD + "Round starting in " + countdownSeconds + " seconds!");

        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (queuedPlayers.size() < getMinPlayers()) {
                    cancelCountdown(ChatColor.RED + "Countdown stopped: not enough players.");
                    return;
                }

                if (countdownSeconds <= 0) {
                    cancel();
                    startGame();
                    return;
                }

                if (countdownSeconds == 30 || countdownSeconds == 15 || countdownSeconds <= 5) {
                    broadcast(ChatColor.YELLOW + "Game starting in " + countdownSeconds + "...");
                }
                countdownSeconds--;
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void cancelCountdown(String message) {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        state = GameState.LOBBY;
        if (message != null) {
            broadcast(message);
        }
    }

    private void startGame() {
        state = GameState.RUNNING;
        timeRemaining = plugin.getConfig().getInt("maxGameTime", 1200);

        gameWorld = worldManager.createGameWorld();
        alivePlayers.clear();
        alivePlayers.addAll(queuedPlayers);

        for (UUID uuid : queuedPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                alivePlayers.remove(uuid);
                continue;
            }
            kills.put(uuid, 0);
            player.setGameMode(GameMode.SURVIVAL);
            Location spawn = gameWorld.getSpawnLocation();
            player.teleport(spawn);
            lootManager.scheduleNextDrop(player.getUniqueId());
        }
        queuedPlayers.clear();
        spectators.clear();

        worldManager.prepareWorld(gameWorld, plugin.getConfig());

        broadcast(ChatColor.GREEN + "Round started! Survive and eliminate your opponents.");

        gameTimerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (timeRemaining <= 0) {
                    endRound(null, ChatColor.YELLOW + "Time limit reached!");
                    return;
                }
                timeRemaining--;
                scoreboardHandler.updateAll();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void endRound(UUID winner, String reason) {
        if (state == GameState.ENDING || state == GameState.LOBBY) {
            return;
        }
        state = GameState.ENDING;

        if (gameTimerTask != null) {
            gameTimerTask.cancel();
            gameTimerTask = null;
        }

        lootManager.stopAll();

        if (winner != null) {
            Player player = Bukkit.getPlayer(winner);
            if (player != null) {
                broadcast(ChatColor.GOLD + player.getName() + ChatColor.GREEN + " wins the round!");
            }
        }

        if (reason != null) {
            broadcast(reason);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                queuedPlayers.clear();
                alivePlayers.clear();
                spectators.clear();

                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.teleport(getLobbySpawn());
                    player.setGameMode(GameMode.ADVENTURE);
                    queuedPlayers.add(player.getUniqueId());
                    kills.put(player.getUniqueId(), 0);
                    scoreboardHandler.updateFor(player);
                }

                if (gameWorld != null) {
                    worldManager.deleteWorldAsync(gameWorld);
                    gameWorld = null;
                }

                state = GameState.LOBBY;
                tryStartCountdown();
            }
        }.runTaskLater(plugin, 100L);
    }

    private void checkForWinner() {
        if (state != GameState.RUNNING) {
            return;
        }

        if (alivePlayers.size() <= 1) {
            UUID winner = alivePlayers.stream().findFirst().orElse(null);
            endRound(winner, null);
        }
    }

    private void broadcast(String message) {
        Bukkit.broadcastMessage(ChatColor.DARK_PURPLE + "[BattleRoyale] " + ChatColor.RESET + message);
    }

    private void startScoreboardLoop() {
        scoreboardTask = new BukkitRunnable() {
            @Override
            public void run() {
                scoreboardHandler.updateAll();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public int getMinPlayers() {
        return plugin.getConfig().getInt("minPlayers", 4);
    }

    public GameState getState() {
        return state;
    }

    public int getCountdownSeconds() {
        return countdownSeconds;
    }

    public int getTimeRemaining() {
        return timeRemaining;
    }

    public World getGameWorld() {
        return gameWorld;
    }

    public Set<UUID> getAlivePlayers() {
        return Collections.unmodifiableSet(alivePlayers);
    }

    public Set<UUID> getQueuedPlayers() {
        return Collections.unmodifiableSet(queuedPlayers);
    }

    public Map<UUID, Integer> getKills() {
        return Collections.unmodifiableMap(kills);
    }

    public ScoreboardHandler getScoreboardHandler() {
        return scoreboardHandler;
    }

    public Location getLobbySpawn() {
        World world = Bukkit.getWorlds().get(0);
        return world.getSpawnLocation();
    }

    public LootManager getLootManager() {
        return lootManager;
    }
}
