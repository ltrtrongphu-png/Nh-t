package com.trongphu.shopplugin;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
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
import java.util.List;

public final class AdvancedShop extends JavaPlugin implements Listener {

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
        getLogger().info("AdvancedShop v2.5 (Admin Update) da san sang!");
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
            
            // XỬ LÝ LỆNH CHO ADMIN (Nếu gõ từ 1 tham số trở lên, ví dụ: /shop reload hoặc /shop admin...)
            if (args.length > 0) {
                
                // Lệnh phụ 1: /shop reload
                if (args[0].equalsIgnoreCase("reload")) {
                    if (!sender.hasPermission("advancedshop.admin")) {
                        sender.sendMessage(ChatColor.RED + "Bạn không có quyền thực hiện lệnh này!");
                        return true;
                    }
                    loadShopConfig();
                    sender.sendMessage(ChatColor.GREEN + "[Shop] Đã tải lại cấu hình config.yml!");
                    return true;
                }

                // Lệnh phụ 2: /shop admin ...
                if (args[0].equalsIgnoreCase("admin")) {
                    if (!sender.hasPermission("advancedshop.admin")) {
                        sender.sendMessage(ChatColor.RED + "Bạn không có quyền Admin của Shop!");
                        return true;
                    }

                    // Hướng dẫn bảng lệnh Admin khi gõ sai cú pháp
                    if (args.length < 4) {
                        sender.sendMessage(ChatColor.RED + "Cách dùng lệnh Admin:");
                        sender.sendMessage(ChatColor.YELLOW + "/shop admin add [tên_người_chơi] [số_tiền] §7- Cộng tiền");
                        sender.sendMessage(ChatColor.YELLOW + "/shop admin remove [tên_người_chơi] [số_tiền] §7- Trừ tiền");
                        sender.sendMessage(ChatColor.YELLOW + "/shop admin set [tên_người_chơi] [số_tiền] §7- Đặt lại tiền");
                        return true;
                    }

                    String action = args[1];
                    Player target = Bukkit.getPlayer(args[2]);
                    
                    if (target == null) {
                        sender.sendMessage(ChatColor.RED + "Không tìm thấy người chơi này đang online!");
                        return true;
                    }

                    double amount;
                    try {
                        amount = Double.parseDouble(args[3]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Số tiền nhập vào phải là một con số hợp lệ!");
                        return true;
                    }

                    // Thực hiện các hành động quản lý tiền tệ của Admin
                    if (action.equalsIgnoreCase("add")) {
                        econ.depositPlayer(target, amount);
                        sender.sendMessage(ChatColor.GREEN + "Đã cộng " + amount + " xu vào tài khoản của " + target.getName());
                        target.sendMessage(ChatColor.GREEN + "Bạn được Admin cộng " + amount + " xu vào tài khoản.");
                    } else if (action.equalsIgnoreCase("remove")) {
                        econ.withdrawPlayer(target, amount);
                        sender.sendMessage(ChatColor.YELLOW + "Đã trừ " + amount + " xu từ tài khoản của " + target.getName());
                        target.sendMessage(ChatColor.RED + "Bạn đã bị Admin trừ " + amount + " xu.");
                    } else if (action.equalsIgnoreCase("set")) {
                        double currentBal = econ.getBalance(target);
                        econ.withdrawPlayer(target, currentBal); // Đưa về 0
                        econ.depositPlayer(target, amount);     // Đặt mức tiền mới
                        sender.sendMessage(ChatColor.GREEN + "Đã đặt lại số dư của " + target.getName() + " thành " + amount + " xu.");
                        target.sendMessage(ChatColor.GOLD + "Số dư tài khoản của bạn đã được Admin đặt lại thành " + amount + " xu.");
                    } else {
                        sender.sendMessage(ChatColor.RED + "Hành động không hợp lệ! Chỉ dùng: add, remove, hoặc set.");
                    }
                    return true;
                }
            }

            // XỬ LÝ LỆNH CHO NGƯỜI CHƠI THƯỜNG (Nếu chỉ gõ trơ trọi lệnh /shop)
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.hasPermission("advancedshop.use")) {
                    openShop(player);
                } else {
                    player.sendMessage(ChatColor.RED + "Bạn không có quyền mở cửa hàng ảo!");
                }
                return true;
            } else {
                sender.sendMessage("Lệnh mở giao diện chỉ áp dụng cho người chơi thực tế!");
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
        if (event.getView().getTitle().equals(shopTitle)) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
            Player player = (Player) event.getWhoClicked();
            Material clickedMat = event.getCurrentItem().getType();

            FileConfiguration config = getConfig();
            for (String key : config.getConfigurationSection("items").getKeys(false)) {
                String path = "items." + key + ".";
                Material targetMat = Material.matchMaterial(config.getString(path + "material"));
                double price = config.getDouble(path + "price");

                if (clickedMat == targetMat) {
                    if (econ.getBalance(player) >= price) {
                        econ.withdrawPlayer(player, price);
                        player.getInventory().addItem(new ItemStack(targetMat, 1));
                        player.sendMessage(ChatColor.GREEN + "[Shop] Bạn đã mua thành công 1 viên " + event.getCurrentItem().getItemMeta().getDisplayName() + ChatColor.GREEN + " với giá " + price + " xu!");
                    } else {
                        player.sendMessage(ChatColor.RED + "[Shop] Bạn không có đủ tiền! Cần thêm: " + (price - econ.getBalance(player)) + " xu.");
                    }
                    player.closeInventory();
                    return;
                }
            }
        }
    }
}
