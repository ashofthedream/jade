package ashes.of.jade.lang;

public class Location {

    public static final Location EMPTY = new Location(0, 0, 0);

    private final int index;
    private final int length;

    private final int line;
    private final int offset;

    public Location(int index, int length, int line, int offset) {
        this.index = index;
        this.length = length;
        this.line = line;
        this.offset = offset;
    }

    public Location(int index, int line, int offset) {
        this(index, 1, line, offset);
    }

    public Location withLength(int length) {
        return new Location(index, length, line, offset);
    }

    public int getStart() {
        return index;
    }

    public int getEnd() {
        return index + length;
    }

    public int getLength() {
        return length;
    }

    public int getLine() {
        return line;
    }

    public int getOffset() {
        return offset;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Location location = (Location) o;

        return  index == location.index &&
                line == location.line &&
                offset == location.offset;
    }

    @Override
    public int hashCode() {
        return 31 * (31 * index + line) + offset;
    }

    @Override
    public String toString() {
        return line + ":" + offset + "(" + index + ")";
    }
}
