package ru.officialdakari.portalgunsf;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.event.ClickCallback;

/** Native Minecraft 1.21.11 dialog for Portal Gun configuration. */
final class PortalGunNativeDialog {

    private PortalGunNativeDialog() {
    }

    static void open(PortalGunPlugin plugin, Player player, ItemStack gun) {
        List<SingleOptionDialogInput.OptionEntry> colours = new ArrayList<>();
        for (String colour : new String[] {"LIME", "CYAN", "PURPLE", "ORANGE", "RED", "BLUE"}) {
            colours.add(SingleOptionDialogInput.OptionEntry.create(colour, Component.text(colour.toLowerCase()), colour.equals(PortalGunState.portalTint(gun))));
        }

        List<SingleOptionDialogInput.OptionEntry> destinations = new ArrayList<>();
        destinations.add(SingleOptionDialogInput.OptionEntry.create("-1", Component.text("No saved destination"), PortalGunState.selectedSlot(gun) == -1));
        for (int slot = 0; slot < PortalGunState.MAX_SAVED_LOCATIONS; slot++) {
            Location saved = PortalGunState.getSavedLocation(gun, slot);
            if (saved != null) {
                String label = "Location " + (slot + 1) + " — " + saved.getWorld().getName() + " " + saved.getBlockX() + ", " + saved.getBlockY() + ", " + saved.getBlockZ();
                destinations.add(SingleOptionDialogInput.OptionEntry.create(String.valueOf(slot), Component.text(label), PortalGunState.selectedSlot(gun) == slot));
            }
        }

        ActionButton save = ActionButton.create(Component.text("Apply Portal Gun settings", NamedTextColor.GREEN),
            Component.text("Saves the name, color, mode and selected destination."), 200,
            DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player responsePlayer) {
                    Bukkit.getScheduler().runTask(plugin, () -> apply(plugin, responsePlayer, view.getText("portal_name"), view.getText("portal_colour"),
                        view.getBoolean("saved_mode"), view.getBoolean("infinite_mode"), view.getText("destination")));
                }
            }, ClickCallback.Options.builder().uses(1).build()));

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Portal Gun Control", NamedTextColor.GREEN))
                .externalTitle(Component.text("Portal Gun", NamedTextColor.GREEN))
                .body(List.of(
                    DialogBody.plainMessage(Component.text("Native 1.21.11 panel. Portal Fluid: " + PortalGunState.charge(gun) + "/" + PortalGunState.MAX_CHARGE)),
                    DialogBody.plainMessage(Component.text("Hold the gun while submitting. Use the old inventory only for deleting saved locations."))
                ))
                .inputs(List.of(
                    DialogInput.text("portal_name", Component.text("Portal name")).initial(PortalGunState.portalName(gun)).maxLength(24).build(),
                    DialogInput.singleOption("portal_colour", 200, colours, Component.text("Portal color"), false),
                    DialogInput.bool("saved_mode", Component.text("Use saved-location mode"), "SAVED".equals(PortalGunState.mode(gun)), "saved", "linked"),
                    DialogInput.singleOption("destination", 300, destinations, Component.text("Saved destination"), false),
                    DialogInput.bool("infinite_mode", Component.text("Infinite mode (staff only)"), PortalGunState.isInfinite(gun), "on", "off")
                ))
                .build())
            .type(DialogType.notice(save)));
        player.showDialog(dialog);
    }

    private static void apply(PortalGunPlugin plugin, Player player, String name, String colour, Boolean savedMode, Boolean infiniteMode, String destination) {
        ItemStack gun = PortalGunPlugin.findHeldPortalGun(player);
        if (gun == null) {
            player.sendMessage(ChatColor.RED + "Hold the Portal Gun while applying its settings.");
            return;
        }
        PortalGunState.setPortalName(gun, name);
        PortalGunState.setPortalTint(gun, colour);
        if (Boolean.TRUE.equals(savedMode) != "SAVED".equals(PortalGunState.mode(gun))) {
            PortalGunState.toggleMode(gun);
        }
        try {
            PortalGunState.setSelectedSlot(gun, Integer.parseInt(destination));
        } catch (NumberFormatException ignored) {
            PortalGunState.setSelectedSlot(gun, -1);
        }
        if (player.hasPermission("sfportalguns.infinite")) {
            PortalGunState.setInfinite(gun, Boolean.TRUE.equals(infiniteMode));
        }
        player.sendMessage(ChatColor.GREEN + "Portal Gun settings saved.");
    }
}
