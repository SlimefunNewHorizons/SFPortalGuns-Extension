package ru.officialdakari.portalgunsf;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Regresion 2026-10-11: el ItemDropHandler de Slimefun es global y la Portal Gun cancelaba CUALQUIER drop
 * de un jugador sin agacharse. Estas pruebas fijan que solo se interviene sobre las propias armas.
 */
public class DropGuardTest {

    @Test
    public void ordinaryItemsAreNeverIntercepted() {
        assertFalse("tirar un item cualquiera sin agacharse debe funcionar", PortalGunItem.shouldInterceptDrop(false, false));
        assertFalse(PortalGunItem.shouldInterceptDrop(false, true));
    }

    @Test
    public void portalGunIsInterceptedOnlyWhenNotSneaking() {
        assertTrue(PortalGunItem.shouldInterceptDrop(true, false));
        assertFalse("agachado se puede tirar la pistola", PortalGunItem.shouldInterceptDrop(true, true));
    }

    @Test
    public void gravityGunDropOnlyActsForTheGunWhileHoldingAnEntity() {
        assertFalse(GravityGunItem.shouldInterceptGravityDrop(false, true));
        assertFalse(GravityGunItem.shouldInterceptGravityDrop(true, false));
        assertTrue(GravityGunItem.shouldInterceptGravityDrop(true, true));
    }
}
