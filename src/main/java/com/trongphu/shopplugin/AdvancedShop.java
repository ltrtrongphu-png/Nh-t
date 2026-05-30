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
        // Khởi tạo file cấu hình config.yml mặc định
        saveDefaultConfig();
        loadShopConfig();

        // Kết nối với hệ thống tiền tệ Vault
        if (!setupEconomy()) {
            getLogger().severe("Khong tim thay plugin Vault! Vo hieu hoa plugin Shop.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("AdvancedShop v2.0 (1.21.4) da kich hoat!");
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
        if (command.getName().equalsIgnoreCase("shop") && sender instanceof Player) {
            openShop((Player) sender);
            return true;
        }
        if (command.getName().equalsIgnoreCase("shopreload") && sender.hasPermission("advancedshop.admin")) {
            loadShopConfig();
            sender.sendMessage(ChatColor.GREEN + "[Shop] Đã tải lại file config.yml thành công!");
            return true;
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
            event.setCancelled(true); // Chống lấy đồ ra khỏi GUI (Anti-Dupe)

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
            Player player = (Player) event.getWhoClicked();
            Material clickedMat = event.getCurrentItem().getType();

            FileConfiguration config = getConfig();
            for (String key : config.getConfigurationSection("items").getKeys(false)) {
                String path = "items." + key + ".";
                Material targetMat = Material.matchMaterial(config.getString(path + "material"));
                double price = config.getDouble(path + "price");

                if (clickedMat == targetMat) {
                    // Kiểm tra và trừ tiền qua Vault
                    if (econ.getBalance(player) >= price) {
                        econ.withdrawPlayer(player, price);
                        player.getInventory().addItem(new ItemStack(targetMat, 1));
                        player.sendMessage(ChatColor.GREEN + "[Shop] Bạn đã mua thành công 1 " + event.getCurrentItem().getItemMeta().getDisplayName() + ChatColor.GREEN + " với giá " + price + " xu!");
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
