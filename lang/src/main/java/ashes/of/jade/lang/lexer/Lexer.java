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
            if (it.isNewLine()) {
                add(lexems, LexemType.NL, it.getLocation());
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


            if (it.getChar() == '>') {
                log.debug("Found symbol: {}, try parse as a Arrow", it.getChar());

                Lexem minus = removeLast(lexems);
                if (minus.is(LexemType.MINUS)) {
                    add(lexems, LexemType.ARROW, minus.getLocation());
                    it.step();
                    continue;
                }

                throw new ParseException(minus.getLocation(), "Expected -> but first char is ");
            }


            if (it.isOperator()) {
                parseOperator(it, lexems);
                continue;
            }

            if (it.isParentOpen()) {
                checkNewLine(it, lexems, "Symbol ( isn't allowed as first token");
                add(lexems, LexemType.PARENT_OPEN, it.getLocation());
                it.step();
                continue;
            }

            if (it.isParentClose()) {
                checkNewLine(it, lexems, "Symbol ) isn't allowed as first token");
                add(lexems, LexemType.PARENT_CLOSE, it.getLocation());
                it.step();
                continue;
            }

            if (it.isCurlyOpen()) {
                checkNewLine(it, lexems, "Symbol { isn't allowed as first token");
                add(lexems, LexemType.CURLY_OPEN, it.getLocation());
                it.step();
                continue;
            }

            if (it.isCurlyClose()) {
                checkNewLine(it, lexems, "Symbol } isn't allowed as first token");
                add(lexems, LexemType.CURLY_CLOSE, it.getLocation());
                it.step();
                continue;
            }

            if (it.isComma()) {
                checkNewLine(it, lexems, "Comma isn't allowed as first token");

                Location loc = it.getLocation();
                Lexem lexem = new Lexem(LexemType.COMMA, loc);
                add(lexems, lexem);
                it.step();
                continue;
            }


            if (it.isAssign()) {
                log.debug("assign, try reduce [var, load{id}, Assign] to one Store{id}");
                checkNewLine(it, lexems, "Assign isn't allowed as first token");

                Lexem id = removeLast(lexems);
                if (!id.is(LexemType.LOAD))
                    throw new ParseException(it.getLocation(), "Load expected");

                Lexem var = removeLast(lexems);
                if (!var.is(LexemType.VAR))
                    throw new ParseException(it.getLocation(), "Var expected");

                Lexem lexem = new Lexem(LexemType.STORE, var.getLocation(), id.getContent());
                lexems.add(lexem);
                log.info("add {},   reduce [{}, {}]", lexem, var, id);
                it.step();
                continue;
            }

            throw new ParseException(it.getLocation(), "Unexpected symbol '%s'", it.getChar());
        }

        add(lexems, LexemType.EOF, it.getLocation());

        return lexems;
    }


    private void parseOperator(SourceCode it, List<Lexem> lexems) {
        checkNewLine(it, lexems, "Operator isn't allowed as first token");

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

        Lexem last = getLast(lexems);
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
        return lexem.is(LexemType.PLUS) ||
                lexem.is(LexemType.MINUS) ||
                lexem.is(LexemType.MULTIPLY) ||
                lexem.is(LexemType.DIVIDE) ||
                lexem.is(LexemType.POWER);
    }


    private void parseNumber(SourceCode it, List<Lexem> lexems) {
        log.debug("Digit found: {}, try parse as a Integer/Double", it.getChar());
        checkNewLine(it, lexems, "Number isn't allowed as first token");

        Location loc = it.getLocation();
        StringBuilder b = new StringBuilder();
        Lexem back1 = getLast(lexems);
        if (back1.is(LexemType.MINUS) || back1.is(LexemType.PLUS)) {
            log.trace("previous lexem is +/-, check that it isn't expr");
            Lexem back2 = lexems.get(lexems.size() - 2);
            if (back2.is(LexemType.STORE) ||
                back2.is(LexemType.ARROW) ||
                back2.is(LexemType.PARENT_OPEN) ||
                back2.is(LexemType.CURLY_OPEN) ||
                back2.is(LexemType.COMMA) ||
                back2.is(LexemType.OUT) ||
                back2.is(LexemType.PLUS) ||
                back2.is(LexemType.MINUS) ||
                back2.is(LexemType.MULTIPLY) ||
                back2.is(LexemType.DIVIDE) ||
                back2.is(LexemType.POWER)) {
                loc = back1.getLocation();

                if (back1.is(LexemType.PLUS))
                    b.append("+");

                if (back1.is(LexemType.MINUS))
                    b.append("-");

                lexems.remove(getLast(lexems));
            }

        }


        while (!it.isEOF() && (it.isDigit() || it.isDot())) {
            b.append(it.getChar());
            it.step(1);
        }

        String token = b.toString();
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
            case "map":     add(lexems, LexemType.MAP, loc); break;
            case "reduce":  add(lexems, LexemType.REDUCE, loc); break;
            case "var":     add(lexems, LexemType.VAR, loc); break;
            case "print":   add(lexems, LexemType.PRINT, loc); break;
            case "out":     add(lexems, LexemType.OUT, loc); break;

            default:
                checkNewLine(loc, lexems, "Identifier isn't allowed as first token");

                add(lexems, LexemType.LOAD, loc, token);
        }
    }


    private void parseString(SourceCode it, List<Lexem> lexems) {
        log.debug("Found letter: \", try parse as a String", it.getChar());
        checkNewLine(it, lexems, "String isn't allowed as first token");

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

        if (it.isEOF() || !it.isDoubleQuote())
            throw new ParseException(loc, "Unexpected EOF: A string without close double quote");

        it.step(1);
        add(lexems, new Lexem(LexemType.STRING, loc, b.toString()));
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


    private void checkNewLine(Location location, List<Lexem> lexems, String message) {
        if (isNewLine(lexems))
            throw new ParseException(location, message);
    }

    private void checkNewLine(SourceCode it, List<Lexem> lexems, String message) {
        if (isNewLine(lexems))
            throw new ParseException(it.getLocation(), message);
    }

    private boolean isNewLine(List<Lexem> lexems) {
        return lexems.isEmpty() || getLast(lexems).is(LexemType.NL);
    }

    private Lexem removeLast(List<Lexem> lexems) {
        return lexems.remove(lexems.size() - 1);
    }

    private Lexem getLast(List<Lexem> lexems) {
        return lexems.get(lexems.size() - 1);
    }
}
