package ashes.of.jade.lang.interpreter;

import ashes.of.jade.lang.Location;
import ashes.of.jade.lang.parser.ParseException;

public class EvalException extends ParseException {

    public EvalException(String content, Location location, String message, Object... args) {
        super(content, location, message, args);
    }

    public EvalException(Location location, String message, Object... args) {
        super(location, message, args);
    }
}
