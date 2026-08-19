package com.github.willrees23.zipline.seat;

import com.github.willrees23.ZiplinesPlugin;
import com.github.willrees23.zipline.ZiplineSettings;
import lombok.experimental.UtilityClass;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

@UtilityClass
public class SeatEntities {

    private final String KEY = "seat";
    private final String SLAB_SUFFIX = "_SLAB";
    private final double SLAB_HEIGHT = 0.5;
    private final double BLOCK_HEIGHT = 1.0;

    private NamespacedKey key;

    public NamespacedKey key() {
        if (key == null) {
            key = new NamespacedKey(ZiplinesPlugin.getInstance(), KEY);
        }
        return key;
    }

    public boolean isSeatEntity(Entity entity) {
        return entity.getPersistentDataContainer().has(key(), PersistentDataType.BYTE);
    }

    public BlockDisplay spawnDisplay(Location location, ZiplineSettings settings) {
        return location.getWorld().spawn(location, BlockDisplay.class, display -> {
            display.setBlock(settings.getSeatMaterial().createBlockData());
            display.setTransformation(transformation(settings));
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.getPersistentDataContainer().set(key(), PersistentDataType.BYTE, (byte) 1);
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
            stand.getPersistentDataContainer().set(key(), PersistentDataType.BYTE, (byte) 1);
        });
    }

    private void weightless(ArmorStand stand) {
        AttributeInstance gravity = stand.getAttribute(Attribute.GRAVITY);
        if (gravity != null) {
            gravity.setBaseValue(0);
        }
    }

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
