package ashes.of.jade.lang;


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

    public boolean is(LexemType... a) {
        for (LexemType type : a) {
            if (is(type))
                return true;
        }

        return false;
    }


    @Override
    public String toString() {
        return type + (content != null && !content.isEmpty() ?  "{" + content + "}" : "");
    }
}
