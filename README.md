# SF PortalGuns Extension

Addon de Slimefun para Paper/Purpur **1.21.11** y Java **21**. Añade movilidad tecnológica sin mods de cliente: portales enlazados, recarga mediante fluido verde, destinos guardados y una Gravity Gun.

## Qué incluye

- **Portal Gun** con dos modos: Linked crea hasta dos portales enlazados; Saved viaja al destino seleccionado.
- **Portal Fluid** recarga la pistola a 100 unidades desde la mano secundaria.
- **Panel de control**: Shift + clic derecho abre el menú.
- **Ocho ubicaciones guardadas por pistola**: clic izquierdo selecciona y clic derecho elimina.
- **Modo infinito** para quien tenga sfportalguns.infinite.
- **Cooldown de 10 segundos** entre usos de teletransporte.
- **Protecciones**: consulta el gestor de protecciones de Slimefun para respetar ProtectionStones y regiones del servidor.
- **Gravity Gun**: levanta o lanza entidades y bloques dentro de los permisos aplicables.

## Uso

1. Fabrica Portal Fluid y la Portal Gun en la Magic Workbench.
2. Deja Portal Fluid en la mano secundaria y usa la pistola para recargarla.
3. Usa la pistola normalmente para disparar portales enlazados.
4. Usa Shift + clic derecho para cambiar de modo, guardar ubicaciones o seleccionar un destino.
5. En modo Saved, usa la pistola para viajar al destino seleccionado.

## Seguridad y compatibilidad

- Requiere **Slimefun4-Drake**.
- No modifica bloques al crear ni usar un portal; por ello no interfiere con FastAsyncWorldEdit.
- La colocación exige permiso de romper el bloque de anclaje y colocar en el espacio del portal.
- El tránsito exige permiso de interacción en origen y destino.
- Las ubicaciones guardadas deben seguir siendo seguras y permitidas cuando se usan.

## Compilar

    mvn clean package

El JAR queda en target/sfportalguns-extension-1.1.0-SNAPSHOT.jar.

## Instalación

1. Detén el servidor de forma segura.
2. Conserva una copia del JAR anterior.
3. Elimina el JAR anterior de PortalGun para evitar dos plugins cargados.
4. Copia el nuevo JAR a plugins junto con Slimefun4-Drake.
5. Inicia el servidor y comprueba la carga antes de entregarlo a jugadores.

## Permisos

| Permiso | Uso |
|---|---|
| sfportalguns.infinite | Activa el modo de carga infinita. Por defecto, sólo OP. |

## Créditos

Código original de officialdakari. Port y mantenimiento para DrakesCraft por DrakesCraft Labs.

Licencia: GPL-3.0-only.
