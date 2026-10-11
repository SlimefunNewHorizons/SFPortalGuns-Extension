package ru.officialdakari.portalgunsf;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

final class PortalGunState {

    static final int MAX_CHARGE = 100;
    static final int SHOT_COST = 5;
    static final int TRAVEL_COST = 25;
    static final int MAX_SAVED_LOCATIONS = 8;
    private static final String[] PORTAL_COLORS = {"LIME", "CYAN", "PURPLE", "ORANGE", "RED", "BLUE"};

    private PortalGunState() {
    }

    static String gunId(ItemStack item) {
        PersistentDataContainer pdc = pdc(item);
        String id = pdc.get(PortalGunPlugin.itemMetaPortalGunId, PersistentDataType.STRING);
        if (id == null) {
            id = UUID.randomUUID().toString();
            put(item, PortalGunPlugin.itemMetaPortalGunId, PersistentDataType.STRING, id);
        }
        return id;
    }

    static int charge(ItemStack item) {
        Integer charge = pdc(item).get(PortalGunPlugin.itemMetaPortalGunCharge, PersistentDataType.INTEGER);
        return charge == null ? MAX_CHARGE : Math.max(0, Math.min(MAX_CHARGE, charge));
    }

    static void setCharge(ItemStack item, int charge) {
        put(item, PortalGunPlugin.itemMetaPortalGunCharge, PersistentDataType.INTEGER, Math.max(0, Math.min(MAX_CHARGE, charge)));
    }

    static boolean consume(ItemStack item, int amount) {
        if (isInfinite(item)) {
            return true;
        }
        int current = charge(item);
        if (current < amount) {
            return false;
        }
        setCharge(item, current - amount);
        return true;
    }

    static void refill(ItemStack item) {
        setCharge(item, MAX_CHARGE);
    }

    static String portalName(ItemStack item) {
        String name = pdc(item).get(PortalGunPlugin.itemMetaPortalGunName, PersistentDataType.STRING);
        return name == null || name.isBlank() ? "Portal" : name;
    }

    static void setPortalName(ItemStack item, String name) {
        String cleaned = name == null ? "" : name.trim();
        if (cleaned.isEmpty()) {
            remove(item, PortalGunPlugin.itemMetaPortalGunName);
        } else {
            put(item, PortalGunPlugin.itemMetaPortalGunName, PersistentDataType.STRING, cleaned.substring(0, Math.min(24, cleaned.length())));
        }
    }

    static String portalTint(ItemStack item) {
        String tint = pdc(item).get(PortalGunPlugin.itemMetaPortalGunTint, PersistentDataType.STRING);
        if (tint == null) {
            tint = PORTAL_COLORS[0];
            put(item, PortalGunPlugin.itemMetaPortalGunTint, PersistentDataType.STRING, tint);
        }
        return tint;
    }

    static void cyclePortalTint(ItemStack item) {
        String current = portalTint(item);
        for (int i = 0; i < PORTAL_COLORS.length; i++) {
            if (PORTAL_COLORS[i].equals(current)) {
                put(item, PortalGunPlugin.itemMetaPortalGunTint, PersistentDataType.STRING, PORTAL_COLORS[(i + 1) % PORTAL_COLORS.length]);
                return;
            }
        }
        put(item, PortalGunPlugin.itemMetaPortalGunTint, PersistentDataType.STRING, PORTAL_COLORS[0]);
    }

    static boolean setPortalTint(ItemStack item, String tint) {
        String normalized = tint == null ? "" : tint.trim().toUpperCase(Locale.ROOT);
        for (String available : PORTAL_COLORS) {
            if (available.equals(normalized)) {
                put(item, PortalGunPlugin.itemMetaPortalGunTint, PersistentDataType.STRING, available);
                return true;
            }
        }
        return false;
    }

    static String availablePortalTints() {
        return String.join(", ", PORTAL_COLORS).toLowerCase(Locale.ROOT);
    }

