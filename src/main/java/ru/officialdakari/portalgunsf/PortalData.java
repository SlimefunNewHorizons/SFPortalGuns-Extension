package ru.officialdakari.portalgunsf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;

public class PortalData {

    public Location location;
    public BlockFace blockFace;
    public Color color;
    public Boolean second;
    public String name;
    public String tint;
    private final List<UUID> visualEntities = new ArrayList<>();

    public void addVisual(Entity entity) {
        visualEntities.add(entity.getUniqueId());
    }

    public void removeVisuals() {
        for (UUID entityId : visualEntities) {
            Entity entity = org.bukkit.Bukkit.getEntity(entityId);
            if (entity != null) {
                entity.remove();
            }
        }
        visualEntities.clear();
    }

    public Boolean intersects(Location loc) {
        if (loc == null || location == null || !location.getWorld().equals(loc.getWorld())) {
            return false;
        }
        Location ploc = location.clone();
        if (blockFace == BlockFace.DOWN) {
            loc = loc.clone().add(0, 2, 0);
            ploc = ploc.add(0.5, 0, 0.5);
            return loc.distance(ploc) < 0.65;
        }
        if (blockFace == BlockFace.UP) {
            loc = loc.clone().add(0, -1, 0);
            ploc = ploc.add(0.5, 0, 0.5);
            return loc.distance(ploc) < 0.65;
        }
        if (blockFace == BlockFace.NORTH) {
            ploc = ploc.add(0.5, 0.5, 0);
            return loc.distance(ploc) < 0.65;
        }
        if (blockFace == BlockFace.SOUTH) {
            ploc = ploc.clone().add(0.5, 0.5, 1);
            return loc.distance(ploc) < 0.65;
        }
        if (blockFace == BlockFace.EAST) {
            ploc = ploc.clone().add(1, 0.5, 0.5);
            return loc.distance(ploc) < 0.65;
        }
        if (blockFace == BlockFace.WEST) {
            ploc = ploc.clone().add(0, 0.5, 0.5);
            return loc.distance(ploc) < 0.65;
        }
        return false;
    }
}
