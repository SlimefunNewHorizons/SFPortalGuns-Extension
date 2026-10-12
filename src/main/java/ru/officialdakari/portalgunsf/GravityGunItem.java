/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ru.officialdakari.portalgunsf;

import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import com.github.drakescraft_labs.slimefun4.api.events.PlayerRightClickEvent;
import com.github.drakescraft_labs.slimefun4.api.items.ItemGroup;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItemStack;
import com.github.drakescraft_labs.slimefun4.api.recipes.RecipeType;
import com.github.drakescraft_labs.slimefun4.core.handlers.EntityInteractHandler;
import com.github.drakescraft_labs.slimefun4.core.handlers.ItemDropHandler;
import com.github.drakescraft_labs.slimefun4.core.handlers.ItemUseHandler;
import com.github.drakescraft_labs.slimefun4.implementation.Slimefun;
import com.github.drakescraft_labs.slimefun4.libraries.dough.protection.Interaction;

/**
 *
 * @author officialdakari
 */
public class GravityGunItem extends SlimefunItem {

    public GravityGunItem(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public void preRegister() {
        EntityInteractHandler handler = this::onEntityClick;
        ItemUseHandler useHandler = this::onItemUse;
        ItemDropHandler drophandler = this::onGgItemDrop;

        addItemHandler(drophandler);
        addItemHandler(handler);
        addItemHandler(useHandler);
    }

    /** Handler GLOBAL de Slimefun: solo actua si el item tirado es la Gravity Gun y el jugador sostiene una entidad. */
    static boolean shouldInterceptGravityDrop(boolean droppedIsGravityGun, boolean holdingEntity) {
        return droppedIsGravityGun && holdingEntity;
    }

    private boolean onGgItemDrop(PlayerDropItemEvent event, Player p, Item item) {
        if (shouldInterceptGravityDrop(PortalGunPlugin.isGravityGun(item.getItemStack()), PortalGunPlugin.holding.containsKey(p.getName()))) {
            Entity entity = PortalGunPlugin.holding.get(p.getName());
            if (entity instanceof BlockDisplay) {
                entity.remove();
                entity = p.getWorld().spawnFallingBlock(entity.getLocation(), PortalGunPlugin.holdingBlock.get(p.getName()).getBlockData());
            }
            if (p.isSneaking()) {
                p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1, 1);
                entity.setVelocity(p.getLocation().getDirection().multiply(3));
            } else {
                p.playSound(p.getLocation(), Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 1, 1);
            }
            PortalGunPlugin.holding.remove(p.getName());
            PortalGunPlugin.holdingBlock.remove(p.getName());
            event.setCancelled(true);
        }
        return false;
    }

    public void onItemUse(PlayerRightClickEvent event) {
        event.cancel();
        Optional<Block> optionalBlock = event.getClickedBlock();
        if (optionalBlock.isEmpty()) {
            return;
        }
        Player p = event.getPlayer();
        if (PortalGunPlugin.holding.containsKey(p.getName())) {
            return;
        }
        Block block = optionalBlock.get();

        Material blockType = block.getType();

        if (blockType == Material.PLAYER_HEAD || blockType == Material.PLAYER_WALL_HEAD || blockType == Material.DISPENSER || blockType == Material.DROPPER || blockType == Material.CHEST || blockType == Material.TRAPPED_CHEST || blockType == Material.BARREL || blockType == Material.HOPPER) {
            return;
        }
        if (blockType == Material.END_GATEWAY || blockType == Material.OBSIDIAN || blockType == Material.REINFORCED_DEEPSLATE || blockType == Material.END_PORTAL_FRAME || blockType == Material.END_PORTAL || blockType == Material.NETHER_PORTAL || blockType == Material.BEDROCK || blockType == Material.COMMAND_BLOCK || blockType == Material.CHAIN_COMMAND_BLOCK || blockType == Material.REPEATING_COMMAND_BLOCK || blockType == Material.BARRIER || blockType == Material.STRUCTURE_BLOCK || blockType == Material.STRUCTURE_VOID) {
            return;
        }
        if (!blockType.isSolid()) {
            return;
        }

        if (!Slimefun.getProtectionManager().hasPermission(p, block, Interaction.BREAK_BLOCK)) {
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1, 1);
            return;
        }

        BlockDisplay fb = (BlockDisplay) p.getWorld().spawnEntity(block.getLocation(), EntityType.BLOCK_DISPLAY);
        fb.setGravity(false);
        fb.setBlock(block.getBlockData());
        fb.setTransformation(new Transformation(new Vector3f(-0.5f, 0.5f, 0), new AxisAngle4f(), new Vector3f(1, 1, 1), new AxisAngle4f()));

        PortalGunPlugin.holdingBlock.put(p.getName(), block.getState());

        block.setType(Material.AIR);

        p.playSound(p.getLocation(), Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 1, 1);
        PortalGunPlugin.holding.put(p.getName(), fb);
    }

    public void onEntityClick(PlayerInteractEntityEvent event, ItemStack item, boolean offHand) {
        Entity entity = event.getRightClicked();
        Player p = event.getPlayer();

        if (entity.getType() == EntityType.PLAYER || entity.getType() == EntityType.AREA_EFFECT_CLOUD || entity.getType() == EntityType.ENDER_DRAGON || entity.getType() == EntityType.WITHER) {
            return;
        }

        if (PortalGunPlugin.holding.containsKey(p.getName())) {
            if (entity instanceof BlockDisplay) {
                entity.remove();
                entity = p.getWorld().spawnFallingBlock(entity.getLocation(), PortalGunPlugin.holdingBlock.get(p.getName()).getBlockData());
            }
            if (p.isSneaking()) {
                p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1, 1);
                entity.setVelocity(p.getLocation().getDirection().multiply(3));
            } else {
                p.playSound(p.getLocation(), Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 1, 1);
            }
            PortalGunPlugin.holding.remove(p.getName());
        } else {
            p.playSound(p.getLocation(), Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 1, 1);
            PortalGunPlugin.holding.put(p.getName(), entity);
        }
    }
}
