package ru.officialdakari.portalgunsf;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

final class PortalVisual {

    private static final int RING_SEGMENTS = 12;
    private static final double RADIUS = 0.60D;

    private PortalVisual() {
    }

    static void spawn(PortalData portal) {
        Location center = center(portal);
        Material material = materialFor(portal.tint);
        for (int segment = 0; segment < RING_SEGMENTS; segment++) {
            double angle = 2D * Math.PI * segment / RING_SEGMENTS;
            Location point = ringPoint(center, portal.blockFace, Math.cos(angle) * RADIUS, Math.sin(angle) * RADIUS);
            BlockDisplay display = point.getWorld().spawn(point, BlockDisplay.class);
            display.setBlock(material.createBlockData());
            display.setBrightness(new Display.Brightness(15, 15));
            display.setViewRange(0.75F);
            display.setShadowRadius(0F);
            display.setShadowStrength(0F);
            display.setTransformation(new Transformation(new Vector3f(), new org.joml.Quaternionf(), new Vector3f(0.18F, 0.18F, 0.18F), new org.joml.Quaternionf()));
            portal.addVisual(display);
        }

        Location label = center.clone().add(0, portal.blockFace == BlockFace.UP ? 0.35 : 0.85, 0);
        TextDisplay text = label.getWorld().spawn(label, TextDisplay.class);
        text.text(Component.text(portal.name + (Boolean.TRUE.equals(portal.second) ? "  II" : "  I"), textColor(portal.tint)));
        text.setBillboard(Display.Billboard.CENTER);
        text.setSeeThrough(true);
        text.setDefaultBackground(false);
        text.setShadowed(true);
        text.setViewRange(0.75F);
        portal.addVisual(text);
    }

    private static Location center(PortalData portal) {
        return switch (portal.blockFace) {
            case NORTH -> portal.location.clone().add(0.5, 0.8, 0.02);
            case SOUTH -> portal.location.clone().add(0.5, 0.8, 0.98);
            case EAST -> portal.location.clone().add(0.98, 0.8, 0.5);
            case WEST -> portal.location.clone().add(0.02, 0.8, 0.5);
            case UP -> portal.location.clone().add(0.5, 1.02, 0.5);
            case DOWN -> portal.location.clone().add(0.5, -0.02, 0.5);
            default -> portal.location.clone().add(0.5, 0.8, 0.5);
        };
    }

    private static Location ringPoint(Location center, BlockFace face, double horizontal, double vertical) {
        return switch (face) {
            case NORTH, SOUTH -> center.clone().add(horizontal, vertical, 0);
            case EAST, WEST -> center.clone().add(0, vertical, horizontal);
            case UP, DOWN -> center.clone().add(horizontal, 0, vertical);
            default -> center;
        };
    }

    private static Material materialFor(String tint) {
        return switch (tint) {
            case "CYAN" -> Material.CYAN_STAINED_GLASS_PANE;
            case "PURPLE" -> Material.PURPLE_STAINED_GLASS_PANE;
            case "ORANGE" -> Material.ORANGE_STAINED_GLASS_PANE;
            case "RED" -> Material.RED_STAINED_GLASS_PANE;
            case "BLUE" -> Material.BLUE_STAINED_GLASS_PANE;
            default -> Material.LIME_STAINED_GLASS_PANE;
        };
    }

    private static TextColor textColor(String tint) {
        return switch (tint) {
            case "CYAN" -> TextColor.color(0x55FFFF);
            case "PURPLE" -> TextColor.color(0xAA00AA);
            case "ORANGE" -> TextColor.color(0xFFAA00);
            case "RED" -> TextColor.color(0xFF5555);
            case "BLUE" -> TextColor.color(0x5555FF);
            default -> TextColor.color(0x55FF55);
        };
    }
}
