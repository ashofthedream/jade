package ashes.of.jade.lang.parser;

import ashes.of.jade.lang.Location;


public class ParseException extends RuntimeException {

    private final Location location;
    private final String content;

    public ParseException(Location location, String message, Object... args) {
        this("", location, message, args);
    }

    public ParseException(String content, Location location, String message, Object... args) {
        super(String.format(message, args));
        this.content = content;
        this.location = location;

    }

    public String getContent() {
        return content;
    }

    public Location getLocation() {
        return location;
    }
}
