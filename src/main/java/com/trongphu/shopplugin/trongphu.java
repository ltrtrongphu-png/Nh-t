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
    private static final String SELL_GUI_TITLE = ChatColor.DARK_GREEN + "💰 Bán Đồ - Đặt Vào Đây";

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
        getCommand("sell").setExecutor(this);
        getCommand("sell").setTabCompleter(this);

        getLogger().info("========================================");
        getLogger().info(" 🛍️  LegendaryShop Premium v2.8 - Ready!");
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

        // ─────────────── /sell ───────────────
        if (command.getName().equalsIgnoreCase("sell")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Lệnh này chỉ dành cho người chơi!");
                return true;
            }
            Player player = (Player) sender;

            if (!player.hasPermission("legendaryshop.sell")) {
                player.sendMessage(PREFIX + ChatColor.RED + "Bạn không có quyền bán đồ!");
                return true;
            }

            if (args.length > 0 && args[0].equalsIgnoreCase("hand")) {
                sellHandItem(player);
                return true;
            }

            if (args.length > 0 && args[0].equalsIgnoreCase("all")) {
                sellAllItems(player);
                return true;
            }

            openSellGui(player);
            return true;
        }

        // ─────────────── /shop ───────────────
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

    private void sellHandItem(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (hand == null || hand.getType() == Material.AIR) {
            player.sendMessage(PREFIX + ChatColor.RED + "Bạn không cầm gì trong tay!");
            return;
        }

        double pricePerUnit = getSellPrice(hand.getType());
        if (pricePerUnit <= 0) {
            player.sendMessage(PREFIX + ChatColor.RED + "❌ Item này không thể bán!");
            return;
        }

        int qty = hand.getAmount();
        double totalEarned = pricePerUnit * qty;

        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        econ.depositPlayer(player, totalEarned);

        player.sendMessage(PREFIX + ChatColor.GREEN + "✓ Đã bán " + ChatColor.WHITE + qty + "x "
                + formatMaterial(hand.getType()) + ChatColor.GREEN + " với giá "
                + ChatColor.YELLOW + (int) totalEarned + " 💲");
    }

    private void sellAllItems(Player player) {
        double totalEarned = 0;
        int totalItems = 0;

        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR) continue;

            double pricePerUnit = getSellPrice(item.getType());
            if (pricePerUnit <= 0) continue;

            totalEarned += pricePerUnit * item.getAmount();
            totalItems += item.getAmount();
            contents[i] = new ItemStack(Material.AIR);
        }

        if (totalItems == 0) {
            player.sendMessage(PREFIX + ChatColor.RED + "❌ Không có item nào có thể bán!");
            return;
        }

        player.getInventory().setContents(contents);
        econ.depositPlayer(player, totalEarned);

        player.sendMessage(PREFIX + ChatColor.GREEN + "✓ Đã bán " + ChatColor.WHITE + totalItems
                + " item" + ChatColor.GREEN + " và nhận được "
                + ChatColor.YELLOW + (int) totalEarned + " 💲");
    }

    private void openSellGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, SELL_GUI_TITLE);

        ItemStack filler = makeFiller();
        for (int i = 27; i < 36; i++) {
            inv.setItem(i, filler);
        }

        ItemStack confirmBtn = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta cm = confirmBtn.getItemMeta();
        if (cm != null) {
            cm.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "✔ XÁC NHẬN BÁN");
            cm.setLore(Arrays.asList(
                    ChatColor.GRAY + "Đặt đồ vào các ô phía trên",
                    ChatColor.GRAY + "rồi nhấn nút này để bán."
            ));
            confirmBtn.setItemMeta(cm);
        }
        inv.setItem(31, confirmBtn);

        player.openInventory(inv);
        player.sendMessage(PREFIX + ChatColor.YELLOW + "Đặt đồ vào ô trống rồi nhấn nút xanh để bán.");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        FileConfiguration config = getConfig();

        // ── Xử lý SELL GUI ──
        if (title.equals(SELL_GUI_TITLE)) {
            int slot = event.getSlot();

            if (slot >= 27 && slot <= 35) {
                event.setCancelled(true);

                if (slot == 31) {
                    processSellGui(player, event.getView().getTopInventory());
                }
            }
            return;
        }

        // ── Xử lý SHOP Main Menu ──
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

        // ── Xử lý SHOP Category ──
        if (config.contains("categories")) {
            for (String categoryKey : config.getConfigurationSection("categories").getKeys(false)) {
                String categoryPath = "categories." + categoryKey + ".";
                String categoryTitle = ChatColor.translateAlternateColorCodes('&',
                        config.getString(categoryPath + "title", "&6" + categoryKey));

                if (!title.equals(categoryTitle)) continue;

                event.setCancelled(true);

                if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                    return;
                }

                int slot = event.getSlot();
                if (!config.contains(categoryPath + "items." + slot)) return;

                String itemPath = categoryPath + "items." + slot + ".";
                String materialName = config.getString(itemPath + "material");
                double price = config.getDouble(itemPath + "price", 0);
                String mobType = config.getString(itemPath + "mob-type");
                boolean useShards = config.getBoolean(itemPath + "use-shards", false);

                Material mat = Material.matchMaterial(materialName);
                if (mat == null) return;

                if (useShards) {
                    double playerShards = getPlayerShards(player);
                    if (playerShards < price) {
                        player.sendMessage(PREFIX + ChatColor.RED + "❌ Không đủ Shard!");
                        player.sendMessage(PREFIX + ChatColor.YELLOW + "Bạn có: " + ChatColor.AQUA + (int) playerShards);
                        player.sendMessage(PREFIX + ChatColor.YELLOW + "Cần: " + ChatColor.AQUA + (int) price);
                        player.closeInventory();
                        return;
                    }
                } else {
                    if (econ.getBalance(player) < price) {
                        player.sendMessage(PREFIX + ChatColor.RED + "❌ Không đủ tiền!");
                        player.sendMessage(PREFIX + ChatColor.YELLOW + "Bạn có: " + ChatColor.AQUA + (int) econ.getBalance(player) + " 💲");
                        player.sendMessage(PREFIX + ChatColor.YELLOW + "Cần: " + ChatColor.AQUA + (int) price + " 💲");
                        player.closeInventory();
                        return;
                    }
                }

                if (useShards) {
                    removeShards(player, price);
                } else {
                    econ.withdrawPlayer(player, price);
                }

                if (mobType != null && !mobType.isEmpty()) {
                    Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
                        String cmd = "ss give spawner " + player.getName() + " " + mobType + " 1";
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                        getLogger().info("[LegendaryShop] " + player.getName() + " mua spawner: " + mobType);
                    }, 1L);
                    player.sendMessage(PREFIX + ChatColor.GREEN + "✓ Mua thành công! Đã nhận spawner!");
                } else {
                    player.getInventory().addItem(new ItemStack(mat, 1));
                    String currencySymbol = useShards ? "💎" : "💲";
                    player.sendMessage(PREFIX + ChatColor.GREEN + "✓ Mua thành công! Đã trừ " + (int) price + " " + currencySymbol);
                }

                player.closeInventory();
                return;
            }
        }
    }

    private void processSellGui(Player player, Inventory sellInv) {
        double totalEarned = 0;
        int totalSold = 0;
        List<ItemStack> unsellable = new ArrayList<>();

        for (int i = 0; i < 27; i++) {
            ItemStack item = sellInv.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;

            double pricePerUnit = getSellPrice(item.getType());
            if (pricePerUnit > 0) {
                totalEarned += pricePerUnit * item.getAmount();
                totalSold += item.getAmount();
                sellInv.setItem(i, null);
            } else {
                unsellable.add(item.clone());
                sellInv.setItem(i, null);
            }
        }

        player.closeInventory();

        for (ItemStack leftover : unsellable) {
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(leftover);
            for (ItemStack drop : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }

        if (totalSold == 0) {
            player.sendMessage(PREFIX + ChatColor.RED + "❌ Không có item nào có thể bán!");
            return;
        }

        econ.depositPlayer(player, totalEarned);
        player.sendMessage(PREFIX + ChatColor.GREEN + "✓ Đã bán " + ChatColor.WHITE + totalSold
                + " item" + ChatColor.GREEN + " và nhận được "
                + ChatColor.YELLOW + (int) totalEarned + " 💲");

        if (!unsellable.isEmpty()) {
            player.sendMessage(PREFIX + ChatColor.YELLOW + "⚠ " + unsellable.size()
                    + " loại item không thể bán và đã được trả về.");
        }
    }

    private double getSellPrice(Material material) {
        FileConfiguration config = getConfig();
        String key = "sell-prices." + material.name();
        if (config.contains(key)) {
            return config.getDouble(key, 0);
        }
        return 0;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("sell")) {
            if (args.length == 1) {
                return Arrays.asList("hand", "all");
            }
            return new ArrayList<>();
        }

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
                    if (slot >= 0 && slot < size) inv.setItem(slot, item);
                } catch (NumberFormatException ignored) {}
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
                        lore.add(ChatColor.GREEN + "Giá: " + (int) price + " " + symbol);
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                    }
                    if (slot >= 0 && slot < size) inv.setItem(slot, item);
                } catch (NumberFormatException ignored) {}
            }
        }
        player.openInventory(inv);
    }

    private ItemStack makeFiller() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            glass.setItemMeta(meta);
        }
        return glass;
    }

    private String formatMaterial(Material mat) {
        String raw = mat.name().replace("_", " ");
        StringBuilder sb = new StringBuilder();
        for (String word : raw.split(" ")) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }

    private double getPlayerShards(Player player) {
        try {
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null ||
                    Bukkit.getPluginManager().getPlugin("xShards") == null) {
                return 0;
            }
            String shardStr = PlaceholderAPI.setPlaceholders(player, "%xshards_balance%");
            if (shardStr == null || shardStr.isEmpty() || shardStr.equals("%xshards_balance%")) return 0;
            return Double.parseDouble(shardStr);
        } catch (Exception e) {
            getLogger().severe("[LegendaryShop] Lỗi lấy Shard: " + e.getMessage());
            return 0;
        }
    }

    private void removeShards(Player player, double amount) {
        try {
            if (Bukkit.getPluginManager().getPlugin("xShards") == null) {
                getLogger().severe("[LegendaryShop] xShards plugin không tìm thấy!");
                return;
            }
            String cmd = "shards take " + player.getName() + " " + (int) amount;
            boolean result = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            getLogger().info("[LegendaryShop] Trừ Shard - Player: " + player.getName()
                    + ", Amount: " + (int) amount + ", Success: " + result);
        } catch (Exception e) {
            getLogger().severe("[LegendaryShop] Lỗi trừ Shard: " + e.getMessage());
        }
    }
}
