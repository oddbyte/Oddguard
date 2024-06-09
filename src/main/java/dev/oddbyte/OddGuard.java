package dev.oddbyte;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class OddGuard extends JavaPlugin implements Listener, TabCompleter {
    private final Map<UUID, Location> pos1 = new HashMap<>();
    private final Map<UUID, Location> pos2 = new HashMap<>();
    private final Map<String, Region> regions = new HashMap<>();
    private final Map<UUID, Boolean> bypass = new HashMap<>();
    private final List<String> availableFlags = Arrays.asList(
            "block-break", "block-place", "use", "damage-animals", "chest-access",
            "ride", "pvp", "sleep", "respawn-anchors", "tnt", "lighter",
            "block-trampling", "frosted-ice-form", "item-frame-rotation",
            "firework-damage", "use-anvil", "use-dripleaf", "fall-damage"
    );

    @Override
    public void onEnable() {
        Bukkit.getLogger().info("OddGuard has been enabled!");
        getServer().getPluginManager().registerEvents(this, this);
        saveDefaultConfig();
        loadRegions();

        // Add /rg alias for /og
        PluginCommand rgCommand = getCommand("rg");
        if (rgCommand != null) {
            rgCommand.setExecutor(this);
            rgCommand.setTabCompleter(this);
        }

        // Set tab completer for /og
        PluginCommand ogCommand = getCommand("og");
        if (ogCommand != null) {
            ogCommand.setExecutor(this);
            ogCommand.setTabCompleter(this);
        }
    }

    @Override
    public void onDisable() {
        Bukkit.getLogger().info("OddGuard has been disabled!");
        saveRegions();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (!command.getName().equalsIgnoreCase("og") && !command.getName().equalsIgnoreCase("rg")) {
            return false;
        }

        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "wand":
                handleWandCommand(player);
                break;
            case "flag":
                handleFlagCommand(player, args);
                break;
            case "flags":
                handleFlagsCommand(player, args);
                break;
            case "bypass":
                handleBypassCommand(player);
                break;
            case "rename":
                handleRenameCommand(player, args);
                break;
            case "list":
                handleListCommand(player);
                break;
            case "delete":
                handleDeleteCommand(player, args);
                break;
            case "create":
                handleCreateCommand(player, args);
                break;
            case "redefine":
                handleRedefineCommand(player, args);
                break;
            case "tp":
                handleTpCommand(player, args);
                break;
            default:
                player.sendMessage(ChatColor.RED + "Unknown command. Use /og for help.");
                break;
        }
        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "OddGuard Commands:");
        player.sendMessage(ChatColor.YELLOW + "/og wand - Get the OddGuard Wand");
        player.sendMessage(ChatColor.YELLOW + "/og flag <region> <flag> <value> - Set flags for a region (value can be true, false, or unset)");
        player.sendMessage(ChatColor.YELLOW + "/og flags [region] - Open GUI to set flags for a region");
        player.sendMessage(ChatColor.YELLOW + "/og bypass - Toggle bypass mode");
        player.sendMessage(ChatColor.YELLOW + "/og rename <oldName> <newName> - Rename a region");
        player.sendMessage(ChatColor.YELLOW + "/og list - List all regions and their flags");
        player.sendMessage(ChatColor.YELLOW + "/og delete <region> - Delete a region");
        player.sendMessage(ChatColor.YELLOW + "/og create <region> - Create a region");
        player.sendMessage(ChatColor.YELLOW + "/og redefine <region> - Redefine a region with new positions");
        player.sendMessage(ChatColor.YELLOW + "/rg tp <region> - Teleport to the center of a region");
    }

    private void handleWandCommand(Player player) {
        if (!player.hasPermission("oddguard.wand")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

        ItemStack wand = new ItemStack(Material.PAPER);
        ItemMeta meta = wand.getItemMeta();
        meta.setDisplayName("OddGuard Wand");
        meta.setUnbreakable(true);
        wand.setItemMeta(meta);
        player.getInventory().addItem(wand);
        player.sendMessage(ChatColor.GREEN + "You have received the OddGuard Wand!");
    }

    private void handleFlagCommand(Player player, String[] args) {
        if (!player.hasPermission("oddguard.flags")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Usage: /og flag <region> <flag> <value>");
            return;
        }

        String regionName = args[1];
        String flag = args[2];
        String valueStr = args[3].toLowerCase();
        Boolean value = null;
        if (valueStr.equals("true")) {
            value = true;
        } else if (valueStr.equals("false")) {
            value = false;
        } else if (!valueStr.equals("unset")) {
            player.sendMessage(ChatColor.RED + "Invalid value. Use true, false, or unset.");
            return;
        }

        Region region = regions.get(regionName);
        if (region != null) {
            region.setFlag(flag, value);
            player.sendMessage(ChatColor.GREEN + "Flag " + flag + " set to " + valueStr + " for region " + regionName);
        } else {
            player.sendMessage(ChatColor.RED + "Region " + regionName + " not found.");
        }
    }

    private void handleFlagsCommand(Player player, String[] args) {
        if (!player.hasPermission("oddguard.flags")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

        Region region;
        if (args.length < 2) {
            region = getRegionPlayerIsIn(player);
            if (region == null) {
                player.sendMessage(ChatColor.RED + "Usage: /og flags <region> or stand in a region.");
                return;
            }
        } else {
            String regionName = args[1];
            region = regions.get(regionName);
            if (region == null) {
                player.sendMessage(ChatColor.RED + "Region " + regionName + " not found.");
                return;
            }
        }

        openFlagGUI(player, region);
    }

    private void handleBypassCommand(Player player) {
        if (!player.hasPermission("oddguard.bypass")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

        boolean bypassing = bypass.getOrDefault(player.getUniqueId(), false);
        bypass.put(player.getUniqueId(), !bypassing);
        player.sendMessage(ChatColor.GREEN + "Bypass mode " + (bypassing ? "disabled" : "enabled") + ".");
    }

    private void handleRenameCommand(Player player, String[] args) {
        if (!player.hasPermission("oddguard.rename")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /og rename <oldName> <newName>");
            return;
        }

        String oldName = args[1];
        String newName = args[2];
        Region region = regions.get(oldName);
        if (region != null) {
            regions.remove(oldName);
            region.setName(newName);
            regions.put(newName, region);
            player.sendMessage(ChatColor.GREEN + "Region " + oldName + " renamed to " + newName);
        } else {
            player.sendMessage(ChatColor.RED + "Region " + oldName + " not found.");
        }
    }

    private void handleListCommand(Player player) {
        if (!player.hasPermission("oddguard.list")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "Regions:");
        for (Region region : regions.values()) {
            player.sendMessage(region.listFlags());
        }
    }

    private void handleDeleteCommand(Player player, String[] args) {
        if (!player.hasPermission("oddguard.delete")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /og delete <region>");
            return;
        }

        String regionName = args[1];
        Region region = regions.get(regionName);
        if (region != null) {
            regions.remove(regionName);
            player.sendMessage(ChatColor.GREEN + "Region " + regionName + " deleted.");
        } else {
            player.sendMessage(ChatColor.RED + "Region " + regionName + " not found.");
        }
    }

    private void handleCreateCommand(Player player, String[] args) {
        if (!player.hasPermission("oddguard.create")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /og create <region>");
            return;
        }

        String regionName = args[1];
        Location location1 = pos1.get(player.getUniqueId());
        Location location2 = pos2.get(player.getUniqueId());
        if (location1 != null && location2 != null) {
            if (regions.containsKey(regionName)) {
                player.sendMessage(ChatColor.RED + "Region with that name already exists.");
                return;
            }
            regions.put(regionName, new Region(regionName, location1, location2));
            player.sendMessage(ChatColor.GREEN + "Region created as " + regionName);
        } else {
            player.sendMessage(ChatColor.RED + "You need to set both positions before creating a region.");
        }
    }

    private void handleRedefineCommand(Player player, String[] args) {
        if (!player.hasPermission("oddguard.redefine")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /og redefine <region>");
            return;
        }

        String regionName = args[1];
        Region region = regions.get(regionName);
        if (region != null) {
            Location location1 = pos1.get(player.getUniqueId());
            Location location2 = pos2.get(player.getUniqueId());
            if (location1 != null && location2 != null) {
                region.setPos1(location1);
                region.setPos2(location2);
                player.sendMessage(ChatColor.GREEN + "Region " + regionName + " redefined.");
            } else {
                player.sendMessage(ChatColor.RED + "You need to set both positions before redefining a region.");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Region " + regionName + " not found.");
        }
    }

    private void handleTpCommand(Player player, String[] args) {
        if (!player.hasPermission("oddguard.tp")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /rg tp <region>");
            return;
        }

        String regionName = args[1];
        Region region = regions.get(regionName);
        if (region != null) {
            Location center = region.getCenter();
            if (center != null) {
                player.teleport(center);
                player.sendMessage(ChatColor.GREEN + "Teleported to the center of region " + regionName);
            } else {
                player.sendMessage(ChatColor.RED + "Could not calculate the center of the region.");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Region " + regionName + " not found.");
        }
    }

    private void openFlagGUI(Player player, Region region) {
        Inventory gui = Bukkit.createInventory(null, 27, "Edit Flags: " + region.getName());

        for (int i = 0; i < availableFlags.size(); i++) {
            String flagName = availableFlags.get(i);
            gui.setItem(i, createFlagItem(flagName, region.getFlag(flagName), getMaterialForFlag(flagName)));
        }

        player.openInventory(gui);
    }

    private Material getMaterialForFlag(String flagName) {
        switch (flagName) {
            case "block-break": return Material.DIAMOND_PICKAXE;
            case "block-place": return Material.DIRT;
            case "use": return Material.OAK_DOOR;
            case "damage-animals": return Material.COW_SPAWN_EGG;
            case "chest-access": return Material.CHEST;
            case "ride": return Material.SADDLE;
            case "pvp": return Material.DIAMOND_SWORD;
            case "sleep": return Material.RED_BED;
            case "respawn-anchors": return Material.RESPAWN_ANCHOR;
            case "tnt": return Material.TNT;
            case "lighter": return Material.FLINT_AND_STEEL;
            case "block-trampling": return Material.FARMLAND;
            case "frosted-ice-form": return Material.FROSTED_ICE;
            case "item-frame-rotation": return Material.ITEM_FRAME;
            case "firework-damage": return Material.FIREWORK_ROCKET;
            case "use-anvil": return Material.ANVIL;
            case "use-dripleaf": return Material.SMALL_DRIPLEAF;
            case "fall-damage": return Material.FEATHER;
            default: return Material.BARRIER;
        }
    }

    private ItemStack createFlagItem(String flagName, Boolean value, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        String valueStr = (value == null) ? "unset" : value.toString();
        meta.setDisplayName(flagName.substring(0, 1).toUpperCase() + flagName.substring(1) + ": " + valueStr);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().startsWith("Edit Flags: ")) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            String regionName = event.getView().getTitle().split(": ")[1];
            Region region = regions.get(regionName);
            if (region == null) return;

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta()) return;

            String flagName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName().split(": ")[0].toLowerCase());
            Boolean currentValue = region.getFlag(flagName);

            if (event.isLeftClick()) {
                region.setFlag(flagName, currentValue == null || !currentValue);
            } else if (event.isRightClick()) {
                region.setFlag(flagName, null);
            }

            ItemMeta meta = clickedItem.getItemMeta();
            String valueStr = (region.getFlag(flagName) == null) ? "unset" : region.getFlag(flagName).toString();
            meta.setDisplayName(flagName.substring(0, 1).toUpperCase() + flagName.substring(1) + ": " + valueStr);
            clickedItem.setItemMeta(meta);
            player.updateInventory();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("oddguard.usewand") && player.getInventory().getItemInMainHand().getType() == Material.PAPER) {
            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                pos1.put(player.getUniqueId(), event.getClickedBlock().getLocation());
                player.sendMessage(ChatColor.GREEN + "Position 1 set.");
                event.setCancelled(true);
            } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                pos2.put(player.getUniqueId(), event.getClickedBlock().getLocation());
                player.sendMessage(ChatColor.GREEN + "Position 2 set.");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!checkBypass(event.getPlayer()) && !canPerformAction(event.getPlayer(), event.getBlock().getLocation(), "block-break")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You do not have permission to break blocks here.");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!checkBypass(event.getPlayer()) && !canPerformAction(event.getPlayer(), event.getBlock().getLocation(), "block-place")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You do not have permission to place blocks here.");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            Player damaged = (Player) event.getEntity();
            Player damager = (Player) event.getDamager();
            if (!checkBypass(damager) && !canPerformAction(damager, damaged.getLocation(), "pvp")) {
                event.setCancelled(true);
                damager.sendMessage(ChatColor.RED + "You do not have permission to engage in PvP here.");
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractEvent(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!checkBypass(player) && !canPerformAction(player, player.getLocation(), "interact")) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You do not have permission to interact here.");
        }

        // Handle the "use-anvil" flag
        if (!checkBypass(player) && event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getType() == Material.ANVIL) {
            if (!canPerformAction(player, event.getClickedBlock().getLocation(), "use-anvil")) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You do not have permission to use anvils here.");
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        if (!checkBypass(player) && (!canPerformAction(player, player.getLocation(), "interact") || !canPerformAction(player, player.getLocation(), "block-place"))) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You do not have permission to edit signs here.");
        }
    }

    private boolean checkBypass(Player player) {
        return bypass.getOrDefault(player.getUniqueId(), false);
    }

    private boolean canPerformAction(Player player, Location location, String action) {
        boolean allow = false;
        boolean deny = false;

        for (Region region : regions.values()) {
            if (region.isInRegion(location)) {
                Boolean flag = region.getFlag(action);
                if (flag != null) {
                    if (flag) allow = true;
                    else deny = true;
                }
            }
        }

        if (allow) return true;
        return !deny;
    }

    private Region getRegionPlayerIsIn(Player player) {
        Location loc = player.getLocation();
        for (Region region : regions.values()) {
            if (region.isInRegion(loc)) {
                return region;
            }
        }
        return null;
    }

    private void loadRegions() {
        FileConfiguration config = getConfig();
        if (config.contains("regions")) {
            for (String key : config.getConfigurationSection("regions").getKeys(false)) {
                Location pos1 = (Location) config.get("regions." + key + ".pos1");
                Location pos2 = (Location) config.get("regions." + key + ".pos2");
                Region region = new Region(key, pos1, pos2);
                if (config.contains("regions." + key + ".flags")) {
                    for (String flag : config.getConfigurationSection("regions." + key + ".flags").getKeys(false)) {
                        region.setFlag(flag, config.getBoolean("regions." + key + ".flags." + flag));
                    }
                }
                regions.put(key, region);
            }
        }
    }

    private void saveRegions() {
        FileConfiguration config = getConfig();
        config.set("regions", null); // Clear old regions

        for (Map.Entry<String, Region> entry : regions.entrySet()) {
            String key = entry.getKey();
            Region region = entry.getValue();
            config.set("regions." + key + ".pos1", region.getPos1());
            config.set("regions." + key + ".pos2", region.getPos2());
            for (Map.Entry<String, Boolean> flagEntry : region.getFlags().entrySet()) {
                config.set("regions." + key + ".flags." + flagEntry.getKey(), flagEntry.getValue());
            }
        }
        saveConfig();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("og") || command.getName().equalsIgnoreCase("rg")) {
            if (args.length == 1) {
                return Arrays.asList("wand", "flag", "flags", "bypass", "rename", "list", "delete", "create", "redefine", "tp");
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("flag") || args[0].equalsIgnoreCase("flags") || args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("redefine") || args[0].equalsIgnoreCase("tp")) {
                    return new ArrayList<>(regions.keySet());
                }
            } else if (args.length == 3 && args[0].equalsIgnoreCase("flag")) {
                return availableFlags;
            } else if (args.length == 4 && args[0].equalsIgnoreCase("flag")) {
                return Arrays.asList("true", "false", "unset");
            }
        }
        return Collections.emptyList();
    }
}
