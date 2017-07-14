package ashes.of.jade.lang.lexer;

import ashes.of.jade.lang.Location;
import ashes.of.jade.lang.parser.ParseException;
import ashes.of.jade.lang.SourceCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.regex.Pattern;


public class Lexer {
    private static final Logger log = LogManager.getLogger(Lexer.class);


    private final static Pattern doublePattern = Pattern.compile("[+|-]?[0-9]+\\.[0-9]+");
    private final static Pattern integerPattern = Pattern.compile("[+|-]?[0-9]+");


    public List<Lexem> parse(String source) {
        return parse(new SourceCode(source));
    }

    /**
     * expr ::= expr op expr | (expr) | identifier | { expr, expr } | number | map(expr, identifier -> expr) | reduce(expr, expr, identifier identifier -> expr)
     * op ::= + | - | * | / | ^
     * stmt ::= var identifier = expr | out expr | print "string"
     * program ::= stmt | program stmt
     * <p>
     * <p>
     * <p>
     * var n = 500
     * var sequence = map({0, n}, i -> (-1)^i / (2 * i + 1))
     * var pi = 4 * reduce (sequence, 0, x y -> x + y)
     * print "pi = "
     * out pi
     */
    public List<Lexem> parse(SourceCode it) {
        log.debug("input:\n{}", it.getSource());

        List<Lexem> lexems = new ArrayList<>();

        while (!it.isEOF()) {
            Location loc = it.getLocation();
            if (it.isNewLine()) {
                add(lexems, LexemType.NL, loc);
                it.step();
                it.newLine();
                continue;
            }


            if (it.isWhitespace()) {
                it.step();
                continue;
            }

            log.trace("state: {} \u2192{}", it.getLineToIndex(), it.getLineToEnd());

            if (it.isLetter()) {
                parseLetters(it, lexems);
                continue;
            }

            if (it.isDoubleQuote()) {
                parseString(it, lexems);
                continue;
            }

            if (it.isDigit()) {
                parseNumber(it, lexems);
                continue;
            }

            if (it.isArrow()) {
                parseArrow(it, lexems);
                continue;
            }

            if (it.isOperator()) {
                parseOperator(it, lexems);
                continue;
            }

            if (it.isParentOpen()) {
                parseParentOpen(it, lexems, loc);
                continue;
            }

            if (it.isParentClose()) {
                parseParentClose(it, lexems, loc);
                continue;
            }

            if (it.isCurlyOpen()) {
                parseCurlyOpen(it, lexems, loc);
                continue;
            }

            if (it.isCurlyClose()) {
                parseCurlyClose(it, lexems, loc);
                continue;
            }

            if (it.isComma()) {
                parseComma(it, lexems, loc);
                continue;
            }


            if (it.isEqual()) {
                parseEqual(it, lexems, loc);
                continue;
            }

            throw new ParseException(loc, "Unexpected symbol '%s'", it.getChar());
        }

        add(lexems, LexemType.EOF, it.getLocation());

        return lexems;
    }



    private void parseLexemAndStep(SourceCode it, List<Lexem> lexems, LexemType type, Location loc) {
        checkIsNotNewLine(loc, lexems, "Symbol isn't allowed as first token");
        add(lexems, type, loc);
        it.step();
    }






    private void parseCurlyClose(SourceCode it, List<Lexem> lexems, Location loc) {
        parseLexemAndStep(it, lexems, LexemType.CURLY_CLOSE, loc);
    }

    private void parseCurlyOpen(SourceCode it, List<Lexem> lexems, Location loc) {
        checkIsNotMiddleOfExpr(loc, "", peek(lexems), "Sequence isn't allowed here");

        checkIsNotNewLine(loc, lexems, "Sequence isn't allowed as first token");
        add(lexems, LexemType.CURLY_OPEN, loc);
        it.step();
    }

    private void parseParentOpen(SourceCode it, List<Lexem> lexems, Location loc) {
        parseLexemAndStep(it, lexems, LexemType.PARENT_OPEN, loc);
    }

    private void parseParentClose(SourceCode it, List<Lexem> lexems, Location loc) {
        parseLexemAndStep(it, lexems, LexemType.PARENT_CLOSE, loc);
//
//        checkIsNotNewLine(loc, lexems, "Symbol ) isn't allowed as first token");
//        add(lexems, LexemType.PARENT_CLOSE, it.getLocation());
//        it.step();
    }

