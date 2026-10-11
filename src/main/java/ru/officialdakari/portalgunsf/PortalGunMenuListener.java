package ru.officialdakari.portalgunsf;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import com.github.drakescraft_labs.slimefun4.libraries.dough.items.CustomItemStack;

final class PortalGunMenuListener implements Listener {

    private static final String TITLE = ChatColor.DARK_GREEN + "Portal Gun Control";

    private final PortalGunPlugin plugin;

    PortalGunMenuListener(PortalGunPlugin plugin) {
        this.plugin = plugin;
    }

    void open(Player player, ItemStack gun) {
        PortalGunMenu holder = new PortalGunMenu(player.getUniqueId());
        Inventory inventory = plugin.getServer().createInventory(holder, 27, TITLE);
        holder.setInventory(inventory);
        render(inventory, player, gun);
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof PortalGunMenu menu)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || !menu.owner().equals(player.getUniqueId()) || event.getRawSlot() >= event.getInventory().getSize()) {
            return;
        }

        ItemStack gun = PortalGunPlugin.findHeldPortalGun(player);
        if (gun == null) {
            player.closeInventory();
            player.sendMessage(ChatColor.RED + "Hold your Portal Gun to configure it.");
            return;
        }

        int slot = event.getRawSlot();
        if (slot == 11) {
            PortalGunState.toggleMode(gun);
            player.sendMessage(ChatColor.GREEN + "Portal mode: " + PortalGunState.mode(gun));
        } else if (slot == 12) {
            PortalGunState.cyclePortalTint(gun);
            player.sendMessage(ChatColor.GREEN + "Portal color: " + PortalGunState.portalTint(gun));
        } else if (slot == 13) {
            if (!player.hasPermission("sfportalguns.infinite")) {
                player.sendMessage(ChatColor.RED + "Infinite mode is staff-only.");
            } else {
                PortalGunState.setInfinite(gun, !PortalGunState.isInfinite(gun));
                player.sendMessage(ChatColor.GOLD + "Infinite mode: " + PortalGunState.isInfinite(gun));
            }
        } else if (slot == 15) {
            if (!PortalGunState.saveLocation(gun, player.getLocation())) {
                player.sendMessage(ChatColor.RED + "All eight saved-location slots are full.");
            } else {
                player.sendMessage(ChatColor.GREEN + "Current location saved.");
            }
        } else if (slot >= 18 && slot < 26) {
            int savedSlot = slot - 18;
            Location saved = PortalGunState.getSavedLocation(gun, savedSlot);
            if (saved == null) {
                player.sendMessage(ChatColor.RED + "That location slot is empty.");
            } else if (event.isRightClick()) {
                PortalGunState.deleteLocation(gun, savedSlot);
                player.sendMessage(ChatColor.YELLOW + "Saved location removed.");
            } else {
                PortalGunState.setSelectedSlot(gun, savedSlot);
                player.sendMessage(ChatColor.AQUA + "Selected destination " + (savedSlot + 1) + ".");
            }
        }

        render(event.getInventory(), player, gun);
    }

    private void render(Inventory inventory, Player player, ItemStack gun) {
        inventory.clear();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        }

        int charge = PortalGunState.charge(gun);
        inventory.setItem(10, new CustomItemStack(Material.LIME_DYE, ChatColor.GREEN + "Portal charge: " + charge + "/" + PortalGunState.MAX_CHARGE,
            ChatColor.GRAY + "Refill by holding Portal Fluid in your offhand."));
        inventory.setItem(11, new CustomItemStack(Material.ENDER_EYE, ChatColor.AQUA + "Mode: " + PortalGunState.mode(gun),
            ChatColor.GRAY + "Click to switch between linked portals and saved destinations."));
        inventory.setItem(12, new CustomItemStack(Material.valueOf(PortalGunState.portalTint(gun) + "_DYE"), ChatColor.LIGHT_PURPLE + "Portal color: " + PortalGunState.portalTint(gun),
            ChatColor.GRAY + "Click to cycle its display color."));
        inventory.setItem(13, new CustomItemStack(Material.NETHER_STAR, ChatColor.GOLD + "Infinite mode: " + PortalGunState.isInfinite(gun),
            player.hasPermission("sfportalguns.infinite") ? ChatColor.GRAY + "Click to toggle staff mode." : ChatColor.RED + "Staff permission required."));
        inventory.setItem(14, new CustomItemStack(Material.NAME_TAG, ChatColor.YELLOW + "Portal name: " + PortalGunState.portalName(gun),
            ChatColor.GRAY + "Use /pg name <name> to change it."));
        inventory.setItem(15, new CustomItemStack(Material.COMPASS, ChatColor.YELLOW + "Save current location",
            ChatColor.GRAY + "Saves this position into the next free slot."));

        int selected = PortalGunState.selectedSlot(gun);
        for (int slot = 0; slot < PortalGunState.MAX_SAVED_LOCATIONS; slot++) {
            Location saved = PortalGunState.getSavedLocation(gun, slot);
            if (saved == null) {
                inventory.setItem(18 + slot, new CustomItemStack(Material.BLACK_STAINED_GLASS_PANE, ChatColor.DARK_GRAY + "Location " + (slot + 1) + ": empty"));
            } else {
                String label = ChatColor.AQUA + "Location " + (slot + 1) + (selected == slot ? ChatColor.GREEN + " [selected]" : "");
                inventory.setItem(18 + slot, new CustomItemStack(Material.ENDER_PEARL, label,
                    ChatColor.GRAY + saved.getWorld().getName() + " " + saved.getBlockX() + ", " + saved.getBlockY() + ", " + saved.getBlockZ(),
                    ChatColor.GRAY + "Left click: select. Right click: delete."));
            }
        }
    }

    private static final class PortalGunMenu implements InventoryHolder {
        private final UUID owner;
        private Inventory inventory;

        private PortalGunMenu(UUID owner) {
            this.owner = owner;
        }

        private UUID owner() {
            return owner;
        }

        private void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
