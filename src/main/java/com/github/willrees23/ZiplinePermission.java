package com.github.willrees23;

/**
 * The permission nodes declared in {@code plugin.yml}.
 */
public enum ZiplinePermission {

    ZIPLINE_START(Node.START),
    ZIPLINE_END(Node.END),
    ZIPLINE_DELETE(Node.DELETE),
    ZIPLINE_EDIT(Node.EDIT),
    ZIPLINE_USE(Node.USE),
    ZIPLINE_LIST(Node.LIST);

    /**
     * The same nodes again, as compile-time constants.
     *
     * <p>The command classes state the permission they need in an annotation, and an annotation can
     * only be given a constant, so they name these rather than the enum constants above.
     */
    public static final class Node {

        public static final String START = "ziplines.start";
        public static final String END = "ziplines.end";
        public static final String DELETE = "ziplines.delete";
        public static final String EDIT = "ziplines.edit";
        public static final String USE = "ziplines.use";
        public static final String LIST = "ziplines.list";

        private Node() {
        }
    }

    private final String node;

    ZiplinePermission(String node) {
        this.node = node;
    }

    public String getNode() {
        return node;
    }
}