    static String mode(ItemStack item) {
        String mode = pdc(item).get(PortalGunPlugin.itemMetaPortalGunMode, PersistentDataType.STRING);
        return mode == null ? "LINKED" : mode;
    }

    static void toggleMode(ItemStack item) {
        put(item, PortalGunPlugin.itemMetaPortalGunMode, PersistentDataType.STRING, mode(item).equals("LINKED") ? "SAVED" : "LINKED");
    }

    static boolean isInfinite(ItemStack item) {
        Byte enabled = pdc(item).get(PortalGunPlugin.itemMetaPortalGunInfinite, PersistentDataType.BYTE);
        return enabled != null && enabled == (byte) 1;
    }

    static void setInfinite(ItemStack item, boolean enabled) {
        put(item, PortalGunPlugin.itemMetaPortalGunInfinite, PersistentDataType.BYTE, (byte) (enabled ? 1 : 0));
    }

    static int selectedSlot(ItemStack item) {
        Integer selected = pdc(item).get(PortalGunPlugin.itemMetaPortalGunSelected, PersistentDataType.INTEGER);
        return selected == null ? -1 : selected;
    }

    static void setSelectedSlot(ItemStack item, int slot) {
        put(item, PortalGunPlugin.itemMetaPortalGunSelected, PersistentDataType.INTEGER, slot);
    }

    static Location getSavedLocation(ItemStack item, int slot) {
        String encoded = pdc(item).get(PortalGunPlugin.savedLocationKey(slot), PersistentDataType.STRING);
        if (encoded == null) {
            return null;
        }

        String[] parts = encoded.split(";", -1);
        if (parts.length != 6) {
            return null;
        }

        World world = Bukkit.getWorld(parts[0]);
        if (world == null) {
            return null;
        }

        try {
            return new Location(world, Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]),
                Float.parseFloat(parts[4]), Float.parseFloat(parts[5]));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static List<Integer> savedSlots(ItemStack item) {
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < MAX_SAVED_LOCATIONS; slot++) {
            if (getSavedLocation(item, slot) != null) {
                slots.add(slot);
            }
        }
        return slots;
    }

    static boolean saveLocation(ItemStack item, Location location) {
        int slot = -1;
        for (int candidate = 0; candidate < MAX_SAVED_LOCATIONS; candidate++) {
            if (getSavedLocation(item, candidate) == null) {
                slot = candidate;
                break;
            }
        }
        if (slot == -1 || location.getWorld() == null) {
            return false;
        }

        String encoded = String.format(Locale.ROOT, "%s;%.3f;%.3f;%.3f;%.2f;%.2f", location.getWorld().getName(),
            location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        put(item, PortalGunPlugin.savedLocationKey(slot), PersistentDataType.STRING, encoded);
        if (selectedSlot(item) == -1) {
            put(item, PortalGunPlugin.itemMetaPortalGunSelected, PersistentDataType.INTEGER, slot);
        }
        return true;
    }

    static void deleteLocation(ItemStack item, int slot) {
        remove(item, PortalGunPlugin.savedLocationKey(slot));
        if (selectedSlot(item) == slot) {
            remove(item, PortalGunPlugin.itemMetaPortalGunSelected);
        }
    }

    private static PersistentDataContainer pdc(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            throw new IllegalStateException("Portal Gun has no item meta");
        }
        return meta.getPersistentDataContainer();
    }

    private static <T, Z> void put(ItemStack item, org.bukkit.NamespacedKey key, PersistentDataType<T, Z> type, Z value) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            throw new IllegalStateException("Portal Gun has no item meta");
        }
        meta.getPersistentDataContainer().set(key, type, value);
        item.setItemMeta(meta);
    }

    private static void remove(ItemStack item, org.bukkit.NamespacedKey key) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            throw new IllegalStateException("Portal Gun has no item meta");
        }
        meta.getPersistentDataContainer().remove(key);
        item.setItemMeta(meta);
    }
}
