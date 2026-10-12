package ru.officialdakari.portalgunsf;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import com.github.drakescraft_labs.slimefun4.api.events.PlayerRightClickEvent;
import com.github.drakescraft_labs.slimefun4.api.items.ItemGroup;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItemStack;
import com.github.drakescraft_labs.slimefun4.api.recipes.RecipeType;
import com.github.drakescraft_labs.slimefun4.core.handlers.ItemDropHandler;
import com.github.drakescraft_labs.slimefun4.core.handlers.ItemUseHandler;

public class PortalGunItem extends SlimefunItem {

    public PortalGunItem(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public void preRegister() {
        addItemHandler((ItemUseHandler) this::onItemUse);
        addItemHandler((ItemDropHandler) this::onDrop);
    }

    /**
     * El ItemDropHandler de Slimefun es GLOBAL: se invoca con CUALQUIER item que tire CUALQUIER jugador, no solo con
     * la Portal Gun. Solo se interviene si el item tirado es una Portal Gun y el jugador no esta agachado.
     * (Regresion del 2026-10-11: sin esta comprobacion nadie podia tirar nada sin agacharse.)
     */
    static boolean shouldInterceptDrop(boolean droppedIsPortalGun, boolean sneaking) {
        return droppedIsPortalGun && !sneaking;
    }

    private boolean onDrop(PlayerDropItemEvent event, Player player, org.bukkit.entity.Item dropped) {
        ItemStack stack = dropped.getItemStack();
        if (!shouldInterceptDrop(PortalGunPlugin.isPortalGun(stack), player.isSneaking())) {
            return false;
        }
        event.setCancelled(true);
        PortalGunPlugin.removePortalPair(PortalGunState.gunId(stack));
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1, 1);
        return false;
    }

    private void onItemUse(PlayerRightClickEvent event) {
        event.cancel();

        Player player = event.getPlayer();
        ItemStack gun = event.getItem();

        if (player.isSneaking()) {
            PortalGunNativeDialog.open(PortalGunPlugin.plugin, player, gun);
            return;
        }

        if (PortalGunPlugin.tryRefillFromOffhand(player, gun)) {
            return;
        }

        if ("SAVED".equals(PortalGunState.mode(gun))) {
            PortalGunPlugin.teleportToSavedLocation(player, gun);
            return;
        }

        if (!PortalGunState.consume(gun, PortalGunState.SHOT_COST)) {
            player.sendMessage(ChatColor.RED + "Your Portal Gun needs Portal Fluid.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 0.7F);
            return;
        }

        Location launch = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(0.4));
        Snowball projectile = (Snowball) player.getWorld().spawnEntity(launch, EntityType.SNOWBALL);
        projectile.setGravity(false);
        projectile.setGlowing(true);
        projectile.setVelocity(launch.getDirection().multiply(4));
        projectile.setMetadata("portalgunball", new FixedMetadataValue(addon.getJavaPlugin(), player.getUniqueId().toString()));
        projectile.setMetadata("portalgunid", new FixedMetadataValue(addon.getJavaPlugin(), PortalGunState.gunId(gun)));
        projectile.setMetadata("color_1", new FixedMetadataValue(addon.getJavaPlugin(), PortalGunPlugin.portalColor(gun, true)));
        projectile.setMetadata("color_2", new FixedMetadataValue(addon.getJavaPlugin(), PortalGunPlugin.portalColor(gun, false)));
        projectile.setMetadata("portal_name", new FixedMetadataValue(addon.getJavaPlugin(), PortalGunState.portalName(gun)));
        projectile.setMetadata("portal_tint", new FixedMetadataValue(addon.getJavaPlugin(), PortalGunState.portalTint(gun)));

        player.playSound(launch, Sound.ENTITY_ENDER_PEARL_THROW, 1, 1);
    }
}
