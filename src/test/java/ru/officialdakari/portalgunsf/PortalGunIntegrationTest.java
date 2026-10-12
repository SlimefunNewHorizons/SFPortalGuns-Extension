package ru.officialdakari.portalgunsf;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import com.github.drakescraft_labs.slimefun4.api.events.PlayerRightClickEvent;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import com.github.drakescraft_labs.slimefun4.core.handlers.ItemUseHandler;
import com.github.drakescraft_labs.slimefun4.implementation.Slimefun;

/** Carga Slimefun y el addon juntos y comprueba que clic derecho con la Portal Gun dispara de verdad. */
public class PortalGunIntegrationTest {

    private ServerMock server;

    @Before
    public void setUp() {
        server = MockBukkit.mock();
        Slimefun slimefun = MockBukkit.load(Slimefun.class);
        MockBukkit.load(PortalGunPlugin.class);
        injectProtectionManager(slimefun);
    }

    /** El ProtectionManager se crea en una tarea posterior al arranque que MockBukkit no ejecuta; se inyecta el real (sin plugins de proteccion). */
    private void injectProtectionManager(Slimefun slimefun) {
        try {
            Object integrations = Slimefun.getIntegrations();
            for (var field : integrations.getClass().getDeclaredFields()) {
                if (field.getType() == com.github.drakescraft_labs.slimefun4.libraries.dough.protection.ProtectionManager.class) {
                    field.setAccessible(true);
                    if (field.get(integrations) == null) {
                        field.set(integrations, new com.github.drakescraft_labs.slimefun4.libraries.dough.protection.ProtectionManager(slimefun));
                    }
                    return;
                }
            }
            throw new IllegalStateException("IntegrationsManager no tiene campo ProtectionManager");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @After
    public void tearDown() {
        MockBukkit.unmock();
    }

    /**
     * Invoca el ItemUseHandler real del item registrado en Slimefun. (El listener de Slimefun no despacha eventos de
     * clic simulados bajo MockBukkit, pero el handler es exactamente lo que ejecuta el servidor al hacer clic.)
     */
    private void use(PlayerMock player, SlimefunItem sfItem, ItemStack stack, Action action, Block block, BlockFace face) {
        PlayerInteractEvent interact = new PlayerInteractEvent(player, action, stack, block, face, EquipmentSlot.HAND);
        PlayerRightClickEvent event = new PlayerRightClickEvent(interact);
        sfItem.callItemHandler(ItemUseHandler.class, handler -> handler.onRightClick(event));
    }

    @Test
    public void rightClickFiresASnowball() {
        SlimefunItem gunItem = SlimefunItem.getById("PORTAL_GUN");
        assertNotNull("la Portal Gun debe estar registrada en Slimefun", gunItem);

        WorldMock world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();
        player.teleport(new Location(world, 0, 70, 0));
        ItemStack gun = gunItem.getItem().clone();
        player.getInventory().setItemInMainHand(gun);

        use(player, gunItem, gun, Action.RIGHT_CLICK_AIR, null, null);

        assertTrue("clic derecho con la Portal Gun debe lanzar una bola de nieve",
            !world.getEntitiesByClass(Snowball.class).isEmpty());
    }

    /**
     * Regresion 2026-10-12: el disparo guardaba el UUID del tirador pero el listener buscaba al jugador por nombre,
     * asi que ningun impacto creaba portales. (La creacion visual usa entidades Display que MockBukkit no implementa.)
     */
    @Test
    public void shooterIsResolvedFromTheProjectileMetadata() {
        PlayerMock player = server.addPlayer();
        org.junit.Assert.assertSame(player, PortalGunEventListener.resolveOwner(player.getUniqueId().toString()));
        org.junit.Assert.assertSame("tambien se acepta el formato antiguo (nombre)", player, PortalGunEventListener.resolveOwner(player.getName()));
        org.junit.Assert.assertNull(PortalGunEventListener.resolveOwner("nadie-con-este-nombre"));
    }

    @Test
    public void gravityGunPicksUpABlock() {
        SlimefunItem gravity = SlimefunItem.getById("GRAVITY_GUN");
        WorldMock world = server.addSimpleWorld("world");
        Block stone = world.getBlockAt(2, 69, 0);
        stone.setType(Material.STONE);
        PlayerMock player = server.addPlayer();
        player.teleport(new Location(world, 0, 70, 0));
        ItemStack item = gravity.getItem().clone();
        player.getInventory().setItemInMainHand(item);

        use(player, gravity, item, Action.RIGHT_CLICK_BLOCK, stone, BlockFace.UP);
        assertTrue("la Gravity Gun debe levantar el bloque", PortalGunPlugin.holding.containsKey(player.getName()));
    }

    @Test
    public void gravityGunIsRegistered() {
        assertNotNull(SlimefunItem.getById("GRAVITY_GUN"));
        assertNotNull(SlimefunItem.getById("PORTAL_FLUID"));
        assertTrue(Material.NETHERITE_HOE == SlimefunItem.getById("PORTAL_GUN").getItem().getType());
    }
}
