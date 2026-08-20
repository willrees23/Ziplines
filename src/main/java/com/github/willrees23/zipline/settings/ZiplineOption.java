package com.github.willrees23.zipline.settings;

import com.github.willrees23.zipline.path.PathType;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * The per-zipline settings that can be changed with {@code /zl edit} or defaulted in
 * {@code config.yml}.
 *
 * <p>Each constant owns everything the rest of the plugin needs to know about it: the key used in
 * commands and configuration, how to read and write the underlying field, and what to offer for tab
 * completion.
 */
public enum ZiplineOption {

    SPEED("speed",
            Setting.number(0.01, 10, ZiplineSettings::getSpeed, ZiplineSettings::setSpeed),
            Suggestions.of("0.5", "1.0", "1.5")),
    PATH_TYPE("path-type",
            Setting.constant(PathType.class, ZiplineSettings::getPathType, ZiplineSettings::setPathType),
            Suggestions.constants(PathType.values())),
    MATERIAL("material",
            Setting.block(ZiplineSettings::getMaterial, ZiplineSettings::setMaterial),
            Suggestions.blocks()),
    PATH_PARTICLE("path-particle",
            Setting.particle(ZiplineSettings::getPathParticle, ZiplineSettings::setPathParticle),
            Suggestions.particles()),
    ENDPOINT_PARTICLE("endpoint-particle",
            Setting.particle(ZiplineSettings::getEndpointParticle, ZiplineSettings::setEndpointParticle),
            Suggestions.particles()),
    TRIGGER("trigger",
            Setting.constant(TriggerMode.class, ZiplineSettings::getTrigger, ZiplineSettings::setTrigger),
            Suggestions.constants(TriggerMode.values())),
    DIRECTION("direction",
            Setting.constant(RideDirection.class, ZiplineSettings::getDirection, ZiplineSettings::setDirection),
            Suggestions.constants(RideDirection.values())),
    MOVEMENT_MODE("movement-mode",
            Setting.constant(MovementMode.class, ZiplineSettings::getMovementMode, ZiplineSettings::setMovementMode),
            Suggestions.constants(MovementMode.values())),
    EXIT_MODE("exit-mode",
            Setting.constant(ExitMode.class, ZiplineSettings::getExitMode, ZiplineSettings::setExitMode),
            Suggestions.constants(ExitMode.values())),
    LAUNCH_POWER("launch-power",
            Setting.number(0, 10, ZiplineSettings::getLaunchPower, ZiplineSettings::setLaunchPower),
            Suggestions.of("1.0", "2.0", "4.0")),
    MAX_RIDERS("max-riders",
            Setting.limit(1, 100, ZiplineSettings::getMaxRiders, ZiplineSettings::setMaxRiders),
            Suggestions.optional("1", "2", "4")),
    RIDE_SOUND("ride-sound",
            Setting.sound(ZiplineSettings::getRideSound, ZiplineSettings::setRideSound),
            Suggestions.sounds()),
    RIDE_SOUND_VOLUME("ride-sound-volume",
            Setting.number(0, 2, ZiplineSettings::getRideSoundVolume, ZiplineSettings::setRideSoundVolume),
            Suggestions.of("0.25", "0.5", "1.0")),
    RIDE_SOUND_INTERVAL("ride-sound-interval",
            Setting.count(1, 100, ZiplineSettings::getRideSoundInterval, ZiplineSettings::setRideSoundInterval),
            Suggestions.of("2", "5", "10", "20")),
    RIDE_SOUND_PITCH_START("ride-sound-pitch-start",
            Setting.number(ZiplineSettings.MIN_PITCH, ZiplineSettings.MAX_PITCH,
                    ZiplineSettings::getRideSoundPitchStart, ZiplineSettings::setRideSoundPitchStart),
            Suggestions.of("0.5", "1.0", "1.5", "2.0")),
    RIDE_SOUND_PITCH_END("ride-sound-pitch-end",
            Setting.number(ZiplineSettings.MIN_PITCH, ZiplineSettings.MAX_PITCH,
                    ZiplineSettings::getRideSoundPitchEnd, ZiplineSettings::setRideSoundPitchEnd),
            Suggestions.of("0.5", "1.0", "1.5", "2.0")),
    END_SOUND("end-sound",
            Setting.sound(ZiplineSettings::getEndSound, ZiplineSettings::setEndSound),
            Suggestions.sounds()),
    END_SOUND_VOLUME("end-sound-volume",
            Setting.number(0, 2, ZiplineSettings::getEndSoundVolume, ZiplineSettings::setEndSoundVolume),
            Suggestions.of("0.25", "0.5", "1.0")),
    END_SOUND_PITCH("end-sound-pitch",
            Setting.number(ZiplineSettings.MIN_PITCH, ZiplineSettings.MAX_PITCH,
                    ZiplineSettings::getEndSoundPitch, ZiplineSettings::setEndSoundPitch),
            Suggestions.of("0.5", "1.0", "1.5", "2.0")),
    SEAT("seat",
            Setting.flag(ZiplineSettings::isSeat, ZiplineSettings::setSeat),
            Suggestions.of("true", "false")),
    SEAT_MATERIAL("seat-material",
            Setting.block(ZiplineSettings::getSeatMaterial, ZiplineSettings::setSeatMaterial),
            Suggestions.slabs()),
    SEAT_SCALE("seat-scale",
            Setting.number(0.05, 2, ZiplineSettings::getSeatScale, ZiplineSettings::setSeatScale),
            Suggestions.of("0.6", "0.8", "1.0")),
    SEAT_OFFSET("seat-offset",
            Setting.number(-2, 2, ZiplineSettings::getSeatOffset, ZiplineSettings::setSeatOffset),
            Suggestions.of("-0.25", "0.0", "0.25")),
    SEAT_RETURN("seat-return",
            Setting.flag(ZiplineSettings::isSeatReturn, ZiplineSettings::setSeatReturn),
            Suggestions.of("true", "false")),
    FALL_DAMAGE("fall-damage",
            Setting.flag(ZiplineSettings::isFallDamage, ZiplineSettings::setFallDamage),
            Suggestions.of("true", "false")),
    SNEAK_EXIT("sneak-exit",
            Setting.flag(ZiplineSettings::isSneakExit, ZiplineSettings::setSneakExit),
            Suggestions.of("true", "false"));

