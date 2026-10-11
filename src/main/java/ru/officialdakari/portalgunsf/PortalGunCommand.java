package ru.officialdakari.portalgunsf;

import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

final class PortalGunCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in game.");
            return true;
        }
        ItemStack gun = PortalGunPlugin.findHeldPortalGun(player);
        if (gun == null) {
            player.sendMessage(ChatColor.RED + "Hold a Portal Gun first.");
            return true;
        }
        if (args.length < 2 || !(args[0].equalsIgnoreCase("name") || args[0].equalsIgnoreCase("color"))) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /pg name <name> or /pg color <" + PortalGunState.availablePortalTints() + ">");
            return true;
        }
        if (args[0].equalsIgnoreCase("name")) {
            PortalGunState.setPortalName(gun, String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)));
            player.sendMessage(ChatColor.GREEN + "Portal name set to " + PortalGunState.portalName(gun) + ".");
            return true;
        }
        if (!PortalGunState.setPortalTint(gun, args[1])) {
            player.sendMessage(ChatColor.RED + "Available colors: " + PortalGunState.availablePortalTints());
            return true;
        }
        player.sendMessage(ChatColor.GREEN + "Portal color set to " + PortalGunState.portalTint(gun) + ".");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("name", "color");
        if (args.length == 2 && args[0].equalsIgnoreCase("color")) return List.of("lime", "cyan", "purple", "orange", "red", "blue");
        return List.of();
    }
}
