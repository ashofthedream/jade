package ashes.of.jade.lang;

public class Location {

    public static final Location EMPTY = new Location(0, 0, 0);

    public final int index;
    public final int line;
    public final int offset;

    public Location(int index, int line, int offset) {
        this.index = index;
        this.line = line;
        this.offset = offset;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Location location = (Location) o;

        return  //index == location.index &&
                line == location.line &&
                offset == location.offset;
    }

    @Override
    public int hashCode() {
        int result = line;
        result = 31 * result + offset;
        return result;
    }

    @Override
    public String toString() {
        return line + ":" + offset;
    }
}
