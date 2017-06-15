package ashes.of.jade.lang;

public class Location {
    public final int line;
    public final int offset;

    public Location(int line, int offset) {
        this.line = line;
        this.offset = offset;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Location location = (Location) o;

        if (line != location.line) return false;
        return offset == location.offset;
    }

    @Override
    public int hashCode() {
        int result = line;
        result = 31 * result + offset;
        return result;
    }

    @Override
    public String toString() {
        return "Location{" + line + ", " + offset + '}';
    }
}