    /**
     * The value that clears an optional setting, and what an unset setting reads back as.
     */
    public static final String NONE = "NONE";

    private static final List<String> KEYS = Arrays.stream(values()).map(ZiplineOption::getKey).toList();

    private final String key;
    private final Setting<?> setting;
    private final Supplier<List<String>> suggestions;

    ZiplineOption(String key, Setting<?> setting, Supplier<List<String>> suggestions) {
        this.key = key;
        this.setting = setting;
        this.suggestions = suggestions;
    }

    /**
     * Returns the option with the given key, or {@code null} if there is no such option.
     */
    public static ZiplineOption fromKey(String key) {
        for (ZiplineOption option : values()) {
            if (option.key.equalsIgnoreCase(key)) {
                return option;
            }
        }
        return null;
    }

    public static List<String> keys() {
        return KEYS;
    }

    public String getKey() {
        return key;
    }

    /**
     * Returns this option's current value, formatted so that {@link #write} accepts it back.
     */
    public String read(ZiplineSettings settings) {
        return setting.read(settings);
    }

    /**
     * Applies {@code value}, returning {@code false} if it was not valid for this option.
     */
    public boolean write(ZiplineSettings settings, String value) {
        return setting.write(settings, value);
    }

    /**
     * Copies this option's value from one settings object to another.
     */
    public void copy(ZiplineSettings from, ZiplineSettings to) {
        setting.copy(from, to);
    }

    public List<String> getSuggestions() {
        return suggestions.get();
    }
}
