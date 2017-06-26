package ashes.of.jade.lang.interpreter;

import ashes.of.jade.lang.Location;
import ashes.of.jade.lang.parser.ParseException;

public class EvalException extends ParseException {
    public EvalException(String message, Location location) {
        super(message, "", location);
    }
}
