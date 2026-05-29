package com.trongphu.shopplugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Arrays;

public final class ShopPlugin extends JavaPlugin implements Listener {

    private final String shopTitle = "§6§lCỬA HÀNG SERVER";

    @Override
    public void onEnable() {
        // Đăng ký sự kiện (Event) cho Plugin
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("ShopPlugin 1.21.4 da duoc kich hoat thanh cong!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("shop")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                openShop(player);
                return true;
            } else {
                sender.sendMessage("Lenh nay chi danh cho Nguoi choi!");
            }
        }
        return false;
    }

    public void openShop(Player player) {
        Inventory shop = Bukkit.createInventory(null, 27, Component.text(shopTitle));

        ItemStack diamond = new ItemStack(Material.DIAMOND);
        ItemMeta diamondMeta = diamond.getItemMeta();
        if (diamondMeta != null) {
            diamondMeta.displayName(Component.text("Kim Cương Quý Giá").color(NamedTextColor.AQUA));
            diamondMeta.lore(Arrays.asList(
                    Component.text("§7Giá mua: §e100 Xu"),
                    Component.text("§aClick để mua 1 viên!")
            ));
            diamond.setItemMeta(diamondMeta);
        }

        shop.setItem(13, diamond);

        player.openInventory(shop);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().title().toString().contains(shopTitle)) {
            event.setCancelled(true); // Không cho người chơi lấy đồ ra khỏi GUI

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

            Player player = (Player) event.getWhoClicked();
            
            if (event.getCurrentItem().getType() == Material.DIAMOND) {
                // Ở đây bạn có thể tích hợp với Vault để trừ tiền của người chơi
                // Đoạn code mẫu giả định người chơi đủ tiền và nhận được đồ:
                player.getInventory().addItem(new ItemStack(Material.DIAMOND, 1));
                player.sendMessage("§a[Shop] Bạn đã mua thành công 1 viên Kim Cương!");
                player.closeInventory();
            }
        }
    }
}