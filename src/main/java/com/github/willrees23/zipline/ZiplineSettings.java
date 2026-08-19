package com.github.willrees23.zipline;

import com.github.willrees23.enums.ExitMode;
import com.github.willrees23.enums.MovementMode;
import com.github.willrees23.enums.PathType;
import com.github.willrees23.enums.TriggerMode;
import com.github.willrees23.enums.ZiplineOption;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;

@Getter
@Setter
public class ZiplineSettings {

    private static final double SPEED_SCALE = 0.4;
    private static final double MIN_PITCH = 0.5;
    private static final double MAX_PITCH = 2.0;

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

    public double getBlocksPerTick() {
        return speed * SPEED_SCALE;
    }

    public double ridePitch(double progress) {
        double clamped = Math.max(0, Math.min(1, progress));
        return rideSoundPitchStart + (rideSoundPitchEnd - rideSoundPitchStart) * clamped;
    }

    public ZiplineSettings copy() {
        ZiplineSettings copy = new ZiplineSettings();
        for (ZiplineOption option : ZiplineOption.values()) {
            copy.set(option, get(option));
        }
        return copy;
    }

    public String get(ZiplineOption option) {
        return switch (option) {
            case SPEED -> String.valueOf(speed);
            case PATH_TYPE -> pathType.name();
            case MATERIAL -> material.name();
            case PATH_PARTICLE -> pathParticle.name();
            case ENDPOINT_PARTICLE -> endpointParticle.name();
            case TRIGGER -> trigger.name();
            case MOVEMENT_MODE -> movementMode.name();
            case EXIT_MODE -> exitMode.name();
            case LAUNCH_POWER -> String.valueOf(launchPower);
            case RIDE_SOUND -> rideSound == null ? ZiplineOption.NONE : rideSound.name();
            case RIDE_SOUND_VOLUME -> String.valueOf(rideSoundVolume);
            case RIDE_SOUND_INTERVAL -> String.valueOf(rideSoundInterval);
            case RIDE_SOUND_PITCH_START -> String.valueOf(rideSoundPitchStart);
            case RIDE_SOUND_PITCH_END -> String.valueOf(rideSoundPitchEnd);
            case END_SOUND -> endSound == null ? ZiplineOption.NONE : endSound.name();
            case END_SOUND_VOLUME -> String.valueOf(endSoundVolume);
            case END_SOUND_PITCH -> String.valueOf(endSoundPitch);
            case SEAT -> String.valueOf(seat);
            case SEAT_MATERIAL -> seatMaterial.name();
            case SEAT_SCALE -> String.valueOf(seatScale);
            case SEAT_OFFSET -> String.valueOf(seatOffset);
            case FALL_DAMAGE -> String.valueOf(fallDamage);
            case SNEAK_EXIT -> String.valueOf(sneakExit);
        };
    }

