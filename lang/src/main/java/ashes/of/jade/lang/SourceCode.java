package ashes.of.jade.lang;


public class SourceCode {

    private final String source;
    private int index = 0;
    private int line = 1;
    private int offset = 1;


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

    public String getSource() {
        return source;
    }



    @Override
    public String toString() {
        return source;
    }
}
