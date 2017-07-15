package ashes.of.jade.lang.lexer;

import ashes.of.jade.lang.Location;
import ashes.of.jade.lang.parser.ParseException;
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
    public List<Lexem> parse(SourceCode code) {
        log.debug("input:\n{}", code.getSource());

        List<Lexem> lexems = code.getLexems();

        while (!code.isEOF()) {
            Location loc = code.getLocation();
            if (code.isNewLine()) {
                code.add(LexemType.NL, loc);
                code.step();
                code.newLine();
                continue;
            }

            if (code.isWhitespace()) {
                code.step();
                continue;
            }

            log.trace("state: {} \u2192{}", code.getLineToIndex(), code.getLineToEnd());

            if (code.isLetter()) {
                parseLetters(code);
                continue;
            }

            if (code.isDoubleQuote()) {
                parseString(code);
                continue;
            }

            if (code.isDigit()) {
                parseNumber(code, lexems);
                continue;
            }

            if (code.isArrow()) {
                parseArrow(code);
                continue;
            }

            if (code.isOperator()) {
                parseOperator(code);
                continue;
            }

            if (code.isParentOpen()) {
                parseParentOpen(code);
                continue;
            }

            if (code.isParentClose()) {
                parseParentClose(code);
                continue;
            }

            if (code.isCurlyOpen()) {
                parseCurlyOpen(code);
                continue;
            }

            if (code.isCurlyClose()) {
                parseCurlyClose(code);
                continue;
            }

            if (code.isComma()) {
                parseComma(code);
                continue;
            }


            if (code.isEqual()) {
                parseEqual(code);
                continue;
            }

            throw new ParseException(loc, "Unexpected symbol '%s'", code.getChar());
        }

        code.add(LexemType.EOF, code.getLocation());

        return lexems;
    }



    private void parseLexemAndStep(SourceCode code, LexemType type, Location loc) {
        checkIsNotNewLine(loc, code.getLexems(), "Symbol isn't allowed as first token");
        code.add(type, loc);
        code.step();
    }


    private void parseCurlyClose(SourceCode code) {
        parseLexemAndStep(code, LexemType.CURLY_CLOSE, code.getLocation());
    }

    private void parseCurlyOpen(SourceCode code) {
        Location loc = code.getLocation();
        checkIsNotMiddleOfExpr(loc, "", code.peek(), "Sequence isn't allowed here");

        checkIsNotNewLine(loc, code.getLexems(), "Sequence isn't allowed as first token");
        code.add(LexemType.CURLY_OPEN, loc);
        code.step();
    }


    private void parseParentOpen(SourceCode code) {
        parseLexemAndStep(code, LexemType.PARENT_OPEN, code.getLocation());
    }

    private void parseParentClose(SourceCode code) {
        parseLexemAndStep(code, LexemType.PARENT_CLOSE, code.getLocation());
    }


    private void parseArrow(SourceCode it) {
        log.debug("Found symbol: {}, try parse as a Arrow", it.getChar());
        Lexem minus = it.pop();
        if (!minus.is(LexemType.MINUS))
            throw new ParseException(minus.getLocation(), "Expected -> but first char is ");

        parseLexemAndStep(it, LexemType.ARROW, minus.getLocation());
    }


    private void parseOperator(SourceCode code) {
        checkIsNotNewLine(code, code.getLexems(), "Operator isn't allowed as first token");

        Location loc = code.getLocation();
        if (code.isPlus()) {
            code.add(LexemType.PLUS, loc);
            code.step();
            return;
        }

        if (code.isMinus()) {
            code.add(LexemType.MINUS, loc);
            code.step();
            return;
        }

        Lexem last = code.peek();
        if (isOperator(last))
            throw new ParseException(loc, "Unexpected operator %s", code.getLocation());

        if (code.isStar()) {
            code.add(LexemType.MULTIPLY, loc);
            code.step();
            return;
        }

        if (code.isBackSlash()) {
            code.add(LexemType.DIVIDE, loc);
            code.step();
            return;
        }

        if (code.isPower()) {
            code.add(LexemType.POWER, loc);
            code.step();
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


    private void parseNumber(SourceCode code, List<Lexem> lexems) {
        log.debug("Digit found: {}, try parse as a Integer/Double", code.getChar());
        checkIsNotNewLine(code, lexems, "Number isn't allowed as first token");

        Location loc = code.getLocation();
        StringBuilder b = new StringBuilder();
        Lexem back1 = code.peek();

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

                lexems.remove(code.peek());
            }
        }


        while (!code.isEOF() && (code.isDigit() || code.isDot())) {
            b.append(code.getChar());
            code.step(1);
        }

        String token = b.toString();
        checkIsNotMiddleOfExpr(loc, token, back1, "Number isn't allowed here");

        if (doublePattern.matcher(token).matches()) {
            code.add(LexemType.DOUBLE, loc, token);
            return;
        }

        if (integerPattern.matcher(token).matches()) {
            code.add(LexemType.INTEGER, loc, token);
            return;
        }

        throw new ParseException(loc, "Invalid number: %s", token);
    }


    private void parseLetters(SourceCode code) {
        log.debug("Found letter: {}, try parse as a Identifier, Call", code.getChar());
        Location loc = code.getLocation();
        StringBuilder b = new StringBuilder();
        while (!code.isEOF() && code.isLetter()) {
            b.append(code.getChar());
            code.step();
        }

        List<Lexem> lexems = code.getLexems();
        String token = b.toString();
        log.debug("Found letters: '{}' at ", token, loc);
        switch (token) {
            case "var":
                checkIsNewLine(token, loc, lexems, "Var allowed only as statement start");
                code.add(LexemType.VAR, loc, "var");
                break;

            case "print":
                checkIsNewLine(token, loc, lexems, "Print allowed only as statement start");
                code.add(LexemType.PRINT, loc, "print");
                break;

            case "out":
                checkIsNewLine(token, loc, lexems, "Out allowed only as statement start");
                code.add(LexemType.OUT, loc, "out");
                break;


            case "map":
                checkIsNotNewLine(token, loc, lexems, "Map isn't allowed as first token");
                code.add(LexemType.MAP, loc, "map");
                break;

            case "reduce":
                checkIsNotNewLine(token, loc, lexems, "Reduce isn't allowed as first token");
                code.add(LexemType.REDUCE, loc, "reduce");
                break;


            default:
                checkIsNotNewLine(token, loc, lexems, "Identifier isn't allowed as first token");
                checkIsNotMiddleOfExpr(loc, token, code.peek(), "Identifier isn't allowed here");

                code.add(LexemType.IDENTIFIER, loc, token);
        }
    }


    private void parseString(SourceCode code) {
        log.debug("Found letter: \", try parse as a String", code.getChar());
        Location loc = code.getLocation();

        code.step(1);
        boolean escape = false;
        StringBuilder b = new StringBuilder();
        while (!code.isEOF() && (code.getChar() != '"' || code.getChar() == '"' && escape) && code.getChar() != '\n') {
            escape = !escape && code.getChar() == '\\';
            if (!escape)
                b.append(code.getChar());

            code.step(1);
        }

        String token = b.toString();

        if (code.isEOF() || !code.isDoubleQuote())
            throw new ParseException(token, loc, "Unexpected EOF: A string without close double quote");

        checkIsNotNewLine(loc, code.getLexems(), "String isn't allowed as first token");
        checkIsNotMiddleOfExpr(loc, token, code.peek(), "Identifier isn't allowed here");

        code.add(LexemType.STRING, loc, token);
        code.step(1);
    }


    private void parseComma(SourceCode code) {
        parseLexemAndStep(code, LexemType.COMMA, code.getLocation());
    }


    private void parseEqual(SourceCode code) {
        parseLexemAndStep(code, LexemType.EQUAL, code.getLocation());
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
        return lexems.isEmpty() || lexems.get(lexems.size() - 1).is(LexemType.NL);
    }
}