    private void parseArrow(SourceCode it, List<Lexem> lexems) {
        log.debug("Found symbol: {}, try parse as a Arrow", it.getChar());
        Lexem minus = pop(lexems);
        if (!minus.is(LexemType.MINUS))
            throw new ParseException(minus.getLocation(), "Expected -> but first char is ");

        parseLexemAndStep(it, lexems, LexemType.ARROW, minus.getLocation());
    }

    private void parseOperator(SourceCode it, List<Lexem> lexems) {
        checkIsNotNewLine(it, lexems, "Operator isn't allowed as first token");

        Location loc = it.getLocation();
        if (it.isPlus()) {
            add(lexems, LexemType.PLUS, loc);
            it.step();
            return;
        }

        if (it.isMinus()) {
            add(lexems, LexemType.MINUS, loc);
            it.step();
            return;
        }

        Lexem last = peek(lexems);
        if (isOperator(last))
            throw new ParseException(loc, "Unexpected operator %s", it.getLocation());

        if (it.isStar()) {
            add(lexems, LexemType.MULTIPLY, loc);
            it.step();
            return;
        }


        if (it.isBackSlash()) {
            add(lexems, LexemType.DIVIDE, loc);
            it.step();
            return;
        }

        if (it.isPower()) {
            add(lexems, LexemType.POWER, loc);
            it.step();
            return;
        }
    }

    private boolean isOperator(Lexem lexem) {
        return  lexem.is(LexemType.PLUS) ||
                lexem.is(LexemType.MINUS) ||
                lexem.is(LexemType.MULTIPLY) ||
                lexem.is(LexemType.DIVIDE) ||
                lexem.is(LexemType.POWER);
    }


    private void parseNumber(SourceCode it, List<Lexem> lexems) {
        log.debug("Digit found: {}, try parse as a Integer/Double", it.getChar());
        checkIsNotNewLine(it, lexems, "Number isn't allowed as first token");

        Location loc = it.getLocation();
        StringBuilder b = new StringBuilder();
        Lexem back1 = peek(lexems);

        if (back1.isAnyOf(LexemType.MINUS, LexemType.PLUS)) {
            log.trace("previous lexem is +/-, check that it isn't expr");
            Lexem back2 = lexems.get(lexems.size() - 2);
            if (back2.isAnyOf(LexemType.EQUAL, LexemType.ARROW, LexemType.PARENT_OPEN, LexemType.CURLY_OPEN,
                              LexemType.COMMA, LexemType.OUT,   LexemType.PLUS,        LexemType.MINUS,
                              LexemType.MULTIPLY, LexemType.DIVIDE, LexemType.POWER)) {
                loc = back1.getLocation();

                if (back1.is(LexemType.PLUS))
                    b.append("+");

                if (back1.is(LexemType.MINUS))
                    b.append("-");

                lexems.remove(peek(lexems));
            }
        }


        while (!it.isEOF() && (it.isDigit() || it.isDot())) {
            b.append(it.getChar());
            it.step(1);
        }

        String token = b.toString();
        checkIsNotMiddleOfExpr(loc, token, back1, "Number isn't allowed here");

        if (doublePattern.matcher(token).matches()) {
            add(lexems, LexemType.DOUBLE, loc, token);
            return;
        }

        if (integerPattern.matcher(token).matches()) {
            add(lexems, LexemType.INTEGER, loc, token);
            return;
        }

        throw new ParseException(loc, "Invalid number: %s", token);
    }


