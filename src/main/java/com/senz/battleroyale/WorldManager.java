package com.senz.battleroyale;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class WorldManager {

    private final BattleRoyalePlugin plugin;

    public WorldManager(BattleRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    public World createGameWorld() {
        String worldName = "battle_" + UUID.randomUUID().toString().substring(0, 8);
        WorldCreator creator = new WorldCreator(worldName);
        creator.environment(World.Environment.NORMAL);
        creator.type(WorldType.NORMAL);
        creator.generateStructures(false);
        World world = creator.createWorld();
        if (world == null) {
            throw new IllegalStateException("Failed to create battle world");
        }
        world.setAutoSave(false);
        Location spawn = new Location(world, 0.5, world.getHighestBlockYAt(0, 0) + 2, 0.5);
        world.setSpawnLocation(spawn);

        // Pre-load the Nether with same border rules
        WorldCreator netherCreator = new WorldCreator(worldName + "_nether");
        netherCreator.environment(World.Environment.NETHER);
        netherCreator.generateStructures(false);
        World nether = netherCreator.createWorld();
        if (nether != null) {
            nether.setAutoSave(false);
        }

        return world;
    }

    public void prepareWorld(World world, FileConfiguration config) {
        double borderSize = config.getDouble("borderSize", 300.0);
        double shrinkSpeed = config.getDouble("borderShrinkSpeed", 5.0); // blocks per minute

        world.getWorldBorder().setCenter(0, 0);
        world.getWorldBorder().setSize(borderSize);
        world.getWorldBorder().setDamageAmount(1.5);
        world.getWorldBorder().setWarningDistance(10);

        double minSize = 20.0;
        if (borderSize > minSize) {
            double distance = borderSize - minSize;
            double seconds = Math.max(30, (distance / Math.max(1.0, shrinkSpeed)) * 60.0);
            world.getWorldBorder().setSize(minSize, (long) seconds);

            World nether = Bukkit.getWorld(world.getName() + "_nether");
            if (nether != null) {
                nether.getWorldBorder().setCenter(0, 0);
                nether.getWorldBorder().setSize(borderSize);
                nether.getWorldBorder().setSize(minSize, (long) seconds);
            }
        }
    }

    public void deleteWorldAsync(World world) {
        String worldName = world.getName();
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.unloadWorld(world, false);
                World nether = Bukkit.getWorld(worldName + "_nether");
                if (nether != null) {
                    Bukkit.unloadWorld(nether, false);
                }

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    deleteWorldFolder(new File(Bukkit.getWorldContainer(), worldName));
                    deleteWorldFolder(new File(Bukkit.getWorldContainer(), worldName + "_nether"));
                });
            }
        }.runTask(plugin);
    }

    private void deleteWorldFolder(File file) {
        if (!file.exists()) {
            return;
        }
        try {
            Files.walk(file.toPath())
                    .sorted((a, b) -> b.compareTo(a))
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to delete world folder " + file.getName() + ": " + e.getMessage());
        }
    }
}
