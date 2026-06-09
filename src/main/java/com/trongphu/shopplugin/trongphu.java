package com.trongphu.shopplugin;

import me.clip.placeholderapi.PlaceholderAPI;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class trongphu extends JavaPlugin implements Listener, TabCompleter {

    private static Economy econ = null;
    private static final String PREFIX = ChatColor.GOLD + "[LegendaryShop] " + ChatColor.RESET;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (!setupEconomy()) {
            getLogger().severe("Không tìm thấy Vault! Vô hiệu hóa plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("shop").setExecutor(this);
        getCommand("shop").setTabCompleter(this);

        getLogger().info("========================================");
        getLogger().info(" 🛍️  LegendaryShop Premium v2.7 - Ready!");
        getLogger().info("========================================");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("shop")) {
            return false;
        }

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("legendaryshop.admin")) {
                    sender.sendMessage(ChatColor.RED + "Bạn không có quyền!");
                    return true;
                }
                reloadConfig();
                sender.sendMessage(ChatColor.GREEN + PREFIX + "Đã tải lại cấu hình!");
                return true;
            }

            if (args[0].equalsIgnoreCase("admin")) {
                if (!sender.hasPermission("legendaryshop.admin")) {
                    sender.sendMessage(ChatColor.RED + "Bạn không có quyền Admin!");
                    return true;
                }

                if (args.length < 4) {
                    sender.sendMessage(ChatColor.YELLOW + "========== ADMIN COMMANDS ==========");
                    sender.sendMessage(ChatColor.GREEN + "/shop admin add <player> <amount>");
                    sender.sendMessage(ChatColor.GREEN + "/shop admin remove <player> <amount>");
                    sender.sendMessage(ChatColor.GREEN + "/shop admin set <player> <amount>");
                    sender.sendMessage(ChatColor.YELLOW + "===================================");
                    return true;
                }

                String action = args[1];
                Player target = Bukkit.getPlayer(args[2]);

                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Người chơi không online!");
                    return true;
                }

                double amount;
                try {
                    amount = Double.parseDouble(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Số tiền không hợp lệ!");
                    return true;
                }

                if (action.equalsIgnoreCase("add")) {
                    econ.depositPlayer(target, amount);
                    sender.sendMessage(ChatColor.GREEN + "✓ Đã cộng " + amount + " 💲 cho " + target.getName());
                    target.sendMessage(ChatColor.GREEN + "Admin đã cộng " + amount + " 💲 cho bạn!");
                } else if (action.equalsIgnoreCase("remove")) {
                    econ.withdrawPlayer(target, amount);
                    sender.sendMessage(ChatColor.YELLOW + "✓ Đã trừ " + amount + " 💲 của " + target.getName());
                    target.sendMessage(ChatColor.RED + "Admin đã trừ " + amount + " 💲 từ bạn!");
                } else if (action.equalsIgnoreCase("set")) {
                    double currentBal = econ.getBalance(target);
                    econ.withdrawPlayer(target, currentBal);
                    econ.depositPlayer(target, amount);
                    sender.sendMessage(ChatColor.GREEN + "✓ Đã đặt tiền của " + target.getName() + " = " + amount + " 💲");
                    target.sendMessage(ChatColor.GREEN + "Admin đã đặt tiền của bạn thành " + amount + " 💲!");
                }
                return true;
            }
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Lệnh này chỉ dành cho người chơi!");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("legendaryshop.use")) {
            player.sendMessage(ChatColor.RED + "Bạn không có quyền mở cửa hàng!");
            return true;
        }

        openMainMenu(player);
        return true;
    }

    private void openMainMenu(Player player) {
        FileConfiguration config = getConfig();

        if (!config.contains("main-menu")) {
            player.sendMessage(ChatColor.RED + "Cấu hình main-menu không tồn tại!");
            return;
        }

        String title = ChatColor.translateAlternateColorCodes('&', 
            config.getString("main-menu.title", "&6Shop"));
        int size = config.getInt("main-menu.size", 27);

        Inventory inv = Bukkit.createInventory(null, size, title);

        if (config.contains("main-menu.items")) {
            for (String key : config.getConfigurationSection("main-menu.items").getKeys(false)) {
                String path = "main-menu.items." + key + ".";

                try {
                    int slot = Integer.parseInt(key);
                    String materialName = config.getString(path + "material");
                    String displayName = ChatColor.translateAlternateColorCodes('&', 
                        config.getString(path + "name", "Item"));
                    String lore = ChatColor.translateAlternateColorCodes('&', 
                        config.getString(path + "lore", ""));

                    Material mat = Material.matchMaterial(materialName);
                    if (mat == null) continue;

                    ItemStack item = new ItemStack(mat);
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(displayName.replaceAll("\"", ""));
                        meta.setLore(Arrays.asList(lore));
                        item.setItemMeta(meta);
                    }

                    if (slot >= 0 && slot < size) {
                        inv.setItem(slot, item);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        player.openInventory(inv);
    }

    private void openCategoryShop(Player player, String category) {
        FileConfiguration config = getConfig();
        String path = "categories." + category + ".";

        if (!config.contains(path)) {
            player.sendMessage(ChatColor.RED + "Category '" + category + "' không tồn tại!");
            return;
        }

        String title = ChatColor.translateAlternateColorCodes('&', 
            config.getString(path + "title", "&6" + category));
        int size = config.getInt(path + "size", 27);

        Inventory inv = Bukkit.createInventory(null, size, title);

        if (config.contains(path + "items")) {
            for (String key : config.getConfigurationSection(path + "items").getKeys(false)) {
                String itemPath = path + "items." + key + ".";

                try {
                    int slot = Integer.parseInt(key);
                    String materialName = config.getString(itemPath + "material");
                    String displayName = ChatColor.translateAlternateColorCodes('&', 
                        config.getString(itemPath + "name", "Item"));
                    double price = config.getDouble(itemPath + "price", 0);
                    boolean useShards = config.getBoolean(itemPath + "use-shards", false);

                    Material mat = Material.matchMaterial(materialName);
                    if (mat == null) continue;

                    ItemStack item = new ItemStack(mat);
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(displayName.replaceAll("\"", ""));
                        List<String> lore = new ArrayList<>();
                        String symbol = useShards ? "💎" : "💲";
                        lore.add(ChatColor.GREEN + "Giá: " + (int)price + " " + symbol);
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                    }

                    if (slot >= 0 && slot < size) {
                        inv.setItem(slot, item);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        FileConfiguration config = getConfig();

        String mainTitle = ChatColor.translateAlternateColorCodes('&', 
            config.getString("main-menu.title", "&6Shop"));
        
        if (title.equals(mainTitle)) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                return;
            }

            int slot = event.getSlot();

            if (config.contains("main-menu.items." + slot)) {
                String category = config.getString("main-menu.items." + slot + ".category");
                if (category != null) {
                    openCategoryShop(player, category);
                }
            }
            return;
        }

        if (config.contains("categories")) {
            for (String categoryKey : config.getConfigurationSection("categories").getKeys(false)) {
                String categoryPath = "categories." + categoryKey + ".";
                String categoryTitle = ChatColor.translateAlternateColorCodes('&', 
                    config.getString(categoryPath + "title", "&6" + categoryKey));

                if (title.equals(categoryTitle)) {
                    event.setCancelled(true);

                    if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                        return;
                    }

                    int slot = event.getSlot();

                    if (config.contains(categoryPath + "items." + slot)) {
                        String itemPath = categoryPath + "items." + slot + ".";
                        String materialName = config.getString(itemPath + "material");
                        double price = config.getDouble(itemPath + "price", 0);
                        String mobType = config.getString(itemPath + "mob-type");
                        boolean useShards = config.getBoolean(itemPath + "use-shards", false);

                        Material mat = Material.matchMaterial(materialName);
                        if (mat == null) return;

                        // ✅ Kiểm tra tiền/shard TRƯỚC khi mua
                        if (useShards) {
                            double playerShards = getPlayerShards(player);
                            if (playerShards < price) {
                                player.sendMessage(PREFIX + ChatColor.RED + "❌ Không đủ Shard!");
                                player.sendMessage(PREFIX + ChatColor.YELLOW + "Bạn có: " + ChatColor.AQUA + (int)playerShards);
                                player.sendMessage(PREFIX + ChatColor.YELLOW + "Cần: " + ChatColor.AQUA + (int)price);
                                player.closeInventory();
                                return;
                            }
                        } else {
                            if (econ.getBalance(player) < price) {
                                player.sendMessage(PREFIX + ChatColor.RED + "❌ Không đủ tiền!");
                                player.sendMessage(PREFIX + ChatColor.YELLOW + "Bạn có: " + ChatColor.AQUA + (int)econ.getBalance(player) + " 💲");
                                player.sendMessage(PREFIX + ChatColor.YELLOW + "Cần: " + ChatColor.AQUA + (int)price + " 💲");
                                player.closeInventory();
                                return;
                            }
                        }

                        // ✅ Trừ tiền/shard
                        if (useShards) {
                            removeShards(player, price);
                        } else {
                            econ.withdrawPlayer(player, price);
                        }
                        
                        // Nếu là spawner
                        if (mobType != null && !mobType.isEmpty()) {
                            Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
                                String command = "ss give spawner " + player.getName() + " " + mobType + " 1";
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                                getLogger().info("[LegendaryShop] " + player.getName() + " mua spawner: " + mobType);
                            }, 1L);
                            
                            player.sendMessage(PREFIX + ChatColor.GREEN + "✓ Mua thành công! Đã nhận spawner!");
                        } else {
                            // Item thường
                            player.getInventory().addItem(new ItemStack(mat, 1));
                            String currencySymbol = useShards ? "💎" : "💲";
                            player.sendMessage(PREFIX + ChatColor.GREEN + "✓ Mua thành công! Đã trừ " + (int)price + " " + currencySymbol);
                        }
                        
                        player.closeInventory();
                    }
                    return;
                }
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("shop")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Arrays.asList("admin", "reload");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            return Arrays.asList("add", "remove", "set");
        }

        return new ArrayList<>();
    }

    /**
     * Lấy số Shard THỰC TẾ của player từ PlaceholderAPI
     */
    private double getPlayerShards(Player player) {
        try {
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null ||
                Bukkit.getPluginManager().getPlugin("xShards") == null) {
                return 0;
            }

            String shardStr = PlaceholderAPI.setPlaceholders(player, "%xshards_balance%");
            
            if (shardStr == null || shardStr.isEmpty() || shardStr.equals("%xshards_balance%")) {
                return 0;
            }

            return Double.parseDouble(shardStr);
        } catch (NumberFormatException e) {
            getLogger().severe("[LegendaryShop] Lỗi parse Shard: " + e.getMessage());
            return 0;
        } catch (Exception e) {
            getLogger().severe("[LegendaryShop] Lỗi lấy Shard: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Trừ shards của player
     */
    private void removeShards(Player player, double amount) {
        try {
            if (Bukkit.getPluginManager().getPlugin("xShards") == null) {
                getLogger().severe("[LegendaryShop] xShards plugin không tìm thấy!");
                return;
            }

            String command = "shards take " + player.getName() + " " + (int)amount;
            boolean result = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            
            getLogger().info("[LegendaryShop] Trừ Shard - Player: " + player.getName() + 
                    ", Amount: " + (int)amount + ", Success: " + result);
        } catch (Exception e) {
            getLogger().severe("[LegendaryShop] Lỗi trừ Shard: " + e.getMessage());
        }
    }
}
