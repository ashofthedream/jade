package ashes.of.jade.lang;

public class Location {

    public static final Location EMPTY = new Location(0, 0, 0);

    private final int index;
    private final int line;
    private final int offset;

    public Location(int index, int line, int offset) {
        this.index = index;
        this.line = line;
        this.offset = offset;
    }

    public int getIndex() {
        return index;
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
        return line + ":" + offset;
    }
}
