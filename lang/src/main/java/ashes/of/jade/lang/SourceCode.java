package ashes.of.jade.lang;


public class SourceCode {

    private final String source;
    private int index = 0;
    private int line = 1;
    private int offset = 1;
    private int newLine = 0;


    public SourceCode(String source) {
        this.source = source;
    }

    public boolean isEnough(int len) {
        return index + len < source.length();
    }

    public char getChar() {
        return source.charAt(index);
    }

    public String getString(int len) {
        return getString(index, len);
    }

    public String getString(int index, int len) {
        return source.substring(index, index + len);
    }

    public void step() {
        step(1);
    }

    public void step(int len) {
        index += len;
        offset += len;
    }

    public boolean isEOF() {
        return remains() < 1;
    }

    public int remains() {
        return source.length() - index;
    }


    public void newLine() {
        line++;
        offset = 1;
        newLine = index;
    }

    public int getCurrentPosition() {
        int i;
        for (i = index; source.charAt(i) != '\n' && i > 0; i--) ;

        return index - i;
    }

    public int getCurrentLineNumber() {
        return 1;
    }

    public String getCurrentLineSource() {
        int i;
        for (i = index; source.charAt(i) != '\n' && i < source.length(); i++) ;


        return source.substring(0, i);
    }


    public int getIndex() {
        return index;
    }


    public Location getLocation() {
        return new Location(line, offset);
    }


    public boolean isWhitespace() {
        return !isEOF() && Character.isWhitespace(getChar());
    }

    public boolean isNewLine() {
        return getChar() == '\n';
    }

    public boolean isLetter() {
        return !isEOF() && Character.isLetter(getChar());
    }

    public boolean isDigit() {
        return Character.isDigit(getChar());
    }

    public boolean isOperator() {
        return isPlus() ||  isMinus() || isBackSlash() || isStar();
    }

    public boolean isPlus() {
        return getChar() == '+';
    }

    private boolean isMinus() {
        return getChar() == '-';
    }

    private boolean isBackSlash() {
        return getChar() == '/';
    }

    private boolean isStar() {
        return getChar() == '*';
    }


    public boolean isAssign() {
        return getChar() == '=';
    }

    public boolean isComma() {
        return getChar() == ',';
    }

    public boolean isDot() {
        return getChar() == '.';
    }

    public boolean isParentOpen() {
        return getChar() == '(';
    }

    public boolean isParentClose() {
        return getChar() == ')';
    }

    public boolean isCurlyOpen() {
        return getChar() == '{';
    }

    public boolean isCurlyClose() {
        return getChar() == '}';
    }


    public String getLine() {
        return source.substring(newLine, index);
    }

    public String getSource() {
        return source;
    }

    @Override
    public String toString() {
        return source;
    }
}
