package ru.officialdakari.portalgunsf;

import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import com.github.drakescraft_labs.slimefun4.implementation.Slimefun;
import com.github.drakescraft_labs.slimefun4.libraries.dough.protection.Interaction;

public class PortalGunEventListener implements Listener {

    ArrayList<UUID> dnt = new ArrayList(); // dnt stands for do not teleport

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHit(ProjectileHitEvent e) {
        Entity projectile = e.getEntity();
        if (projectile.getType() == EntityType.SNOWBALL) {
            if (e.getHitBlock() == null || e.getHitBlockFace() == null || e.getHitBlock().getType() == Material.AIR) {
                return;
            }
            Snowball ball = (Snowball) projectile;
            if (ball.hasMetadata("portalgunball")) {
                String playerName = ball.getMetadata("portalgunball").get(0).asString();
                String pgId = ball.getMetadata("portalgunid").get(0).asString();
                Player p = Bukkit.getPlayer(playerName);
                if (p == null) {
                    return;
                }
                Location loc = ball.getLocation();

                Location placement = e.getHitBlock().getLocation().clone().add(e.getHitBlockFace().getModX(), e.getHitBlockFace().getModY(), e.getHitBlockFace().getModZ());
                if (!Slimefun.getProtectionManager().hasPermission(p, e.getHitBlock().getLocation(), Interaction.BREAK_BLOCK)
                    || !Slimefun.getProtectionManager().hasPermission(p, placement, Interaction.PLACE_BLOCK)) {
                    p.sendMessage("§c§oYou cannot create a portal in this protected area.");
                    return;
                }

                if (!PortalGunPlugin.isPortalPlaceable(e.getHitBlock().getLocation(), e.getHitBlockFace())) {
                    p.sendMessage("\u00A7c\u00A7oCan't place a portal there.");
                    return;
                }
                if (!PortalGunPlugin.portals.containsKey(pgId)) {
                    PortalGunPlugin.portals.put(pgId, new ArrayList<PortalData>());
                }

                ArrayList<PortalData> al = PortalGunPlugin.portals.get(pgId);
                if (al.size() == 2) {
                    al.remove(0);
                }

                p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1, 1);

                String color1 = ball.getMetadata("color_1").get(0).asString();
                String[] s1 = color1.split(";");

                boolean second = !al.isEmpty() && !al.get(0).second;

                String color = ball.getMetadata(second ? "color_2" : "color_1").get(0).asString();
                String[] s = color.split(";");

                PortalData pd = new PortalData();
                pd.blockFace = e.getHitBlockFace();
                pd.location = e.getHitBlock().getLocation();
                pd.color = Color.fromRGB(Integer.parseInt(s[0]), Integer.parseInt(s[1]), Integer.parseInt(s[2]));
                pd.second = second;

                al.add(pd);

                PortalGunPlugin.portals.put(pgId, al);
            }
        }
    }

    public void handleGravGun(Player p) {
        if (!PortalGunPlugin.holding.containsKey(p.getName())) {
            return;
        }
        Entity entity = PortalGunPlugin.holding.get(p.getName());
        if (p == null) {
            PortalGunPlugin.holding.remove(p.getName());
            return;
        }
        RayTraceResult rtr = p.rayTraceBlocks(6, FluidCollisionMode.NEVER);
        if (rtr != null) {
            Location loc = rtr.getHitPosition().toLocation(p.getWorld());
            if (entity.getType() == EntityType.BLOCK_DISPLAY) {
                loc = loc.setDirection(new Vector(0, 0, 0));
            }
            entity.teleport(loc.add(0, 0.8, 0));
        } else {
            Location loc = p.getLocation().clone().add(p.getLocation().getDirection().multiply(4));
            if (entity.getType() == EntityType.BLOCK_DISPLAY) {
                loc = loc.setDirection(new Vector(0, 0, 0));
            }
            entity.teleport(loc.add(0, 0.8, 0));
        }
        entity.setFallDistance(0);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        Location loc = e.getTo();

        handleGravGun(p);

        if (dnt.contains(p.getUniqueId())) {
            return;
        }

        for (ArrayList<PortalData> al : PortalGunPlugin.portals.values()) {
            if (al.size() == 2) {
                if (al.get(0).intersects(loc)) {
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                    dnt.add(p.getUniqueId());

                    PortalData t = al.get(1);
                    if (!PortalGunPlugin.canTravel(p, loc, t.location)) {
                        return;
                    }
                    Location tloc = t.location.clone();

                    tloc.setYaw(loc.getYaw());
                    tloc.setPitch(loc.getPitch());

                    if (al.get(0).blockFace == al.get(1).blockFace) {
                        tloc.setYaw((tloc.getYaw() + 180) % 360);
                    }

                    if ((al.get(0).blockFace == BlockFace.NORTH && al.get(1).blockFace == BlockFace.EAST)
                            || (al.get(0).blockFace == BlockFace.EAST && al.get(1).blockFace == BlockFace.SOUTH)
                            || (al.get(0).blockFace == BlockFace.SOUTH && al.get(1).blockFace == BlockFace.WEST)
                            || (al.get(0).blockFace == BlockFace.WEST && al.get(1).blockFace == BlockFace.NORTH)) {
                        tloc.setYaw((tloc.getYaw() - 90) % 360);
                    }

                    if ((al.get(1).blockFace == BlockFace.NORTH && al.get(0).blockFace == BlockFace.EAST)
                            || (al.get(1).blockFace == BlockFace.EAST && al.get(0).blockFace == BlockFace.SOUTH)
                            || (al.get(1).blockFace == BlockFace.SOUTH && al.get(0).blockFace == BlockFace.WEST)
                            || (al.get(1).blockFace == BlockFace.WEST && al.get(0).blockFace == BlockFace.NORTH)) {
                        tloc.setYaw((tloc.getYaw() + 90) % 360);
                    }

                    if (t.blockFace == BlockFace.UP) {
                        p.teleport(tloc.add(0.5, 1, 0.5));
                    }
                    if (t.blockFace == BlockFace.DOWN) {
                        p.teleport(tloc.add(0.5, -2, 0.5));
                    }
                    if (t.blockFace == BlockFace.NORTH) {
                        p.teleport(tloc.add(0.5, 0, -0.02));
                    }
                    if (t.blockFace == BlockFace.SOUTH) {
                        p.teleport(tloc.add(0.5, 0, 1.5));
                    }
                    if (t.blockFace == BlockFace.EAST) {
                        p.teleport(tloc.add(1.5, 0, 0.5));
                    }
                    if (t.blockFace == BlockFace.WEST) {
                        p.teleport(tloc.add(-0.02, 0, 0.5));
                    }

                    Bukkit.getScheduler().runTaskLater(PortalGunPlugin.plugin, (task) -> {
                        if (t.blockFace == BlockFace.UP) {
                            p.setVelocity(new Vector(0, 0.3, 0));
                        }
                        if (t.blockFace == BlockFace.DOWN) {
                            p.setVelocity(new Vector(0, -0.3, 0));
                        }
                        if (t.blockFace == BlockFace.NORTH) {
                            p.setVelocity(new Vector(0, 0, -0.3));
                        }
                        if (t.blockFace == BlockFace.SOUTH) {
                            p.setVelocity(new Vector(0, 0, 0.3));
                        }
                        if (t.blockFace == BlockFace.EAST) {
                            p.setVelocity(new Vector(0.3, 0, 0));
                        }
                        if (t.blockFace == BlockFace.WEST) {
                            p.setVelocity(new Vector(-0.3, 0, 0));
                        }
                    }, 1L);
                    Bukkit.getScheduler().runTaskLater(PortalGunPlugin.plugin, (task) -> {
                        dnt.remove(p.getUniqueId());
                    }, 5L);
                } else if (al.get(1).intersects(loc)) {
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                    dnt.add(p.getUniqueId());

                    PortalData t = al.get(0);
                    if (!PortalGunPlugin.canTravel(p, loc, t.location)) {
                        return;
                    }
                    Location tloc = t.location.clone();

                    tloc.setYaw(loc.getYaw());
                    tloc.setPitch(loc.getPitch());

                    if (al.get(0).blockFace == al.get(1).blockFace) {
                        tloc.setYaw((tloc.getYaw() + 180) % 360);
                    }

                    if ((al.get(0).blockFace == BlockFace.NORTH && al.get(1).blockFace == BlockFace.EAST)
                            || (al.get(0).blockFace == BlockFace.EAST && al.get(1).blockFace == BlockFace.SOUTH)
                            || (al.get(0).blockFace == BlockFace.SOUTH && al.get(1).blockFace == BlockFace.WEST)
                            || (al.get(0).blockFace == BlockFace.WEST && al.get(1).blockFace == BlockFace.NORTH)) {
                        tloc.setYaw((tloc.getYaw() + 90) % 360);
                    }

                    if ((al.get(1).blockFace == BlockFace.NORTH && al.get(0).blockFace == BlockFace.EAST)
                            || (al.get(1).blockFace == BlockFace.EAST && al.get(0).blockFace == BlockFace.SOUTH)
                            || (al.get(1).blockFace == BlockFace.SOUTH && al.get(0).blockFace == BlockFace.WEST)
                            || (al.get(1).blockFace == BlockFace.WEST && al.get(0).blockFace == BlockFace.NORTH)) {
                        tloc.setYaw((tloc.getYaw() - 90) % 360);
                    }

                    if (t.blockFace == BlockFace.UP) {
                        p.teleport(tloc.add(0.5, 1, 0.5));
                    }
                    if (t.blockFace == BlockFace.DOWN) {
                        p.teleport(tloc.add(0.5, -2, 0.5));
                    }
                    if (t.blockFace == BlockFace.NORTH) {
                        p.teleport(tloc.add(0.5, 0, -0.02));
                    }
                    if (t.blockFace == BlockFace.SOUTH) {
                        p.teleport(tloc.add(0.5, 0, 1.5));
                    }
                    if (t.blockFace == BlockFace.EAST) {
                        p.teleport(tloc.add(1.5, 0, 0.5));
                    }
                    if (t.blockFace == BlockFace.WEST) {
                        p.teleport(tloc.add(-0.02, 0, 0.5));
                    }

                    Bukkit.getScheduler().runTaskLater(PortalGunPlugin.plugin, (task) -> {
                        if (t.blockFace == BlockFace.UP) {
                            p.setVelocity(new Vector(0, 0.3, 0));
                        }
                        if (t.blockFace == BlockFace.DOWN) {
                            p.setVelocity(new Vector(0, -0.3, 0));
                        }
                        if (t.blockFace == BlockFace.NORTH) {
                            p.setVelocity(new Vector(0, 0, -0.3));
                        }
                        if (t.blockFace == BlockFace.SOUTH) {
                            p.setVelocity(new Vector(0, 0, 0.3));
                        }
                        if (t.blockFace == BlockFace.EAST) {
                            p.setVelocity(new Vector(0.3, 0, 0));
                        }
                        if (t.blockFace == BlockFace.WEST) {
                            p.setVelocity(new Vector(-0.3, 0, 0));
                        }
                    }, 1L);
                    Bukkit.getScheduler().runTaskLater(PortalGunPlugin.plugin, (task) -> {
                        dnt.remove(p.getUniqueId());
                    }, 5L);
                }
            }
        }
    }

}
