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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class trongphu extends JavaPlugin implements Listener, TabCompleter {

    private static Economy econ = null;
    private String shopTitle;
    private int shopSize;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadShopConfig();

        if (!setupEconomy()) {
            getLogger().severe("Khong tim thay Vault! Vo hieu hoa plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("shop").setTabCompleter(this);
        
        getLogger().info("========================================");
        getLogger().info(" LegendaryShop Premium v1.0 - Kich Hoat!");
        getLogger().info("========================================");
    }

    private void loadShopConfig() {
        reloadConfig();
        FileConfiguration config = getConfig();
        shopTitle = ChatColor.translateAlternateColorCodes('&', config.getString("shop-title", "&6Cửa Hàng"));
        shopSize = config.getInt("shop-size", 27);
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("shop")) {

            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("reload")) {
                    if (!sender.hasPermission("legendaryshop.admin")) {
                        sender.sendMessage(ChatColor.RED + "Bạn không có quyền!");
                        return true;
                    }
                    loadShopConfig();
                    sender.sendMessage(ChatColor.GREEN + "[LegendaryShop] Đã tải lại cấu hình config.yml!");
                    return true;
                }

                if (args[0].equalsIgnoreCase("admin")) {
                    if (!sender.hasPermission("legendaryshop.admin")) {
                        sender.sendMessage(ChatColor.RED + "Bạn không có quyền Admin!");
                        return true;
                    }

                    if (args.length < 4) {
                        sender.sendMessage(ChatColor.RED + "Cú pháp Admin:");
                        sender.sendMessage(ChatColor.YELLOW + "/shop admin add [tên] [số_tiền]");
                        sender.sendMessage(ChatColor.YELLOW + "/shop admin remove [tên] [số_tiền]");
                        sender.sendMessage(ChatColor.YELLOW + "/shop admin set [tên] [số_tiền]");
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
                        sender.sendMessage(ChatColor.GREEN + "Đã cộng " + amount + " xu cho " + target.getName());
                    } else if (action.equalsIgnoreCase("remove")) {
                        econ.withdrawPlayer(target, amount);
                        sender.sendMessage(ChatColor.YELLOW + "Đã trừ " + amount + " xu của " + target.getName());
                    } else if (action.equalsIgnoreCase("set")) {
                        double currentBal = econ.getBalance(target);
                        econ.withdrawPlayer(target, currentBal);
                        econ.depositPlayer(target, amount);
                        sender.sendMessage(ChatColor.GREEN + "Đã đặt tiền của " + target.getName() + " thành " + amount + " xu.");
                    }
                    return true;
                }
            }

            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.hasPermission("legendaryshop.use")) {
                    openShop(player);
                } else {
                    player.sendMessage(ChatColor.RED + "Bạn không có quyền mở shop!");
                }
                return true;
            } else {
                sender.sendMessage("Lệnh mở giao diện chỉ dành cho người chơi.");
                return true;
            }
        }
        return false;
    }

    public void openShop(Player player) {
        Inventory inv = Bukkit.createInventory(null, shopSize, shopTitle);
        FileConfiguration config = getConfig();

        if (config.getConfigurationSection("items") == null) return;

        for (String key : config.getConfigurationSection("items").getKeys(false)) {
            String path = "items." + key + ".";
            Material mat = Material.matchMaterial(config.getString(path + "material"));
            int slot = config.getInt(path + "slot");

            if (mat != null && slot >= 0 && slot < shopSize) {
                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', config.getString(path + "display-name")));
                    List<String> lore = new ArrayList<>();
                    for (String s : config.getStringList(path + "lore")) {
                        lore.add(ChatColor.translateAlternateColorCodes('&', s));
                    }
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                inv.setItem(slot, item);
            }
        }
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Kiểm tra tiêu đề menu có khớp không
        if (event.getView().getTitle().equals(shopTitle)) {
            event.setCancelled(true); // Chặn không cho người chơi lấy đồ ra ngoài

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
            
            Player player = (Player) event.getWhoClicked();
            int clickedSlot = event.getSlot(); // Lấy vị trí ô vừa bấm

            FileConfiguration config = getConfig();
            if (config.getConfigurationSection("items") == null) return;

            // Vòng lặp quét qua danh sách vật phẩm trong file cấu hình
            for (String key : config.getConfigurationSection("items").getKeys(false)) {
                String path = "items." + key + ".";
                int targetSlot = config.getInt(path + "slot");

                // Nếu ô bấm trùng khớp hoàn toàn với ô cấu hình
                if (clickedSlot == targetSlot) {
                    Material targetMat = Material.matchMaterial(config.getString(path + "material"));
                    if (targetMat == null) return;

                    double price = config.getDouble(path + "price");

                    // Kiểm tra tiền của người chơi qua hệ thống Vault Economy
                    if (econ.getBalance(player) >= price) {
                        econ.withdrawPlayer(player, price); // Trừ tiền
                        player.getInventory().addItem(new ItemStack(targetMat, 1)); // Phát đồ
                        player.sendMessage(ChatColor.GREEN + "[LegendaryShop] Mua thành công vật phẩm!");
                    } else {
                        player.sendMessage(ChatColor.RED + "[LegendaryShop] Bạn không có đủ tiền để mua vật phẩm này!");
                    }
                    player.closeInventory(); // Đóng giao diện sau khi click
                    return;
                }
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("admin", "reload");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            return Arrays.asList("add", "remove", "set");
        }
        return new ArrayList<>();
    }
}