    public boolean set(ZiplineOption option, String value) {
        switch (option) {
            case SPEED -> {
                Double parsed = parseRange(value, 0.01, 10);
                if (parsed == null) {
                    return false;
                }
                speed = parsed;
                return true;
            }
            case PATH_TYPE -> {
                PathType parsed = parseEnum(PathType.class, value);
                if (parsed == null) {
                    return false;
                }
                pathType = parsed;
                return true;
            }
            case MATERIAL -> {
                Material parsed = parseBlock(value);
                if (parsed == null) {
                    return false;
                }
                material = parsed;
                return true;
            }
            case PATH_PARTICLE -> {
                Particle parsed = parseParticle(value);
                if (parsed == null) {
                    return false;
                }
                pathParticle = parsed;
                return true;
            }
            case ENDPOINT_PARTICLE -> {
                Particle parsed = parseParticle(value);
                if (parsed == null) {
                    return false;
                }
                endpointParticle = parsed;
                return true;
            }
            case TRIGGER -> {
                TriggerMode parsed = parseEnum(TriggerMode.class, value);
                if (parsed == null) {
                    return false;
                }
                trigger = parsed;
                return true;
            }
            case MOVEMENT_MODE -> {
                MovementMode parsed = parseEnum(MovementMode.class, value);
                if (parsed == null) {
                    return false;
                }
                movementMode = parsed;
                return true;
            }
            case EXIT_MODE -> {
                ExitMode parsed = parseEnum(ExitMode.class, value);
                if (parsed == null) {
                    return false;
                }
                exitMode = parsed;
                return true;
            }
            case LAUNCH_POWER -> {
                Double parsed = parseRange(value, 0, 10);
                if (parsed == null) {
                    return false;
                }
                launchPower = parsed;
                return true;
            }
            case RIDE_SOUND -> {
                if (value.equalsIgnoreCase(ZiplineOption.NONE)) {
                    rideSound = null;
                    return true;
                }
                Sound parsed = parseSound(value);
                if (parsed == null) {
                    return false;
                }
                rideSound = parsed;
                return true;
            }
            case RIDE_SOUND_VOLUME -> {
                Double parsed = parseRange(value, 0, 2);
                if (parsed == null) {
                    return false;
                }
                rideSoundVolume = parsed;
                return true;
            }
            case RIDE_SOUND_INTERVAL -> {
                Integer parsed = parseInterval(value);
                if (parsed == null) {
                    return false;
                }
                rideSoundInterval = parsed;
                return true;
            }
            case RIDE_SOUND_PITCH_START -> {
                Double parsed = parseRange(value, MIN_PITCH, MAX_PITCH);
                if (parsed == null) {
                    return false;
                }
                rideSoundPitchStart = parsed;
                return true;
            }
            case RIDE_SOUND_PITCH_END -> {
                Double parsed = parseRange(value, MIN_PITCH, MAX_PITCH);
                if (parsed == null) {
                    return false;
                }
                rideSoundPitchEnd = parsed;
                return true;
            }
            case END_SOUND -> {
                if (value.equalsIgnoreCase(ZiplineOption.NONE)) {
                    endSound = null;
                    return true;
                }
                Sound parsed = parseSound(value);
                if (parsed == null) {
                    return false;
                }
                endSound = parsed;
                return true;
            }
            case END_SOUND_VOLUME -> {
                Double parsed = parseRange(value, 0, 2);
                if (parsed == null) {
                    return false;
                }
                endSoundVolume = parsed;
                return true;
            }
            case END_SOUND_PITCH -> {
                Double parsed = parseRange(value, MIN_PITCH, MAX_PITCH);
                if (parsed == null) {
                    return false;
                }
                endSoundPitch = parsed;
                return true;
            }
            case SEAT -> {
                Boolean parsed = parseBoolean(value);
                if (parsed == null) {
                    return false;
                }
                seat = parsed;
                return true;
            }
            case SEAT_MATERIAL -> {
                Material parsed = parseBlock(value);
                if (parsed == null) {
                    return false;
                }
                seatMaterial = parsed;
                return true;
            }
            case SEAT_SCALE -> {
                Double parsed = parseRange(value, 0.05, 2);
                if (parsed == null) {
                    return false;
                }
                seatScale = parsed;
                return true;
            }
            case SEAT_OFFSET -> {
                Double parsed = parseRange(value, -2, 2);
                if (parsed == null) {
                    return false;
                }
                seatOffset = parsed;
                return true;
            }
            case FALL_DAMAGE -> {
                Boolean parsed = parseBoolean(value);
                if (parsed == null) {
                    return false;
                }
                fallDamage = parsed;
                return true;
            }
            case SNEAK_EXIT -> {
                Boolean parsed = parseBoolean(value);
                if (parsed == null) {
                    return false;
                }
                sneakExit = parsed;
                return true;
            }
        }
        return false;
    }

    private Double parseRange(String value, double minimum, double maximum) {
        try {
            double parsed = Double.parseDouble(value);
            return parsed >= minimum && parsed <= maximum ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Material parseBlock(String value) {
        Material parsed = Material.matchMaterial(value);
        return parsed != null && parsed.isBlock() ? parsed : null;
    }

    private Sound parseSound(String value) {
        try {
            return Sound.valueOf(value.toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private Integer parseInterval(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed >= 1 && parsed <= 100 ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Boolean parseBoolean(String value) {
        if (value.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
        }
        if (value.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
        }
        return null;
    }

    private Particle parseParticle(String value) {
        Particle parsed = parseEnum(Particle.class, value);
        return parsed != null && parsed.getDataType() == Void.class ? parsed : null;
    }

    private <T extends Enum<T>> T parseEnum(Class<T> type, String value) {
        try {
            return Enum.valueOf(type, value.toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
