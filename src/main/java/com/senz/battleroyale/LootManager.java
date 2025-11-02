package com.senz.battleroyale;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class LootManager {

    private final BattleRoyalePlugin plugin;
    private final Random random = new Random();

    private final Map<UUID, BukkitTask> scheduledTasks = new HashMap<>();
    private final Map<UUID, Long> nextDropAt = new HashMap<>();
    private final List<LootKit> kits = new ArrayList<>();
    private final Map<LootRarity, Integer> rarityWeights = new EnumMap<>(LootRarity.class);
    private boolean randomEnchantments;

    public LootManager(BattleRoyalePlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        cancelAllTasks();
        kits.clear();
        rarityWeights.clear();
        randomEnchantments = plugin.getConfig().getBoolean("randomEnchantments", true);

        rarityWeights.put(LootRarity.COMMON, plugin.getConfig().getInt("rarityWeights.common", 60));
        rarityWeights.put(LootRarity.UNCOMMON, plugin.getConfig().getInt("rarityWeights.uncommon", 30));
        rarityWeights.put(LootRarity.RARE, plugin.getConfig().getInt("rarityWeights.rare", 10));

        Map<String, Boolean> kitToggles = new HashMap<>();
        if (plugin.getConfig().getConfigurationSection("kits") != null) {
            for (String key : plugin.getConfig().getConfigurationSection("kits").getKeys(false)) {
                kitToggles.put(key, plugin.getConfig().getBoolean("kits." + key));
            }
        }

        if (kitToggles.getOrDefault("sword", true)) {
            kits.add(new LootKit("Sword", LootRarity.COMMON, createSwordKit()));
        }
        if (kitToggles.getOrDefault("mace", true)) {
            kits.add(new LootKit("Mace", LootRarity.UNCOMMON, createMaceKit()));
        }
        if (kitToggles.getOrDefault("uhc", true)) {
            kits.add(new LootKit("UHC", LootRarity.COMMON, createUhcKit()));
        }
        if (kitToggles.getOrDefault("axe_shield", true)) {
            kits.add(new LootKit("Axe & Shield", LootRarity.COMMON, createAxeShieldKit()));
        }
        if (kitToggles.getOrDefault("sniper", true)) {
            kits.add(new LootKit("Sniper", LootRarity.UNCOMMON, createSniperKit()));
        }
        if (kitToggles.getOrDefault("fairy", true)) {
            kits.add(new LootKit("Fairy", LootRarity.RARE, createFairyKit()));
        }
        if (kitToggles.getOrDefault("mace_elytra", true)) {
            kits.add(new LootKit("Mace + Elytra", LootRarity.RARE, createMaceElytraKit()));
        }
    }

    public void scheduleNextDrop(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return;
        }
        int min = plugin.getConfig().getInt("minLootIntervalSeconds", 60);
        int max = plugin.getConfig().getInt("maxLootIntervalSeconds", 180);
        int interval = random.nextInt(Math.max(1, max - min + 1)) + min;

        nextDropAt.put(playerId, System.currentTimeMillis() + (interval * 1000L));

        BukkitTask existing = scheduledTasks.remove(playerId);
        if (existing != null) {
            existing.cancel();
        }

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                giveLoot(playerId);
            }
        }.runTaskLater(plugin, interval * 20L);
        scheduledTasks.put(playerId, task);
    }

    private void giveLoot(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            scheduledTasks.remove(playerId);
            nextDropAt.remove(playerId);
            return;
        }
        if (kits.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No loot kits are enabled.");
            return;
        }

        int minItems = plugin.getConfig().getInt("minItemsPerDrop", 1);
        int maxItems = plugin.getConfig().getInt("maxItemsPerDrop", 3);
        int itemsToGive = random.nextInt(Math.max(1, maxItems - minItems + 1)) + minItems;

        for (int i = 0; i < itemsToGive; i++) {
            LootKit kit = selectRandomKit();
            ItemStack item = kit.randomItem(random).clone();
            applyRandomEnchantments(item);
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
            overflow.values().forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        }

        player.sendMessage(ChatColor.GOLD + "You received a loot drop!");
        scheduleNextDrop(playerId);
    }

    public void cancel(UUID playerId) {
        BukkitTask task = scheduledTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        nextDropAt.remove(playerId);
    }

    public void stopAll() {
        cancelAllTasks();
        nextDropAt.clear();
    }

    private void cancelAllTasks() {
        for (BukkitTask task : scheduledTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        scheduledTasks.clear();
    }

    public int getSecondsUntilDrop(UUID playerId) {
        Long time = nextDropAt.get(playerId);
        if (time == null) {
            return -1;
        }
        long diff = time - System.currentTimeMillis();
        return diff <= 0 ? 0 : (int) (diff / 1000L);
    }

    private LootKit selectRandomKit() {
        int totalWeight = 0;
        for (LootKit kit : kits) {
            totalWeight += Math.max(1, rarityWeights.getOrDefault(kit.rarity(), 1));
        }
        int roll = random.nextInt(Math.max(1, totalWeight));
        for (LootKit kit : kits) {
            roll -= Math.max(1, rarityWeights.getOrDefault(kit.rarity(), 1));
            if (roll < 0) {
                return kit;
            }
        }
        return kits.get(random.nextInt(kits.size()));
    }

    private List<ItemStack> createSwordKit() {
        List<ItemStack> items = new ArrayList<>();
        items.add(createWeapon(Material.NETHERITE_SWORD, Map.of(
                Enchantment.DAMAGE_ALL, new LevelRange(3, 5),
                Enchantment.DAMAGE_UNDEAD, new LevelRange(3, 4),
                Enchantment.FIRE_ASPECT, new LevelRange(1, 2),
                Enchantment.LOOT_BONUS_MOBS, new LevelRange(3, 3),
                Enchantment.MENDING, new LevelRange(1, 1),
                Enchantment.DURABILITY, new LevelRange(3, 3)
        )));
        items.add(createArmor(Material.NETHERITE_HELMET));
        items.add(createArmor(Material.NETHERITE_CHESTPLATE));
        items.add(createArmor(Material.NETHERITE_LEGGINGS));
        items.add(createBoots(Material.NETHERITE_BOOTS));
        items.add(new ItemStack(Material.GOLDEN_APPLE, 4));
        items.add(createPotion(PotionType.STRENGTH, false));
        items.add(createPotion(PotionType.SPEED, false));
        return items;
    }

    private List<ItemStack> createMaceKit() {
        List<ItemStack> items = new ArrayList<>();
        items.add(createWeapon(Material.MACE, Map.of(
                Enchantment.DAMAGE_ALL, new LevelRange(4, 5),
                Enchantment.DURABILITY, new LevelRange(3, 3),
                Enchantment.KNOCKBACK, new LevelRange(1, 2),
                Enchantment.FIRE_ASPECT, new LevelRange(1, 1),
                Enchantment.MENDING, new LevelRange(1, 1),
                Enchantment.DENSITY, new LevelRange(4, 5),
                Enchantment.WIND_BURST, new LevelRange(2, 3)
        )));
        items.add(createArmor(Material.NETHERITE_HELMET));
        items.add(createArmor(Material.NETHERITE_CHESTPLATE));
        items.add(createArmor(Material.NETHERITE_LEGGINGS));
        items.add(createArmor(Material.NETHERITE_BOOTS));
        items.add(new ItemStack(Material.ENDER_PEARL, 2));
        items.add(createPotion(PotionType.INSTANT_HEAL, true));
        return items;
    }

    private List<ItemStack> createUhcKit() {
        List<ItemStack> items = new ArrayList<>();
        items.add(createWeapon(Material.DIAMOND_SWORD, Map.of(
                Enchantment.DAMAGE_ALL, new LevelRange(2, 3),
                Enchantment.DURABILITY, new LevelRange(3, 3)
        )));
        items.add(createArmor(Material.DIAMOND_HELMET));
        items.add(createArmor(Material.DIAMOND_CHESTPLATE));
        items.add(createArmor(Material.DIAMOND_LEGGINGS));
        items.add(createArmor(Material.DIAMOND_BOOTS));
        items.add(createBow(Map.of(
                Enchantment.ARROW_DAMAGE, new LevelRange(2, 4),
                Enchantment.ARROW_INFINITE, new LevelRange(1, 1),
                Enchantment.ARROW_FIRE, new LevelRange(1, 1),
                Enchantment.DURABILITY, new LevelRange(3, 3)
        )));
        items.add(new ItemStack(Material.ARROW, 4));
        items.add(new ItemStack(Material.LAVA_BUCKET));
        items.add(new ItemStack(Material.WATER_BUCKET));
        items.add(createPotion(PotionType.REGEN, false));
        items.add(createPotion(PotionType.INSTANT_HEAL, false));
        items.add(new ItemStack(Material.GOLDEN_APPLE, 2));
        return items;
    }

    private List<ItemStack> createAxeShieldKit() {
        List<ItemStack> items = new ArrayList<>();
        items.add(createWeapon(Material.NETHERITE_AXE, Map.of(
                Enchantment.DAMAGE_ALL, new LevelRange(3, 4),
                Enchantment.DURABILITY, new LevelRange(3, 3),
                Enchantment.SWEEPING_EDGE, new LevelRange(2, 3)
        )));
        items.add(applyEnchantments(new ItemStack(Material.SHIELD), Map.of(
                Enchantment.DURABILITY, new LevelRange(3, 3)
        )));
        items.add(new ItemStack(Material.BREAD, 16));
        items.add(createPotion(PotionType.STRENGTH, false));
        items.add(new ItemStack(Material.FIRE_CHARGE, 4));
        return items;
    }

    private List<ItemStack> createSniperKit() {
        List<ItemStack> items = new ArrayList<>();
        items.add(createBow(Map.of(
                Enchantment.ARROW_DAMAGE, new LevelRange(4, 4),
                Enchantment.ARROW_INFINITE, new LevelRange(1, 1),
                Enchantment.ARROW_FIRE, new LevelRange(1, 1),
                Enchantment.DURABILITY, new LevelRange(3, 3)
        )));
        items.add(createWeapon(Material.DIAMOND_SWORD, Map.of(
                Enchantment.DAMAGE_ALL, new LevelRange(3, 3),
                Enchantment.DURABILITY, new LevelRange(3, 3)
        )));
        items.add(createArmor(Material.DIAMOND_HELMET));
        items.add(createArmor(Material.DIAMOND_CHESTPLATE));
        items.add(createArmor(Material.DIAMOND_LEGGINGS));
        items.add(createArmor(Material.DIAMOND_BOOTS));
        items.add(new ItemStack(Material.ARROW, 32));
        items.add(new ItemStack(Material.GOLDEN_APPLE, 2));
        return items;
    }

    private List<ItemStack> createFairyKit() {
        List<ItemStack> items = new ArrayList<>();
        items.add(applyEnchantments(new ItemStack(Material.ELYTRA), Map.of(
                Enchantment.DURABILITY, new LevelRange(3, 3),
                Enchantment.MENDING, new LevelRange(1, 1)
        )));
        items.add(new ItemStack(Material.FIREWORK_ROCKET, 12));
        items.add(createWeapon(Material.DIAMOND_SWORD, Map.of(
                Enchantment.DAMAGE_ALL, new LevelRange(3, 3),
                Enchantment.FIRE_ASPECT, new LevelRange(1, 1),
                Enchantment.DURABILITY, new LevelRange(3, 3)
        )));
        items.add(createArmor(Material.DIAMOND_HELMET));
        items.add(createArmor(Material.DIAMOND_CHESTPLATE));
        items.add(createArmor(Material.DIAMOND_LEGGINGS));
        items.add(createBoots(Material.DIAMOND_BOOTS));
        items.add(createPotion(PotionType.SPEED, false));
        return items;
    }

    private List<ItemStack> createMaceElytraKit() {
        List<ItemStack> items = new ArrayList<>();
        items.add(applyEnchantments(new ItemStack(Material.ELYTRA), Map.of(
                Enchantment.DURABILITY, new LevelRange(3, 3),
                Enchantment.MENDING, new LevelRange(1, 1)
        )));
        items.add(new ItemStack(Material.FIREWORK_ROCKET, 16));
        items.add(createWeapon(Material.MACE, Map.of(
                Enchantment.DENSITY, new LevelRange(5, 5),
                Enchantment.WIND_BURST, new LevelRange(3, 3),
                Enchantment.MENDING, new LevelRange(1, 1),
                Enchantment.DURABILITY, new LevelRange(3, 3)
        )));
        items.add(createArmor(Material.NETHERITE_HELMET));
        items.add(createArmor(Material.NETHERITE_CHESTPLATE));
        items.add(createArmor(Material.NETHERITE_LEGGINGS));
        items.add(createArmor(Material.NETHERITE_BOOTS));
        items.add(createPotion(PotionType.STRENGTH, false));
        return items;
    }

    private ItemStack createWeapon(Material material, Map<Enchantment, LevelRange> enchants) {
        return applyEnchantments(new ItemStack(material), enchants);
    }

    private ItemStack createArmor(Material material) {
        Map<Enchantment, LevelRange> enchants = new HashMap<>();
        enchants.put(Enchantment.PROTECTION_ENVIRONMENTAL, new LevelRange(3, 4));
        enchants.put(Enchantment.DURABILITY, new LevelRange(3, 3));
        if (material == Material.NETHERITE_BOOTS || material == Material.DIAMOND_BOOTS) {
            enchants.put(Enchantment.PROTECTION_FALL, new LevelRange(4, 4));
        }
        return applyEnchantments(new ItemStack(material), enchants);
    }

    private ItemStack createBoots(Material material) {
        return createArmor(material);
    }

    private ItemStack createBow(Map<Enchantment, LevelRange> enchants) {
        return applyEnchantments(new ItemStack(Material.BOW), enchants);
    }

    private ItemStack createPotion(PotionType type, boolean splash) {
        ItemStack stack = new ItemStack(splash ? Material.SPLASH_POTION : Material.POTION);
        ItemMeta meta = stack.getItemMeta();
        if (meta instanceof PotionMeta potionMeta) {
            potionMeta.setBasePotionData(new PotionData(type));
            stack.setItemMeta(potionMeta);
        }
        return stack;
    }

    private ItemStack applyEnchantments(ItemStack item, Map<Enchantment, LevelRange> enchantments) {
        for (Map.Entry<Enchantment, LevelRange> entry : enchantments.entrySet()) {
            Enchantment enchantment = entry.getKey();
            LevelRange range = entry.getValue();
            int level = randomEnchantments ? randomLevel(range) : range.max();
            if (enchantment != Enchantment.PROTECTION_EXPLOSIONS) {
                item.addUnsafeEnchantment(enchantment, level);
            }
        }
        return item;
    }

    private int randomLevel(LevelRange range) {
        if (range.min() == range.max()) {
            return range.min();
        }
        return random.nextInt(range.max() - range.min() + 1) + range.min();
    }

    private record LootKit(String name, LootRarity rarity, List<ItemStack> items) {
        public ItemStack randomItem(Random random) {
            return items.get(random.nextInt(items.size()));
        }
    }

    private enum LootRarity {
        COMMON,
        UNCOMMON,
        RARE
    }

    private record LevelRange(int min, int max) {
    }
}
