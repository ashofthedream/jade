package ashes.of.jade.lang.lexer;


import ashes.of.jade.lang.Location;

public class Lexem {
    private final LexemType type;
    private final Location location;
    private final String content;

    public Lexem(LexemType type, Location location, String content) {
        this.type = type;
        this.location = location;
        this.content = content;
    }

    public Lexem(LexemType type, Location location) {
        this(type, location, "");
    }


    public Lexem(LexemType type) {
        this(type, new Location(0, 0), "");
    }

    public LexemType getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public Location getLocation() {
        return location;
    }

    public boolean is(LexemType type) {
        return this.type == type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Lexem lexem = (Lexem) o;

        if (type != lexem.type) return false;
        if (!location.equals(lexem.location)) return false;
        return content != null ? content.equals(lexem.content) : lexem.content == null;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + location.hashCode();
        result = 31 * result + (content != null ? content.hashCode() : 0);
        return result;
    }


    @Override
    public String toString() {
        return type + (content != null && !content.isEmpty() ?  "{" + content + "}" : "") + " " + location;
    }
}