    private void parseLetters(SourceCode it, List<Lexem> lexems) {
        log.debug("Found letter: {}, try parse as a Identifier, Call", it.getChar());
        Location loc = it.getLocation();
        StringBuilder b = new StringBuilder();
        while (!it.isEOF() && it.isLetter()) {
            b.append(it.getChar());
            it.step();
        }

        String token = b.toString();
        log.debug("Found letters: '{}' at ", token, loc);
        switch (token) {
            case "print":
                checkIsNewLine(token, loc, lexems, "Print allowed only as statement start");
                add(lexems, LexemType.PRINT, loc, "print");
                break;

            case "out":
                checkIsNewLine(token, loc, lexems, "Out allowed only as statement start");
                add(lexems, LexemType.OUT, loc, "out");
                break;

            case "map":
                checkIsNotNewLine(token, loc, lexems, "Map isn't allowed as first token");
                add(lexems, LexemType.MAP, loc, "map");
                break;

            case "reduce":
                checkIsNotNewLine(token, loc, lexems, "Reduce isn't allowed as first token");
                add(lexems, LexemType.REDUCE, loc, "reduce");
                break;

            case "var":
                checkIsNewLine(token, loc, lexems, "Var allowed only as statement start");
                add(lexems, LexemType.VAR, loc, "var");
                break;

            default:
                checkIsNotNewLine(token, loc, lexems, "Identifier isn't allowed as first token");
                checkIsNotMiddleOfExpr(loc, token, peek(lexems), "Identifier isn't allowed here");

                add(lexems, LexemType.IDENTIFIER, loc, token);
        }
    }


    private void parseString(SourceCode it, List<Lexem> lexems) {
        log.debug("Found letter: \", try parse as a String", it.getChar());
        Location loc = it.getLocation();

        it.step(1);
        boolean escape = false;
        StringBuilder b = new StringBuilder();
        while (!it.isEOF() && (it.getChar() != '"' || it.getChar() == '"' && escape) && it.getChar() != '\n') {
            escape = !escape && it.getChar() == '\\';
            if (!escape)
                b.append(it.getChar());
            it.step(1);
        }
        String token = b.toString();

        if (it.isEOF() || !it.isDoubleQuote())
            throw new ParseException(token, loc, "Unexpected EOF: A string without close double quote");

        checkIsNotNewLine(loc, lexems, "String isn't allowed as first token");
        checkIsNotMiddleOfExpr(loc, token, peek(lexems), "Identifier isn't allowed here");

        it.step(1);
        add(lexems, new Lexem(LexemType.STRING, loc, token));
    }

    private void parseComma(SourceCode it, List<Lexem> lexems, Location loc) {
        parseLexemAndStep(it, lexems, LexemType.COMMA, loc);
    }

    private void parseEqual(SourceCode it, List<Lexem> lexems, Location loc) {
        parseLexemAndStep(it, lexems, LexemType.EQUAL, loc);
    }


    private Lexem pop(List<Lexem> lexems) {
        return lexems.remove(lexems.size() - 1);
    }

    private Lexem peek(List<Lexem> lexems) {
        return lexems.get(lexems.size() - 1);
    }

    private void add(List<Lexem> lexems, Lexem lexem) {
        log.info("add {}", lexem);
        lexems.add(lexem);
    }

    private void add(List<Lexem> lexems, LexemType type, Location location) {
        add(lexems, new Lexem(type, location));
    }

    private void add(List<Lexem> lexems, LexemType type, Location location, String content) {
        add(lexems, new Lexem(type, location, content));
    }



    private void checkIsNotMiddleOfExpr(Location loc, String token, Lexem peek, String message) {
        if (isMiddleOfExpr(peek))
            throw new ParseException(token, loc, message);
    }

    private boolean isMiddleOfExpr(Lexem lexem) {
        return lexem.isAnyOf(
                LexemType.INTEGER, LexemType.DOUBLE, LexemType.STRING,
                LexemType.CURLY_CLOSE, LexemType.PARENT_CLOSE, LexemType.MAP, LexemType.REDUCE);
    }


    private void checkIsNewLine(String token, Location location, List<Lexem> lexems, String message) {
        if (!isNewLine(lexems))
            throw new ParseException(token, location, message);
    }


    private void checkIsNotNewLine(String token, Location location, List<Lexem> lexems, String message) {
        if (isNewLine(lexems))
            throw new ParseException(token, location, message);
    }

    private void checkIsNotNewLine(Location location, List<Lexem> lexems, String message) {
        checkIsNotNewLine("", location, lexems, message);
    }

    private void checkIsNotNewLine(SourceCode it, List<Lexem> lexems, String message) {
        if (isNewLine(lexems))
            throw new ParseException(it.getLocation(), message);
    }


    private boolean isNewLine(List<Lexem> lexems) {
        return lexems.isEmpty() || peek(lexems).is(LexemType.NL);
    }
}
