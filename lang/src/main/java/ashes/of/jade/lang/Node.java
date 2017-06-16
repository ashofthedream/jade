package ashes.of.jade.lang;


public interface Node {

    default void setLocation(Location location) {
    }

    default Location getLocation() {
        return null;
    }

    default LexemType getType() {
        return null;
    }

    default boolean is(LexemType type) {
        return type == getType();
    }

    default void add(Node node) {

    }

    default boolean isSeq() {
        return false;
    }

    default boolean isString() {
        return false;
    }

    default boolean isDouble() {
        return false;
    }

    default boolean isInteger() {
        return false;
    }

    default Object asSeq() {
        return null;
    }

    default String asString() {
        return null;
    }

    default long asInteger() {
        return 0;
    }

    default double asDouble() {
        return 0;
    }

    default Node eval() {
        return null;
    }
}