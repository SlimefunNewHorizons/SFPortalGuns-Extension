package ru.officialdakari.portalgunsf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.drakescraft_labs.slimefun4.api.SlimefunAddon;
import com.github.drakescraft_labs.slimefun4.api.items.ItemGroup;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItemStack;
import com.github.drakescraft_labs.slimefun4.api.recipes.RecipeType;
import com.github.drakescraft_labs.slimefun4.implementation.Slimefun;
import com.github.drakescraft_labs.slimefun4.implementation.SlimefunItems;
import com.github.drakescraft_labs.slimefun4.libraries.dough.items.CustomItemStack;
import com.github.drakescraft_labs.slimefun4.libraries.dough.protection.Interaction;

public class PortalGunPlugin extends JavaPlugin implements SlimefunAddon {

    static NamespacedKey itemMetaPortalGunId;
    static NamespacedKey itemMetaPortalGunColor1;
    static NamespacedKey itemMetaPortalGunColor2;
    static NamespacedKey itemMetaPortalGunCharge;
    static NamespacedKey itemMetaPortalGunMode;
    static NamespacedKey itemMetaPortalGunInfinite;
    static NamespacedKey itemMetaPortalGunSelected;
    static NamespacedKey itemMetaPortalGunName;
    static NamespacedKey itemMetaPortalGunTint;
    static HashMap<String, ArrayList<PortalData>> portals = new HashMap<>();
    static HashMap<String, Entity> holding = new HashMap<>();
    static HashMap<String, org.bukkit.block.BlockState> holdingBlock = new HashMap<>();
    private static final Map<UUID, Long> portalCooldowns = new HashMap<>();
    static PortalGunPlugin plugin;

    private static final Random RANDOM = new Random();
    private static final String PORTAL_GUN_ID = "PORTAL_GUN";
    private static final String PORTAL_FLUID_ID = "PORTAL_FLUID";
    private PortalGunMenuListener menuListener;

    /** Fabrica de claves de ubicaciones guardadas; los tests la sustituyen porque no cargan el plugin completo. */
    static java.util.function.IntFunction<NamespacedKey> savedLocationKeyFactory =
        slot -> new NamespacedKey(plugin, "saved_location_" + slot);

    static NamespacedKey savedLocationKey(int slot) {
        return savedLocationKeyFactory.apply(slot);
    }

    static PortalGunMenuListener getMenuListener() {
        return plugin.menuListener;
    }

    static ItemStack findHeldPortalGun(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (isPortalGun(main)) {
            return main;
        }
        ItemStack offhand = player.getInventory().getItemInOffHand();
        return isPortalGun(offhand) ? offhand : null;
    }

    static boolean isPortalGun(ItemStack item) {
        SlimefunItem slimefunItem = SlimefunItem.getByItem(item);
        return slimefunItem != null && PORTAL_GUN_ID.equals(slimefunItem.getId());
    }

    static boolean tryRefillFromOffhand(Player player, ItemStack gun) {
        ItemStack fluid = player.getInventory().getItemInOffHand();
        SlimefunItem slimefunItem = SlimefunItem.getByItem(fluid);
        if (slimefunItem == null || !PORTAL_FLUID_ID.equals(slimefunItem.getId()) || PortalGunState.charge(gun) >= PortalGunState.MAX_CHARGE) {
            return false;
        }

        if (!PortalGunState.isInfinite(gun)) {
            fluid.setAmount(fluid.getAmount() - 1);
        }
        PortalGunState.refill(gun);
        player.playSound(player.getLocation(), Sound.ITEM_BOTTLE_FILL, 1, 1.2F);
        player.sendMessage(ChatColor.GREEN + "Portal Gun recharged.");
        return true;
    }

    static void teleportToSavedLocation(Player player, ItemStack gun) {
        int slot = PortalGunState.selectedSlot(gun);
        Location destination = slot < 0 ? null : PortalGunState.getSavedLocation(gun, slot);
        if (destination == null || destination.getWorld() == null) {
            player.sendMessage(ChatColor.RED + "Select a saved location in the Portal Gun menu.");
            return;
        }
        if (!destination.getBlock().isPassable() || !destination.clone().add(0, 1, 0).getBlock().isPassable()) {
            player.sendMessage(ChatColor.RED + "That saved destination is not currently safe.");
            return;
        }
        if (!PortalGunState.consume(gun, PortalGunState.TRAVEL_COST)) {
            player.sendMessage(ChatColor.RED + "You need more Portal Fluid to travel.");
            return;
        }
        if (!canTravel(player, player.getLocation(), destination)) {
            PortalGunState.setCharge(gun, PortalGunState.charge(gun) + PortalGunState.TRAVEL_COST);
            return;
        }
        player.teleport(destination);
        player.playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
    }

    static String portalColor(ItemStack item, boolean first) {
        NamespacedKey key = first ? itemMetaPortalGunColor1 : itemMetaPortalGunColor2;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String color = pdc.get(key, PersistentDataType.STRING);
        if (color == null) {
            int red = first ? 40 + RANDOM.nextInt(80) : 150 + RANDOM.nextInt(100);
            int green = first ? 120 + RANDOM.nextInt(120) : 20 + RANDOM.nextInt(90);
            int blue = first ? 140 + RANDOM.nextInt(100) : 10 + RANDOM.nextInt(70);
            color = red + ";" + green + ";" + blue;
            pdc.set(key, PersistentDataType.STRING, color);
            item.setItemMeta(meta);
        }
        return color;
    }

