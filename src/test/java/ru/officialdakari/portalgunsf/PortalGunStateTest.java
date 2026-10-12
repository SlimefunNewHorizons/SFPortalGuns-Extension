package ru.officialdakari.portalgunsf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

/** Logica pura de la Portal Gun: carga, nombre, color, modo y ubicaciones guardadas (sin cargar Slimefun). */
public class PortalGunStateTest {

    private ServerMock server;
    private World world;
    private ItemStack gun;

    private static NamespacedKey key(String name) {
        return new NamespacedKey("sfportalguns", name);
    }

    @Before
    public void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        PortalGunPlugin.itemMetaPortalGunId = key("id");
        PortalGunPlugin.itemMetaPortalGunCharge = key("charge");
        PortalGunPlugin.itemMetaPortalGunMode = key("mode");
        PortalGunPlugin.itemMetaPortalGunInfinite = key("infinite");
        PortalGunPlugin.itemMetaPortalGunSelected = key("selected");
        PortalGunPlugin.itemMetaPortalGunName = key("name");
        PortalGunPlugin.itemMetaPortalGunTint = key("tint");
        PortalGunPlugin.savedLocationKeyFactory = slot -> key("saved_location_" + slot);
        gun = new ItemStack(Material.IRON_HORSE_ARMOR);
    }

    @After
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    public void newGunStartsFullyCharged() {
        assertEquals(PortalGunState.MAX_CHARGE, PortalGunState.charge(gun));
    }

    @Test
    public void consumeDeductsChargeAndRefusesWhenInsufficient() {
        assertTrue(PortalGunState.consume(gun, PortalGunState.SHOT_COST));
        assertEquals(PortalGunState.MAX_CHARGE - PortalGunState.SHOT_COST, PortalGunState.charge(gun));

        PortalGunState.setCharge(gun, 4);
        assertFalse(PortalGunState.consume(gun, PortalGunState.SHOT_COST));
        assertEquals("un disparo rechazado no debe gastar carga", 4, PortalGunState.charge(gun));
    }

    @Test
    public void chargeIsClampedToValidRange() {
        PortalGunState.setCharge(gun, 5000);
        assertEquals(PortalGunState.MAX_CHARGE, PortalGunState.charge(gun));
        PortalGunState.setCharge(gun, -20);
        assertEquals(0, PortalGunState.charge(gun));
    }

    @Test
    public void infiniteModeNeverConsumesCharge() {
        PortalGunState.setCharge(gun, 0);
        PortalGunState.setInfinite(gun, true);
        assertTrue(PortalGunState.consume(gun, PortalGunState.TRAVEL_COST));
        assertEquals(0, PortalGunState.charge(gun));
    }

    @Test
    public void refillRestoresMaximumCharge() {
        PortalGunState.setCharge(gun, 3);
        PortalGunState.refill(gun);
        assertEquals(PortalGunState.MAX_CHARGE, PortalGunState.charge(gun));
    }

    @Test
    public void nameIsTrimmedCappedAndResettable() {
        assertEquals("Portal", PortalGunState.portalName(gun));
        PortalGunState.setPortalName(gun, "  Casa de Rick  ");
        assertEquals("Casa de Rick", PortalGunState.portalName(gun));
        PortalGunState.setPortalName(gun, "x".repeat(60));
        assertEquals(24, PortalGunState.portalName(gun).length());
        PortalGunState.setPortalName(gun, "   ");
        assertEquals("Portal", PortalGunState.portalName(gun));
    }

    @Test
    public void tintDefaultsToLimeCyclesAndValidatesInput() {
        assertEquals("LIME", PortalGunState.portalTint(gun));
        assertTrue(PortalGunState.setPortalTint(gun, " purple "));
        assertEquals("PURPLE", PortalGunState.portalTint(gun));
        assertFalse(PortalGunState.setPortalTint(gun, "magenta"));
        assertEquals("PURPLE", PortalGunState.portalTint(gun));

        PortalGunState.setPortalTint(gun, "blue");
        PortalGunState.cyclePortalTint(gun);
        assertEquals("el ciclo vuelve al primer color", "LIME", PortalGunState.portalTint(gun));
    }

    @Test
    public void modeTogglesBetweenLinkedAndSaved() {
        assertEquals("LINKED", PortalGunState.mode(gun));
        PortalGunState.toggleMode(gun);
        assertEquals("SAVED", PortalGunState.mode(gun));
        PortalGunState.toggleMode(gun);
        assertEquals("LINKED", PortalGunState.mode(gun));
    }

    @Test
    public void savedLocationsRoundTripAndAutoSelectFirst() {
        Location spot = new Location(world, 10.5, 64, -20.25, 90f, 15f);
        assertTrue(PortalGunState.saveLocation(gun, spot));

        Location back = PortalGunState.getSavedLocation(gun, 0);
        assertNotNull(back);
        assertEquals(10.5, back.getX(), 0.001);
        assertEquals(64, back.getY(), 0.001);
        assertEquals(-20.25, back.getZ(), 0.001);
        assertEquals(90f, back.getYaw(), 0.01f);
        assertEquals("la primera ubicacion queda seleccionada", 0, PortalGunState.selectedSlot(gun));
    }

    @Test
    public void savedLocationsAreCappedAtEight() {
        for (int i = 0; i < PortalGunState.MAX_SAVED_LOCATIONS; i++) {
            assertTrue(PortalGunState.saveLocation(gun, new Location(world, i, 70, i)));
        }
        assertFalse("la novena ubicacion debe rechazarse", PortalGunState.saveLocation(gun, new Location(world, 99, 70, 99)));
        assertEquals(PortalGunState.MAX_SAVED_LOCATIONS, PortalGunState.savedSlots(gun).size());
    }

    @Test
    public void deletingASlotFreesItAndClearsSelection() {
        PortalGunState.saveLocation(gun, new Location(world, 1, 70, 1));
        PortalGunState.saveLocation(gun, new Location(world, 2, 70, 2));
        assertEquals(0, PortalGunState.selectedSlot(gun));

        PortalGunState.deleteLocation(gun, 0);
        assertNull(PortalGunState.getSavedLocation(gun, 0));
        assertEquals("borrar la ubicacion seleccionada limpia la seleccion", -1, PortalGunState.selectedSlot(gun));

        assertTrue(PortalGunState.saveLocation(gun, new Location(world, 3, 70, 3)));
        assertEquals("el hueco liberado se reutiliza", 3.0, PortalGunState.getSavedLocation(gun, 0).getX(), 0.001);
    }

    @Test
    public void corruptedSavedLocationIsIgnoredNotCrashing() {
        var meta = gun.getItemMeta();
        meta.getPersistentDataContainer().set(PortalGunPlugin.savedLocationKey(0), PersistentDataType.STRING, "world;abc;1;2");
        gun.setItemMeta(meta);
        assertNull(PortalGunState.getSavedLocation(gun, 0));
        assertTrue(PortalGunState.savedSlots(gun).isEmpty());
    }

    @Test
    public void unknownWorldInSavedLocationIsIgnored() {
        var meta = gun.getItemMeta();
        meta.getPersistentDataContainer().set(PortalGunPlugin.savedLocationKey(1), PersistentDataType.STRING,
            "mundo_borrado;1.0;2.0;3.0;0.0;0.0");
        gun.setItemMeta(meta);
        assertNull(PortalGunState.getSavedLocation(gun, 1));
    }

    @Test
    public void gunIdIsStable() {
        String first = PortalGunState.gunId(gun);
        assertEquals(first, PortalGunState.gunId(gun));
    }
}
