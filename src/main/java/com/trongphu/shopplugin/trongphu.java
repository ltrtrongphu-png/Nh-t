package com.trongphu.shopplugin;

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

    @Override
    public void onEnable() {
        // Lưu config mặc định nếu chưa tồn tại
        saveDefaultConfig();

        // Setup Vault Economy
        if (!setupEconomy()) {
            getLogger().severe("Khong tim thay Vault! Vo hieu hoa plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register events & commands
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("shop").setExecutor(this);
        getCommand("shop").setTabCompleter(this);

        getLogger().info("========================================");
        getLogger().info(" LegendaryShop Premium v2.6 - Kich Hoat!");
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

        // Xử lý subcommands
        if (args.length > 0) {
            // /shop reload
            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("legendaryshop.admin")) {
                    sender.sendMessage(ChatColor.RED + "Bạn không có quyền!");
                    return true;
                }
                reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "[LegendaryShop] Đã tải lại cấu hình!");
                return true;
            }

            // /shop admin add/remove/set <player> <amount>
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
                } else {
                    sender.sendMessage(ChatColor.RED + "Action không hợp lệ!");
                }
                return true;
            }
        }

        // /shop - Mở main menu
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

    // Mở menu chính
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

    // Mở category shop
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

        // Kiểm tra main menu
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

        // Kiểm tra category shops
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

                        // Kiểm tra tiền hoặc shard
                        boolean canAfford = useShards ? checkShards(player, price) : econ.getBalance(player) >= price;

                        if (canAfford) {
                            // Trừ tiền hoặc shard
                            if (useShards) {
                                removeShards(player, price);
                            } else {
                                econ.withdrawPlayer(player, price);
                            }
                            
                            // Nếu là spawner, dùng SmartSpawner command
                            if (mobType != null && !mobType.isEmpty()) {
                                try {
                                    // Delay 1 tick để ensure player loaded
                                    Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
                                        String command = "ss give spawner " + player.getName() + " " + mobType + " 1";
                                        getLogger().warning("[LegendaryShop DEBUG] Command: " + command);
                                        boolean result = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                                        getLogger().warning("[LegendaryShop DEBUG] Command executed: " + result);
                                    }, 1L);
                                    
                                    player.sendMessage(ChatColor.GREEN + "✓ Mua thành công! Đã nhận spawner!");
                                } catch (Exception e) {
                                    getLogger().severe("[LegendaryShop ERROR] " + e.getMessage());
                                    player.sendMessage(ChatColor.RED + "✗ Lỗi khi cấp spawner! Liên hệ admin.");
                                }
                            } else {
                                // Item thường
                                player.getInventory().addItem(new ItemStack(mat, 1));
                                String currencySymbol = useShards ? "💎" : "💲";
                                player.sendMessage(ChatColor.GREEN + "✓ Mua thành công! Đã trừ " + (int)price + " " + currencySymbol);
                            }
                        } else {
                            String currencySymbol = useShards ? "💎 Shard" : "💲";
                            player.sendMessage(ChatColor.RED + "✗ Bạn không có đủ " + currencySymbol + "! Cần " + (int)price);
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

        if (args.length == 3 && args[0].equalsIgnoreCase("admin")) {
            return null; // Hiển thị danh sách player online
        }

        return new ArrayList<>();
    }

        public FileConfiguration getConfig() {
        return super.getConfig();
    }

    // Kiểm tra shards của player
    private boolean checkShards(Player player, double amount) {
        try {
            if (Bukkit.getPluginManager().getPlugin("xShards") == null) {
                return false;
            }
            // Dùng command để check
            return true; // Giả định player có thể mua nếu plugin tồn tại
        } catch (Exception e) {
            return false;
        }
    }

    // Trừ shards của player
    private void removeShards(Player player, double amount) {
        try {
            if (Bukkit.getPluginManager().getPlugin("xShards") != null) {
                String command = "shards take " + player.getName() + " " + (int)amount;
                boolean result = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                getLogger().warning("[LegendaryShop] Shards command - Executed: " + result + ", Command: " + command);
            } else {
                getLogger().warning("[LegendaryShop] xShards plugin not found!");
            }
        } catch (Exception e) {
            getLogger().severe("[LegendaryShop] Error removing shards: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
