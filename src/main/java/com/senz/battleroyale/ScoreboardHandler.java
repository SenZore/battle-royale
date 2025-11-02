package com.senz.battleroyale;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ScoreboardHandler {

    private final BattleRoyalePlugin plugin;

    public ScoreboardHandler(BattleRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    public void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateFor(player);
        }
    }

    public void updateFor(Player player) {
        Scoreboard scoreboard = createScoreboard(player.getUniqueId());
        player.setScoreboard(scoreboard);
    }

    public void reset(Player player) {
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        if (scoreboardManager != null) {
            player.setScoreboard(scoreboardManager.getMainScoreboard());
        }
    }

    private Scoreboard createScoreboard(UUID playerId) {
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        if (scoreboardManager == null) {
            throw new IllegalStateException("Scoreboard manager unavailable");
        }

        Scoreboard scoreboard = scoreboardManager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("battle", "dummy", ChatColor.GOLD + "Battle Royale");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        GameManager gameManager = plugin.getGameManager();
        List<String> lines = new ArrayList<>();

        lines.add(ChatColor.YELLOW + "State: " + formatState(gameManager.getState()));
        int playerCount = gameManager.getState() == GameState.RUNNING
                ? gameManager.getAlivePlayers().size()
                : gameManager.getQueuedPlayers().size();
        lines.add(ChatColor.YELLOW + "Players: " + playerCount + "/" + gameManager.getMinPlayers());

        if (gameManager.getState() == GameState.STARTING) {
            lines.add(ChatColor.YELLOW + "Start in: " + gameManager.getCountdownSeconds() + "s");
        } else if (gameManager.getState() == GameState.RUNNING) {
            lines.add(ChatColor.YELLOW + "Time left: " + formatTime(gameManager.getTimeRemaining()));
        } else {
            lines.add(ChatColor.YELLOW + "Time left: --");
        }

        double borderSize = 0;
        if (gameManager.getGameWorld() != null) {
            WorldBorder border = gameManager.getGameWorld().getWorldBorder();
            borderSize = border.getSize();
        }
        lines.add(ChatColor.YELLOW + "Border: " + (borderSize > 0 ? (int) borderSize : "--"));

        int kills = gameManager.getKills().getOrDefault(playerId, 0);
        lines.add(ChatColor.YELLOW + "Kills: " + kills);

        int lootTime = plugin.getGameManager().getLootManager().getSecondsUntilDrop(playerId);
        lines.add(ChatColor.YELLOW + "Next loot: " + (lootTime >= 0 ? lootTime + "s" : "--"));

        lines.add(ChatColor.GRAY.toString());
        lines.add(ChatColor.GRAY + "battle-royale");

        int score = lines.size();
        for (String line : lines) {
            objective.getScore(line).setScore(score--);
        }
        return scoreboard;
    }

    private String formatState(GameState state) {
        return switch (state) {
            case LOBBY -> "Lobby";
            case STARTING -> "Starting";
            case RUNNING -> "Running";
            case ENDING -> "Ending";
        };
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }
}
