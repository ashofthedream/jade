package ashes.of.jade.lang.parser;

import ashes.of.jade.lang.Location;


public class ParseException extends RuntimeException {

    private final Location location;

    public ParseException(Location location, String message, Object... args) {
        super(String.format(message, args));
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }
}
