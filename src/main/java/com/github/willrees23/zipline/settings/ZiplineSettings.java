package com.github.willrees23.zipline.settings;

import com.github.willrees23.zipline.path.PathType;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;

/**
 * The tunable behaviour of a single zipline.
 *
 * <p>Values are read and written through {@link ZiplineOption}, which owns the parsing rules and
 * the permitted range of each field.
 */
@Getter
@Setter
public class ZiplineSettings {

    /**
     * Minecraft clamps playback pitch to this range, so there is no point accepting more.
     */
    public static final double MIN_PITCH = 0.5;
    public static final double MAX_PITCH = 2.0;

    private static final double SPEED_SCALE = 0.4;

    private double speed = 1.0;
    private PathType pathType = PathType.BLOCK;
    private Material material = Material.OAK_FENCE;
    private Particle pathParticle = Particle.CLOUD;
    private Particle endpointParticle = Particle.END_ROD;
    private TriggerMode trigger = TriggerMode.WALK;
    private MovementMode movementMode = MovementMode.MOUNTED;
    private ExitMode exitMode = ExitMode.DROP;
    private double launchPower = 1.5;
    private Sound rideSound = Sound.BLOCK_NOTE_BLOCK_BASS;
    private double rideSoundVolume = 0.5;
    private int rideSoundInterval = 2;
    private double rideSoundPitchStart = 0.5;
    private double rideSoundPitchEnd = 2.0;
    private Sound endSound = Sound.ENTITY_ENDER_DRAGON_FLAP;
    private double endSoundVolume = 1.0;
    private double endSoundPitch = 1.0;
    private boolean seat = true;
    private Material seatMaterial = Material.OAK_SLAB;
    private double seatScale = 0.8;
    private double seatOffset = 0.0;
    private boolean fallDamage = false;
    private boolean sneakExit = false;

    /**
     * Converts the player-facing speed into the distance covered per server tick.
     */
    public double getBlocksPerTick() {
        return speed * SPEED_SCALE;
    }

    /**
     * Interpolates the ride pitch across the length of the ride, so the sound rises as you travel.
     */
    public double ridePitch(double progress) {
        double clamped = Math.max(0, Math.min(1, progress));
        return rideSoundPitchStart + (rideSoundPitchEnd - rideSoundPitchStart) * clamped;
    }

    public ZiplineSettings copy() {
        ZiplineSettings copy = new ZiplineSettings();
        for (ZiplineOption option : ZiplineOption.values()) {
            option.copy(this, copy);
        }
        return copy;
    }
}
