# LegendaryShop
🛍️ LegendaryShop - Professional Economy System

LegendaryShop Premium is a comprehensive economy solution for Minecraft servers, providing secure transactions, flexible payment methods, and powerful admin controls for building stable server economies.

✨ Key Features FeatureDescription💰 Dual CurrencyVault Economy & xShards support🛒 Multiple CategoriesOrganize products by type📦 Diverse ItemsRegular items & Mob Spawners🔐 Secure CheckoutBalance verification BEFORE purchase⚙️ Admin ToolsPowerful commands & configuration📊 Transaction LogsComplete purchase history🎨 Beautiful UIIntuitive menu system⏱️ Timestamp TrackingTrack all transactions

🎮 Commands === "Player Commands" /shop # Open main shop === "Admin Commands" /shop admin add <player> <amount> # Add money to player /shop admin remove <player> <amount> # Remove money from player /shop admin set <player> <amount> # Set exact amount /shop reload # Reload configuration

🔐 Permission Setup (LuckPerms) === "Add to Staff Group" bash /lp group staff permission set legendaryshop.use true /lp group staff permission set legendaryshop.admin true === "Individual Player" bash /lp user <player> permission set legendaryshop.use true

📋 Permissions List legendaryshop.use # Access the shop legendaryshop.admin # All admin commands

🛡️ Security Features

[!IMPORTANT] Secure Transactions - Balance is verified BEFORE any purchase! Players cannot buy without sufficient funds!

Player clicks item → System checks balance
Insufficient funds → Purchase blocked + Warning
Sufficient funds → Money/Shards deducted
Transaction logged → Admin audit trail No exploits. No cheating. Complete control!
🔧 Features Breakdown

✅ Auto Config Generation - Creates files on first run ✅ Multiple Payment Methods - Vault & Shards ✅ Mob Spawner Sales - SmartSpawner integration ✅ Category Support - Organize by product type ✅ Admin Notifications - Track transactions ✅ Detailed Logging - Complete purchase history ✅ Color Formatting - Professional messages ✅ Custom Pricing - Set price per item ✅ Flexible Config - Easy YAML customization ✅ Real-time Updates - Reload without restart

📥 Installation

Download LegendaryShop.jar Place in plugins/ folder Restart server Edit plugins/LegendaryShop/config.yml Run /shop reload

📊 Configuration Example === "Main Menu" yaml main-menu: title: "&6🛍️ Main Shop" size: 27 items: 0: material: DIAMOND name: "&b💎 Gems Shop" category: gems === "Category Setup" yaml categories: gems: title: "&6💎 Gem Store" size: 27 items: 0: material: DIAMOND name: "&bDiamond" price: 1000 use-shards: false === "Spawner Shop" yaml spawners: title: "&6🦴 Spawner Store" items: 0: material: SPAWNER name: "&cZombie Spawner" price: 50 use-shards: true mob-type: "zombie"

📝 Example Usage Player runs: /shop → Opens beautiful shop menu → Clicks category → Views items with prices → Clicks to buy → Money/Shards deducted → Item received → Transaction logged

🌟 Why LegendaryShop?

[!SUCCESS] Complete Economy Control Without Complexity

✅ Zero Exploitation - Strict payment validation ✅ Easy Setup - Auto-configuration on first run ✅ Flexible Payments - Vault & Shards support ✅ Professional UI - Beautiful menu system ✅ Complete Logging - Audit all transactions ✅ Paper 1.21.4+ Compatible ✅ No Performance Impact - Optimized code

⚙️ Requirements

✅ Vault - Economy system ✅ PlaceholderAPI - Dynamic placeholders ⚠️ xShards - Shard currency (optional) ⚠️ SmartSpawner - Mob spawner sales (optional)
