package ashes.of.jade.lang;



public class ParseException extends RuntimeException {

    private final String code;
    private final Location location;

    public ParseException(String message, SourceCode it) {
        super(message);
        this.code = it.getCurrentLineSource();
        this.location = it.getLocation();
    }

    public ParseException(String message, String code, Location location) {
        super(message);
        this.code = code;
        this.location = location;
    }

    public String getCode() {
        return code;
    }
    public Location getLocation() {
        return location;
    }
}
