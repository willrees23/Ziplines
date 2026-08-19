package com.github.willrees23.zipline.settings;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;

import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Binds one {@link ZiplineOption} to the {@link ZiplineSettings} field it controls, so that the
 * option can be read, written and copied without the caller knowing the field's type.
 *
 * <p>The type parameter never appears in the public signatures, which lets {@link ZiplineOption}
 * hold a heterogeneous {@code Setting<?>} without casting.
 *
 * @param <T> the type of the underlying settings field
 */
final class Setting<T> {

    private final Function<String, T> parser;
    private final Function<T, String> printer;
    private final Function<ZiplineSettings, T> reader;
    private final BiConsumer<ZiplineSettings, T> writer;
    private final boolean nullable;

    private Setting(Function<String, T> parser,
                    Function<T, String> printer,
                    Function<ZiplineSettings, T> reader,
                    BiConsumer<ZiplineSettings, T> writer,
                    boolean nullable) {
        this.parser = parser;
        this.printer = printer;
        this.reader = reader;
        this.writer = writer;
        this.nullable = nullable;
    }

    /** Returns the current value formatted the way {@link #write} expects to read it back. */
    String read(ZiplineSettings settings) {
        T value = reader.apply(settings);
        return value == null ? ZiplineOption.NONE : printer.apply(value);
    }

    /** Parses {@code raw} and stores it, returning {@code false} if the value was rejected. */
    boolean write(ZiplineSettings settings, String raw) {
        if (nullable && raw.equalsIgnoreCase(ZiplineOption.NONE)) {
            writer.accept(settings, null);
            return true;
        }
        T value = parser.apply(raw);
        if (value == null) {
            return false;
        }
        writer.accept(settings, value);
        return true;
    }

    /** Transfers the value between two settings objects without going through text. */
    void copy(ZiplineSettings from, ZiplineSettings to) {
        writer.accept(to, reader.apply(from));
    }

    static Setting<Double> number(double minimum,
                                  double maximum,
                                  Function<ZiplineSettings, Double> reader,
                                  BiConsumer<ZiplineSettings, Double> writer) {
        return new Setting<>(raw -> {
            Double parsed = parseDouble(raw);
            return parsed != null && parsed >= minimum && parsed <= maximum ? parsed : null;
        }, String::valueOf, reader, writer, false);
    }

    static Setting<Integer> count(int minimum,
                                  int maximum,
                                  Function<ZiplineSettings, Integer> reader,
                                  BiConsumer<ZiplineSettings, Integer> writer) {
        return new Setting<>(raw -> {
            Integer parsed = parseInt(raw);
            return parsed != null && parsed >= minimum && parsed <= maximum ? parsed : null;
        }, String::valueOf, reader, writer, false);
    }

    static Setting<Boolean> flag(Function<ZiplineSettings, Boolean> reader,
                                 BiConsumer<ZiplineSettings, Boolean> writer) {
        return new Setting<>(raw -> {
            if (raw.equalsIgnoreCase("true")) {
                return Boolean.TRUE;
            }
            return raw.equalsIgnoreCase("false") ? Boolean.FALSE : null;
        }, String::valueOf, reader, writer, false);
    }

    static <E extends Enum<E>> Setting<E> constant(Class<E> type,
                                                   Function<ZiplineSettings, E> reader,
                                                   BiConsumer<ZiplineSettings, E> writer) {
        return new Setting<>(raw -> parseConstant(type, raw), Enum::name, reader, writer, false);
    }

    /** Accepts any material that can be placed as a block. */
    static Setting<Material> block(Function<ZiplineSettings, Material> reader,
                                   BiConsumer<ZiplineSettings, Material> writer) {
        return new Setting<>(raw -> {
            Material material = Material.matchMaterial(raw);
            return material != null && material.isBlock() ? material : null;
        }, Material::name, reader, writer, false);
    }

    /**
     * Accepts only particles that carry no extra data, since those are the ones that can be spawned
     * from a bare name without the caller supplying a colour, block state or item stack.
     */
    static Setting<Particle> particle(Function<ZiplineSettings, Particle> reader,
                                      BiConsumer<ZiplineSettings, Particle> writer) {
        return new Setting<>(raw -> {
            Particle particle = parseConstant(Particle.class, raw);
            return particle != null && particle.getDataType() == Void.class ? particle : null;
        }, Particle::name, reader, writer, false);
    }

    /** Accepts a sound name or {@code NONE} to silence it. */
    static Setting<Sound> sound(Function<ZiplineSettings, Sound> reader,
                                BiConsumer<ZiplineSettings, Sound> writer) {
        return new Setting<>(Sounds::parse, Sounds::name, reader, writer, true);
    }

    private static <E extends Enum<E>> E parseConstant(Class<E> type, String raw) {
        try {
            return Enum.valueOf(type, raw.toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static Double parseDouble(String raw) {
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Integer parseInt(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