    static boolean canTravel(Player player, Location from, Location destination) {
        long now = System.currentTimeMillis();
        long lastUse = portalCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long remaining = 10_000L - (now - lastUse);
        if (remaining > 0) {
            player.sendMessage(ChatColor.RED + "Portal stabilizing: wait " + ((remaining + 999) / 1000) + " seconds.");
            return false;
        }

        if (!Slimefun.getProtectionManager().hasPermission(player, from, Interaction.INTERACT_BLOCK)
            || !Slimefun.getProtectionManager().hasPermission(player, destination, Interaction.INTERACT_BLOCK)) {
            player.sendMessage(ChatColor.RED + "You cannot use a portal across these protected regions.");
            return false;
        }

        portalCooldowns.put(player.getUniqueId(), now);
        return true;
    }

    static boolean isPortalPlaceable(Location loc, BlockFace face) {
        if (portalExists(loc)) {
            return false;
        }
        Location target = loc.clone().add(face.getModX(), face.getModY(), face.getModZ());
        if (!target.getBlock().isPassable()) {
            return false;
        }
        return switch (face) {
            case NORTH, SOUTH, EAST, WEST -> target.clone().add(0, 1, 0).getBlock().isPassable();
            default -> true;
        };
    }

    static boolean portalExists(Location location) {
        for (ArrayList<PortalData> portalPair : portals.values()) {
            for (PortalData portal : portalPair) {
                if (portal.location.getWorld().equals(location.getWorld()) && portal.location.distanceSquared(location) < 4) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onEnable() {
        plugin = this;
        itemMetaPortalGunId = new NamespacedKey(this, "unique_portals_id");
        itemMetaPortalGunColor1 = new NamespacedKey(this, "portal_color_one");
        itemMetaPortalGunColor2 = new NamespacedKey(this, "portal_color_two");
        itemMetaPortalGunCharge = new NamespacedKey(this, "portal_charge");
        itemMetaPortalGunMode = new NamespacedKey(this, "portal_mode");
        itemMetaPortalGunInfinite = new NamespacedKey(this, "infinite_mode");
        itemMetaPortalGunSelected = new NamespacedKey(this, "selected_destination");
        itemMetaPortalGunName = new NamespacedKey(this, "portal_name");
        itemMetaPortalGunTint = new NamespacedKey(this, "portal_tint");

        ItemGroup group = new ItemGroup(new NamespacedKey(this, "portal_guns"), CustomItemStack.create(Material.LIME_DYE,
            "&2&lPortal Guns Extension", "&7Charged interdimensional technology"));

        SlimefunItemStack fluid = new SlimefunItemStack(PORTAL_FLUID_ID, Material.SLIME_BALL, "&aPortal Fluid",
            "&7Refills a Portal Gun to 100 charge.");
        new SlimefunItem(group, fluid, RecipeType.MAGIC_WORKBENCH, new ItemStack[] {
            new ItemStack(Material.SLIME_BALL), SlimefunItems.BATTERY.asOne(), new ItemStack(Material.ENDER_PEARL),
            null, new ItemStack(Material.LIME_DYE), null, null, null, null
        }).register(this);

        SlimefunItemStack portalGun = new SlimefunItemStack(PORTAL_GUN_ID, Material.NETHERITE_HOE, "&aPortal Gun",
            "&7Right click: fire portal", "&7Shift + right click: control panel", "&7Use Portal Fluid in offhand to recharge.");
        new PortalGunItem(group, portalGun, RecipeType.MAGIC_WORKBENCH, new ItemStack[] {
            new ItemStack(Material.BOW), new ItemStack(Material.IRON_BLOCK), new ItemStack(Material.ENDER_PEARL),
            null, SlimefunItems.ELECTRO_MAGNET.asOne(), null, null, null, null
        }).register(this);

        SlimefunItemStack gravityGun = new SlimefunItemStack("GRAVITY_GUN", Material.GOLDEN_HOE, "&bGravity Gun",
            "&7Grab blocks and entities");
        new GravityGunItem(group, gravityGun, RecipeType.MAGIC_WORKBENCH, new ItemStack[] {
            new ItemStack(Material.FISHING_ROD), new ItemStack(Material.IRON_BLOCK), new ItemStack(Material.ENDER_PEARL),
            null, SlimefunItems.ELECTRO_MAGNET.asOne(), null, null, null, null
        }).register(this);

        menuListener = new PortalGunMenuListener(this);
        Bukkit.getPluginManager().registerEvents(new PortalGunEventListener(), this);
        Bukkit.getPluginManager().registerEvents(menuListener, this);
        PortalGunCommand command = new PortalGunCommand();
        getCommand("portalguns").setExecutor(command);
        getCommand("portalguns").setTabCompleter(command);
    }

    static void removePortalPair(String gunId) {
        ArrayList<PortalData> pair = portals.remove(gunId);
        if (pair != null) {
            pair.forEach(PortalData::removeVisuals);
        }
    }

    @Override
    public void onDisable() {
        portals.values().forEach(pair -> pair.forEach(PortalData::removeVisuals));
        portals.clear();
        holding.clear();
        holdingBlock.clear();
    }

    @Override
    public JavaPlugin getJavaPlugin() {
        return this;
    }

    @Override
    public String getBugTrackerURL() {
        return "https://github.com/SlimefunNewHorizons/SFPortalGuns-Extension/issues";
    }
}
