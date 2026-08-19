package com.github.willrees23.zipline.seat;

import com.github.willrees23.zipline.settings.ZiplineSettings;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * Spawns the entities a rider sits on, and recognises them again afterwards.
 *
 * <p>A seat is two entities: an invisible armour stand that is moved along the line and carries the
 * player, and an optional block display that gives the rider something visible to sit on. Both are
 * tagged in their persistent data so that listeners can tell a seat apart from an armour stand that
 * happens to belong to the server owner, and neither is saved with the chunk.
 */
public class SeatFactory {

    private static final String KEY = "seat";
    private static final String SLAB_SUFFIX = "_SLAB";
    private static final double SLAB_HEIGHT = 0.5;
    private static final double BLOCK_HEIGHT = 1.0;

    private final NamespacedKey key;

    public SeatFactory(Plugin plugin) {
        this.key = new NamespacedKey(plugin, KEY);
    }

    public boolean isSeat(Entity entity) {
        return entity.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    public BlockDisplay spawnDisplay(Location location, ZiplineSettings settings) {
        return location.getWorld().spawn(location, BlockDisplay.class, display -> {
            display.setBlock(settings.getSeatMaterial().createBlockData());
            display.setTransformation(transformation(settings));
            display.setPersistent(false);
            display.setInvulnerable(true);
            tag(display);
        });
    }

    public ArmorStand spawnVehicle(Location location) {
        return location.getWorld().spawn(location, ArmorStand.class, stand -> {
            stand.setVisible(false);
            stand.setInvisible(true);
            stand.setSmall(true);
            stand.setMarker(false);
            stand.setBasePlate(false);
            stand.setArms(false);
            stand.setGravity(true);
            weightless(stand);
            stand.setInvulnerable(true);
            stand.setSilent(true);
            stand.setCollidable(false);
            stand.setPersistent(false);
            tag(stand);
        });
    }

    private void tag(Entity entity) {
        entity.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
    }

    /**
     * Leaves gravity switched on but with no strength, so the stand still counts as a moving vehicle
     * while the ride drives its velocity directly.
     */
    private void weightless(ArmorStand stand) {
        AttributeInstance gravity = stand.getAttribute(Attribute.GRAVITY);
        if (gravity != null) {
            gravity.setBaseValue(0);
        }
    }

    /**
     * Centres the block under the rider and drops it by its own height, so the top face rather than
     * the bottom corner sits where the seat is meant to be.
     */
    private Transformation transformation(ZiplineSettings settings) {
        float scale = (float) settings.getSeatScale();
        double height = settings.getSeatMaterial().name().endsWith(SLAB_SUFFIX) ? SLAB_HEIGHT : BLOCK_HEIGHT;
        float vertical = (float) (settings.getSeatOffset() - height * scale);
        return new Transformation(
                new Vector3f(-scale / 2, vertical, -scale / 2),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f(0, 0, 0, 1));
    }
}
