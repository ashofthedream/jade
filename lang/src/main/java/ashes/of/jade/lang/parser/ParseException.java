package ashes.of.jade.lang.parser;


import ashes.of.jade.lang.Location;
import ashes.of.jade.lang.SourceCode;

public class ParseException extends RuntimeException {

    private final String line;
    private final Location location;

    public ParseException(String message, SourceCode it) {
        super(message);
        this.line = it.getLineToIndex();
        this.location = it.getLocation();
    }

    public ParseException(String message, String code, Location location) {
        super(message);
        this.line = code;
        this.location = location;
    }

    public String getLine() {
        return line;
    }

    public Location getLocation() {
        return location;
    }
}
