package ashes.of.jade.lang.lexer;


import ashes.of.jade.lang.Location;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;



public class SourceCode {
    private static final Logger log = LogManager.getLogger(SourceCode.class);

    /**
     * Source code
     */
    private final String source;

    /**
     * Parsed lexems
     */
    private final List<Lexem> lexems = new ArrayList<>();

    /**
     * Current index
     */
    private int index = 0;

    /**
     * Current line number
     */
    private int line = 1;

    /**
     * Symbol index in current lint
     */
    private int offset = 1;

    /**
     * Index of last new line symbol
     */
    private int newLine = 0;


    public SourceCode(String source) {
        this.source = source;
    }

    public char getChar() {
        return source.charAt(index);
    }

    public String getString(int index, int len) {
        return source.substring(index, index + len);
    }

    public String getString(int len) {
        return getString(index, len);
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


    public String getLineToEnd() {
        int i;
        for (i = index; i < source.length() && source.charAt(i) != '\n'; i++) ;


        return source.substring(index, i);
    }

    public String getLineToIndex() {
        return source.substring(newLine, Math.min(index, source.length()));
    }

    public int getIndex() {
        return index;
    }


    public Location getLocation() {
        return new Location(index, line, offset);
    }


    public boolean isDoubleQuote() {
        return getChar() == '"';
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
        return isPlus() || isMinus() || isBackSlash() || isStar() || isPower();
    }

    public boolean isPlus() {
        return getChar() == '+';
    }

    public boolean isMinus() {
        return getChar() == '-';
    }

    public boolean isBackSlash() {
        return getChar() == '/';
    }

    public boolean isStar() {
        return getChar() == '*';
    }

    public boolean isPower() {
        return getChar() == '^';
    }

    public boolean isEqual() {
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

    public boolean isArrow() {
        return getChar() == '>';
    }

    public String getSource() {
        return source;
    }


    public Lexem pop() {
        return lexems.remove(lexems.size() - 1);
    }

    public Lexem peek() {
        return lexems.get(lexems.size() - 1);
    }


    public void add(Lexem lexem) {
        log.info("add {}", lexem);
        lexems.add(lexem);
    }

    public void add(LexemType type, Location location, String content) {
        add(new Lexem(type, content.isEmpty() ? location : location.withLength(content.length()), content));
    }

    public void add(LexemType type, Location location) {
        add(type, location, "");
    }


    public List<Lexem> getLexems() {
        return lexems;
    }

    @Override
    public String toString() {
        return source;
    }
}
