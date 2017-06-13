package ashes.of.jade.lang;

class ParseException extends RuntimeException {

    private final String code;
    private final int line;
    private final int position;

    public ParseException(String message, SourceCode it) {
        super(message);
        this.code = it.getCurrentLineSource();
        this.line = it.getCurrentLineNumber();
        this.position = it.getCurrentPosition();
    }

    public String getCode() {
        return code;
    }

    public int getLine() {
        return line;
    }

    public int getPosition() {
        return position;
    }
}
