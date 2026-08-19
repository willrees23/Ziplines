package com.github.willrees23.enums;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public enum ZiplineOption {

    SPEED("speed"),
    PATH_TYPE("path-type"),
    MATERIAL("material"),
    PATH_PARTICLE("path-particle"),
    ENDPOINT_PARTICLE("endpoint-particle"),
    TRIGGER("trigger"),
    MOVEMENT_MODE("movement-mode"),
    EXIT_MODE("exit-mode"),
    LAUNCH_POWER("launch-power"),
    RIDE_SOUND("ride-sound"),
    RIDE_SOUND_VOLUME("ride-sound-volume"),
    RIDE_SOUND_INTERVAL("ride-sound-interval"),
    RIDE_SOUND_PITCH_START("ride-sound-pitch-start"),
    RIDE_SOUND_PITCH_END("ride-sound-pitch-end"),
    END_SOUND("end-sound"),
    END_SOUND_VOLUME("end-sound-volume"),
    END_SOUND_PITCH("end-sound-pitch"),
    SEAT("seat"),
    SEAT_MATERIAL("seat-material"),
    SEAT_SCALE("seat-scale"),
    SEAT_OFFSET("seat-offset"),
    FALL_DAMAGE("fall-damage"),
    SNEAK_EXIT("sneak-exit");

    private static final List<String> BOOLEANS = List.of("true", "false");
    private static final String SLAB_SUFFIX = "_SLAB";

    public static final String NONE = "NONE";

    private static List<String> blockMaterials;
    private static List<String> slabMaterials;
    private static List<String> plainParticles;
    private static List<String> sounds;

    private final String key;

    ZiplineOption(String key) {
        this.key = key;
    }

    public static ZiplineOption fromKey(String key) {
        for (ZiplineOption option : values()) {
            if (option.key.equalsIgnoreCase(key)) {
                return option;
            }
        }
        return null;
    }

    public static List<String> keys() {
        return Arrays.stream(values()).map(ZiplineOption::getKey).toList();
    }

    public List<String> getSuggestions() {
        return switch (this) {
            case SPEED -> List.of("0.5", "1.0", "1.5");
            case PATH_TYPE -> names(PathType.values());
            case MATERIAL -> blockMaterials();
            case PATH_PARTICLE, ENDPOINT_PARTICLE -> plainParticles();
            case TRIGGER -> names(TriggerMode.values());
            case MOVEMENT_MODE -> names(MovementMode.values());
            case EXIT_MODE -> names(ExitMode.values());
            case LAUNCH_POWER -> List.of("1.0", "2.0", "4.0");
            case RIDE_SOUND, END_SOUND -> sounds();
            case RIDE_SOUND_VOLUME, END_SOUND_VOLUME -> List.of("0.25", "0.5", "1.0");
            case RIDE_SOUND_INTERVAL -> List.of("2", "5", "10", "20");
            case RIDE_SOUND_PITCH_START, RIDE_SOUND_PITCH_END, END_SOUND_PITCH -> List.of("0.5", "1.0", "1.5", "2.0");
            case SEAT_MATERIAL -> slabMaterials();
            case SEAT_SCALE -> List.of("0.6", "0.8", "1.0");
            case SEAT_OFFSET -> List.of("-0.25", "0.0", "0.25");
            case SEAT, FALL_DAMAGE, SNEAK_EXIT -> BOOLEANS;
        };
    }

    private static List<String> names(Enum<?>[] constants) {
        return Arrays.stream(constants).map(Enum::name).toList();
    }

    private static List<String> blockMaterials() {
        if (blockMaterials == null) {
            List<String> names = new ArrayList<>();
            for (Material material : Material.values()) {
                if (material.isBlock() && !material.isLegacy()) {
                    names.add(material.name());
                }
            }
            blockMaterials = List.copyOf(names);
        }
        return blockMaterials;
    }

    private static List<String> slabMaterials() {
        if (slabMaterials == null) {
            List<String> names = new ArrayList<>();
            for (String name : blockMaterials()) {
                if (name.endsWith(SLAB_SUFFIX)) {
                    names.add(name);
                }
            }
            slabMaterials = List.copyOf(names);
        }
        return slabMaterials;
    }

    private static List<String> sounds() {
        if (sounds == null) {
            List<String> names = new ArrayList<>();
            names.add(NONE);
            for (Sound sound : Sound.values()) {
                names.add(sound.name());
            }
            sounds = List.copyOf(names);
        }
        return sounds;
    }

    private static List<String> plainParticles() {
        if (plainParticles == null) {
            List<String> names = new ArrayList<>();
            for (Particle particle : Particle.values()) {
                if (particle.getDataType() == Void.class) {
                    names.add(particle.name());
                }
            }
            plainParticles = List.copyOf(names);
        }
        return plainParticles;
    }
}
